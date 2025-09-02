package it.unibo.agar.servers.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.MemberEvent
import it.unibo.agar.servers.MyEvent
import it.unibo.agar.servers.child.ChildServer
import it.unibo.agar.servers.child.ChildServer.ChildEvent
import akka.cluster.typed.Cluster
import it.unibo.agar.client.controller.ClientActor.ClientEvent
import it.unibo.agar.servers.mother.MotherServer.MotherEvent.ClientUp
import akka.actor.typed.receptionist.ServiceKey

object MotherServer:

  val serviceKey = ServiceKey[MotherEvent]("mother-server-service")

  var children: List[ActorRef[ChildEvent]] = List.empty
  var clients: List[ActorRef[ClientEvent]] = List.empty

  enum MotherEvent:

    case ClientUp(client: ActorRef[ClientEvent])
    case ChildServerUp(child: ActorRef[ChildEvent])
    case ClientLeft(client: ActorRef[ClientEvent])
    case ChildServerLeft(child: ActorRef[ChildEvent])

  def apply(): Behavior[MotherEvent] = Behaviors.setup: ctx =>
    println("😍 Main Server up")

    ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)
    ctx.spawn(MembersManager(ctx.self), "MembersManager")

    import MotherEvent.*
    Behaviors.receiveMessage {
      case ClientUp(client) =>
        Behaviors.same
      case ChildServerUp(child) =>
        Behaviors.same
      case ClientLeft(client) =>
        Behaviors.same
      case ChildServerLeft(child) =>
        Behaviors.same
    }
