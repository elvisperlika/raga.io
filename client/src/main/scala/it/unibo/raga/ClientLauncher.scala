package it.unibo.raga

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ConfigParameters.RANDOM_PORT
import it.unibo.raga.controller.ClientActor
import it.unibo.raga.controller.ClientActor.LocalClientEvent

object ClientLauncher1:

  def main(args: Array[String]): Unit =
    startUp()

object ClientLauncher2:

  def main(args: Array[String]): Unit =
    startUp()

object ClientLauncher3:

  def main(args: Array[String]): Unit =
    startUp()

object Spawn5Bots:

  def main(args: Array[String]): Unit =
    for _ <- 1 to 5 do startUp(isBot = true)

object Spawn10Bots:

  def main(args: Array[String]): Unit =
    for _ <- 1 to 10 do startUp(isBot = true)

def startUp(isBot: Boolean = false): Unit =
  val dynamicConfigString =
    s"""
      akka.remote.artery.canonical.hostname = "$LOCALHOST"
      akka.remote.artery.canonical.port = "$RANDOM_PORT"
    """
  val config = ConfigFactory
    .parseString(dynamicConfigString)
    .withFallback(ConfigFactory.load())

  ActorSystem[ClientEvent | LocalClientEvent](ClientActor(isBot), ACTOR_SYSTEM_NAME, config)
