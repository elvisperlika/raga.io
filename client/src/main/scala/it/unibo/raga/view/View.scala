package it.unibo.raga.view

import akka.actor.typed.ActorRef
import it.unibo.raga.controller.ClientActor.LocalClientEvent
import it.unibo.raga.utils.Parameters.HEIGHT
import it.unibo.raga.utils.Parameters.WIDTH

import java.awt.Color
import java.awt.Dimension
import scala.swing.MainFrame

class View(clientActor: ActorRef[LocalClientEvent]) extends MainFrame:

  title = "Raga.io"
  preferredSize = new Dimension(WIDTH, HEIGHT)

  enum NetworkStatus(val text: String):

    case Online extends NetworkStatus("Online")
    case Offline extends NetworkStatus("Offline")

  import scala.swing._
  import scala.swing.BorderPanel.Position._

  val networkStatusLabel = makeLabel("")
  showOffline()

  val nicknameTextField = makeTextField("Bob")

  val joinRandomRoomButton = makeButton("Join random battle")

  val createAndJoinRoomButton = makeButton("Create and join a room")

  val roomCodeTextField = makeTextField("1")

  val joinFriendsRoomButton = makeButton("Join friend's room")

  def makeLabel(text: String): Label =
    val label = new Label(text)
    label.font = label.font.deriveFont(java.awt.Font.BOLD)
    label

  def makeButton(text: String): Button =
    val button = new Button(text)
    button.foreground = java.awt.Color.BLUE
    button

  def makeTextField(placeholder: String): TextField =
    new TextField:
      columns = 10
      maximumSize = new Dimension(150, 20)
      text = placeholder
      horizontalAlignment = scala.swing.Alignment.Center

  val panel = new BoxPanel(Orientation.Vertical):
    val bigVSpaceSize = 50
    val smallVSpaceSize = 10

    contents += Swing.VStrut(bigVSpaceSize)
    contents += makeLabel("Enter your nickname (mandatory).")
    contents += makeLabel("GREEN if available, RED otherwise):")
    contents += Swing.VStrut(smallVSpaceSize)
    contents += nicknameTextField
    contents += Swing.VStrut(smallVSpaceSize)
    contents += joinRandomRoomButton

    contents += Swing.VStrut(bigVSpaceSize)
    contents += makeLabel("Create a room and join it!")
    contents += makeLabel("You'll see the room's code on the top-left corner.")
    contents += Swing.VStrut(smallVSpaceSize)
    contents += createAndJoinRoomButton

    contents += Swing.VStrut(bigVSpaceSize)
    contents += makeLabel("Write friend's room code")
    contents += Swing.VStrut(smallVSpaceSize)
    contents += roomCodeTextField
    contents += Swing.VStrut(smallVSpaceSize)
    contents += joinFriendsRoomButton
    contents += Swing.VStrut(smallVSpaceSize)
    contents += Swing.VStrut(smallVSpaceSize)
    contents += networkStatusLabel

    border = Swing.EmptyBorder(30, 30, 30, 30)
    // Center alignment for all components
    contents.foreach(_.xLayoutAlignment = 0.5)

  contents = new BorderPanel:
    layout(panel) = Center

  def showOnline(): Unit =
    networkStatusLabel.text = NetworkStatus.Online.text
    networkStatusLabel.foreground = Color.GREEN

  def showOffline(): Unit =
    networkStatusLabel.text = NetworkStatus.Offline.text
    networkStatusLabel.foreground = Color.RED

  listenTo(joinRandomRoomButton, createAndJoinRoomButton, joinFriendsRoomButton)
  reactions += { case event.ButtonClicked(btn) =>
    btn match
      case `joinRandomRoomButton` => clientActor ! LocalClientEvent.JoinRandomRoom
      case `createAndJoinRoomButton` => clientActor ! LocalClientEvent.CreateAndJoinRoom
      case `joinFriendsRoomButton` => clientActor ! LocalClientEvent.JoinFriendsRoom
  }

  def getNickname(): String = nicknameTextField.text.trim
