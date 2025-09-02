package it.unibo.child

import it.unibo.protocol.ConfigParameters.LOCALHOST
import com.typesafe.config.ConfigFactory
import akka.actor.typed.ActorSystem
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.agar.servers.child.ChildServer
import it.unibo.protocol.ConfigParameters.CHILD_1_PORT

object ChildServerApp:

  def main(args: Array[String]): Unit =
    val port = CHILD_1_PORT
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[ChildEvent](ChildServer(), ACTOR_SYSTEM_NAME, config)
