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
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import com.patson.model.Computation
import com.patson.model.LinkConsumptionDetails
import play.api.libs.json.JsObject

object LinkApplication extends Controller {
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
      val quality = json.\("quality").as[Int]
      
      val link = Link(fromAirport, toAirport, airline, price, distance.toInt, capacity, quality, distance.toInt * 60 / 800, 1)
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
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
      val airplaneIds = json.\("airplanes").as[List[Int]]
      val frequencyPerAirplane = json.\("frequencyPerAirplane").as[Int]

      var capacity = 0; 
      var frequency = 0;
      var duration = Integer.MAX_VALUE;
      val airplanes = airplaneIds.foldRight(List[Airplane]()) { (airplaneId, foldList) =>
        AirplaneSource.loadAirplaneById(airplaneId) match { 
          case Some(airplane) =>
            capacity += airplane.model.capacity * frequencyPerAirplane
            //verify frequency is valid here!
            duration = Computation.calculateDuration(airplane, distance) //unit is minute ...actually can just calculate once...o well
            val maxFrequency = Computation.calculateMaxFrequency(airplane, duration)
            if (maxFrequency > frequencyPerAirplane) { //TODO log error!
              println("max frequecny exceeded, max " + maxFrequency + " found " + frequencyPerAirplane)
              frequency += maxFrequency  
            } else {
              frequency += frequencyPerAirplane
            }
            
            
            airplane :: foldList
          case None => foldList
        }
      }
      val quality = 0 //TODO calculate quality
      
      val link = Link(fromAirport, toAirport, airline, price, distance, capacity, quality, duration, frequency)
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
      "fromAirportCity" -> JsString(link.from.city),
      "toAirportCity" -> JsString(link.to.city),
      "airlineId" -> JsNumber(link.airline.id),
      "price" -> JsNumber(link.price),
      "distance" -> JsNumber(link.distance),
      "capacity" -> JsNumber(link.capacity),
      "quality" -> JsNumber(link.capacity),
      "duration" -> JsNumber(link.duration),
      "frequency" -> JsNumber(link.frequency),
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
  
  implicit object AirplaneWithMaxFrequencyWrites extends Writes[AirplaneWithPlanRouteInfo] {
    def writes(airplaneWithMaxFrequency: AirplaneWithPlanRouteInfo): JsValue = {
      Json.toJson(airplaneWithMaxFrequency.airplane).asInstanceOf[JsObject] + ("duration" , JsNumber(airplaneWithMaxFrequency.duration)) + ("maxFrequency" -> JsNumber(airplaneWithMaxFrequency.maxFrequency))
    }
  }
  
  implicit object LinkWithProfitWrites extends Writes[(Link, Int)] {
    def writes(linkWithProfit: (Link, Int)): JsValue = { 
      val link = linkWithProfit._1
      val profit = linkWithProfit._2
      Json.toJson(link).asInstanceOf[JsObject] + ("profit" -> JsNumber(profit))
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
        //check if the assigned planes are either previously unassigned or assigned to this link
        val airplanesForThisLink = newLink.assignedAirplanes
        val occupiedAirplanes = airplanesForThisLink.flatMap { airplaneForThisLink => 
          val assignedLink = AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneForThisLink.id).get._2
          if (assignedLink.isDefined && assignedLink.get.id != newLink.id) {
            List(airplaneForThisLink)
          } else {
            List.empty
          }
        } 
        
        if (occupiedAirplanes.isEmpty) {
          
          println("PUT " + newLink)
          
          LinkSource.saveLink(newLink) match {
            case Some(link) =>
              Created(Json.toJson(link)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
            case None => UnprocessableEntity("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          }
        } else {
           BadRequest("Cannot insert link - some airplanes already occupied " + occupiedAirplanes).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
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
  
  def getLinks(airlineId : Int, getProfit : Boolean) = Action {
    val links = LinkSource.loadLinksByAirlineId(airlineId)
    if (!getProfit) {
      Ok(Json.toJson(links)).withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      )
    } else {
      val consumptions = LinkSource.loadLinkConsumptionsByAirline(airlineId).foldLeft(Map[Int, LinkConsumptionDetails]()) { (foldMap, linkConsumptionDetails) =>
        foldMap + (linkConsumptionDetails.linkId -> linkConsumptionDetails)
      }
      val linksWithProfit = links.map { link =>  
        (link, consumptions.get(link.id).fold(0)(_.profit))  
      }
      Ok(Json.toJson(linksWithProfit)).withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      )
    }
     
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
            val freeAirplanesWithPlanRouteInfo = freeAirplanes.map { airplane =>
              val duration = Computation.calculateDuration(airplane, distance)
              val maxFrequency = Computation.calculateMaxFrequency(airplane, duration)
              AirplaneWithPlanRouteInfo(airplane, duration, maxFrequency)
            }
            Ok(Json.obj("distance" -> distance, "suggestedPrice" -> Pricing.computeStandardPrice(distance)) + ("freeAirplanes", Json.toJson(freeAirplanesWithPlanRouteInfo))).withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
            )
          case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
        case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }

  
  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
  case class AirplaneWithPlanRouteInfo(airplane : Airplane, duration : Int, maxFrequency : Int)
}
