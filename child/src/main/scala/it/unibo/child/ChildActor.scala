package it.unibo.child

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.protocol.*
import it.unibo.protocol.ConfigParameters.DEFAULT_FOOD_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_PLAYER_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_HEIGHT
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_WIDTH
import it.unibo.protocol.ConfigParameters.INIT_FOOD_NUMBER
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY

object ChildActor:

  def apply(): Behavior[ChildEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🤖 Child node Up")
    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CHILD_SERVICE_KEY, ctx.self)
    Behaviors.receiveMessage {
      case SetUp(worldId, motherRef) =>
        work(
          world = World(
            id = worldId, width = DEFAULT_WORLD_WIDTH, height = DEFAULT_WORLD_HEIGHT,
            players = Seq.empty, foods = generateFoods(INIT_FOOD_NUMBER)
          ),
          managedPlayers = Map.empty,
          motherRef = motherRef
        )
      case _ =>
        ctx.log.warn(s"🤖 Received unexpected message in setup state")
        Behaviors.same
    }

  def work(
      world: World,
      managedPlayers: Map[ID, ActorRef[ClientEvent]],
      motherRef: ActorRef[MotherEvent]
  ): Behavior[ChildEvent] = Behaviors.receive: (ctx, msg) =>
    msg match
      case RequestWorld(nickName, replyTo, playerRef) =>
        ctx.log.info(s"🤖 Player $nickName requested to join the world ${world.id}")
        val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
        val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
        val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
        val newWorld = world.copy(players = world.players :+ newPlayer)
        replyTo ! RemoteWorld(newWorld, newPlayer)
        work(newWorld, managedPlayers + (nickName -> playerRef), motherRef)

      case RequestRemoteWorldUpdate(updatedWorld, (playerId, playerRef)) =>
        ctx.log.info(s"->🤖<- Received world update from player $playerId")
        val mergedWorld = mergeWorlds(world, updatedWorld, playerId)
        ctx.spawnAnonymous(Behaviors.setup[Nothing] { _ =>
          if managedPlayers.nonEmpty then
            managedPlayers.foreach: (_, ref) =>
              ref ! ReceivedRemoteWorld(mergedWorld)
          Behaviors.stopped
        })
        work(mergedWorld, managedPlayers, motherRef)

      case ChildClientLeft(client) =>
        ctx.log.info(s"🤖 A client has left: ${client.path}")
        val playerId = managedPlayers.find(p => p._2 == client)
        playerId match
          case Some(player) =>
            val newManagedPlayers = managedPlayers.filterNot(_._1 == player._1)
            val newWorldPlayers = world.players.filterNot(_.id == player._1)
            val newWorld = world.copy(players = newWorldPlayers)
            newManagedPlayers.foreach: (_, ref) =>
              ref ! ReceivedRemoteWorld(newWorld)
            work(newWorld, newManagedPlayers, motherRef)

          case None =>
            ctx.log.info(s"🤖 PLAYER ID NOT FOUND")
            work(world, managedPlayers, motherRef)

      case EatenPlayer(playerId) =>
        ctx.log.info(s"🤖 Player $playerId has been eaten and will be removed from the world")
        val newWorldPlayers = world.players.filterNot(_.id == playerId)
        val newWorld = world.copy(players = newWorldPlayers)
        managedPlayers.foreach: (_, ref) =>
          ref ! ReceivedRemoteWorld(newWorld)
        managedPlayers.find(_._1 == playerId) match
          case Some((_, ref)) => ref ! EndGame()
          case None => ctx.log.info(s"🤖 PLAYER ID NOT FOUND: $playerId")
        work(newWorld, managedPlayers, motherRef)

      case RequestWorldInRoom(nickName, roomCode, clientRef) =>
        ctx.log.info(s"🤖 Player $nickName requests to join room $roomCode")
        motherRef ! ClientAskToJoinRoom(clientRef, roomCode, nickName, ctx.self)
        Behaviors.same

      case RequestPrivateRoom(clientNickName, clientRef) =>
        ctx.log.info(s"🤖 Player $clientNickName request to create a private room")
        motherRef ! RequestPrivateRoomCreation(clientRef, clientNickName)
        Behaviors.same

      case PlayerJoinedRoom(nickName, client) =>
        ctx.log.info(s"🎉 New player $nickName joined this room!")
        val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
        val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
        val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
        val newWorld = world.copy(players = world.players :+ newPlayer)
        val newManagedPlayers = managedPlayers + (nickName -> client)

        client ! InitWorld(newWorld, newPlayer, ctx.self)

        managedPlayers.foreach {
          case (id, ref) if id != nickName =>
            ref ! NewPlayerJoined(newPlayer)
          case _ =>
        }
        work(newWorld, newManagedPlayers, motherRef)

      case NewSetUp(world, clients) =>
        ctx.log.info(s"🤖 Setting up new world ${world.id} with ${clients.size} clients")
        clients.foreach: client =>
          val player = world.players.find(_.id == client._1).get
          client._2 ! NewManager(ctx.self, world, player)
        ctx.log.info(s"🤖 Received New SetUp with ${clients.size} clients")
        work(world, clients, motherRef)

      case RequestWorldToBackup(replyTo) =>
        ctx.log.info(s"🤖🤖🤖🤖 Received request to backup the world ${world.id}")
        replyTo ! WorldToBackup(world, managedPlayers)
        Behaviors.same

  /** Merges two worlds by keeping all players and foods, ensuring the requesting player's data is
    * updated.
    *
    * @param oldWorld
    *   Current world state
    * @param newWorld
    *   Updated world state from a player
    * @param playerId
    *   ID of the player requesting the update
    * @return
    *   Merged world state
    */
  def mergeWorlds(oldWorld: World, newWorld: World, playerId: ID): World =
    val otherPlayers = oldWorld.players.filterNot(_.id == playerId)
    val requestingPlayer = newWorld.players.filter(_.id == playerId)
    val existingFoodIds = newWorld.foods.map(_.id).toSet
    val extraFoods =
      generateFoods(INIT_FOOD_NUMBER).filterNot(food => existingFoodIds.contains(food.id))
    World(
      id = oldWorld.id,
      width = DEFAULT_WORLD_WIDTH,
      height = DEFAULT_WORLD_HEIGHT,
      players = otherPlayers ++ requestingPlayer,
      foods = newWorld.foods ++ extraFoods
    )

  /** Generates n food items at random positions within the world bounds.
    *
    * @param n
    *   Number of food items to generate
    * @return
    *   Sequence of generated Food items
    */
  def generateFoods(n: Int): Seq[Food] =
    (1 to n) map (i =>
      Food(
        s"food-$i",
        scala.util.Random.nextDouble() * DEFAULT_WORLD_WIDTH,
        scala.util.Random.nextDouble() * DEFAULT_WORLD_HEIGHT,
        DEFAULT_FOOD_SIZE
      )
    )
