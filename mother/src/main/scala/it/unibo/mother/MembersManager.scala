package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.Listing
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.MotherEvent.ClientLeft
import it.unibo.protocol.MotherEvent.ClientUp
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY

object MembersManager:

  sealed trait Command
  case class WrappedListing(lst: Listing) extends Command

  val WorkerServiceKey = ServiceKey[ClientEvent]("worker-service")

  def apply(motherRef: ActorRef[MotherEvent]): Behavior[Command] = Behaviors.setup: ctx =>
    ctx.log.info(s"🪀 Members Manager is Up")

    val listingAdapter = ctx.messageAdapter[Receptionist.Listing](WrappedListing.apply)
    ctx.system.receptionist ! Receptionist.Subscribe(CLIENT_SERVICE_KEY, listingAdapter)

    behavior(motherRef, Set.empty)

  def behavior(motherRef: ActorRef[MotherEvent], currentClients: Set[ActorRef[ClientEvent]]): Behavior[Command] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case WrappedListing(listing) =>
          ctx.log.info(s"🪀 Cluster notify listing")
          val newClients = listing.serviceInstances(CLIENT_SERVICE_KEY)
          val connected = newClients -- currentClients
          val disconnected = currentClients -- newClients
          connected.foreach(motherRef ! ClientUp(_))
          disconnected.foreach(motherRef ! ClientLeft(_))
          behavior(motherRef, newClients)
        case _ =>
          ctx.log.info("Do nothing")
          behavior(motherRef, currentClients)
