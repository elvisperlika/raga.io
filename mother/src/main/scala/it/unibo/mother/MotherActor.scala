package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.ChildClientLeft
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
import it.unibo.protocol.ServiceNotAvailable
import it.unibo.protocol.SetUp

private case class ChildState(
    ref: ActorRef[ChildEvent],
    clients: List[ActorRef[ClientEvent]] = List.empty,
    worldId: ID
)

private case class MotherState(
    children: List[ChildState] = List.empty,
    pendingClients: List[ActorRef[ClientEvent]] = List.empty,
    rooms: Map[ID, ChildState] = Map.empty
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
        val freeChild = findFreeChild(state)
        freeChild match
          case None =>
            ctx.log.info("😍 No child servers available")
            client ! ServiceNotAvailable()
            behavior(state.copy(pendingClients = client :: state.pendingClients))
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

        val newChildState = ChildState(ref = child, worldId = newID)
        //val newRooms = state.rooms + (newID -> newChildState)

        state.pendingClients.foreach { client =>
          ctx.log.info(s"😍 Assigning child server ${child.path} to pending client ${client.path}")
          client ! GamaManagerAddress(child)
        }
        behavior(state.copy(children = ChildState(ref = child, worldId = newID) :: state.children,
        rooms = newRooms,
        ))

      case ClientLeft(client) =>
        ctx.log.info(s"😍 Client Left: ${client.path}")

        val updatedChildren = state.children.map { child =>
          child.copy(clients = child.clients.filterNot(_ == client))
        }

        state.children.find(_.clients.contains(client)) match
          case Some(child) =>
            ctx.log.info(s"😍 Notified child server ${child.ref.path} about client ${client.path} disconnection")
            child.ref ! ChildClientLeft(client)
          case _ =>

        var newPendingClients = state.pendingClients
        if state.pendingClients.contains(client) then newPendingClients = state.pendingClients.filterNot(_ == client)

        behavior(state.copy(children = updatedChildren, pendingClients = newPendingClients))

      case ChildServerLeft(child) =>
        ctx.log.info(s"😍 Child Left: ${child.path}")
        behavior(state.copy(children = state.children.filterNot(_.ref == child)))

      case JoinFriendsRoom(client, roomId) =>
        state.rooms.get(roomId) match
          case Some(childState) =>
            ctx.log.info(s"😍 Client ${client.path} joining room $roomId")
            val updatedChild = childState.copy(clients = client :: childState.clients)
            val updatedRooms = state.rooms + (roomId -> updatedChild)

            client ! GamaManagerAddress(childState.ref)

            behavior(state.copy(
              children = state.children.map(c => if c.worldId == roomId then updatedChild else c),
              rooms = updatedRooms
            ))

          case None =>
            ctx.log.info(s"😭 Room $roomId not found for client ${client.path}")
            client ! JoinFriendsRoomFailed(roomId)
            Behaviors.same

      case CreateFriendsRoom(client) =>
        findFreeChild(state) match
          case None =>
            client ! ServiceNotAvailable()
            Behaviors.same
          case Some(child) =>
            val newID = generateWorldID(state.children.map(_.worldId))
            ctx.log.info(s"😍 Creating friends room $newID on ${child.ref.path}")

            val updatedChild = child.copy(clients = client :: child.clients)
            val newRooms = state.rooms + (newID -> updatedChild)

            client ! FriendsRoomCreated(newID)
            client ! GamaManagerAddress(updatedChild.ref)

            behavior(state.copy(
              children = state.children.map(c => if c.ref == child.ref then updatedChild else c),
              rooms = newRooms
            ))

  /** Generate a unique world ID not present in the given list of IDs
    *
    * @param ids
    *   IDs already in use
    * @return
    *   A free unique ID
    */
  private def generateWorldID(ids: Seq[ID]): ID =
    Iterator.continually(createID).find(id => !ids.contains(id)).get

  /** Creates a random ID consisting of 3 uppercase letters.
    *
    * @return
    *   A random ID
    */
  private def createID: ID =
    scala.util.Random.alphanumeric.filter(_.isLetter).take(3).mkString.toUpperCase

  /** Finds the child server with the least number of connected clients.
    *
    * @param state
    *   Current state of the mother actor
    * @return
    *   An option containing the least loaded child server, or None if no child servers are available
    */
  private def findFreeChild(state: MotherState): Option[ChildState] =
    state.children.sortBy(_.clients.size).headOption
