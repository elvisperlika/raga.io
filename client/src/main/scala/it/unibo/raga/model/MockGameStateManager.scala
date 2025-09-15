package it.unibo.raga.model

trait GameStateManager:

  def getWorld: LocalWorld

  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit

case class GameStateManagerImpl(
    world: LocalWorld,
    speed: Double = 10.0,
    private var playerDirection: (String, (Double, Double)) = ("", (0.0, 0.0))
):

  def getWorld: LocalWorld = world

  def movePlayerDirection(id: String, dx: Double, dy: Double): GameStateManagerImpl =
    copy(playerDirection = (id, (dx, dy)))

  def tick(): GameStateManagerImpl =
    val (id, (dx, dy)) = playerDirection
    world.playerById(id) match
      case Some(player) =>
        val updatedPlayer = updatePlayerPosition(player, dx, dy)
        val updatedWorld = updateWorldAfterMovement(updatedPlayer)
        copy(world = updatedWorld)
      case None =>
        // Player not found, ignore movement
        this

  private def updatePlayerPosition(player: LocalPlayer, dx: Double, dy: Double): LocalPlayer =
    val newX = (player.x + dx * speed).max(0).min(world.width)
    val newY = (player.y + dy * speed).max(0).min(world.height)
    player.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(player: LocalPlayer): LocalWorld =
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

class MockGameStateManager(
    var world: LocalWorld,
    val speed: Double = 10.0
) extends GameStateManager:

  private var directions: Map[String, (Double, Double)] = Map.empty
  def getWorld: LocalWorld = world

  // Move a player in a given direction (dx, dy)
  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
    directions = directions.updated(id, (dx, dy))

  def tick(): Unit =
    directions.foreach:
      case (id, (dx, dy)) =>
        world.playerById(id) match
          case Some(player) =>
            world = updateWorldAfterMovement(updatePlayerPosition(player, dx, dy))
          case None =>
          // Player not found, ignore movement

  private def updatePlayerPosition(player: LocalPlayer, dx: Double, dy: Double): LocalPlayer =
    val newX = (player.x + dx * speed).max(0).min(world.width)
    val newY = (player.y + dy * speed).max(0).min(world.height)
    player.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(player: LocalPlayer): LocalWorld =
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
