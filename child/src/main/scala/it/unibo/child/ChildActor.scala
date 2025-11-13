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
import akka.actor.typed.scaladsl.AskPattern.*
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
import scala.concurrent.duration.DurationInt

import it.unibo.protocol._
import akka.util.Timeout
import akka.actor.typed.Scheduler
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure

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

      case RequestWorldInRoom(nickName, roomCode, clientReplyTo, playerRef) =>
        if roomCode == world.id then
          ctx.log.info(s"🤖 Player $nickName tried to join room $roomCode but this is room ${world.id}")
          val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
          val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
          val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
          val newWorld = world.copy(players = world.players :+ newPlayer)
          clientReplyTo ! (ctx.self, RemoteWorld(newWorld, Player(nickName, 0, 0, 0)))
          work(newWorld, managedPlayers + (nickName -> playerRef), motherRef)
        else
          ctx.log.info(s"🤖 Player $nickName joining room $roomCode from room ${world.id}")
          given Timeout = 3.seconds
          given Scheduler = ctx.system.scheduler
          given ExecutionContext = ctx.executionContext
          // motherRef ! JoinFriendsRoom(playerRef, roomCode, nickName)
          motherRef
            .ask[Boolean](childReplyTo =>
              JoinFriendsRoom(playerRef, roomCode, nickName, childReplyTo)
            )
            .onComplete {
              case Success(true) =>
                var managedPlayersUpdated = managedPlayers.removed(nickName)
                work(world, managedPlayersUpdated, motherRef)
              case Failure(ex) =>
                ctx.log.error(s"🤖Failed to join friends room: ${ex.getMessage}", ex)
                clientReplyTo ! CodeNotFound()
                work(world, managedPlayers, motherRef)
              case Success(false) =>
                ctx.log.error(s"🤖Failed to join friends room")
                clientReplyTo ! CodeNotFound()
                work(world, managedPlayers, motherRef)
            }
          Behaviors.same

      case CreateFriendsRoom(nickName, client) =>
        val roomId = java.util.UUID.randomUUID().toString.take(6)
        ctx.log.info(s"🏠 Child ${ctx.self.path} creating friends room $roomId for $nickName (${client.path})")

        val newWorld = World(
          id = roomId, width = DEFAULT_WORLD_WIDTH, height = DEFAULT_WORLD_HEIGHT, players = Seq.empty,
          foods = generateFoods(INIT_FOOD_NUMBER)
        )

        val ownerNick = "owner"
        val randX = scala.util.Random.nextDouble() * (newWorld.width - DEFAULT_PLAYER_SIZE)
        val randY = scala.util.Random.nextDouble() * (newWorld.height - DEFAULT_PLAYER_SIZE)
        val ownerPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)

        val updatedWorld = newWorld.copy(players = Seq(ownerPlayer))
        val updatedManagedPlayers = Map(nickName -> client)

        // val dummyPlayer = Player("owner", 50, 50, DEFAULT_PLAYER_SIZE)
        client ! InitWorld(updatedWorld, ownerPlayer, ctx.self)
        client ! ReceivedRemoteWorld(updatedWorld)

        motherRef ! RoomCreated(roomId, ctx.self, client)
        work(updatedWorld, updatedManagedPlayers, motherRef)

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
