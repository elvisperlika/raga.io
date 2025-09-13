package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.Listing
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientUp
import it.unibo.protocol.ClientLeft
import it.unibo.protocol.ChildServerUp
import it.unibo.protocol.ChildServerLeft

object MembersManager:

  sealed trait Command
  case class WrappedListing(lst: Listing) extends Command

  val WorkerServiceKey = ServiceKey[ClientEvent]("worker-service")

  def apply(motherRef: ActorRef[MotherEvent]): Behavior[Command] = Behaviors.setup { ctx =>
    ctx.log.info(s"🪀 Members Manager is Up")

    val listingAdapter = ctx.messageAdapter[Receptionist.Listing](WrappedListing.apply)
    ctx.system.receptionist ! Receptionist.Subscribe(CLIENT_SERVICE_KEY, listingAdapter)
    ctx.system.receptionist ! Receptionist.Subscribe(CHILD_SERVICE_KEY, listingAdapter)

    behavior(motherRef, Set.empty, Set.empty)
  }

  def behavior(
      motherRef: ActorRef[MotherEvent],
      currentClients: Set[ActorRef[ClientEvent]],
      currentChildren: Set[ActorRef[ChildEvent]]
  ): Behavior[Command] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case WrappedListing(lst) =>
          lst match
            case CLIENT_SERVICE_KEY.Listing(newClients) =>
              val connectedClients = newClients -- currentClients
              val disconnected = currentClients -- newClients
              connectedClients.foreach(motherRef ! ClientUp(_))
              connectedClients.foreach(client => ctx.log.info(s"🪀 Connected client: ${client.path}"))

              disconnected.foreach(motherRef ! ClientLeft(_))
              behavior(motherRef, newClients, currentChildren)

            case CHILD_SERVICE_KEY.Listing(newChildren) =>
              val connectedChildren = newChildren -- currentChildren
              val disconnectedChildren = currentChildren -- newChildren
              connectedChildren.foreach(motherRef ! ChildServerUp(_))
              connectedChildren.foreach(child => ctx.log.info(s"🪀 Connected child: ${child.path}"))

              disconnectedChildren.foreach(motherRef ! ChildServerLeft(_))
              behavior(motherRef, currentClients, newChildren)
