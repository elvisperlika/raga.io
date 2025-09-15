package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ChildServerLeft
import it.unibo.protocol.ChildServerUp
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ClientLeft
import it.unibo.protocol.ClientUp
import it.unibo.protocol.GamaManagerAddress
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.ServiceKeys.MOTHER_SERVICE_KEY

private case class MotherState(
    children: List[ActorRef[ChildEvent]] = List.empty,
    clients: List[ActorRef[ClientEvent]] = List.empty
)

object MotherActor:

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    ctx.log.info("😍 Main Server up")
    ctx.system.receptionist ! Receptionist.Register(MOTHER_SERVICE_KEY, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")
    behavior(state = MotherState())

  def behavior(state: MotherState): Behavior[MotherEvent] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case ClientUp(client) =>
          ctx.log.info(s"😍 Client Up: ${client.path}")

          // TODO: find the child with lowest work balance and send to client
          val freeChild = state.children.lastOption
          freeChild match
            case None => ctx.log.info("😍 No child servers available")
            case Some(c) =>
              ctx.log.info(s"😍 Assigning child server ${c.path} to client ${client.path}")
              client ! GamaManagerAddress(c)

          behavior(state.copy(clients = client :: state.clients))

        case ChildServerUp(child) =>
          ctx.log.info(s"😍 Child Up: ${child.path}")
          behavior(state.copy(children = child :: state.children))

        case ClientLeft(client) =>
          ctx.log.info(s"😍 Client Left: ${client.path}")
          behavior(state.copy(clients = state.clients.filterNot(_ == client)))

        case ChildServerLeft(child) =>
          ctx.log.info(s"😍 Child Left: ${child.path}")
          behavior(state.copy(children = state.children.filterNot(_ == child)))
