package it.unibo.raga

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.raga.controller.ClientActor
import it.unibo.raga.controller.ClientActor.LocalClientEvent

object BobClientLauncher:

  def main(args: Array[String]): Unit =
    startUp("Bob")

object AliceClientLauncher:

  def main(args: Array[String]): Unit =
    startUp("Alice")

def startUp(name: String): Unit =
  val dynamicConfigString =
    s"""
      akka.remote.artery.canonical.hostname = "$LOCALHOST"
      akka.remote.artery.canonical.port = "0"
    """
  val config = ConfigFactory
    .parseString(dynamicConfigString)
    .withFallback(ConfigFactory.load())

  ActorSystem[ClientEvent | LocalClientEvent](ClientActor(name), ACTOR_SYSTEM_NAME, config)
