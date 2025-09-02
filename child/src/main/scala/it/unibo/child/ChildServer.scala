package it.unibo.agar.servers.child

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import it.unibo.protocol.ChildEvent

object ChildServer:

  val ChildKey = ServiceKey[ChildEvent]("ChildServer")

  def apply(): Behavior[ChildEvent] =
    Behaviors.setup: ctx =>
      val cluster = Cluster(ctx.system)
      cluster.manager ! Join(cluster.selfMember.address)

      ctx.system.receptionist ! Receptionist.Register(ChildKey, ctx.self)
      // ctx.log.info(s"🥶 ${cluster.selfAddress} -> Up")
      Behaviors.receiveMessage:
        case ChildEvent.X | ChildEvent.Y =>
          println("🥶 Child Server received a message from main!")
          Behaviors.same
