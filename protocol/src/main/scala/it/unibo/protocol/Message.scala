package it.unibo.protocol

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.ClusterEvent.MemberEvent
type ID = String
type RoomCode = String
type PlayerRef = (ID, ActorRef[ClientEvent])

trait Message

/* -------------------------------------------- Child Events -------------------------------------------- */

trait ChildEvent extends Message

case class RequestWorld(
    nickName: String,
    replyTo: ActorRef[RemoteWorld],
    playerRef: ActorRef[ClientEvent]
) extends ChildEvent
case class RequestWorldInRoom(
    nickName: ID,
    roomCode: RoomCode,
    client: ActorRef[ClientEvent]
) extends ChildEvent
case class RemoteWorld(world: World, player: Player) extends ChildEvent
case class SetUp(worldId: ID, motherRef: ActorRef[MotherEvent], backupRef: ActorRef[BackupEvent])
    extends ChildEvent
case class ChildClientLeft(client: ActorRef[ClientEvent]) extends ChildEvent
case class EatenPlayer(id: ID) extends ChildEvent
case class RequestPrivateRoom(nickName: String, client: ActorRef[ClientEvent]) extends ChildEvent
case class PlayerJoinedRoom(nickName: String, client: ActorRef[ClientEvent]) extends ChildEvent
case class NewSetUp(world: World, clients: Map[String, ActorRef[ClientEvent]]) extends ChildEvent
case class RequestWorldToBackup(replyTo: ActorRef[SaveWorldData]) extends ChildEvent
case class PlayerMove(id: ID, newX: Double, newY: Double) extends ChildEvent

trait BackupEvent extends Message
case class SaveWorldData(
    seconds: ActorRef[ChildEvent],
    world: World,
    managedPlayers: Map[String, ActorRef[ClientEvent]]
) extends BackupEvent

/* -------------------------------------------- Client Events -------------------------------------------- */

trait ClientEvent extends Message

/** The client join the network. */
case class JoinNetwork(event: MemberEvent) extends ClientEvent
case class UpdateView() extends ClientEvent
case class GameManagerAddress(ref: ActorRef[ChildEvent]) extends ClientEvent
case class ReceivedRemoteWorld(world: World) extends ClientEvent
case class ServiceNotAvailable() extends ClientEvent
case class EndGame() extends ClientEvent
case class FriendsRoomCreated(roomId: ID) extends ClientEvent
case class JoinFriendsRoomFailed(roomId: ID) extends ClientEvent
case class NewPlayerJoined(player: Player) extends ClientEvent
case class InitWorld(world: World, player: Player, managerRef: ActorRef[ChildEvent])
    extends ClientEvent
case class CodeNotFound() extends ClientEvent
case class PrivateManagerAddress(ref: ActorRef[ChildEvent]) extends ClientEvent
case class NewManager(
    newManagerRef: ActorRef[ChildEvent],
    newWorld: World,
    player: Player
) extends ClientEvent

/* -------------------------------------------- Mother Events -------------------------------------------- */

trait MotherEvent extends Message

case class ClientUp(client: ActorRef[ClientEvent]) extends MotherEvent
case class ChildServerUp(child: ActorRef[ChildEvent]) extends MotherEvent
case class ClientLeft(client: ActorRef[ClientEvent]) extends MotherEvent
case class ChildServerLeft(child: ActorRef[ChildEvent]) extends MotherEvent
case class ClientAskToJoinRoom(
    client: ActorRef[ClientEvent],
    roomCode: RoomCode,
    nickName: String,
    replyTo: ActorRef[ChildEvent]
) extends MotherEvent
case class RoomCreated(
    roomId: ID,
    childRef: ActorRef[ChildEvent],
    owner: ActorRef[ClientEvent]
) extends MotherEvent

// Private room creation
case class RequestPrivateRoomCreation(
    client: ActorRef[ClientEvent],
    nickName: String
) extends MotherEvent

/* -------------------------------------------- Service Keys -------------------------------------------- */

object ServiceKeys:

  val CLIENT_SERVICE_KEY: ServiceKey[ClientEvent] =
    ServiceKey[ClientEvent]("client-service")
  val MOTHER_SERVICE_KEY: ServiceKey[MotherEvent] =
    ServiceKey[MotherEvent]("mother-server-service")
  val CHILD_SERVICE_KEY: ServiceKey[ChildEvent] =
    ServiceKey[ChildEvent]("child-server-service")

/* -------------------------------------------- World entities -------------------------------------------- */

trait Entity:

  def id: String
  def mass: Double
  def x: Double
  def y: Double
  def radius: Double = math.sqrt(mass / math.Pi)

  def distanceTo(other: Entity): Double =
    val dx = x - other.x
    val dy = y - other.y
    math.hypot(dx, dy)

case class Player(id: ID, x: Double, y: Double, mass: Double) extends Entity:

  def grow(entity: Entity): Player =
    copy(mass = mass + entity.mass)

case class Food(id: ID, x: Double, y: Double, mass: Double) extends Entity

case class World(
    id: ID,
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: Seq[Food]
)
