package websocket.chat

import akka.actor._
import java.util.Calendar
import java.text.SimpleDateFormat

import play.api.libs.json._
import com.patson.data.{AllianceSource, UserSource}
import com.patson.model.User
import com.patson.util.UserCache
import play.api.Logger
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json.toJsFieldJsValueWrapper

import scala.math.BigDecimal.int2bigDecimal

/**
 * Actor that receives message from websocket and send message out
 */
class ChatClientActor(out: ActorRef, chatControllerActor: ActorRef, val user : User, lastMessageId : Option[Long]) extends Actor {
  val logger = Logger(this.getClass)
  chatControllerActor ! Join(user, lastMessageId)

  override def postStop() = {
    logger.info("Stopping chat client on user " + user.userName + " id " + user.id)
  }
  
  val allianceId = getAllianceId(user) 
  
  val sdf = new SimpleDateFormat("HH:mm:ss")

  def receive = {
    // this handles incoming messages from the websocket
    case text: String =>
      val json_text: JsValue = Json.parse(text)
		  val airlineId = json_text.\("airlineId").as[Int]

      //reload user as status might have flipped
      val user = UserCache.getUser(this.user.id).get

	    user.getAccessibleAirlines().find( _.id == airlineId) match {
	      case None => logger.warn("user " + user + " has no access to airline " + airlineId + " airline is not found")
	      case Some(airline) =>
	        //val otext =  airline.name + ": " + json_text.\("text").as[String]
          val chatMessage = ChatMessage(airline, user, json_text.\("text").as[String])
    		  val room = json_text.\("room").as[String]
    		  
	        val allianceRoomIdOption =  
	          if (room != "chatBox-1") { //message for alliance room
      			  AllianceSource.loadAllianceMemberByAirline(airline).map(_.allianceId) 
    		    } else {
    		      None
    		    }
	        chatControllerActor ! IncomingMessage(chatMessage, allianceRoomIdOption) //notify the chat controller actor of this incoming message
	    }
		  
  	  
  	// handles message writes to websocket
    case OutgoingMessage(id, timestamp, chatMessage, allianceRoomIdOption) => {
      if (allianceRoomIdOption.isEmpty || (allianceRoomIdOption == allianceId)) { 
        var jsonMessage = Json.obj("timestamp" -> timestamp, "airlineName" -> chatMessage.airline.name, "level" -> chatMessage.user.level, "text" -> chatMessage.text, "imagePermission" -> ImgCommand.hasPermission(chatMessage), "id" -> id)
        allianceRoomIdOption.foreach { allianceRoomId =>
          jsonMessage = jsonMessage + ("allianceRoomId" -> JsNumber(allianceRoomId))
        }

        if (!chatMessage.user.isChatBanned) { //send to everyone
          out ! jsonMessage.toString
        } else {
          if (allianceRoomIdOption == allianceId) { //alliance works for banned user
            out ! jsonMessage.toString
          } else { //otherwise only send to those that are also in penalty box
            //reload user as status might have flipped
            val user = UserCache.getUser(this.user.id).get
            if (user.isChatBanned) {
              out ! jsonMessage.toString
            }
          }
        }
      }
    }

    case TriggerPing => {
      out ! "ping"
    }
  }
  
  def getAllianceId(user : User) : Option[Int] = {
    if (user.getAccessibleAirlines().isEmpty) {
      None
    } else {
      AllianceSource.loadAllianceMemberByAirline(user.getAccessibleAirlines()(0)).map(_.allianceId) //load first owned airline only for now    
    }
  }
}
