package models

import akka.actor._
import play.api.Logger
import scala.collection.mutable.Queue

// our domain message protocol
case object Join
case object Leave
final case class ClientSentMessage(text: String)

// Chat actor
class Chat extends Actor {
  // initial message-handling behavior
  var log = Queue[String]()
  
  def receive = process(Set.empty)
	
  def process(subscribers: Set[ActorRef]): Receive = {
    case Join =>
      // replaces message-handling behavior by the new one
      context become process(subscribers + sender)
	  //You can turn these loggers off if needed
	  //Logger.info("Chat socket connected")
	  log.foreach { i => sender ! i }

    case Leave =>
      context become process(subscribers - sender)
	  //You can turn these loggers off if needed
	  //Logger.info("Chat socket disconnected")

    case msg: ClientSentMessage =>
      // Check if replay of log
	  if (msg.text.indexOf("[LOGGED]") > -1) {
		  sender ! msg
	  } else {
		  // send messages to all
		  log.enqueue("[LOGGED]"+ msg.text)
		  //Setting to 50 now .. want to change to SQL logging soon
		  while (log.size > 50) { log.dequeue() } 
		  (subscribers).foreach { _ ! msg }
		  //You can turn these loggers off if needed
		  //Logger.info("Message:" + msg.text)
	  }
  }
}
