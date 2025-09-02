package it.unibo.protocol

import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.typed.ActorRef

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
