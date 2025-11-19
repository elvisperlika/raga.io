package it.unibo.raga.view

import akka.actor.typed.ActorRef
import it.unibo.raga.controller.ClientActor.LocalClientEvent
import it.unibo.raga.model.LocalWorld

import java.awt.Graphics2D
import scala.swing.*

class LocalView(var world: LocalWorld, playerId: String, actorRef: ActorRef[LocalClientEvent])
    extends MainFrame:

  title = s"Raga.io - Local View ($playerId)"
  preferredSize = new Dimension(400, 400)

  var direction = (0.0, 0.0) // (dx, dy)

  private var lastUpdateTime = System.currentTimeMillis()
  private val updateInterval = 100 // milliseconds of input lag

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val playerOpt = world.players.find(_.id == playerId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY)

    reactions += { case e: event.MouseMoved =>
      val now = System.currentTimeMillis()
      if (now - lastUpdateTime > updateInterval) {
        val mousePos = e.point
        val playerOpt = world.players.find(_.id == playerId)
        playerOpt.foreach: player =>
          val dx = (mousePos.x - size.width / 2) * 0.01
          val dy = (mousePos.y - size.height / 2) * 0.01
          // actorRef ! LocalClientEvent.MovePlayer(dx, dy)
          direction = (dx, dy)
        lastUpdateTime = now
        repaint()
      }
    }

  def updateWorld(newWorld: LocalWorld): Unit =
    world = newWorld
