package it.unibo.child

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.DEFAULT_FOOD_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_PLAYER_SIZE
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_HEIGHT
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_WIDTH
import it.unibo.protocol.Food
import it.unibo.protocol.ID
import it.unibo.protocol.Player
import it.unibo.protocol.ReceivedRemoteWorld
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.RequestRemoteWorldUpdate
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY
import it.unibo.protocol.World

object ChildActor:

  case class SendRemoteWorldUpdate() extends ChildEvent

  def apply(): Behavior[ChildEvent] = Behaviors.setup: ctx =>
    ctx.log.info("🤖 Child node Up")
    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CHILD_SERVICE_KEY, ctx.self)

    val foods = (1 to 100).map(i =>
      Food(
        s"food-$i",
        scala.util.Random.nextDouble() * DEFAULT_WORLD_WIDTH,
        scala.util.Random.nextDouble() * DEFAULT_WORLD_HEIGHT,
        DEFAULT_FOOD_SIZE
      )
    )
    work(world = World(DEFAULT_WORLD_WIDTH, DEFAULT_WORLD_HEIGHT, Seq.empty, foods), players = Map.empty)

  def work(world: World, players: Map[ID, ActorRef[ClientEvent]]): Behavior[ChildEvent] = Behaviors.receive:
    (ctx, msg) =>
      msg match
        case RequestWorld(nickName, replyTo, playerRef) =>
          ctx.log.info(s"🤖 World requested by ${replyTo.path} with nickname $nickName")
          // TODO: find empty space in the world to spawn the player
          val randX = scala.util.Random.nextDouble() * (world.width - DEFAULT_PLAYER_SIZE)
          val randY = scala.util.Random.nextDouble() * (world.height - DEFAULT_PLAYER_SIZE)
          val newPlayer = Player(nickName, randX, randY, DEFAULT_PLAYER_SIZE)
          val newWorld = world.copy(players = world.players :+ newPlayer)
          replyTo ! RemoteWorld(newWorld, newPlayer)
          work(newWorld, players + (nickName -> playerRef))

        case RequestRemoteWorldUpdate(updatedWorld, (playerId, playerRef)) =>
          ctx.log.info(s"🤖 World update received for player $playerId")
          val mergedWorld = mergeWorlds(world, updatedWorld, playerId)

          ctx.spawnAnonymous(Behaviors.setup[Nothing] { anonymousCtx =>
            if players.nonEmpty then
              anonymousCtx.log.info(s"🤖🤖🤖 Sending world update to players")
              players.foreach: (_, ref) =>
                ref ! ReceivedRemoteWorld(mergedWorld)
            Behaviors.stopped
          })
          work(mergedWorld, players)

  def mergeWorlds(oldWorld: World, newWorld: World, playerId: ID): World =
    val otherPlayers = oldWorld.players.filterNot(_.id == playerId)
    val requestingPlayer = newWorld.players.filter(_.id == playerId)
    World(
      width = oldWorld.width,
      height = oldWorld.height,
      players = otherPlayers ++ requestingPlayer,
      foods = newWorld.foods
    )
