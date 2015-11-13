package controllers

import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import com.patson.Util
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.AirportSource
import com.patson.data.LinkSource
import com.patson.model.Link
import com.patson.model.LinkConsumptionDetails
import com.patson.model.airplane.Airplane
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json.Format
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.Action
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Controller
import com.patson.model.Pricing

object LinkApplication extends Controller {

  implicit object AirplaneWrites extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplane.id),
      "ownerId" -> JsNumber(airplane.owner.id), 
      "name" -> JsString(airplane.model.name),
      "capacity" -> JsNumber(airplane.model.capacity),
      "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
      "speed" -> JsNumber(airplane.model.speed),
      "range" -> JsNumber(airplane.model.range),
      "price" -> JsNumber(airplane.model.price)))
    }
  }
  
  object TestLinkReads extends Reads[Link] {
     def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      val capacity = json.\("capacity").as[Int]
      val price = json.\("price").as[Int]
      val fromAirport = AirportSource.loadAirportById(fromAirportId).get
      val toAirport = AirportSource.loadAirportById(toAirportId).get
      val airline = AirlineSource.loadAirlineById(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude)
      
      val link = Link(fromAirport, toAirport, airline, price, distance.toInt, capacity)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
  }
  
  
  implicit object LinkFormat extends Format[Link] {
    def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      //val capacity = json.\("capacity").as[Int]
      val price = json.\("price").as[Int]
      val fromAirport = AirportSource.loadAirportById(fromAirportId).get
      val toAirport = AirportSource.loadAirportById(toAirportId).get
      val airline = AirlineSource.loadAirlineById(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude)
      val airplaneIds = json.\("airplanes").as[List[Int]]
      var capacity = 0; //TODO capacity calculation should take frequency into consideration
      val airplanes = airplaneIds.foldRight(List[Airplane]()) { (airplaneId, foldList) =>
        AirplaneSource.loadAirplaneById(airplaneId) match { 
          case Some(airplane) =>
            capacity += airplane.model.capacity
            airplane :: foldList
          case None => foldList
        }
      }
      
      val link = Link(fromAirport, toAirport, airline, price, distance.toInt, capacity)
      link.assignedAirplanes = airplanes
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
    
    def writes(link: Link): JsValue = JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportId" -> JsNumber(link.from.id),
      "toAiportId" -> JsNumber(link.to.id),
      "fromAirportCode" -> JsString(link.from.iata),
      "toAirportCode" -> JsString(link.to.iata),
      "fromAirportName" -> JsString(link.from.name),
      "toAirportName" -> JsString(link.to.name),
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
  
  implicit object LinkConsumptionFormat extends Writes[LinkConsumptionDetails] {
    def writes(linkConsumption: LinkConsumptionDetails): JsValue = {
      val fromAirport = AirportSource.loadAirportById(linkConsumption.fromAirportId)
      val toAirport = AirportSource.loadAirportById(linkConsumption.toAirportId)
      val airline = AirlineSource.loadAirlineById(linkConsumption.airlineId)
          JsObject(List(
      "linkId" -> JsNumber(linkConsumption.linkId),
      "fromAirportCode" -> JsString(fromAirport.map(_.iata).getOrElse("XXX")),
      "fromAirportName" -> JsString(fromAirport.map(_.name).getOrElse("<unknown>")),
      "toAirportCode" -> JsString(toAirport.map(_.iata).getOrElse("XXX")),
      "toAirportName" -> JsString(toAirport.map(_.name).getOrElse("<unknown>")),
      "airlineName" -> JsString(airline.map(_.name).getOrElse("<unknown>")),
      "price" -> JsNumber(linkConsumption.price),
      "distance" -> JsNumber(linkConsumption.distance),
      "profit" -> JsNumber(linkConsumption.profit),
      "capacity" -> JsNumber(linkConsumption.capacity),
      "soldSeats" -> JsNumber(linkConsumption.soldSeats)))
      
    }
  }
  
  case class PlanLinkData(airlineId: Int, fromAirportId: Int, toAirportId: Int)
  val planLinkForm = Form(
    mapping(
      "airlineId" -> number,
      "fromAirportId" -> number,
      "toAirportId" -> number
    )(PlanLinkData.apply)(PlanLinkData.unapply)
  )
  
  def addTestLink() = Action { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val newLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link](TestLinkReads)
      println("PUT (test)" + newLink)
      
      LinkSource.saveLink(newLink) match {
        case Some(link) =>
          Created(Json.toJson(link)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
        case None => UnprocessableEntity("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
    } else {
      BadRequest("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  
  def addLink() = Action { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val newLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
      if (newLink.assignedAirplanes.isEmpty) {
        BadRequest("Cannot insert link - no airplane assigned").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      } else {
        println("PUT " + newLink)
        
        LinkSource.saveLink(newLink) match {
          case Some(link) =>
            Created(Json.toJson(link)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
          case None => UnprocessableEntity("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
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
  
  def getLinks(airlineId : Int) = Action {
     val links = LinkSource.loadLinksByAirlineId(airlineId)
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
  
  def getLinkConsumption(linkId : Int) = Action {
     val linkConsumptions = LinkSource.loadLinkConsumptionsByLinkId(linkId) 
     if (linkConsumptions.isEmpty) {
       Ok(Json.obj()).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")  
     } else {
       Ok(Json.toJson(linkConsumptions(0))).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
     }
  }
  
  def getAllLinkConsumptions() = Action {
     val linkConsumptions = LinkSource.loadLinkConsumptions()
    Ok(Json.toJson(linkConsumptions)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }


  
  def planLink() = Action { implicit request =>
    val PlanLinkData(airlineId, fromAirportId, toAirportId) = planLinkForm.bindFromRequest.get
    AirportSource.loadAirportById(fromAirportId) match {
      case Some(fromAirport) =>
        AirportSource.loadAirportById(toAirportId) match {
          case Some(toAirport) =>
            val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
            val airplanesWithAssignedLinks = AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
            val freeAirplanes = airplanesWithAssignedLinks.filter {
              case (_ , Some(_)) => false
              case (airplane, None) => 
                airplane.model.range >= distance
            }.map(_._1)
            Ok(Json.obj("distance" -> distance, "suggestedPrice" -> Pricing.computeStandardPrice(distance)) + ("freeAirplanes", Json.toJson(freeAirplanes))).withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
            )
          case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
        case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
 

  
  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
}
