package websocket.chat

import org.apache.pekko.actor._

import java.util.Calendar
import java.text.SimpleDateFormat
import play.api.libs.json._
import com.patson.data.{AllianceSource, ChatSource, UserSource}
import com.patson.model.{AllianceRole, User}
import com.patson.model.chat.ChatMessage
import com.patson.util.UserCache
import play.api.Logger
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json.toJsFieldJsValueWrapper

import scala.math.BigDecimal.int2bigDecimal

/**
 * Actor that receives message from websocket and send message out
 */
class ChatClientActor(out: ActorRef, chatControllerActor: ActorRef, val user : User) extends Actor {
  val logger = Logger(this.getClass)
  chatControllerActor ! Join(user)

  override def postStop() = {
    logger.info("Stopping chat client on user " + user.userName + " id " + user.id)
  }

  val allianceIdOption = getAllianceId(user)

  val sdf = new SimpleDateFormat("HH:mm:ss")


  implicit object ChatMessageWrites extends Writes[ChatMessage] {
    override def writes(chatMessage : ChatMessage) : JsValue = {
      var result = Json.obj("timestamp" -> chatMessage.time.getTimeInMillis, "airlineName" -> chatMessage.airline.name, "level" -> chatMessage.user.level, "text" -> chatMessage.text, "imagePermission" -> ImgCommand.hasPermission(chatMessage), "id" -> chatMessage.id)
      if (chatMessage.roomId != GENERAL_ROOM_ID) {
        result = result + ("allianceRoomId" -> JsNumber(chatMessage.roomId))
      }
      result
    }
  }

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

          json_text.\("type").asOpt[String] match {
            case Some(callType) =>
              if (callType == "ack") {
                val newLastChatId = json_text.\("ackId").as[Long]
                if (newLastChatId > ChatSource.getLastChatId(user.id).getOrElse(0L)) { //avoid reverting when there are multiple connections
                  ChatSource.updateLastChatId(user.id, newLastChatId)
                }
              } else if (callType == "previous") {
                chatControllerActor ! PreviousMessagesRequest(airline, json_text.\("firstMessageId").as[Long], json_text.\("roomId").as[Int])
              }

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

        val jsonMessage = Json.toJson(chatMessage).asInstanceOf[JsObject] + ("latest" -> JsBoolean(latest))

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

      //only send back to the user that joined the session
    case SessionStart(lastMessageId : Long, unreadMessageCount: Int, messages : List[ChatMessage]) => {
      val jsonMessage = Json.obj("type" -> "newSession", "lastMessageId" -> lastMessageId, "unreadMessageCount" -> unreadMessageCount, "messages" -> Json.toJson(messages))
      out ! jsonMessage.toString
    }

    case PreviousMessagesResponse(previousMessages) => {
      out ! Json.obj("type" -> "previous", "messages" -> previousMessages).toString()
    }

    case TriggerPing => {
      out ! "ping"
    }
  }

  def getAllianceId(user : User) : Option[Int] = {
    if (user.getAccessibleAirlines().isEmpty) {
      None
    } else {
      AllianceSource.loadAllianceMemberByAirline(user.getAccessibleAirlines()(0)) match {  //load first owned airline only for now
        case Some(allianceMember) =>
          if (allianceMember.role != AllianceRole.APPLICANT) Some(allianceMember.allianceId) else None
        case None => None
      }
    }
  }
}
