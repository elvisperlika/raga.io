package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.ServiceKeys.MOTHER_SERVICE_KEY
import it.unibo.protocol.ClientUp
import it.unibo.protocol.GamaManagerAddress
import it.unibo.protocol.ChildServerUp
import it.unibo.protocol.ClientLeft
import it.unibo.protocol.ChildServerLeft

private case class MotherState(
    children: List[ActorRef[ChildEvent]] = List.empty,
    clients: List[ActorRef[ClientEvent]] = List.empty
)

object MotherActor:

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    println("😍 Main Server up")

    ctx.system.receptionist ! Receptionist.Register(MOTHER_SERVICE_KEY, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")
    behavior(state = MotherState())

  def behavior(state: MotherState): Behavior[MotherEvent] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case ClientUp(client) =>
          println(s"😍 Client Up: ${client.path}")
          // TODO: find the child with lowest work balance and send to client
          val freeChild = state.children.last
          if freeChild == null then println("🛑 No child servers available")
          else
            println(s"😍 Assigning child server ${freeChild.path} to client ${client.path}")
            client ! GamaManagerAddress(freeChild)
          behavior(state.copy(clients = client :: state.clients))

        case ChildServerUp(child) =>
          println(s"😍 Child Up: ${child.path}")
          behavior(state.copy(children = child :: state.children))

        case ClientLeft(client) =>
          println(s"😍 Client Left: ${client.path}")
          behavior(state.copy(clients = state.clients.filterNot(_ == client)))

        case ChildServerLeft(child) =>
          println(s"😍 Child Left: ${child.path}")
          behavior(state.copy(children = state.children.filterNot(_ == child)))
