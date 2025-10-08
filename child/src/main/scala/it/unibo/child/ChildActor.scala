package it.unibo.child

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.protocol.ChildClientLeft
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.DEFAULT_FOOD_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_PLAYER_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_HEIGHT
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_WIDTH
import it.unibo.protocol.ConfigParameters.INIT_FOOD_NUMBER
import it.unibo.protocol.EatenPlayer
import it.unibo.protocol.Food
import it.unibo.protocol.ID
import it.unibo.protocol.Player
import it.unibo.protocol.ReceivedRemoteWorld
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.RequestRemoteWorldUpdate
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY
import it.unibo.protocol.SetUp
import it.unibo.protocol.World
import it.unibo.protocol.EndGame

import it.unibo.protocol._

object ChildActor:

  def apply(): Behavior[ChildEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🤖 Child node Up")
    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CHILD_SERVICE_KEY, ctx.self)
    Behaviors.receiveMessage {
      case SetUp(worldId, motherRef) =>
        work(
          world = World(
            id = worldId, width = DEFAULT_WORLD_WIDTH, height = DEFAULT_WORLD_HEIGHT, players = Seq.empty,
            foods = generateFoods(INIT_FOOD_NUMBER)
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
        val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
        val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
        val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
        val newWorld = world.copy(players = world.players :+ newPlayer)
        replyTo ! RemoteWorld(newWorld, newPlayer)
        work(newWorld, managedPlayers + (nickName -> playerRef), motherRef)

      case RequestRemoteWorldUpdate(updatedWorld, (playerId, playerRef)) =>
        val mergedWorld = mergeWorlds(world, updatedWorld, playerId)
        ctx.spawnAnonymous(Behaviors.setup[Nothing] { anonymousCtx =>
          if managedPlayers.nonEmpty then
            managedPlayers.foreach: (_, ref) =>
              ref ! ReceivedRemoteWorld(mergedWorld)
          Behaviors.stopped
        })
        work(mergedWorld, managedPlayers, motherRef)

      case ChildClientLeft(client) =>
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
        val newWorldPlayers = world.players.filterNot(_.id == playerId)
        val newWorld = world.copy(players = newWorldPlayers)
        managedPlayers.foreach: (_, ref) =>
          ref ! ReceivedRemoteWorld(newWorld)
        managedPlayers.find(_._1 == playerId) match
          case Some((_, ref)) => ref ! EndGame()
          case None => ctx.log.info(s"🤖 PLAYER ID NOT FOUND: $playerId")
        work(newWorld, managedPlayers, motherRef)

      case RequestWorldInRoom(nickName, roomCode, replyTo, playerRef) =>
        if roomCode != world.id then
          ctx.log.info(s"🤖 Player $nickName tried to join room $roomCode but this is room ${world.id}")
          replyTo ! RemoteWorld(World("", 0, 0, Seq.empty, Seq.empty), Player("", 0, 0, 0))
          Behaviors.same
        else
          val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
          val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
          val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)

          val newWorld = world.copy(players = world.players :+ newPlayer)

          val newManagedPlayers = managedPlayers + (nickName -> playerRef)

          replyTo ! RemoteWorld(newWorld, newPlayer)

          motherRef ! JoinFriendsRoom(playerRef, roomCode, nickName)

          work(newWorld, managedPlayers + (nickName -> playerRef), motherRef)

      case CreateFriendsRoom(client: ActorRef[ClientEvent]) =>
        ctx.log.info(s"🏠 Creating a new friends room on this child actor")

        val roomId = java.util.UUID.randomUUID().toString.take(6)

        val newWorld = World(
          id = roomId,
          width = DEFAULT_WORLD_WIDTH,
          height = DEFAULT_WORLD_HEIGHT,
          players = Seq.empty,
          foods = generateFoods(INIT_FOOD_NUMBER)
        )
        client ! FriendsRoomCreated(roomId)
        client ! InitWorld(newWorld, Player("", 0, 0, 0)) 
        client ! GamaManagerAddress(ctx.self)

        motherRef ! RoomCreated(roomId, ctx.self, client)
        
        work(newWorld, Map.empty, motherRef)

      case PlayerJoinedRoom(nickName, client) =>
        ctx.log.info(s"🎉 New player $nickName joined this room!")
        val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
        val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)

        val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)

        val newWorld = world.copy(players = world.players :+ newPlayer)
        val newManagedPlayers = managedPlayers + (nickName -> client)

        // invio al nuovo arrivato lo stato del mondo
        client ! InitWorld(newWorld, newPlayer)

        // invio agli altri client il nuovo player
        managedPlayers.foreach {
          case (id, ref) if id != nickName =>
            ref ! NewPlayerJoined(newPlayer)
          case _ =>
        }

        work(newWorld, newManagedPlayers, motherRef)
        Behaviors.same

  /** Merges two worlds by keeping all players and foods, ensuring the requesting player's data is updated.
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
    val extraFoods = generateFoods(INIT_FOOD_NUMBER).filterNot(food => existingFoodIds.contains(food.id))
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
