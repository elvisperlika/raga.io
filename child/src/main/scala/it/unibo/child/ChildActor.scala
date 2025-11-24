package it.unibo.child

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.protocol.*
import it.unibo.protocol.ConfigParameters.DEFAULT_PLAYER_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_HEIGHT
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_WIDTH
import it.unibo.protocol.ConfigParameters.INIT_FOOD_NUMBER
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY

import scala.concurrent.duration.DurationInt

private case class Tick() extends ChildEvent

object ChildActor:

  def apply(): Behavior[ChildEvent] = Behaviors.setup: ctx =>
    // ctx.log.info("🤖 Child node Up")
    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CHILD_SERVICE_KEY, ctx.self)
    Behaviors.receiveMessage {
      case SetUp(worldId, motherRef, backupRef) =>
        work(
          world = World(
            id = worldId, width = DEFAULT_WORLD_WIDTH, height = DEFAULT_WORLD_HEIGHT,
            players = Seq.empty, foods = WorldUpdater.generateFoods(INIT_FOOD_NUMBER)
          ),
          managedPlayers = Map.empty,
          motherRef = motherRef,
          backupRef = backupRef
        )
      case _ =>
        // ctx.log.warn(s"🤖 Received unexpected message in setup state")
        Behaviors.same
    }

  def work(
      world: World,
      managedPlayers: Map[ID, ActorRef[ClientEvent]],
      motherRef: ActorRef[MotherEvent],
      backupRef: ActorRef[BackupEvent],
      playersDirections: Map[ID, (Double, Double)] = Map.empty
  ): Behavior[ChildEvent] = Behaviors.withTimers: timer =>
    timer.startTimerAtFixedRate(Tick(), 10.millis)
    Behaviors.receive: (ctx, msg) =>
      msg match
        case Tick() if managedPlayers.nonEmpty =>
          val updatedWorld = playersDirections.foldLeft(world): (w, pd) =>
            val (id, (dx, dy)) = pd
            WorldUpdater.tick(w, id, (dx, dy))
          val eatenPlayers =
            managedPlayers.filterKeys(id => !updatedWorld.players.exists(_.id == id))
          eatenPlayers.foreach: (_, ref) =>
            ref ! EndGame()
          val managedPlayersUpdated =
            managedPlayers.filterKeys(id => updatedWorld.players.exists(_.id == id)).toMap
          managedPlayersUpdated.foreach: (_, ref) =>
            ref ! ReceivedRemoteWorld(updatedWorld)
          backupRef ! SaveWorldData(ctx.self, updatedWorld, managedPlayersUpdated)
          work(updatedWorld, managedPlayersUpdated, motherRef, backupRef, playersDirections)

        case Tick() =>
          Behaviors.same

        case PlayerMove(id, dirX, dirY) =>
          // ctx.log.info(s"🤖 Received movement from player $id: ($dirX, $dirY)")
          work(
            world,
            managedPlayers,
            motherRef,
            backupRef,
            playersDirections + (id -> (dirX, dirY))
          )

        case RequestWorld(nickName, replyTo, playerRef) =>
          // ctx.log.info(s"🤖 Player $nickName requested to join the world ${world.id}")
          val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
          val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
          val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
          val newWorld = world.copy(players = world.players :+ newPlayer)
          replyTo ! RemoteWorld(newWorld, newPlayer)
          work(newWorld, managedPlayers + (nickName -> playerRef), motherRef, backupRef)

        case ChildClientLeft(client) =>
          // ctx.log.info(s"🤖 A client has left: ${client.path}")
          val playerId = managedPlayers.find(p => p._2 == client)
          playerId match
            case Some(player) =>
              val newManagedPlayers = managedPlayers.filterNot(_._1 == player._1)
              val newWorldPlayers = world.players.filterNot(_.id == player._1)
              val newWorld = world.copy(players = newWorldPlayers)
              newManagedPlayers.foreach: (_, ref) =>
                ref ! ReceivedRemoteWorld(newWorld)
              work(newWorld, newManagedPlayers, motherRef, backupRef)

            case None =>
              // ctx.log.info(s"🤖 PLAYER ID NOT FOUND")
              work(world, managedPlayers, motherRef, backupRef)

        case EatenPlayer(playerId) =>
          // ctx.log.info(s"🤖 Player $playerId has been eaten and will be removed from the world")
          val newWorldPlayers = world.players.filterNot(_.id == playerId)
          val newWorld = world.copy(players = newWorldPlayers)
          managedPlayers.foreach: (_, ref) =>
            ref ! ReceivedRemoteWorld(newWorld)
          managedPlayers.find(_._1 == playerId) match
            case Some((_, ref)) => ref ! EndGame()
            case None => // ctx.log.info(s"🤖 PLAYER ID NOT FOUND: $playerId")
          work(newWorld, managedPlayers, motherRef, backupRef)

        case RequestWorldInRoom(nickName, roomCode, clientRef) =>
          // ctx.log.info(s"🤖 Player $nickName requests to join room $roomCode")
          motherRef ! ClientAskToJoinRoom(clientRef, roomCode, nickName, ctx.self)
          Behaviors.same

        case RequestPrivateRoom(clientNickName, clientRef) =>
          // ctx.log.info(s"🤖 Player $clientNickName request to create a private room")
          motherRef ! RequestPrivateRoomCreation(clientRef, clientNickName)
          Behaviors.same

        case PlayerJoinedRoom(nickName, client) =>
          // ctx.log.info(s"🎉 New player $nickName joined this room!")
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
          work(newWorld, newManagedPlayers, motherRef, backupRef)

        case NewSetUp(world, clients) =>
          // ctx.log.info(s"🤖 Setting up new world ${world.id} with ${clients.size} clients")
          clients.foreach: client =>
            val player = world.players.find(_.id == client._1).get
            client._2 ! NewManager(ctx.self, world, player)
          // ctx.log.info(s"🤖 Received New SetUp with ${clients.size} clients")
          work(world, clients, motherRef, backupRef)
