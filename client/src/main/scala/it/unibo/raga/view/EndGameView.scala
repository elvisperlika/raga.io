package it.unibo.raga.view

import java.awt.Dimension
import scala.swing.Alignment
import scala.swing.Label
import scala.swing.MainFrame

class EndGameView extends MainFrame:

  title = "Game Over"
  preferredSize = new Dimension(400, 200)

  contents = new Label("Game Over! Thanks for playing!"):
    horizontalAlignment = Alignment.Center
    verticalAlignment = Alignment.Center
