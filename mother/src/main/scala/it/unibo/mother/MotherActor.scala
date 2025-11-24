package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.mother.BackupActor.RequestBackup
import it.unibo.protocol.*
import it.unibo.protocol.ServiceKeys.MOTHER_SERVICE_KEY

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Success

private case class ChildState(
    ref: ActorRef[ChildEvent],
    clients: List[ActorRef[ClientEvent]] = List.empty,
    worldId: ID,
    isPrivate: Boolean = false
)

private case class MotherState(
    children: List[ChildState] = List.empty,
    pendingClients: List[ActorRef[ClientEvent]] = List.empty,
    rooms: Map[ID, ChildState] = Map.empty
)

object MotherActor:

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    // ctx.log.info("😍 Main Server up")
    ctx.system.receptionist ! Receptionist.Register(MOTHER_SERVICE_KEY, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")
    val backupActor = ctx.spawn(BackupActor(ctx.self), "BackupActor")
    behavior(state = MotherState(), backupActor = backupActor)

  def behavior(
      state: MotherState,
      backupActor: ActorRef[BackupEvent]
  ): Behavior[MotherEvent] = Behaviors.receive: (ctx, msg) =>
    msg match
      case ClientUp(client) =>
        ctx.log.info(s"😍 Client Up: ${client.path}")

        val freeChild = findFreeChild(state)
        freeChild match
          case None =>
            // ctx.log.info("😍 No child servers available")
            client ! ServiceNotAvailable()
            behavior(
              state.copy(pendingClients = client :: state.pendingClients),
              backupActor
            )
          case Some(child) =>
            // ctx.log.info(
            //   s"😍 Assigning child server ${child.ref.path} to client ${client.path}"
            // )
            client ! GameManagerAddress(child.ref)
            val updatedChildren = state.children.map { child =>
              if freeChild.contains(child) then child.copy(clients = client :: child.clients)
              else child
            }
            behavior(state.copy(children = updatedChildren), backupActor)

      case ChildServerUp(child) =>
        // ctx.log.info(s"😍 Child Up: ${child.path}")
        backupActor ! BackupActor.FollowChild(child)
        val newID = generateWorldID(state.children.map(_.worldId))
        child ! SetUp(newID, ctx.self, backupActor)

        val newChildState = ChildState(ref = child, worldId = newID)
        // val newRooms = state.rooms + (newID -> newChildState)

        state.pendingClients.foreach {
          client =>
            ctx.log.info(
              s"😍 Assigning child server ${child.path} to pending client ${client.path}"
            )
          client ! GameManagerAddress(child)
        }
        behavior(
          state.copy(children = ChildState(ref = child, worldId = newID) :: state.children),
          backupActor
        )

      case ClientLeft(client) =>
        // ctx.log.info(s"😍 Client Left: ${client.path}")

        val updatedChildren = state.children.map { child =>
          child.copy(clients = child.clients.filterNot(_ == client))
        }

        state.children.find(_.clients.contains(client)) match
          case Some(child) =>
            // ctx.log.info(s"😍 Notified child server ${child.ref.path} about client ${client.path} disconnection")
            child.ref ! ChildClientLeft(client)
          case _ =>

        var newPendingClients = state.pendingClients
        if state.pendingClients.contains(client) then
          newPendingClients = state.pendingClients.filterNot(_ == client)

        behavior(
          state.copy(children = updatedChildren, pendingClients = newPendingClients),
          backupActor
        )

      case ChildServerLeft(child) =>
        // ctx.log.info(s"😍 Child Left: ${child.path}")
        given Timeout = 5.seconds
        given Scheduler = ctx.system.scheduler
        given ExecutionContext = ctx.executionContext

        state.children.find(c => c.ref == child) match
          case Some(childState) if childState.clients.nonEmpty =>
            ctx.log.info(s"😍 Recovering clients from child server ${child.path}")
            backupActor
              .ask[SaveWorldData](replyTo => RequestBackup(child, replyTo))
              .onComplete {
                case Success(worldToBackup) =>
                  backupActor ! BackupActor.UnfollowChild(child)
                  findFreeChild(state) match
                    case Some(freeChild) =>
                      val newWorld = worldToBackup.world
                      val clients = worldToBackup.managedPlayers
                      freeChild.ref ! NewSetUp(newWorld, clients)
                    case None =>
                case _ =>
              }
          case _ =>
          // ctx.log.info(s"😍 No clients to recover from child server ${child.path}")

        behavior(
          state.copy(children = state.children.filterNot(_.ref == child)),
          backupActor
        )

      case ClientAskToJoinRoom(client, roomCode, nickName, replyTo) =>
        // ctx.log.info(s"👉 Client $nickName asked to join room $roomCode")
        // state.children.foreach(c => ctx.log.info(s"👉 Room available: ${c.worldId}"))
        state.children.find(_.worldId == roomCode) match
          case Some(roomState) =>
            client ! PrivateManagerAddress(roomState.ref)
            val updatedChild = roomState.copy(clients = client :: roomState.clients)
            val updatedRooms = state.rooms + (roomCode -> updatedChild)
            behavior(
              state.copy(
                children =
                  state.children.map(c => if c.worldId == roomCode then updatedChild else c),
                rooms = updatedRooms
              ),
              backupActor
            )
          case _ =>
            // ctx.log.info("😭 Room not found.")
            client ! JoinFriendsRoomFailed(roomCode)
            Behaviors.same

      case RequestPrivateRoomCreation(clientRef, clientNickName) =>
        findFreeChild(state).filter(_.isPrivate == false) match
          case None =>
            // ctx.log.info("😭 No available child servers to create a friends room.")
            clientRef ! ServiceNotAvailable()
            Behaviors.same

          case Some(child) =>
            // ctx.log.info(
            //   s"😍 Asking ${child.ref.path.name} to create a friends room for $clientNickName (${clientRef.path.name})"
            // )
            clientRef ! PrivateManagerAddress(child.ref)
            val updatedChild = child.copy(clients = clientRef :: child.clients)
            val updatedChildren =
              state.children.map(c => if c.ref == child.ref then updatedChild else c)
            behavior(state.copy(children = updatedChildren), backupActor)

      case RoomCreated(roomId, childRef, owner) =>
        // ctx.log.info(
        //   s"😍 Room $roomId created by ${childRef.path}, owner ${owner.path}"
        // )
        val updatedChildren = state.children.map { c =>
          if c.ref == childRef then c.copy(clients = owner :: c.clients) else c
        }

        val newChildStateOpt = updatedChildren.find(_.ref == childRef)
        val updatedRooms = newChildStateOpt match
          case Some(cs) => state.rooms + (roomId -> cs)
          case None => state.rooms

        owner ! GameManagerAddress(childRef)
        owner ! FriendsRoomCreated(roomId)

        behavior(
          state.copy(children = updatedChildren, rooms = updatedRooms),
          backupActor
        )

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
    *   An option containing the least loaded child server, or None if no child servers are
    *   available
    */
  private def findFreeChild(state: MotherState): Option[ChildState] =
    state.children.sortBy(_.clients.size).headOption
