package it.unibo.raga

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ConfigParameters.RANDOM_PORT
import it.unibo.raga.controller.ClientActor
import it.unibo.raga.controller.ClientActor.LocalClientEvent

object Run10Clients:

  def main(args: Array[String]): Unit =
    (1 to 10) foreach (_ => ClientLauncher1.main(args))

object ClientLauncher1:

  def main(args: Array[String]): Unit =
    startUp()

object ClientLauncher2:

  def main(args: Array[String]): Unit =
    startUp()

def startUp(): Unit =
  val dynamicConfigString =
    s"""
      akka.remote.artery.canonical.hostname = "$LOCALHOST"
      akka.remote.artery.canonical.port = "$RANDOM_PORT"
    """
  val config = ConfigFactory
    .parseString(dynamicConfigString)
    .withFallback(ConfigFactory.load())

  ActorSystem[ClientEvent | LocalClientEvent](ClientActor(), ACTOR_SYSTEM_NAME, config)
