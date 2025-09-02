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

object MotherServer:

  var children: Set[ActorRef[ChildEvent]] = Set.empty

  enum MotherEvent extends MyEvent:

    case MyMemberEvent(event: MemberEvent)
    case ChildrenUpdated(listing: Receptionist.Listing)

  def apply(): Behavior[MotherEvent] =
    Behaviors.setup: ctx =>
      println("😍 Main Server up")
      ctx.spawn(MembersManager(), "MembersManager")

      Behaviors.same
