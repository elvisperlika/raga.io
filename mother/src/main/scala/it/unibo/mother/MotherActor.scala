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
        val freeChild = findFreeChild(state)
        freeChild match
          case None =>
            client ! ServiceNotAvailable()
            behavior(
              state.copy(pendingClients = client :: state.pendingClients),
              backupActor
            )
          case Some(child) =>
            client ! GameManagerAddress(child.ref)
            val updatedChildren = state.children.map { child =>
              if freeChild.contains(child) then child.copy(clients = client :: child.clients)
              else child
            }
            behavior(state.copy(children = updatedChildren), backupActor)

      case ChildServerUp(child) =>
        backupActor ! BackupActor.FollowChild(child)
        val newID = generateWorldID(state.children.map(_.worldId))
        child ! SetUp(newID, ctx.self, backupActor)
        val newChildState = ChildState(ref = child, worldId = newID)
        state.pendingClients.foreach(_ ! GameManagerAddress(child))
        behavior(
          state.copy(children = ChildState(ref = child, worldId = newID) :: state.children),
          backupActor
        )

      case ClientLeft(client) =>
        val updatedChildren = state.children.map { child =>
          child.copy(clients = child.clients.filterNot(_ == client))
        }
        state.children.find(_.clients.contains(client)) match
          case Some(child) => child.ref ! ChildClientLeft(client)
          case _ =>
        var newPendingClients = state.pendingClients
        if state.pendingClients.contains(client) then
          newPendingClients = state.pendingClients.filterNot(_ == client)
        behavior(
          state.copy(children = updatedChildren, pendingClients = newPendingClients),
          backupActor
        )

      case ChildServerLeft(child) =>
        given Timeout = 5.seconds
        given Scheduler = ctx.system.scheduler
        given ExecutionContext = ctx.executionContext
        state.children.find(c => c.ref == child) match
          case Some(childState) if childState.clients.nonEmpty =>
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
        behavior(
          state.copy(children = state.children.filterNot(_.ref == child)),
          backupActor
        )

      case ClientAskToJoinRoom(client, roomCode, nickName, replyTo) =>
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
            client ! JoinFriendsRoomFailed(roomCode)
            Behaviors.same

      case RequestPrivateRoomCreation(clientRef, clientNickName) =>
        findFreeChild(state).filter(_.isPrivate == false) match
          case None =>
            clientRef ! ServiceNotAvailable()
            Behaviors.same

          case Some(child) =>
            clientRef ! PrivateManagerAddress(child.ref)
            val updatedChild = child.copy(clients = clientRef :: child.clients)
            val updatedChildren =
              state.children.map(c => if c.ref == child.ref then updatedChild else c)
            behavior(state.copy(children = updatedChildren), backupActor)

      case RoomCreated(roomId, childRef, owner) =>
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
