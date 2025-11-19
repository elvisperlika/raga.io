package it.unibo.raga.model

/** Object responsible for AI movement logic, separate from the game state management */
object AIMovement:

  def nearestFoodLocation(player: String, world: LocalWorld): Option[(Double, Double)] =
    world.playerById(player).flatMap { p =>
      world.foods.minByOption(f => p.distanceTo(f)).map(f => (f.x, f.y))
    }

  def nearestPlayerLocation(ai: LocalPlayer, world: LocalWorld): Option[(Double, Double)] =
    world.players
      .filterNot(_.id == ai.id)
      .filter(p => p.mass < ai.mass)
      .minByOption(p => ai.distanceTo(p))
      .map(p => (p.x, p.y))

  def getAIDirection(ai: LocalPlayer, world: LocalWorld): (Double, Double) =
    val target = nearestFoodLocation(ai.id, world).orElse(nearestPlayerLocation(ai, world))
    target match
      case Some((tx, ty)) =>
        val dx = tx - ai.x
        val dy = ty - ai.y
        val dist = math.hypot(dx, dy)
        if dist > 0 then (dx / dist, dy / dist) else (0.0, 0.0)
      case None => (0.0, 0.0)
