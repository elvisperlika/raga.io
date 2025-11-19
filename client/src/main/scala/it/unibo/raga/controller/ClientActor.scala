package it.unibo.raga.controller

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.*
import akka.cluster.typed.*
import akka.util.Timeout
import it.unibo.protocol.*
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.raga.controller.WorldConverter.*
import it.unibo.raga.model.AIMovement
import it.unibo.raga.model.ImmutableGameStateManager
import it.unibo.raga.model.LocalPlayer
import it.unibo.raga.view.EndGameView
import it.unibo.raga.view.LocalView
import it.unibo.raga.view.View

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

object ClientActor:

  enum LocalClientEvent:

    case JoinRandomRoom
    case CreateAndJoinRoom
    case JoinFriendsRoom(code: String)
    case Tick
    case ReceivedWorld(world: World, player: Player, managerRef: ActorRef[ChildEvent])
    case JoinFriendsRoomFailed(code: String)

  def apply(isBot: Boolean): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    var view = new View(ctx.self)
    view.visible = true
    view.showAlert("Offline")

    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CLIENT_SERVICE_KEY, ctx.self)
    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(JoinNetwork.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    viewBehavior(view, isBot = isBot)

  /** Defines the behavior of the client actor in response to various events while the user is
    * interacting with the homepage UI.
    *
    * @param view
    *   View instance that user interacts with
    * @param manager
    *   Optional reference to the game manager actor
    */
  def viewBehavior(
      view: View,
      manager: Option[ActorRef[ChildEvent]] = None,
      isBot: Boolean
  ): Behavior[ClientEvent | LocalClientEvent] =
    Behaviors.setup: ctx =>
      if isBot then
        ctx.log.info("🏀 Bot client starting...")
        val botNickName = s"Bot_${scala.util.Random.alphanumeric.take(5).mkString}"
        view.setNickname(botNickName)
        ctx.self ! LocalClientEvent.JoinRandomRoom

      Behaviors.receive: (_, msg) =>
        msg match
          case JoinNetwork(MemberUp(member)) =>
            view.showAlert("Connecting...")
            Behaviors.same

          case LocalClientEvent.JoinRandomRoom =>
            ctx.log.info(s"🏀 Join Button pressed...")
            val nickName = view.getNickname()
            manager match
              case Some(_) if nickName.isEmpty =>
                view.showAlert("Please enter a nickname")
                Behaviors.same
              case Some(ref) if nickName.nonEmpty =>
                requestWorld(nickName, ctx, ref)
              case _ =>
                view.showAlert("Service Not Available, please wait...")
                Behaviors.same

          case GameManagerAddress(managerRef) =>
            ctx.log.info(s"🏀 Gama Manager found: ${managerRef.path}")
            view.showAlert("Connected")

            // val nickName = view.getNickname()
            // ctx.log.info(s"🙋 Joining room as player: $nickName")
            // managerRef ! PlayerJoinedRoom(nickName, ctx.self)
            viewBehavior(view, Some(managerRef), isBot)

          case ServiceNotAvailable() =>
            view.showAlert("Service Not Available, please wait...")
            Behaviors.same

          case LocalClientEvent.ReceivedWorld(remoteWorld, player, managerRef) =>
            ctx.log.info(s"🏀 First world received")
            val localWorld = createLocalWorld(remoteWorld)
            val localPlayer = LocalPlayer(player.id, player.x, player.y, player.mass)
            val model = new ImmutableGameStateManager(localWorld)
            val gameView = new LocalView(model.world, player.id, ctx.self)
            gameView.setLocationRelativeTo(view)
            gameView.visible = true
            view.close()
            ctx.self ! LocalClientEvent.Tick
            run(model, gameView, (-1, -1), localPlayer, managerRef, isBot)

          case LocalClientEvent.JoinFriendsRoom(code) =>
            ctx.log.info(s"🏀 Join friend’s room pressed with code: $code")
            val nickName = view.getNickname()
            manager match
              case Some(ref) =>
                ref ! RequestWorldInRoom(nickName, code, ctx.self)
                Behaviors.same
              case _ =>
                view.showAlert("Service Not Available, please wait...")
                Behaviors.same

          case LocalClientEvent.JoinFriendsRoomFailed(code) =>
            view.showAlert(s"Room with code $code not found")
            Behaviors.same

          case LocalClientEvent.CreateAndJoinRoom =>
            ctx.log.info("🏀 Create & Join Room pressed...")
            val nickName = view.getNickname()
            manager match
              case Some(_) if nickName.isEmpty =>
                view.showAlert("Please enter a nickname")
                Behaviors.same
              case Some(ref) if nickName.nonEmpty =>
                ctx.log.info(s"🏀 Asking to create a room for $nickName")
                ref ! RequestPrivateRoom(nickName, ctx.self)
              case _ =>
                view.showAlert("Service Not Available, please wait...")
            Behaviors.same

          case PrivateManagerAddress(privateManagerRef) =>
            ctx.log.info(s"🏀 Private Gama Manager found: ${privateManagerRef.path}")
            val nickName = view.getNickname()
            privateManagerRef ! PlayerJoinedRoom(nickName, ctx.self)
            viewBehavior(view, Some(privateManagerRef), isBot)

          case InitWorld(world, player, managerRef) =>
            ctx.log.info(s"🌍 Received world ${world.id} with ${world.players.size} players")
            val localWorld = createLocalWorld(world)
            val localPlayer = LocalPlayer(player.id, player.x, player.y, player.mass)
            val model = new ImmutableGameStateManager(localWorld)
            val gameView = new LocalView(model.world, player.id, ctx.self)
            gameView.setLocationRelativeTo(view)
            gameView.visible = true
            view.close()

            ctx.self ! LocalClientEvent.Tick
            run(model, gameView, (-1, -1), localPlayer, managerRef, isBot)

          case _ => Behaviors.same

  private def requestWorld(
      nickName: String,
      ctx: ActorContext[ClientEvent | LocalClientEvent],
      manager: ActorRef[ChildEvent]
  ): Behavior[ClientEvent | LocalClientEvent] =
    given Timeout = 3.seconds
    given Scheduler = ctx.system.scheduler
    given ExecutionContext = ctx.executionContext

    ctx.log.info(s"🏀 Requesting World to ${manager.path}...")
    manager.ask[RemoteWorld](replyTo => RequestWorld(nickName, replyTo, ctx.self)).onComplete {
      case Success(remoteWorld) =>
        ctx.self ! LocalClientEvent.ReceivedWorld(remoteWorld.world, remoteWorld.player, manager)
      case Failure(ex) =>
        ctx.log.error(s"🏀 Failed to request world from manager: ${ex.getMessage}", ex)
    }
    Behaviors.same

  /** Run the game with the given world and player.
    *
    * @param world
    *   World to play in
    * @param player
    *   Player controlled by this client
    */
  def run(
      model: ImmutableGameStateManager,
      gameView: LocalView,
      previousDirection: (Double, Double) = (-1, -1),
      player: LocalPlayer,
      managerRef: ActorRef[ChildEvent],
      isBot: Boolean
  ): Behavior[ClientEvent | LocalClientEvent] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case ReceivedRemoteWorld(remoteWorld) =>
          val world = createLocalWorld(remoteWorld)
          val newModel = new ImmutableGameStateManager(world)
          val updatedPlayer = newModel.world.playerById(player.id).get
          val (dx, dy) =
            if isBot then AIMovement.getAIDirection(updatedPlayer, world)
            else gameView.direction

          managerRef ! PlayerMove(updatedPlayer.id, dx, dy)
          gameView.updateWorld(newModel.world)
          gameView.repaint()
          run(newModel, gameView, (dx, dy), updatedPlayer, managerRef, isBot)

        case EndGame() =>
          gameView.visible = false
          val endGameView = EndGameView()
          endGameView.setLocationRelativeTo(gameView)
          endGame(endGameView)

        case NewManager(newManagerRef, newWorld, player) =>
          ctx.log.info(s"🏀 New Manager Address received: ${newManagerRef.path}")
          val localWorld = createLocalWorld(newWorld)
          val newModel = new ImmutableGameStateManager(localWorld)
          val localPlayer = LocalPlayer(player.id, player.x, player.y, player.mass)
          run(newModel, gameView, previousDirection, localPlayer, newManagerRef, isBot)

        case _ =>
          Behaviors.same

  def endGame(endGameView: EndGameView): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup:
    ctx =>
      endGameView.visible = true
      Behaviors.same
