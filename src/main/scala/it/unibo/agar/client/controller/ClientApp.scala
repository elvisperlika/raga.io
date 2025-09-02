package it.unibo.agar.client.controller

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.agar.client.view.View

import scala.concurrent.duration.DurationInt
import scala.swing.*
import scala.swing.Swing.onEDT
import akka.actor.typed.ActorRef
import it.unibo.agar.client.controller.ClientActor.ClientEvent
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.typed.Subscribe
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberUp

object ClientActor:

  val view = new View()
  view.visible = true

  enum ClientEvent:

    case MyMemberEvent(event: MemberEvent)
    case Tick

  def apply(): Behavior[ClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    val cluster = Cluster(ctx.system)

    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(ClientEvent.MyMemberEvent.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    Behaviors.withTimers: timer =>
      timer.startTimerAtFixedRate(ClientEvent.Tick, 1.second)

      import ClientEvent.*
      Behaviors.receiveMessage {
        case Tick =>
          ctx.log.info("🏀 Client do Tick")
          onEDT(view.repaint())
          Behaviors.same
        case ClientEvent.MyMemberEvent(MemberUp(member)) =>
          ctx.log.info(s"🏀 Client has joined the cluster with member: ${member.uniqueAddress}")
          view.showOnline()
          Behaviors.same
        case _ =>
          Behaviors.same
      }
