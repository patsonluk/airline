package websocket.chat

import akka.actor._
import scala.collection.mutable.Queue
import scala.collection.mutable.Map
import play.api.libs.json._
import play.api.libs.json.JsValue.jsValueToJsLookup
import com.patson.model.User
import com.patson.data.AllianceSource
import play.api.Logger

// our domain message protocol
case class Join(user : User)
case class Leave(user : User)
class Message
final case class ClientSentMessage(text: String)

final case class IncomingMessage(text: String, allianceId : Option[Int])
final case class OutgoingMessage(text: String, allianceId : Option[Int])



/**
 *  a single actor that handles when a ClientActor joins or leaves
 *  
 *  When a message is received from a ClientActor it would notify this actor, and this actor will send it out to all the corresponding subscribers (ClientActors)
 */

class ChatControllerActor extends Actor {
  // initial message-handling behavior
  
  val maxMessagePerRoom = 50
  
  val generalMessageHistory = Queue[OutgoingMessage]()
  val allianceMessageHistory = Map[Int, Queue[OutgoingMessage]]()
  
  def receive = process(Set.empty)
	
  def process(subscribers: Set[ActorRef]): Receive = {
    case Join(user) => {
      // replaces message-handling behavior by the new one
      context become process(subscribers + sender)
	  //You can turn these loggers off if needed
  	  //Logger.info("Chat socket connected")
      
      // resend the Archived Message
  	  generalMessageHistory.foreach(sender ! _)
      
      user.getAccessibleAirlines().foreach { airline => 
        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          allianceMessageHistory.get(allianceMember.allianceId).foreach { archivedMessages =>
            archivedMessages.foreach(sender ! _)
          }
        }
      }
    }
  	  

    case Leave => {
      context become process(subscribers - sender)
	  //You can turn these loggers off if needed
	  //Logger.info("Chat socket disconnected")
    }

    case IncomingMessage(text, allianceRoomIdOption) => {
      val outMessage = OutgoingMessage(text, allianceRoomIdOption)
		  
      //put message into history and send to subscribers
      allianceRoomIdOption match {
        case None => {
          generalMessageHistory.enqueue(outMessage)
          while (generalMessageHistory.size > maxMessagePerRoom) { 
		        generalMessageHistory.dequeue() 
		      }
          (subscribers).foreach { _ ! outMessage }
        }
        case Some(allianceRoomId) =>
          val messageQueue = allianceMessageHistory.getOrElseUpdate(allianceRoomId, Queue[OutgoingMessage]())
          messageQueue.enqueue(outMessage)
          while (messageQueue.size > maxMessagePerRoom) { 
		        messageQueue.dequeue() 
		      }
          
          (subscribers).foreach { _ ! outMessage } //not the best, as we should be able to filter based on alliance Id here
      }
      
		  
		  //You can turn these loggers off if needed
		  //Logger.info("Message:" + msg.text)
    }
  }
}
