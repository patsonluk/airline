package controllers

import play.api.libs.json._
import play.api.mvc._
import models.Book._
import play.api.libs.json.Json
import com.patson.model.Airport
import com.patson.model.Airline
import com.patson.data.AirportSource
import com.patson.Util
import com.patson.model.Link
import com.patson.data.LinkSource
import com.patson.data.AirlineSource


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
      
      JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "iata" -> JsString(airport.iata),
      "size" -> JsNumber(airport.size),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "population" -> JsNumber(airport.population),
      "incomeLevel" -> JsNumber(if (incomeLevel < 0) 0 else incomeLevel),
      "appealList" -> JsArray(airport.airlineAppeals.toList.map {  
        case (airline, appeal) => Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "loyalty" -> appeal.loyalty, "awareness" -> appeal.awareness)
        }
      )))
    }
  }
  
  implicit object AirlineFormat extends Format[Airline] {
    def reads(json: JsValue): JsResult[Airline] = {
      val airline = Airline.fromId((json \ "id").as[Int])
      JsSuccess(airline)
    }
    
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name)))
  }
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance)))
  }
  
  
  
  
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
  
  def getAllAirlines() = Action {
     val airlines = AirlineSource.loadAllAirlines()
    Ok(Json.toJson(airlines)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirline(airlineId : Int) = Action {
     AirlineSource.loadAirlineById(airlineId, true) match {
       case Some(airline) =>  Ok(Json.toJson(airline)(OwnedAirlineWrites)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*") //TODO make sure you really own the airline!
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
//  def saveBook = Action(BodyParsers.parse.json) { request =>
//    val b = request.body.validate[Book]
//    b.fold(
//      errors => {
//        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
//      },
//      book => {
//        addBook(book)
//        Ok(Json.obj("status" -> "OK"))
//      }
//    )
//  }
}
