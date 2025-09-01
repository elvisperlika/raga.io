package it.unibo.agar.servers.child

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import it.unibo.agar.servers.MyEvent

object ChildServer:

  enum ChildEvent extends MyEvent:

    case X
    case Y

  val ChildKey = ServiceKey[ChildEvent]("ChildServer")

  def apply(): Behavior[ChildEvent] =
    Behaviors.setup: ctx =>
      ctx.system.receptionist ! Receptionist.Register(ChildKey, ctx.self)
      ctx.log.info("🥶 Child Server up")
      Behaviors.receiveMessage:
        case ChildEvent.X | ChildEvent.Y =>
          println("🥶 Child Server received a message from main!")
          Behaviors.same
