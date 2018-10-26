package models

import akka.actor._
import play.api.Logger

// our domain message protocol
case object Join
case object Leave
final case class ClientSentMessage(text: String)

// Chat actor
class Chat extends Actor {
  // initial message-handling behavior
  def receive = process(Set.empty)

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join =>
      // replaces message-handling behavior by the new one
      context become process(subscribers + sender)
	  Logger.info("Chat socket connected")

    case Leave =>
      context become process(subscribers - sender)
	  Logger.info("Chat socket disconnected")

    case msg: ClientSentMessage =>
      // send messages to all subscribers except sender
	  (subscribers).foreach { _ ! msg }
	  Logger.info("Message:" + msg)
  }
}