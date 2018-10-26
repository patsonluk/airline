package models

import akka.actor._
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class ClientActor(out: ActorRef, chat: ActorRef) extends Actor {

  chat ! Join

  override def postStop() = chat ! Leave

  def receive = {
    // this handles messages from the websocket
    case text: String =>
	  val sdf = new SimpleDateFormat("hh:mm:ss")
      chat ! ClientSentMessage("[" + sdf.format(Calendar.getInstance().getTime())+ "] " + text)

    case ClientSentMessage(text) =>
      out ! text
  }
}