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
      work(backups = Map.empty)

  private def work(
      backups: Map[ActorRef[ChildEvent], (World, Map[String, ActorRef[ClientEvent]])]
  ): Behavior[BackupEvent] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case FollowChild(childRef) =>
          work(backups + (childRef -> null))

        case UnfollowChild(childRef) =>
          work(backups - childRef)

        case SaveWorldData(sender, world, managedPlayers) =>
          val newBackups = backups + (sender -> (world, managedPlayers))
          work(newBackups)

        case RequestBackup(child, replyTo) =>
          val backup = backups.find(b => b._1.path == child.path)
          backup match
            case Some(backup) =>
              val (world, managedPlayers) = backup._2
              replyTo ! SaveWorldData(child, world, managedPlayers)
            case _ =>
          Behaviors.same

        case Log(msg) =>
          Behaviors.same
