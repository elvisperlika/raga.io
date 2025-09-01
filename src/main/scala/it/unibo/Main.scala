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
object RootBehavior:

  def apply(role: String): Behavior[Nothing] =
    Behaviors.setup: ctx =>
      role match
        case "main" =>
          ctx.spawn(MotherServer(), "MainServer")
          Behaviors.same
        case "child" =>
          ctx.spawn(ChildServer(), "ChildServer")
          Behaviors.same

private case class Node(hostname: String, port: Int, role: String)

object Main:

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
    ActorSystem[Nothing](RootBehavior(role), "GameCluster", config)
