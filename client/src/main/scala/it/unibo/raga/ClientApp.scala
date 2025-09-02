package it.unibo.raga

import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ClientEvent
import it.unibo.raga.controller.ClientActor
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import akka.actor.typed.ActorSystem
import it.unibo.protocol.ConfigParameters.CLIENT_1_PORT

object ClientApp:

  def main(args: Array[String]): Unit =
    val port = CLIENT_1_PORT
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[ClientEvent](ClientActor(), ACTOR_SYSTEM_NAME, config)
