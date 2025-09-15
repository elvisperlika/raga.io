package it.unibo.raga

import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ConfigParameters.LOCALHOST
import it.unibo.protocol.ClientEvent
import it.unibo.raga.controller.ClientActor
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import akka.actor.typed.ActorSystem
import it.unibo.protocol.ConfigParameters.CLIENT_1_PORT
import it.unibo.raga.controller.ClientActor.LocalClientEvent

object ClientLauncher:

  def main(args: Array[String]): Unit =
    var port = CLIENT_1_PORT.toString
    if args.size == 1 then port = args(0)
    val dynamicConfigString =
      s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
    val config = ConfigFactory
      .parseString(dynamicConfigString)
      .withFallback(ConfigFactory.load())
    ActorSystem[ClientEvent | LocalClientEvent](ClientActor(), ACTOR_SYSTEM_NAME, config)
