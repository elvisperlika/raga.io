package it.unibo.agar.servers.mother

import it.unibo.agar.servers.MyEvent
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.cluster.typed.Cluster
import it.unibo.agar.servers.child.ChildServer
import akka.cluster.typed.Subscribe
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.Member
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.MemberJoined
import akka.cluster.ClusterEvent.MemberWeaklyUp
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.MemberLeft
import akka.cluster.ClusterEvent.MemberPreparingForShutdown
import akka.cluster.ClusterEvent.MemberReadyForShutdown
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.ClusterEvent.MemberDowned
import akka.cluster.ClusterEvent.MemberRemoved

object MotherServer:

  enum MotherEvent extends MyEvent:

    case MyMemberEvent(event: MemberEvent)
    case ChildrenUpdated(listing: Receptionist.Listing)

  def apply(): Behavior[MotherEvent] =
    Behaviors.setup: ctx =>
      println("😍 Main Server up")
      val cluster = Cluster(ctx.system)

      val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MotherEvent.MyMemberEvent.apply)
      cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[ClusterEvent.MemberEvent])

      val listingAdapter = ctx.messageAdapter[Receptionist.Listing](MotherEvent.ChildrenUpdated.apply)
      ctx.system.receptionist ! Receptionist.Subscribe(ChildServer.ChildKey, listingAdapter)

      import MotherEvent.*
      Behaviors.receiveMessage:
        case MyMemberEvent(event) =>
          event match
            case MemberUp(member) if member.hasRole("child") =>
              println(s"😍 Main server sees child node ${member.address} is up. Trying to find the child server...")
              Behaviors.same
            case _ =>
              Behaviors.same

        case ChildrenUpdated(ChildServer.ChildKey.Listing(children)) =>
          children.foreach: child =>
            ctx.log.info(s"👉 Sending X to child $child")
            child ! ChildServer.ChildEvent.X
          Behaviors.same

        case _ =>
          Behaviors.same
