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
import it.unibo.protocol.Player
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.protocol.World
import it.unibo.raga.view.View

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.swing.*
import scala.swing.Swing.onEDT
import scala.util.Failure
import scala.util.Success
import it.unibo.protocol.JoinNetwork
import it.unibo.protocol.UpdateView
import it.unibo.protocol.GamaManagerAddress

object ClientActor:

  enum LocalClientEvent:

    case JoinRandomRoom
    case CreateAndJoinRoom
    case JoinFriendsRoom
    case UpdateView
    case ReceivedWorld(world: World, player: Player)

  var manager: Option[ActorRef[ChildEvent]] = None

  def apply(): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    var view = new View(ctx.self)
    view.visible = true

    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CLIENT_SERVICE_KEY, ctx.self)

    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(JoinNetwork.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    Behaviors.receive: (_, msg) =>
      msg match
        case LocalClientEvent.UpdateView =>
          onEDT(view.repaint())
          Behaviors.same

        case JoinNetwork(MemberUp(member)) =>
          view.showOnline()
          Behaviors.same

        case LocalClientEvent.JoinRandomRoom =>
          ctx.log.info(s"🏀 Join Button pressed...")
          manager match
            case Some(ref) => requestWorld(ctx, ref)
            case _ =>
              ctx.log.info(s"🏀 Service not available now, please wait.")
              Behaviors.same

        case GamaManagerAddress(managerRef) =>
          ctx.log.info(s"🏀 Gama Manager found: ${managerRef.path}")
          manager = Some(managerRef)
          Behaviors.same

        case LocalClientEvent.ReceivedWorld(world, player) =>
          ctx.log.info(s"🏀 World received: $world")
          run(world, player)

        case _ =>
          ctx.log.info(s"🏀 Message not recognized: $msg")
          Behaviors.same

  def requestWorld(
      ctx: ActorContext[ClientEvent | LocalClientEvent],
      manager: ActorRef[ChildEvent]
  ): Behavior[ClientEvent | LocalClientEvent] =
    given Timeout = 3.seconds
    given Scheduler = ctx.system.scheduler
    given ExecutionContext = ctx.executionContext

    ctx.log.info(s"🏀 Requesting World from ${manager.path}...")
    val response: Future[RemoteWorld] =
      manager.ask[RemoteWorld](replyTo => RequestWorld(replyTo))
    response.onComplete {
      case Success(remoteWorld) => ctx.self ! LocalClientEvent.ReceivedWorld(remoteWorld.world, remoteWorld.player)
      case Failure(ex) => ctx.log.error(s"🏀 Failed to request world from manager: ${ex.getMessage}", ex)
    }
    Behaviors.same

  /** Run the game.
    *
    * @return
    */
  def run(world: World, player: Player): Behavior[ClientEvent | LocalClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Run the game")
    Behaviors.receive: (_, msg) =>
      msg match
        case LocalClientEvent.UpdateView =>
          ctx.log.info("🏀 Repainting view")
          Behaviors.same
        case _ =>
          ctx.log.info(s"🏀 Message not recognized 2: $msg")
          Behaviors.same
