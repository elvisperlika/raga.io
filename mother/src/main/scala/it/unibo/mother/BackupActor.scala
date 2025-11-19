package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.protocol.BackupEvent
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.SaveWorldData
import it.unibo.protocol.World

object BackupActor:

  case class FollowChild(clientRef: ActorRef[ChildEvent]) extends BackupEvent
  case class UnfollowChild(clientRef: ActorRef[ChildEvent]) extends BackupEvent
  case class Backup() extends BackupEvent
  case class Log(msg: String) extends BackupEvent
  case class RequestBackup(child: ActorRef[ChildEvent], replyTo: ActorRef[SaveWorldData])
      extends BackupEvent
  case class BackupCompleted(
      newBackups: Map[ActorRef[ChildEvent], (World, Map[String, ActorRef[ClientEvent]])]
  ) extends BackupEvent

  def apply(motherRef: ActorRef[MotherEvent]): Behavior[BackupEvent] =
    Behaviors.setup: ctx =>
      ctx.log.info("🗄️ Backup Actor is Up")
      work(backups = Map.empty)

  private def work(
      backups: Map[ActorRef[ChildEvent], (World, Map[String, ActorRef[ClientEvent]])]
  ): Behavior[BackupEvent] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case FollowChild(childRef) =>
          ctx.log.info(s"🗄️ Following child: ${childRef.path}")
          work(backups + (childRef -> null))

        case UnfollowChild(childRef) =>
          ctx.log.info(s"🗄️ Unfollowing child: ${childRef.path}")
          work(backups - childRef)

        case SaveWorldData(sender, world, managedPlayers) =>
          ctx.self ! Log(s"🗄️ ${sender.path} its backuped.")
          ctx.self ! Log(
            s"✅🗄️ Backup details: World ID = ${world.id}, " +
              s"Players = ${managedPlayers.keys.mkString(", ")}"
          )
          val newBackups = backups + (sender -> (world, managedPlayers))
          work(newBackups)

        case RequestBackup(child, replyTo) =>
          ctx.log.info(s"🗄️ Received backup request for child -> ${child.path}")
          val backup = backups.find(b => b._1.path == child.path)
          backup match
            case Some(backup) =>
              ctx.log.info(s"✅🗄️ Providing backup for child -> ${child.path}")
              val (world, managedPlayers) = backup._2
              ctx.log.info(s"✅🗄️ Backup details: World ID = ${world.id}, Players = ${managedPlayers.keys.mkString(", ")}")
              replyTo ! SaveWorldData(child, world, managedPlayers)
            case _ =>
              ctx.log.info(s"❌🗄️ No backup available for child -> ${child.path}")
          Behaviors.same

        case Log(msg) =>
          ctx.log.info(msg)
          Behaviors.same
