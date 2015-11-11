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
    
    def writes(airport: Airport): JsValue = JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "iata" -> JsString(airport.iata),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude)))
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
  
  
  implicit object LinkFormat extends Format[Link] {
    def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      val capacity = json.\("capacity").as[Int]
      val price = json.\("price").as[Double]
//      val fromAirport = Airport.fromId(fromAirportId)
//      val toAirport = Airport.fromId(toAirportId)
            //val airline = Airline.fromId(airlineId)
      val fromAirport = AirportSource.loadAirportById(fromAirportId).get
      val toAirport = AirportSource.loadAirportById(toAirportId).get
      val airline = AirlineSource.loadAirlineById(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude)
      
      val link = Link(fromAirport, toAirport, airline, price, distance, capacity)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
    
    def writes(link: Link): JsValue = JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportId" -> JsNumber(link.from.id),
      "toAiportId" -> JsNumber(link.to.id),
      "airlineId" -> JsNumber(link.airline.id),
      "price" -> JsNumber(link.price),
      "distance" -> JsNumber(link.distance),
      "capacity" -> JsNumber(link.capacity),
      "availableSeat" -> JsNumber(link.availableSeats),
      "fromLatitude" -> JsNumber(link.from.latitude),
      "fromLongitude" -> JsNumber(link.from.longitude),
      "toLatitude" -> JsNumber(link.to.latitude),
      "toLongitude" -> JsNumber(link.to.longitude)))
  }
  
  implicit object LinkConsumptionFormat extends Writes[(Link, Int)] {
    def writes(linkConsumption: (Link, Int)): JsValue = {
      linkConsumption match {
        case(link, consumption) =>
          JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportCode" -> JsString(link.from.iata),
      "fromAirportName" -> JsString(link.from.name),
      "toAirportCode" -> JsString(link.to.iata),
      "toAirportName" -> JsString(link.to.name),
      "airlineName" -> JsString(link.airline.name),
      "price" -> JsNumber(link.price),
      "distance" -> JsNumber(link.distance),
      "capacity" -> JsNumber(link.capacity),
      "consumption" -> JsNumber(consumption)))
      }
    }
  }
  
  def getAirports(count : Int) = Action {
    val airports = AirportSource.loadAllAirports()
    val selectedAirports = airports.takeRight(count)
    Ok(Json.toJson(selectedAirports)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAllAirlines() = Action {
     val airlines = AirlineSource.loadAllAirlines()
    Ok(Json.toJson(airlines)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def addLink() = Action { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val newLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
      println("PUT " + newLink)
      
      LinkSource.saveLink(newLink) match {
        case Some(link) =>
          Created(Json.toJson(link)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
        case None => UnprocessableEntity("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
    } else {
      BadRequest("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  
  def getLink(linkId : Int) = Action { request =>
    val link = LinkSource.loadLinkById(linkId)
    Ok(Json.toJson(link)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAllLinks() = Action {
     val links = LinkSource.loadAllLinks()
    Ok(Json.toJson(links)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def deleteAllLinks() = Action {
    val count = LinkSource.deleteAllLinks()
    Ok(Json.obj("count" -> count)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }
  
  def deleteLink(linkId: Int) = Action {
    val count = LinkSource.deleteLink(linkId)  
    Ok(Json.obj("count" -> count)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }
  
  def getAllLinkConsumptions() = Action {
     val linkConsumptions = LinkSource.loadLinkConsumptions()
    Ok(Json.toJson(linkConsumptions)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
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
