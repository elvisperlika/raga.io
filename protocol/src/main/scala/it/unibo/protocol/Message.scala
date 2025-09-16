package it.unibo.protocol

import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.ServiceKey

trait Message

/* -------------------------------------------- Child Events -------------------------------------------- */

trait ChildEvent extends Message

case class RequestWorld(nickName: String, replyTo: ActorRef[RemoteWorld], playerRef: ActorRef[ClientEvent])
    extends ChildEvent
case class RemoteWorld(world: World, player: Player) extends ChildEvent

/* -------------------------------------------- Client Events -------------------------------------------- */

trait ClientEvent extends Message

case class JoinNetwork(event: MemberEvent) extends ClientEvent
case class UpdateView() extends ClientEvent
case class GamaManagerAddress(ref: ActorRef[ChildEvent]) extends ClientEvent

/* -------------------------------------------- Mother Events -------------------------------------------- */

trait MotherEvent extends Message

case class ClientUp(client: ActorRef[ClientEvent]) extends MotherEvent
case class ChildServerUp(child: ActorRef[ChildEvent]) extends MotherEvent
case class ClientLeft(client: ActorRef[ClientEvent]) extends MotherEvent
case class ChildServerLeft(child: ActorRef[ChildEvent]) extends MotherEvent

/* -------------------------------------------- Service Keys -------------------------------------------- */

object ServiceKeys:

  val CLIENT_SERVICE_KEY: ServiceKey[ClientEvent] = ServiceKey[ClientEvent]("client-service")
  val MOTHER_SERVICE_KEY: ServiceKey[MotherEvent] = ServiceKey[MotherEvent]("mother-server-service")
  val CHILD_SERVICE_KEY: ServiceKey[ChildEvent] = ServiceKey[ChildEvent]("child-server-service")

/* -------------------------------------------- World entities -------------------------------------------- */

private trait Entity

case class Player(id: String, x: Double, y: Double, mass: Double) extends Entity

case class Food(id: String, x: Double, y: Double, mass: Double) extends Entity

case class World(
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: Seq[Food]
)
