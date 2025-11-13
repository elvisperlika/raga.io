package it.unibo.raga.controller

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.typed._
import akka.cluster.ClusterEvent._
import akka.util.Timeout
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.EatenPlayer
import it.unibo.protocol.EndGame
import it.unibo.protocol.GameManagerAddress
import it.unibo.protocol.JoinNetwork
import it.unibo.protocol.Player
import it.unibo.protocol.ReceivedRemoteWorld
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.RequestRemoteWorldUpdate
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.protocol.ServiceNotAvailable
import it.unibo.protocol.World
import it.unibo.raga.controller.WorldConverter.*
import it.unibo.raga.model.ImmutableGameStateManager
import it.unibo.raga.model.LocalPlayer
import it.unibo.raga.view.EndGameView
import it.unibo.raga.view.LocalView
import it.unibo.raga.view.View

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import it.unibo.protocol.*

object ClientActor:

  enum LocalClientEvent:

    case JoinRandomRoom
    case CreateAndJoinRoom
    case JoinFriendsRoom(code: String)
    case Tick
    case ReceivedWorld(world: World, player: Player, managerRef: ActorRef[ChildEvent])
    case JoinFriendsRoomFailed(code: String)

  def apply(): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    var view = new View(ctx.self)
    view.visible = true
    view.showAlert("Offline")

    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CLIENT_SERVICE_KEY, ctx.self)
    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(JoinNetwork.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    viewBehavior(view)

  /** Defines the behavior of the client actor in response to various events while the user is interacting with the
    * homepage UI.
    *
    * @param view
    *   View instance that user interacts with
    * @param manager
    *   Optional reference to the game manager actor
    */
  def viewBehavior(view: View, manager: Option[ActorRef[ChildEvent]] = None): Behavior[ClientEvent | LocalClientEvent] =
    Behaviors.setup: ctx =>
      Behaviors.receive: (_, msg) =>
        msg match
          case JoinNetwork(MemberUp(member)) =>
            view.showAlert("Connecting...")
            Behaviors.same

          case LocalClientEvent.JoinRandomRoom =>
            ctx.log.info(s"🏀 Join Button pressed...")
            val nickName = view.getNickname()
            manager match
              case Some(ref) =>
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
            viewBehavior(view, Some(managerRef))

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
            run(model, gameView, localPlayer, managerRef)

          case LocalClientEvent.JoinFriendsRoom(code) =>
            ctx.log.info(s"🏀 Join friend’s room pressed with code: $code")
            val nickName = view.getNickname()
            manager match
              case Some(ref) =>
                requestWorldWithRoomCode(nickName, code, ctx, ref)
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
              case Some(ref) =>
                ctx.log.info(s"🏀 Asking to create a room for $nickName")
                ref ! CreateFriendsRoom(nickName, ctx.self)
              case None =>
                view.showAlert("Service Not Available, please wait...")
            Behaviors.same

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
            run(model, gameView, localPlayer, managerRef)

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

  private def requestWorldWithRoomCode(
      nickName: String,
      roomCode: String,
      ctx: ActorContext[ClientEvent | LocalClientEvent],
      manager: ActorRef[ChildEvent]
  ): Behavior[ClientEvent | LocalClientEvent] =
    given Timeout = 3.seconds
    given Scheduler = ctx.system.scheduler
    given ExecutionContext = ctx.executionContext

    ctx.log.info(s"🏀 Requesting World in room $roomCode to ${manager.path}...")
    manager
      .ask[(ActorRef[ChildEvent], RemoteWorld) | CodeNotFound](replyTo =>
        RequestWorldInRoom(nickName, roomCode, replyTo, ctx.self)
      )
      .onComplete {
        case Success((newManager, remoteWorld)) =>
          ctx.self ! LocalClientEvent.ReceivedWorld(remoteWorld.world, remoteWorld.player, newManager)
        case Success(CodeNotFound()) =>
          ctx.log.warn(s"🏀 Room with code $roomCode not found")
          ctx.self ! LocalClientEvent.JoinFriendsRoomFailed(roomCode)
        case Failure(ex) =>
          ctx.log.error(s"🏀 Failed to request world from manager: ${ex.getMessage}", ex)
          ctx.self ! LocalClientEvent.JoinFriendsRoomFailed(roomCode)
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
      player: LocalPlayer,
      managerRef: ActorRef[ChildEvent],
      isSynced: Boolean = true
  ): Behavior[ClientEvent | LocalClientEvent] = Behaviors.withTimers: timer =>
    timer.startTimerAtFixedRate(LocalClientEvent.Tick, 50.millis)
    Behaviors.receive: (ctx, msg) =>
      msg match
        case LocalClientEvent.Tick if isSynced =>
          val (dx, dy) = gameView.direction
          val newModel = model.movePlayerDirection(player.id, dx, dy).tick()
          val eatenPlayers = model.world.players.filterNot(p => newModel.world.players.exists(_.id == p.id))
          if eatenPlayers.nonEmpty then
            ctx.log.info(s"🏀 Players eaten: ${eatenPlayers.map(_.id).mkString(", ")}")
            eatenPlayers.foreach(player => managerRef ! EatenPlayer(player.id))
          val remoteWorld = createRemoteWorld(newModel.world)
          managerRef ! RequestRemoteWorldUpdate(remoteWorld, (player.id, ctx.self))
          run(newModel, gameView, player, managerRef, isSynced = false)

        case ReceivedRemoteWorld(remoteWorld) =>
          val world = createLocalWorld(remoteWorld)
          val newModel = new ImmutableGameStateManager(world)
          gameView.updateWorld(newModel.world)
          gameView.repaint()
          run(newModel, gameView, player, managerRef, isSynced = true)

        case EndGame() =>
          gameView.visible = false
          val endGameView = EndGameView()
          endGameView.setLocationRelativeTo(gameView)
          endGame(endGameView)

        case _ =>
          Behaviors.same

  def endGame(endGameView: EndGameView): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    endGameView.visible = true
    Behaviors.same
