package it.unibo.child

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.RequestWorld
import it.unibo.protocol.ServiceKeys.CHILD_SERVICE_KEY
import it.unibo.protocol.World
import it.unibo.protocol.Player
import it.unibo.protocol.RemoteWorld
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_WIDTH
import it.unibo.protocol.ConfigParameters.DEFAULT_WORLD_HEIGHT
import it.unibo.protocol.ConfigParameters.DEFAULT_PLAYER_SIZE
import it.unibo.protocol.Food
import it.unibo.protocol.ConfigParameters.CENTER_X
import it.unibo.protocol.ConfigParameters.CENTER_Y
import it.unibo.protocol.ConfigParameters.DEFAULT_FOOD_SIZE

object ChildActor:

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
    work(world = World(DEFAULT_WORLD_WIDTH, DEFAULT_WORLD_HEIGHT, Seq.empty, foods))

  def work(world: World): Behavior[ChildEvent] = Behaviors.receive: (ctx, msg) =>
    msg match
      case RequestWorld(nickName, replyTo) =>
        ctx.log.info(s"World requested by ${replyTo.path} with nickname $nickName")
        // TODO: find empty space in the world to spawn the player
        val newPlayer = Player(nickName, CENTER_X, CENTER_Y, DEFAULT_PLAYER_SIZE)
        val newWorld = world.copy(players = world.players :+ newPlayer)
        replyTo ! RemoteWorld(newWorld, newPlayer)
        work(newWorld)
