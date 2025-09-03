package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent

private case class MotherState(
    children: List[ActorRef[ChildEvent]] = List.empty,
    clients: List[ActorRef[ClientEvent]] = List.empty
)

object MotherActor:

  val serviceKey = ServiceKey[MotherEvent]("mother-server-service")

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    println("😍 Main Server up")

    ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")
    behavior(state = MotherState())

  def behavior(state: MotherState): Behavior[MotherEvent] =
    import MotherEvent.*
    Behaviors.receiveMessage {
      case ClientUp(client) =>
        println(s"😍 Client Up: ${client.path}")
        behavior(state.copy(clients = client :: state.clients))
      case ChildServerUp(child) =>
        behavior(state.copy(children = child :: state.children))
      case ClientLeft(client) =>
        println(s"😍 Client Left: ${client.path}")
        behavior(state.copy(clients = state.clients.filterNot(_ == client)))
      case ChildServerLeft(child) =>
        behavior(state.copy(children = state.children.filterNot(_ == child)))
    }
