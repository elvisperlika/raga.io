package it.unibo.protocol

import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.ServiceKey

trait RagaEvent

enum ChildEvent extends RagaEvent:

  case X
  case Y

enum ClientEvent extends RagaEvent:

  case MyMemberEvent(event: MemberEvent)
  case Tick

enum MotherEvent extends RagaEvent:

  case ClientUp(client: ActorRef[ClientEvent])
  case ChildServerUp(child: ActorRef[ChildEvent])
  case ClientLeft(client: ActorRef[ClientEvent])
  case ChildServerLeft(child: ActorRef[ChildEvent])

object ServiceKeys:

  val CLIENT_SERVICE_KEY: ServiceKey[ClientEvent] = ServiceKey[ClientEvent]("client-service")
  val MOTHER_SERVICE_KEY: ServiceKey[MotherEvent] = ServiceKey[MotherEvent]("mother-server-service")
  val CHILD_SERVICE_KEY: ServiceKey[ChildEvent] = ServiceKey[ChildEvent]("child-server-service")
