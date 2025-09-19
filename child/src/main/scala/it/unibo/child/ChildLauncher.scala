package it.unibo.child

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ConfigParameters.ACTOR_SYSTEM_NAME
import it.unibo.protocol.ConfigParameters.CHILD_1_PORT
import it.unibo.protocol.ConfigParameters.CHILD_2_PORT
import it.unibo.protocol.ConfigParameters.LOCALHOST

object ChildLauncher1:

  def main(args: Array[String]): Unit =
    startUp(CHILD_1_PORT)

object ChildLauncher2:

  def main(args: Array[String]): Unit =
    startUp(CHILD_2_PORT)

private def startUp(port: Int): Unit =
  val dynamicConfigString =
    s"""
        akka.remote.artery.canonical.hostname = "$LOCALHOST"
        akka.remote.artery.canonical.port = "$port"
      """
  val config = ConfigFactory
    .parseString(dynamicConfigString)
    .withFallback(ConfigFactory.load())
  ActorSystem[ChildEvent](ChildActor(), ACTOR_SYSTEM_NAME, config)
