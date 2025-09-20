package it.unibo.mother

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ConfigParameters.MOTHER_PORT
import it.unibo.protocol.MotherEvent

object MotherLauncher:

  def main(args: Array[String]): Unit =
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$MOTHER_PORT"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[MotherEvent](MotherActor(), ACTOR_SYSTEM_NAME, config)
