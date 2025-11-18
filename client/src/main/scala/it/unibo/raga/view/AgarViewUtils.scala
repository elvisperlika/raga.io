package it.unibo.raga.view

import it.unibo.raga.model.LocalWorld

import java.awt.Color
import java.awt.Graphics2D

object AgarViewUtils:

  private val playerBorderColor = Color.black
  private val worldNameColor = Color.DARK_GRAY
  private val playerLabelOffsetX = 10
  private val playerLabelOffsetY = 0
  private val playerInnerOffset = 2
  private val playerInnerBorder = 4
  private val worldNameOffsetX = 5
  private val worldNameOffsetY = 15
  private val playerPalette: Array[Color] =
    Array(Color.blue, Color.orange, Color.cyan, Color.pink, Color.yellow, Color.red, Color.green,
      Color.lightGray)

  private def playerColor(id: String): Color =
    val randIdx = Math.abs(id.hashCode)
    playerPalette(randIdx % playerPalette.length)

  def drawWorld(
      g: Graphics2D,
      world: LocalWorld,
      offsetX: Double = 0,
      offsetY: Double = 0
  ): Unit =
    def toScreenCenter(x: Double, y: Double, radius: Int): (Int, Int) =
      ((x - offsetX - radius).toInt, (y - offsetY - radius).toInt)

    def toScreenLabel(x: Double, y: Double): (Int, Int) =
      ((x - offsetX - playerLabelOffsetX).toInt, (y - offsetY - playerLabelOffsetY).toInt)

    // Draw world name
    g.setColor(worldNameColor)
    g.drawString(s"World ID: ${world.id}", worldNameOffsetX, worldNameOffsetY)

    // Draw foods
    g.setColor(Color.green)
    world.foods.foreach: food =>
      val radius = food.radius.toInt
      val diameter = radius * 2
      val (foodX, foodY) = toScreenCenter(food.x, food.y, radius)
      g.fillOval(foodX, foodY, diameter, diameter)

    // Draw players
    world.players.foreach: player =>
      val radius = player.radius.toInt
      val diameter = radius * 2
      val (borderX, borderY) = toScreenCenter(player.x, player.y, radius)
      g.setColor(playerBorderColor)
      g.drawOval(borderX, borderY, diameter, diameter)
      g.setColor(playerColor(player.id))
      val (innerX, innerY) = toScreenCenter(player.x, player.y, radius - playerInnerOffset)
      g.fillOval(innerX, innerY, diameter - playerInnerBorder, diameter - playerInnerBorder)
      g.setColor(playerBorderColor)
      val (labelX, labelY) = toScreenLabel(player.x, player.y)
      g.drawString(player.id, labelX, labelY)
      // Draw player position above the player
      val positionString = f"(${player.x}%.1f, ${player.y}%.1f)"
      val positionOffsetY = 15 // pixels above the label
      g.drawString(positionString, labelX, labelY - positionOffsetY)
