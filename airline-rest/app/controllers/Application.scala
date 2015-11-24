package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json.Json
import com.patson.model.Airport
import com.patson.model.Airline
import com.patson.data.AirportSource
import com.patson.Util
import com.patson.model.Link
import com.patson.data.LinkSource
import com.patson.data.AirlineSource

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number

object Application extends Controller {

  implicit object AirportFormat extends Format[Airport] {
    def reads(json: JsValue): JsResult[Airport] = {
      val airport = Airport.fromId((json \ "id").as[Int])
      JsSuccess(airport)
    }
    
    def writes(airport: Airport): JsValue = {
      val averageIncome = airport.power / airport.population
      val incomeLevel = (Math.log(averageIncome / 1000) / Math.log(1.1)).toInt
//      val appealMap = airport.airlineAppeals.foldRight(Map[Airline, Int]()) { 
//        case(Tuple2(airline, appeal), foldMap) => foldMap + Tuple2(airline, appeal.loyalty)  
//      }
//      val awarenessMap = airport.airlineAppeals.foldRight(Map[Airline, Int]()) { 
//        case(Tuple2(airline, appeal), foldMap) => foldMap + Tuple2(airline, appeal.awareness)  
//      }
      
      var airportObject = JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "iata" -> JsString(airport.iata),
      "city" -> JsString(airport.city),
      "size" -> JsNumber(airport.size),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "population" -> JsNumber(airport.population),
      "slots" -> JsNumber(airport.slots),
      "availableSlots" -> JsNumber(airport.availableSlots),
      "incomeLevel" -> JsNumber(if (incomeLevel < 0) 0 else incomeLevel)))
      
      
      if (airport.isSlotAssignmentsInitialized) {
        airportObject = airportObject + ("slotAssignmentList" -> JsArray(airport.getAirlineSlotAssignments().toList.map {  
          case (airlineId, slotAssignment) => Json.obj("airlineId" -> airlineId, "airlineName" -> AirlineSource.loadAirlineById(airlineId).fold("<unknown>")(_.name), "slotAssignment" -> slotAssignment)
          }
        ))
      }
      if (airport.isAirlineAppealsInitialized) {
        airportObject = airportObject + ("appealList" -> JsArray(airport.getAirlineAppeals().toList.map {  
          case (airlineId, appeal) => Json.obj("airlineId" -> airlineId, "airlineName" -> AirlineSource.loadAirlineById(airlineId).fold("<unknown>")(_.name), "loyalty" -> BigDecimal(appeal.loyalty).setScale(2, BigDecimal.RoundingMode.HALF_EVEN), "awareness" -> BigDecimal(appeal.awareness).setScale(2,  BigDecimal.RoundingMode.HALF_EVEN))
          }
        ))
      }
      
      airportObject
    }
  }
  
  case class AirportSlotData(airlineId: Int, slotCount: Int)
  val airportSlotForm = Form(
    mapping(
      "airlineId" -> number,
      "slotCount" -> number
    )(AirportSlotData.apply)(AirportSlotData.unapply)
  )
  
  def getAirports(count : Int) = Action {
    val airports = AirportSource.loadAllAirports()
    val selectedAirports = airports.takeRight(count)
    Ok(Json.toJson(selectedAirports)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirport(airportId : Int) = Action {
     AirportSource.loadAirportById(airportId, true) match {
       case Some(airport) =>  Ok(Json.toJson(airport)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
       case None => NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
     }
  }
  def getAirportSlots(airportId : Int, airlineId : Int) = Action {
    AirportSource.loadAirportById(airportId, true) match {  
       case Some(airport) =>  
         val maxSlots = airport.getMaxSlotAssignment(airlineId)
         val assignedSlots = airport.getAirlineSlotAssignment(airlineId)
         Ok(Json.obj("assignedSlots" -> JsNumber(assignedSlots), "maxSlots" -> JsNumber(maxSlots))).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
       case None => NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
     }
  }
  
  def postAirportSlots(airportId : Int) = Action { implicit request =>
     AirportSource.loadAirportById(airportId, true) match {  
       case Some(airport) =>  
         val AirportSlotData(airlineId, slotCount) = airportSlotForm.bindFromRequest.get
         try {
           airport.setAirlineSlotAssignment(airlineId, slotCount)
           Ok(Json.toJson(airport)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         } catch {
           case e:IllegalArgumentException  => 
             BadRequest("Not allowed to allocate this amount of slots").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         }
       case None => NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
     }
  }
  
  def options(path: String) = Action {
  Ok("").withHeaders(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
    "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Max-Age" -> (60 * 60 * 24).toString
  )
  }

  case class LinkInfo(fromId : Int, toId : Int, price : Double, capacity : Int)  
}
