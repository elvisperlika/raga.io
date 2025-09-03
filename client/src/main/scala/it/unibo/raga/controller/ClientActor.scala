package it.unibo.raga.controller

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ServiceKeys.CLIENT_SERVICE_KEY
import it.unibo.raga.view.View

import scala.concurrent.duration.DurationInt
import scala.swing.*
import scala.swing.Swing.onEDT
import akka.actor.typed.receptionist.Receptionist

object ClientActor:

  val view = new View()
  view.visible = true

  def apply(): Behavior[ClientEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🏀 Client node Up")
    val cluster = Cluster(ctx.system)

    ctx.system.receptionist ! Receptionist.Register(CLIENT_SERVICE_KEY, ctx.self)

    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(ClientEvent.MyMemberEvent.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    Behaviors.withTimers: timer =>
      timer.startTimerAtFixedRate(ClientEvent.Tick, 1.second)

      import ClientEvent.*
      Behaviors.receiveMessage {
        case Tick =>
          onEDT(view.repaint())
          Behaviors.same
        case ClientEvent.MyMemberEvent(MemberUp(member)) =>
          ctx.log.info(s"🏀 Client has joined the cluster with member: ${member.uniqueAddress}")
          view.showOnline()
          Behaviors.same
        case _ =>
          Behaviors.same
      }
