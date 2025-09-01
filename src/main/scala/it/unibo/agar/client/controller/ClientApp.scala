package it.unibo.agar.client.controller

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT
import it.unibo.agar.client.view.View

object ClientApp extends SimpleSwingApplication:

  private val view = new View()
  private val timer = new Timer()
  private val task: TimerTask = new TimerTask:
    override def run(): Unit =
      onEDT(Window.getWindows.foreach(_.repaint()))
      println("Run")

  timer.scheduleAtFixedRate(task, 0, 30) // every 30ms

  override def top: Frame = view
