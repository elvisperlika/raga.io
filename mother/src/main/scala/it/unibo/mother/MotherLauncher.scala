package it.unibo.mother

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ConfigParameters.MOTHER_PORT
import it.unibo.protocol.MotherEvent

object MotherLauncher:

  def main(args: Array[String]): Unit =
    var port = MOTHER_PORT.toString
    if args.size == 1 then port = args(0)
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[MotherEvent](MotherActor(), ACTOR_SYSTEM_NAME, config)
