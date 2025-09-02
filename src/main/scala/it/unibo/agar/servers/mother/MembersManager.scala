package it.unibo.agar.servers.mother

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import akka.actor.typed.ActorRef

object MembersManager:

  def apply(): Behavior[MemberEvent] = Behaviors.setup: ctx =>
    ctx.log.info(s"🪀 Members Manager is Up")
    val cluster = Cluster(ctx.system)
    cluster.subscriptions ! Subscribe(ctx.self, classOf[MemberEvent])

    Behaviors.receiveMessage {
      case MemberUp(member) =>
        ctx.log.info(s"🪀 New Member is Up: ${member.uniqueAddress} with roles ${member.roles}")
        Behaviors.same
      case _ =>
        Behaviors.same
    }
