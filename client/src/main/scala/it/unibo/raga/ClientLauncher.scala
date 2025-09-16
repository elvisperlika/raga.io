package it.unibo.raga

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.CLIENT_1_PORT
import it.unibo.protocol.ConfigParameters.CLIENT_2_PORT
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.raga.controller.ClientActor
import it.unibo.raga.controller.ClientActor.LocalClientEvent

object BobClientLauncher:

  def main(args: Array[String]): Unit =
    startUp(CLIENT_1_PORT, "Bob")

object AliceClientLauncher:

  def main(args: Array[String]): Unit =
    startUp(CLIENT_2_PORT, "Alice")

def startUp(port: Int, name: String): Unit =
  val dynamicConfigString =
    s"""
      akka.remote.artery.canonical.hostname = "$LOCALHOST"
      akka.remote.artery.canonical.port = "$port"
    """
  val config = ConfigFactory
    .parseString(dynamicConfigString)
    .withFallback(ConfigFactory.load())
  ActorSystem[ClientEvent | LocalClientEvent](ClientActor(name), ACTOR_SYSTEM_NAME, config)
