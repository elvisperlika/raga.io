package it.unibo

import akka.actor.typed.ActorSystem
import akka.cluster.ClusterEvent.*
import com.typesafe.config.ConfigFactory
import it.unibo.agar.client.controller.ClientActor
import it.unibo.agar.client.controller.ClientActor.ClientEvent
import it.unibo.agar.servers.child.ChildServer
import it.unibo.agar.servers.child.ChildServer.ChildEvent
import it.unibo.agar.servers.mother.MotherServer
import it.unibo.agar.servers.mother.MotherServer.MotherEvent

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
      println("Usage: run <host> <port> <role>")
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
        println("Role not available... use <main>, <child> or <client>.")
        sys.exit(1)
