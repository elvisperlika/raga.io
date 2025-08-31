package it.unibo.test

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
import it.unibo.test.ChildServer.ChildEvent
import akka.cluster.ClusterEvent
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.receptionist.Receptionist

trait Event

object MainServer:

  enum MainEvent extends Event:

    case MyMemberEvent(event: MemberEvent)
    case ChildrenUpdated(listing: Receptionist.Listing)

  def apply(): Behavior[MainEvent] =
    Behaviors.setup: ctx =>
      println("😍 Main Server up")
      val cluster = Cluster(ctx.system)
      val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MainEvent.MyMemberEvent.apply)
      cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[ClusterEvent.MemberEvent])

      val listingAdapter = ctx.messageAdapter[Receptionist.Listing](MainEvent.ChildrenUpdated.apply)
      ctx.system.receptionist ! Receptionist.Subscribe(ChildServer.ChildKey, listingAdapter)

      Behaviors.receiveMessage:
        case MainEvent.MyMemberEvent(event) =>
          event match
            case MemberUp(member) if member.hasRole("child") =>
              println(s"😍 Main server sees child node ${member.address} is up. Trying to find the child server...")
              // send to message to member
              Behaviors.same
            case _ =>
              Behaviors.same

        case MainEvent.ChildrenUpdated(ChildServer.ChildKey.Listing(children)) =>
          children.foreach: child =>
            ctx.log.info(s"👉 Sending X to child $child")
            child ! ChildServer.ChildEvent.X
          Behaviors.same

        case _ =>
          Behaviors.same

object ChildServer:

  enum ChildEvent extends Event:

    case X
    case Y

  val ChildKey = ServiceKey[ChildEvent]("ChildServer")

  def apply(): Behavior[ChildEvent] =
    Behaviors.setup: ctx =>
      ctx.system.receptionist ! Receptionist.Register(ChildKey, ctx.self)
      ctx.log.info("🥶 Child Server up")
      Behaviors.receiveMessage:
        case ChildEvent.X | ChildEvent.Y =>
          println("🥶 Child Server received a message from main!")
          Behaviors.same

object RootBehavior:

  def apply(role: String): Behavior[Nothing] =
    Behaviors.setup: ctx =>
      role match
        case "main" =>
          ctx.spawn(MainServer(), "MainServer")
          Behaviors.same
        case "child" =>
          ctx.spawn(ChildServer(), "ChildServer")
          Behaviors.same

object Main:

  def main(args: Array[String]): Unit =
    if args.length < 3 then
      println("Usage: run <host> <port> <role> [seed-nodes...]")
      sys.exit(1)

    val host = args(0)
    val port = args(1)
    val role = args(2)

    // Create a dynamic configuration string
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$host"
        akka.remote.artery.canonical.port = "$port" 
        akka.cluster.roles = ["$role"]
      """

    // Load the base configuration from application.conf and override with dynamic values
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())

    ActorSystem[Nothing](RootBehavior(role), "GameCluster", config)
