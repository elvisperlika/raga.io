package it.unibo.agar.client.controller

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT
import it.unibo.agar.client.view.View
import akka.actor.typed.Behavior
import it.unibo.agar.servers.child.ChildServer.ChildEvent
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.DurationInt
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import java.awt.Graphics
import java.awt.Color
import it.unibo.agar.client.model.MockGameStateManager
import it.unibo.agar.client.model.GameInitializer
import it.unibo.agar.client.model.World

object ClientActor:

  enum ClientEvent:

    case Tick

  def apply(): Behavior[ClientEvent] = Behaviors.setup: ctx =>
    val view = new View()
    view.visible = true

    Behaviors.withTimers: timers =>
      timers.startTimerAtFixedRate("tick", ClientEvent.Tick, 30.millis)
      import ClientEvent.*
      Behaviors.receiveMessage {
        case Tick =>
          ctx.log.info("🏀 Client do Tick")
          onEDT(view.repaint())
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    Behaviors.same
