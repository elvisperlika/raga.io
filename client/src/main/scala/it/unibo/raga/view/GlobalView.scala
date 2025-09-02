package it.unibo.raga.view

import java.awt.Graphics2D
import scala.swing.*
import it.unibo.raga.model.MockGameStateManager

class GlobalView(manager: MockGameStateManager) extends MainFrame:

  title = "Raga.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      AgarViewUtils.drawWorld(g, world)
