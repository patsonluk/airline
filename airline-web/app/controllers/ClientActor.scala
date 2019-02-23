package models

import akka.actor._
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import play.api.libs.json.Writes
import play.api.libs.json._
import com.patson.data.AirlineSource
import com.patson.data.AllianceSource
import com.patson.model.Airline
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

class ClientActor(out: ActorRef, chat: ActorRef, userId : Int) extends Actor {

  chat ! Join

  override def postStop() = chat ! Leave

  def receive = {
    // this handles messages from the websocket
    case text: String =>
	  val sdf = new SimpleDateFormat("HH:mm:ss")
      if (text.indexOf("[LOGGED]") > -1) {
	   chat ! ClientSentMessage(text)
	  } else {
		  val json_text: JsValue = Json.parse(text)
		  val airline = AirlineSource.loadAirlineById(userId)
		  val otext =  "[" + sdf.format(Calendar.getInstance().getTime())+ "] " + airline.get.name + ": " + json_text.\("text").as[String]
		  val room = json_text.\("room").as[String]
		  var o_room = -1
		  if (room != "chatBox-1") {
			// Get Alliance ID
			o_room = 0; // Default
			val airlines = Map[Int, Airline]()
			val airline2 = airlines.getOrElseUpdate(userId, AirlineSource.loadAirlineById(userId, false).getOrElse(Airline.fromId(userId)))
			  AllianceSource.loadAllianceMemberByAirline(airline2).foreach { member =>
				o_room=member.allianceId
			  }  
		  } 
		  val out_text = Json.obj( "room" -> o_room, "text" -> otext)
		  chat ! ClientSentMessage(out_text.toString)
	  }
	   
    case ClientSentMessage(text) =>
	
		val json_text: JsValue = Json.parse(text)
		val room = json_text.\("room").as[Int]
		var o_room = 0;
		val airlines = Map[Int, Airline]()
		val airline2 = airlines.getOrElseUpdate(userId, AirlineSource.loadAirlineById(userId, false).getOrElse(Airline.fromId(userId)))
		  AllianceSource.loadAllianceMemberByAirline(airline2).foreach { member =>
			 o_room = member.allianceId
		  } 
	
		if ((room < 0) || (room == o_room)) {
			out ! text.replaceFirst("\\W*(\\[LOGGED\\])","")
		}
      
  }
}
