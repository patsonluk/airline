package models

import akka.actor._
import play.api.Logger
import scala.collection.mutable.Queue
import play.api.libs.json.Writes
import play.api.libs.json._

// our domain message protocol
case object Join
case object Leave
final case class ClientSentMessage(text: String)

// Chat actor
class Chat extends Actor {
  // initial message-handling behavior
  
  // General Chat Log (50)
  var g_log = Queue[String]()
  
  // Alliance Chat Log (1000)
  var a_log = Queue[String]()
  
  def receive = process(Set.empty)
	
  def process(subscribers: Set[ActorRef]): Receive = {
    case Join =>
      // replaces message-handling behavior by the new one
      context become process(subscribers + sender)
	  //You can turn these loggers off if needed
	  //Logger.info("Chat socket connected")
	  g_log.foreach { i => sender ! i }
	  a_log.foreach { i => sender ! i }
	  

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
		  
		  // Check Room for Logging
		  val json_text: JsValue = Json.parse(msg.text)
		  val room = json_text.\("room").as[Int]
		  
		  if (room < 0) {
			g_log.enqueue("[LOGGED]"+ msg.text)
			//Setting to 50 now .. want to change to SQL logging soon
			while (g_log.size > 50) { g_log.dequeue() } 
		  } else {
		    a_log.enqueue("[LOGGED]"+ msg.text)
			//Setting to 50 now .. want to change to SQL logging soon
			while (a_log.size > 1000) { a_log.dequeue() }
		  }
		  (subscribers).foreach { _ ! msg }
		  //You can turn these loggers off if needed
		  //Logger.info("Message:" + msg.text)
	  }
  }
}
