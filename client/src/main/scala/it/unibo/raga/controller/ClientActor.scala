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
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import akka.util.Timeout
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.GamaManagerAddress
import it.unibo.protocol.JoinNetwork
import it.unibo.protocol.Player
import it.unibo.protocol.ReceivedRemoteWorld
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.RequestRemoteWorldUpdate
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.protocol.World
import it.unibo.raga.controller.WorldConverter.*
import it.unibo.raga.model.ImmutableGameStateManager
import it.unibo.raga.model.LocalPlayer
import it.unibo.raga.view.LocalView
import it.unibo.raga.view.View

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.swing.*
import scala.swing.Swing.onEDT
import scala.util.Failure
import scala.util.Success

object ClientActor:

  enum LocalClientEvent:

    case JoinRandomRoom
    case CreateAndJoinRoom
    case JoinFriendsRoom
    case Tick
    case ReceivedWorld(world: World, player: Player, managerRef: ActorRef[ChildEvent])

  var manager: Option[ActorRef[ChildEvent]] = None

  def apply(name: String): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    var view = new View(ctx.self, name)
    view.visible = true

    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CLIENT_SERVICE_KEY, ctx.self)

    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(JoinNetwork.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    Behaviors.receive: (_, msg) =>
      msg match
        case LocalClientEvent.Tick =>
          onEDT(view.repaint())
          Behaviors.same

        case JoinNetwork(MemberUp(member)) =>
          view.showOnline()
          Behaviors.same

        case LocalClientEvent.JoinRandomRoom =>
          ctx.log.info(s"🏀 Join Button pressed...")
          val nickName = view.getNickname()
          manager match
            case Some(ref) => requestWorld(nickName, ctx, ref)
            case _ =>
              ctx.log.info(s"🏀 Service not available now, please wait.")
              Behaviors.same

        case GamaManagerAddress(managerRef) =>
          ctx.log.info(s"🏀 Gama Manager found: ${managerRef.path}")
          manager = Some(managerRef)
          Behaviors.same

        case LocalClientEvent.ReceivedWorld(remoteWorld, player, managerRef) =>
          ctx.log.info(s"🏀 First world received")
          val localWorld = createLocalWorld(remoteWorld)
          val localPlayer = LocalPlayer(player.id, player.x, player.y, player.mass)
          val model = new ImmutableGameStateManager(localWorld)

          val gameView = new LocalView(model.world, player.id, ctx.self)
          gameView.visible = true
          view.close()
          ctx.self ! LocalClientEvent.Tick
          run(model, gameView, localPlayer, managerRef)

        case _ =>
          ctx.log.info(s"🏀 Message not recognized: $msg")
          Behaviors.same

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
      player: LocalPlayer,
      managerRef: ActorRef[ChildEvent],
      isSynced: Boolean = true
  ): Behavior[ClientEvent | LocalClientEvent] = Behaviors.withTimers: timer =>
    timer.startTimerAtFixedRate(LocalClientEvent.Tick, 100.milliseconds)
    Behaviors.receive: (ctx, msg) =>
      msg match
        case LocalClientEvent.Tick if isSynced =>
          ctx.log.info("🏀 TICK")
          val (dx, dy) = gameView.direction
          val newModel = model.movePlayerDirection(player.id, dx, dy).tick()
          val remoteWorld = createRemoteWorld(newModel.world)
          ctx.log.info(s"🏀 Requesting world update for player ${player.id}")
          managerRef ! RequestRemoteWorldUpdate(remoteWorld, (player.id, ctx.self))
          run(newModel, gameView, player, managerRef, isSynced = false)

        case ReceivedRemoteWorld(world) =>
          ctx.log.info(s"🏀 World received")
          val localWorld = createLocalWorld(world)
          val newModel = new ImmutableGameStateManager(localWorld)
          gameView.updateWorld(newModel.world)
          gameView.repaint()
          run(newModel, gameView, player, managerRef, isSynced = true)

        case _ =>
          ctx.log.info(s"🏀 Message not recognized...")
          Behaviors.same // Use Behaviors.same if the model isn't changed and you want to continue
