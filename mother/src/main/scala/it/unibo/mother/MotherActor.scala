package it.unibo.mother

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.ActorRef
import it.unibo.protocol.ChildEvent
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.MembersManager
import akka.actor.typed.receptionist.Receptionist
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent

object MotherActor:

  val serviceKey = ServiceKey[MotherEvent]("mother-server-service")

  var children: List[ActorRef[ChildEvent]] = List.empty
  var clients: List[ActorRef[ClientEvent]] = List.empty

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    println("😍 Main Server up")

    ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")

    import MotherEvent.*
    Behaviors.receiveMessage {
      case ClientUp(client) =>
        Behaviors.same
      case ChildServerUp(child) =>
        Behaviors.same
      case ClientLeft(client) =>
        Behaviors.same
      case ChildServerLeft(child) =>
        Behaviors.same
    }
