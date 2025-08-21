package it.unibo.agar.controller

import it.unibo.agar.model.AIMovement
import it.unibo.agar.model.GameInitializer
import it.unibo.agar.model.MockGameStateManager
import it.unibo.agar.model.World
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT
import it.unibo.agar.utils.Parameters.WIDTH
import it.unibo.agar.utils.Parameters.HEIGHT
import it.unibo.agar.view.View

object Main extends SimpleSwingApplication:

  private val view = new View()
  private val timer = new Timer()
  private val task: TimerTask = new TimerTask:
    override def run(): Unit =
      onEDT(Window.getWindows.foreach(_.repaint()))
      println("Run")

  timer.scheduleAtFixedRate(task, 0, 30) // every 30ms

  override def top: Frame = view
