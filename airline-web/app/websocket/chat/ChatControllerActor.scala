package websocket.chat

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import akka.actor._

import scala.collection.mutable.{ListBuffer, Map, Queue}
import play.api.libs.json._
import play.api.libs.json.JsValue.jsValueToJsLookup
import com.patson.model.User
import com.patson.data.AllianceSource
import play.api.Logger

import scala.collection.mutable
import scala.concurrent.ExecutionContext

// our domain message protocol
case class Join(user : User, lastMessageId : Option[Long])
case class Leave(user : User)
case class TriggerPing()
class Message
final case class ClientSentMessage(text: String)

final case class IncomingMessage(text: String, allianceId : Option[Int])
final case class OutgoingMessage(id: Long, timestamp : Long, text: String, allianceId : Option[Int])



/**
 *  a single actor that handles when a ClientActor joins or leaves
 *  
 *  When a message is received from a ClientActor it would notify this actor, and this actor will send it out to all the corresponding subscribers (ClientActors)
 */

class ChatControllerActor extends Actor {
  // initial message-handling behavior
  val logger = Logger(this.getClass)
  val maxMessagePerRoom = 100
  val ec: ExecutionContext = ExecutionContext.global
  
  val generalMessageHistory = Queue[OutgoingMessage]()
  val allianceMessageHistory = Map[Int, Queue[OutgoingMessage]]()
  val clientActors = mutable.LinkedHashSet[ActorRef]()
  
  def receive = process(Set.empty)

  val messageIdCounter = new AtomicLong(0)

  context.system.scheduler.schedule(Duration.ZERO, Duration.ofSeconds(10), self, TriggerPing, ec, self)

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join(user, lastMessageId) => {
      // replaces message-handling behavior by the new one
      //context become process(subscribers + sender)

      clientActors += sender
      context.watch(sender)
      ChatControllerActor.addActiveUser(sender, user)
	  //You can turn these loggers off if needed
      logger.info("Chat socket connected " + sender + " for user " + user.userName + " current active sessions : " + clientActors.size + " unique users : " + ChatControllerActor.getActiveUsers().size)


      // resend the Archived Message
  	  generalMessageHistory.filter(message => lastMessageId.isEmpty || message.id > lastMessageId.get).foreach(sender ! _)
      
      user.getAccessibleAirlines().foreach { airline => 
        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          allianceMessageHistory.get(allianceMember.allianceId).foreach { archivedMessages =>
            archivedMessages.filter(message => lastMessageId.isEmpty || message.id > lastMessageId.get).foreach(sender ! _)
          }
        }
      }
    }

//    case Leave => {
//      context become process(subscribers - sender)
//	  //You can turn these loggers off if needed
//	    logger.info("Chat socket disconnected " + sender)
//    }
    case Terminated(chatClientActor) => {
      context.unwatch(chatClientActor)
      clientActors -= chatClientActor
      ChatControllerActor.removeActiveUser(chatClientActor)
    }

    case IncomingMessage(text, allianceRoomIdOption) => {
      val outMessage = OutgoingMessage(messageIdCounter.incrementAndGet(), System.currentTimeMillis(), text, allianceRoomIdOption)
		  
      //put message into history and send to subscribers
      allianceRoomIdOption match {
        case None => {
          generalMessageHistory.enqueue(outMessage)
          while (generalMessageHistory.size > maxMessagePerRoom) { 
		        generalMessageHistory.dequeue() 
		      }
          clientActors.foreach { _ ! outMessage }
        }
        case Some(allianceRoomId) =>
          val messageQueue = allianceMessageHistory.getOrElseUpdate(allianceRoomId, Queue[OutgoingMessage]())
          messageQueue.enqueue(outMessage)
          while (messageQueue.size > maxMessagePerRoom) { 
		        messageQueue.dequeue() 
		      }

          clientActors.foreach { _ ! outMessage } //not the best, as we should be able to filter based on alliance Id here
      }
		  //You can turn these loggers off if needed
		  //Logger.info("Message:" + msg.text)
    }
    case TriggerPing => { //ping all clients, since play does NOT have ping support yet...https://github.com/playframework/playframework/issues/3861
      clientActors.foreach( _ ! TriggerPing)
    }
  }
}

object ChatControllerActor {
  import scala.jdk.CollectionConverters._
  val activeUsers = new ConcurrentHashMap[ActorRef, User]().asScala

  def getActiveUsers() : Set[User] = {
    activeUsers.values.toSet
  }

  def addActiveUser(sender : ActorRef, user : User) : Unit = {
    activeUsers.put(sender, user)
  }

  def removeActiveUser(sender : ActorRef) : Option[User] = {
    activeUsers.remove(sender)
  }
}
