package it.unibo.mother

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.protocol.BackupCommand
import it.unibo.protocol.ChildEvent
import it.unibo.protocol.ClientEvent
import it.unibo.protocol.MotherEvent
import it.unibo.protocol.RequestWorldToBackup
import it.unibo.protocol.World
import it.unibo.protocol.WorldToBackup

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Success

object BackupActor:

  case class FollowChild(clientRef: ActorRef[ChildEvent]) extends BackupCommand
  case class UnfollowChild(clientRef: ActorRef[ChildEvent]) extends BackupCommand
  case class Backup() extends BackupCommand
  case class Log(msg: String) extends BackupCommand
  case class RequestBackup(child: ActorRef[ChildEvent], replyTo: ActorRef[WorldToBackup])
      extends BackupCommand
  case class BackupCompleted(
      newBackups: Map[ActorRef[ChildEvent], (World, Map[String, ActorRef[ClientEvent]])]
  ) extends BackupCommand

  def apply(motherRef: ActorRef[MotherEvent]): Behavior[BackupCommand] =
    Behaviors.setup: ctx =>
      ctx.log.info("🗄️ Backup Actor is Up")
      work(backups = Map.empty)

  private def work(
      backups: Map[ActorRef[ChildEvent], (World, Map[String, ActorRef[ClientEvent]])]
  ): Behavior[BackupCommand] =
    Behaviors.withTimers: timer =>
      timer.startTimerAtFixedRate(Backup(), 3.seconds)
      Behaviors.receive: (ctx, msg) =>
        msg match
          case FollowChild(childRef) =>
            ctx.log.info(s"🗄️ Following child: ${childRef.path}")
            work(backups + (childRef -> null))

          case UnfollowChild(childRef) =>
            ctx.log.info(s"🗄️ Unfollowing child: ${childRef.path}")
            work(backups - childRef)

          case Backup() =>
            ctx.log.info(s"🗄️ Performing backup of ${backups.size} children")
            given Timeout = 5.seconds
            given Scheduler = ctx.system.scheduler
            given ExecutionContext = ctx.executionContext

            backups.keys.foreach: childRef =>
              ctx.log.info(s"🗄️ Backing up child: ${childRef.path}")
              childRef
                .ask[WorldToBackup](replyTo => RequestWorldToBackup(replyTo))
                .onComplete(result =>
                  result match
                    case Success(worldBackup) =>
                      ctx.self ! Log(s"🗄️ ${childRef.path} its backuped.")
                      val (world, managedPlayers) = (worldBackup.world, worldBackup.managedPlayers)
                      ctx.self ! Log(
                        s"✅🗄️ Backup details: World ID = ${world.id}, " +
                          s"Players = ${managedPlayers.keys.mkString(", ")}"
                      )
                      val newBackups = backups + (childRef -> (world, managedPlayers))
                      ctx.self ! BackupCompleted(newBackups)
                    case _ =>
                      ctx.self ! Log(s"🗄️ Failed to backup child: ${childRef.path}")
                )
            Behaviors.same

          case BackupCompleted(newBackups) =>
            ctx.log.info(s"🗄️ Backup completed. Updated backups stored.")
            work(newBackups)

          case RequestBackup(child, replyTo) =>
            ctx.log.info(s"🗄️ Received backup request for child -> ${child.path}")
            val backup = backups.find(b => b._1.path == child.path)
            backup match
              case Some(backup) =>
                ctx.log.info(s"✅🗄️ Providing backup for child -> ${child.path}")
                val (world, managedPlayers) = backup._2
                ctx.log.info(s"✅🗄️ Backup details: World ID = ${world.id}, Players = ${managedPlayers.keys.mkString(", ")}")

                replyTo ! WorldToBackup(world, managedPlayers)
              case _ =>
                ctx.log.info(s"❌🗄️ No backup available for child -> ${child.path}")
            Behaviors.same

          case Log(msg) =>
            ctx.log.info(msg)
            Behaviors.same
