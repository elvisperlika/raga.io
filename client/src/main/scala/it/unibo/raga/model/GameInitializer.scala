package it.unibo.raga.model

import scala.util.Random

object GameInitializer:

  def initialPlayers(numPlayers: Int, width: Int, height: Int, initialMass: Double = 120.0): Seq[LocalPlayer] =
    (1 to numPlayers).map[LocalPlayer](i =>
      LocalPlayer(s"p$i", Random.nextInt(width), Random.nextInt(height), initialMass)
    )

  def initialFoods(numFoods: Int, width: Int, height: Int, initialMass: Double = 100.0): Seq[LocalFood] =
    (1 to numFoods).map[LocalFood](i => LocalFood(s"f$i", Random.nextInt(width), Random.nextInt(height), initialMass))
