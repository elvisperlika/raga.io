package it.unibo.agar.servers.child

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import it.unibo.agar.servers.MyEvent
import akka.cluster.Cluster

object ChildServer:

  enum ChildEvent extends MyEvent:

    case X
    case Y

  val ChildKey = ServiceKey[ChildEvent]("ChildServer")

  def apply(): Behavior[ChildEvent] =
    Behaviors.setup: ctx =>
      val cluster = Cluster(ctx.system)
      ctx.system.receptionist ! Receptionist.Register(ChildKey, ctx.self)
      ctx.log.info(s"🥶 ${cluster.selfAddress} -> Up")
      Behaviors.receiveMessage:
        case ChildEvent.X | ChildEvent.Y =>
          println("🥶 Child Server received a message from main!")
          Behaviors.same
