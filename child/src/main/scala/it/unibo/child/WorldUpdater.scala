package it.unibo.child

import it.unibo.protocol.ConfigParameters
import it.unibo.protocol.ConfigParameters.INIT_FOOD_NUMBER
import it.unibo.protocol.Food
import it.unibo.protocol.Player
import it.unibo.protocol.World

object WorldUpdater:

  val speed = 5.0

  def mergeWorlds(oldWorld: World, newWorld: World, playerId: String): World =
    if !oldWorld.players.exists(_.id == playerId) then oldWorld
    else
      val mergedPlayers = oldWorld.players.map { oldPlayer =>
        newWorld.players.find(_.id == oldPlayer.id) match
          case Some(newPlayer) => newPlayer
          case None => oldPlayer
      }
      newWorld.copy(players = mergedPlayers)

  def tick(world: World, id: String, direction: (Double, Double)): World =
    val (dx, dy) = direction
    world.players.find(_.id == id) match
      case Some(player) =>
        val updatedPlayer = updatePlayerPosition(world, player, dx, dy)
        val updatedWorld = updateWorldAfterMovement(world, updatedPlayer)
        val existingFoodIds = updatedWorld.foods.map(_.id).toSet
        val extraFoods =
          generateFoods(INIT_FOOD_NUMBER).filterNot(food => existingFoodIds.contains(food.id))
        updatedWorld.copy(foods = updatedWorld.foods ++ extraFoods)
      case None =>
        // Player not found, ignore movement
        world

  def generateFoods(n: Int): Seq[Food] =
    (1 to n) map (i =>
      Food(
        s"food-$i",
        scala.util.Random.nextDouble() * ConfigParameters.DEFAULT_WORLD_WIDTH,
        scala.util.Random.nextDouble() * ConfigParameters.DEFAULT_WORLD_HEIGHT,
        ConfigParameters.DEFAULT_FOOD_SIZE
      )
    )

  private def updatePlayerPosition(world: World, player: Player, dx: Double, dy: Double): Player =
    val baseMass = ConfigParameters.DEFAULT_PLAYER_SIZE
    val newSpeed = speed * math.pow(baseMass / player.mass, 0.3).min(1.0)
    val newX = (player.x + dx * newSpeed).max(0).min(world.width)
    val newY = (player.y + dy * newSpeed).max(0).min(world.height)
    player.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(world: World, player: Player): World =
    val foodEaten = world.foods.filter(food => EatingManager.canEatFood(player, food))
    val playerEatsFood = foodEaten.foldLeft(player)((p, food) => p.grow(food))
    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)

extension (w: World)

  def playersExcludingSelf(player: Player): Seq[Player] =
    w.players.filterNot(_.id == player.id)

  def updatePlayer(player: Player): World =
    w.copy(players = w.players.map(p => if (p.id == player.id) player else p))

  def removePlayers(removedPlayers: Seq[Player]): World =
    w.copy(players = w.players.filterNot(p => removedPlayers.map(_.id).contains(p.id)))

  def removeFoods(removedFoods: Seq[Food]): World =
    w.copy(foods = w.foods.filterNot(f => removedFoods.contains(f)))
