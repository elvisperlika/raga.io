package it.unibo.raga.model

sealed trait Entity:

  def id: String
  def mass: Double
  def x: Double
  def y: Double
  def radius: Double = math.sqrt(mass / math.Pi)

  def distanceTo(other: Entity): Double =
    val dx = x - other.x
    val dy = y - other.y
    math.hypot(dx, dy)

case class LocalPlayer(id: String, x: Double, y: Double, mass: Double) extends Entity:

  def grow(entity: Entity): LocalPlayer =
    copy(mass = mass + entity.mass)

case class LocalFood(id: String, x: Double, y: Double, mass: Double = 100.0) extends Entity

case class LocalWorld(
    id: String = "<unknown>",
    width: Int,
    height: Int,
    players: Seq[LocalPlayer],
    foods: Seq[LocalFood]
):

  def playersExcludingSelf(player: LocalPlayer): Seq[LocalPlayer] =
    players.filterNot(_.id == player.id)

  def playerById(id: String): Option[LocalPlayer] =
    players.find(_.id == id)

  def updatePlayer(player: LocalPlayer): LocalWorld =
    copy(players = players.map(p => if (p.id == player.id) player else p))

  def removePlayers(ids: Seq[LocalPlayer]): LocalWorld =
    copy(players = players.filterNot(p => ids.map(_.id).contains(p.id)))

  def removeFoods(ids: Seq[LocalFood]): LocalWorld =
    copy(foods = foods.filterNot(f => ids.contains(f)))
