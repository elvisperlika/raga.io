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
import it.unibo.protocol.ID
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.ServiceKeys.MOTHER_SERVICE_KEY
import it.unibo.protocol.SetUp

private case class ChildState(
    ref: ActorRef[ChildEvent],
    clients: List[ActorRef[ClientEvent]] = List.empty,
    worldId: ID
)

private case class MotherState(
    children: List[ChildState] = List.empty
)

object MotherActor:

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    ctx.log.info("😍 Main Server up")
    ctx.system.receptionist ! Receptionist.Register(MOTHER_SERVICE_KEY, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")
    behavior(state = MotherState())

  def behavior(state: MotherState): Behavior[MotherEvent] = Behaviors.receive: (ctx, msg) =>
    msg match
      case ClientUp(client) =>
        ctx.log.info(s"😍 Client Up: ${client.path}")
        val freeChild = state.children.sortBy(_.clients.size).headOption
        freeChild match
          case None => ctx.log.info("😍 No child servers available")
          case Some(child) =>
            ctx.log.info(s"😍 Assigning child server ${child.ref.path} to client ${client.path}")
            client ! GamaManagerAddress(child.ref)
        val updatedChildren = state.children.map { child =>
          if freeChild.contains(child) then child.copy(clients = client :: child.clients)
          else child
        }
        behavior(state.copy(children = updatedChildren))

      case ChildServerUp(child) =>
        ctx.log.info(s"😍 Child Up: ${child.path}")
        val newID = generateWorldID(state.children.map(_.worldId))
        child ! SetUp(newID)
        behavior(state.copy(children = ChildState(ref = child, worldId = newID) :: state.children))

      case ClientLeft(client) =>
        ctx.log.info(s"😍 Client Left: ${client.path}")
        val updatedChildren = state.children.map { child =>
          child.copy(clients = child.clients.filterNot(_ == client))
        }
        behavior(state.copy(children = updatedChildren))

      case ChildServerLeft(child) =>
        ctx.log.info(s"😍 Child Left: ${child.path}")
        behavior(state.copy(children = state.children.filterNot(_.ref == child)))

  def generateWorldID(ids: Seq[ID]): ID =
    val id: ID = scala.util.Random.alphanumeric.filter(_.isLetter).take(3).mkString.toUpperCase
    if ids.contains(id) then generateWorldID(ids)
    else id
