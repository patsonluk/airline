package websocket.chat

import akka.actor._
import java.util.Calendar
import java.text.SimpleDateFormat

import play.api.libs.json._
import com.patson.data.{AllianceSource, ChatSource, UserSource}
import com.patson.model.User
import com.patson.model.chat.ChatMessage
import com.patson.util.UserCache
import play.api.Logger
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json.toJsFieldJsValueWrapper

import scala.math.BigDecimal.int2bigDecimal

/**
 * Actor that receives message from websocket and send message out
 */
class ChatClientActor(out: ActorRef, chatControllerActor: ActorRef, val user : User, lastMessageId : Long) extends Actor {
  val logger = Logger(this.getClass)
  chatControllerActor ! Join(user, lastMessageId)

  override def postStop() = {
    logger.info("Stopping chat client on user " + user.userName + " id " + user.id)
  }
  
  val allianceIdOption = getAllianceId(user)
  
  val sdf = new SimpleDateFormat("HH:mm:ss")
  val GENERAL_ROOM_ID = 0

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

          json_text.\("ackId").asOpt[Long] match {
            case Some(ackId) =>
              ChatSource.updateLastChatId(user.id, ackId)
            case None =>  //normal message
              val room = json_text.\("room").as[String]

              val roomId =
                if (room != "chatBox-1") { //message for alliance room
                  AllianceSource.loadAllianceMemberByAirline(airline).map(_.allianceId).getOrElse(GENERAL_ROOM_ID)
                } else {
                  GENERAL_ROOM_ID
                }

              val chatMessage = ChatMessage(airline, user, roomId, json_text.\("text").as[String], Calendar.getInstance())
              chatControllerActor ! IncomingMessage(chatMessage) //notify the chat controller actor of this incoming message

          }

	    }
		  
  	  
  	// handles message writes to websocket
    case OutgoingMessage(chatMessage, latest) => {
      if (chatMessage.roomId == GENERAL_ROOM_ID || (Some(chatMessage.roomId) == allianceIdOption)) {
        var jsonMessage = Json.obj("timestamp" -> chatMessage.time.getTimeInMillis, "airlineName" -> chatMessage.airline.name, "level" -> chatMessage.user.level, "text" -> chatMessage.text, "imagePermission" -> ImgCommand.hasPermission(chatMessage), "id" -> chatMessage.id)
        if (chatMessage.roomId != GENERAL_ROOM_ID) {
          jsonMessage = jsonMessage + ("allianceRoomId" -> JsNumber(chatMessage.roomId))
        }

        jsonMessage = jsonMessage + ("latest", JsBoolean(latest))

        if (!chatMessage.user.isChatBanned) { //send to everyone
          out ! jsonMessage.toString
        } else {
          if (Some(chatMessage.roomId) == allianceIdOption) { //alliance works for banned user
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
