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

object ChildActor:

  def apply(): Behavior[ChildEvent] = Behaviors.setup: ctx =>
    println("🤖 Child Server up")
    val cluster = Cluster(ctx.system)
    ctx.system.receptionist ! Receptionist.Register(CHILD_SERVICE_KEY, ctx.self)

    work(worlds = Seq.empty)

  def work(worlds: Seq[World]): Behavior[ChildEvent] = Behaviors.receiveMessage:
    case RequestWorld(replyTo) =>
      println(s"🤖 World requested by ${replyTo.path}")
      val newPlayer = Player("id", 500, 500, 100) // TODO: remove hardcoded player
      val newWorld: World = World(1000, 1000, players = Seq(newPlayer), Seq.empty)
      replyTo ! RemoteWorld(newWorld, newPlayer)
      work(worlds :+ newWorld)
