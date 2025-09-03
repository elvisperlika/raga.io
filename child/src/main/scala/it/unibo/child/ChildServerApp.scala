package it.unibo.child

import it.unibo.protocol.ConfigParameters.LOCALHOST
import com.typesafe.config.ConfigFactory
import akka.actor.typed.ActorSystem
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME

import it.unibo.protocol.ConfigParameters.CHILD_1_PORT

object ChildServerApp:

  def main(args: Array[String]): Unit =
    var port = CHILD_1_PORT.toString
    if args.size == 1 then port = args(0)
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[ChildEvent](ChildServer(), ACTOR_SYSTEM_NAME, config)
