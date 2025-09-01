package it.unibo

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import akka.cluster.ClusterEvent._
import akka.actor.typed.Behavior
import com.typesafe.config.ConfigFactory

import akka.actor.typed.ActorRef
import akka.cluster.Member
import scala.concurrent.duration.DurationInt
import akka.cluster.ClusterEvent
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.receptionist.Receptionist
import it.unibo.agar.servers.mother.MotherServer
import it.unibo.agar.servers.child.ChildServer
import it.unibo.agar.servers.mother.MotherServer.MotherEvent
import it.unibo.agar.servers.child.ChildServer.ChildEvent
import it.unibo.agar.client.controller.ClientActor.ClientEvent
import scala.annotation.meta.companionClass
import it.unibo.agar.client.controller.ClientActor
import it.unibo.agar.client.view.View

private enum Role(val value: String):

  case Client extends Role("client")
  case Mother extends Role("mother")
  case Child extends Role("child")

object Main:

  val ACTOR_SYSTEM_NAME = "GameCluster"

  def main(args: Array[String]): Unit =
    if args.length == 3 then
      val hostname = args(0)
      val port = args(1)
      val role = args(2)
      startup(hostname, port, role)
    else
      println("Usage: run <host> <port> <role> [seed-nodes...]")
      sys.exit(1)

  private def startup(hostname: String, port: String, role: String): Unit =
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$hostname"
        akka.remote.artery.canonical.port = "$port"
        akka.cluster.roles = ["$role"]
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    import Role.*
    role match
      case Mother.value => ActorSystem[MotherEvent](MotherServer(), ACTOR_SYSTEM_NAME, config)
      case Child.value => ActorSystem[ChildEvent](ChildServer(), ACTOR_SYSTEM_NAME, config)
      case Client.value => ActorSystem[ClientEvent](ClientActor(), ACTOR_SYSTEM_NAME, config)
      case _ =>
        println("Role not available...")
        sys.exit(1)
