package it.unibo.raga.controller

import it.unibo.protocol.Food
import it.unibo.protocol.Player
import it.unibo.protocol.World
import it.unibo.raga.model.LocalFood
import it.unibo.raga.model.LocalPlayer
import it.unibo.raga.model.LocalWorld

object WorldConverter:

  /** Convert a remote world to a local world.
    *
    * @param remoteWorld
    *   Remote world received from the server
    * @return
    *   Local world to be used by the client
    */
  def createLocalWorld(remoteWorld: World): LocalWorld =
    val localPlayers = remoteWorld.players.map(p => LocalPlayer(p.id, p.x, p.y, p.mass))
    val localFoods = remoteWorld.foods.map(f => LocalFood(f.id, f.x, f.y, f.mass))
    LocalWorld(remoteWorld.id, remoteWorld.width, remoteWorld.height, localPlayers, localFoods)

  /** Convert a local world to a remote world.
    *
    * @param localWorld
    *   Local world used by the client
    * @return
    *   Remote world to be sent to the server
    */
  def createRemoteWorld(localWorld: LocalWorld): World =
    val remotePlayers = localWorld.players.map(p => Player(p.id, p.x, p.y, p.mass))
    val remoteFoods = localWorld.foods.map(f => Food(f.id, f.x, f.y, f.mass))
    World(localWorld.id, localWorld.width, localWorld.height, remotePlayers, remoteFoods)
