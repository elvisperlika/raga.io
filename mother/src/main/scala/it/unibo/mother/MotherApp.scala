package it.unibo.mother

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.ConfigParameters.MOTHER_PORT

object MotherApp:

  def main(args: Array[String]): Unit =
    val port = MOTHER_PORT
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[MotherEvent](MotherActor(), ACTOR_SYSTEM_NAME, config)
