package websocket.chat

import akka.actor._
import java.util.Calendar
import java.text.SimpleDateFormat
import play.api.libs.json._
import com.patson.data.AllianceSource
import com.patson.model.User
import play.api.Logger
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import scala.math.BigDecimal.int2bigDecimal

/**
 * Actor that receives message from websocket and send message out
 */
class ChatClientActor(out: ActorRef, chatControllerActor: ActorRef, user : User) extends Actor {

  chatControllerActor ! Join(user)

  override def postStop() = chatControllerActor ! Leave(user)
  
  val allianceId = getAllianceId(user) 
  
  val sdf = new SimpleDateFormat("HH:mm:ss")

  def receive = {
    // this handles incoming messages from the websocket
    case text: String =>
      val json_text: JsValue = Json.parse(text)
		  val airlineId = json_text.\("airlineId").as[Int]
		  
	    user.getAccessibleAirlines().find( _.id == airlineId) match {
	      case None => Logger.warn("user " + user + " has no access to airline " + airlineId + " airline is not found")
	      case Some(airline) =>
	        val otext =  "[" + sdf.format(Calendar.getInstance().getTime())+ "] " + airline.name + ": " + json_text.\("text").as[String]
    		  val room = json_text.\("room").as[String]
    		  
	        val allianceRoomIdOption =  
	          if (room != "chatBox-1") { //message for alliance room
      			  AllianceSource.loadAllianceMemberByAirline(airline).map(_.allianceId) 
    		    } else {
    		      None
    		    }
	        chatControllerActor ! IncomingMessage(otext, allianceRoomIdOption) //notify the chat actor of this incoming message
	    }
		  
  	  
  	// handles message writes to websocket
    case OutgoingMessage(text, allianceRoomIdOption) => {
      if (allianceRoomIdOption.isEmpty || (allianceRoomIdOption == allianceId)) { 
        var jsonMessage = Json.obj("text" -> text)
        allianceRoomIdOption.foreach { allianceRoomId =>
          jsonMessage = jsonMessage + ("allianceRoomId" -> JsNumber(allianceRoomId))
        }
        out ! jsonMessage.toString
      }
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
