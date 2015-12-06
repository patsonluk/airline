package controllers

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import com.patson.Util
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.AirportSource
import com.patson.data.LinkSource
import com.patson.model._
import com.patson.model.Computation
import com.patson.model.LinkConsumptionDetails
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json._
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc._
import com.patson.data.airplane.ModelSource
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.mvc.Security.AuthenticatedRequest
import controllers.AuthenticationObject.AuthenticatedAirline
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.RouteHistorySource

class LinkApplication extends Controller {
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
      val rawQuality = json.\("quality").as[Int]
      
      val link = Link(fromAirport, toAirport, airline, price, distance.toInt, capacity, rawQuality, distance.toInt * 60 / 800, 1)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
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
  
  implicit object ModelPlanLinkInfoWrites extends Writes[ModelPlanLinkInfo] {
    def writes(modelPlanLinkInfo : ModelPlanLinkInfo): JsValue = {
      val jsObject = JsObject(List(
      "modelId" -> JsNumber(modelPlanLinkInfo.model.id), 
      "modelName" -> JsString(modelPlanLinkInfo.model.name),
      "duration" -> JsNumber(modelPlanLinkInfo.duration), 
      "maxFrequency" -> JsNumber(modelPlanLinkInfo.maxFrequency),
      "isAssigned" -> JsBoolean(modelPlanLinkInfo.isAssigned)))
      
      var airplaneArray = JsArray()
      modelPlanLinkInfo.airplanes.foreach {
        case(airplane, isAssigned) => 
          airplaneArray = airplaneArray.append(JsObject(List("airplaneId" -> JsNumber(airplane.id), "isAssigned" -> JsBoolean(isAssigned))))
      }
      jsObject + ("airplanes" -> airplaneArray)
    }
    
  }
  
  implicit object LinkWithProfitWrites extends Writes[(Link, Int)] {
    def writes(linkWithProfit: (Link, Int)): JsValue = { 
      val link = linkWithProfit._1
      val profit = linkWithProfit._2
      Json.toJson(link).asInstanceOf[JsObject] + ("profit" -> JsNumber(profit))
    }
  }
  
  implicit object RouteWrites extends Writes[Route] {
    def writes(route : Route): JsValue = { 
      Json.toJson(route.links)
    }
  }
  implicit object linkWithDirectionWrites extends Writes[LinkWithCost] {
    def writes(linkWithDirection : LinkWithCost): JsValue = {
      JsObject(List(
        "linkId" -> JsNumber(linkWithDirection.link.id),
        "fromAirportId" -> JsNumber(linkWithDirection.from.id),
        "toAirportId" -> JsNumber(linkWithDirection.to.id),
        "fromAirportCode" -> JsString(linkWithDirection.from.iata),
        "toAirportCode" -> JsString(linkWithDirection.to.iata),
        "fromAirportName" -> JsString(linkWithDirection.from.name),
        "toAirportName" -> JsString(linkWithDirection.to.name),
        "airlineId" -> JsNumber(linkWithDirection.link.airline.id),
        "airlineName" -> JsString(linkWithDirection.link.airline.name),
        "fromLatitude" -> JsNumber(linkWithDirection.from.latitude),
        "fromLongitude" -> JsNumber(linkWithDirection.from.longitude),
        "toLatitude" -> JsNumber(linkWithDirection.to.latitude),
        "toLongitude" -> JsNumber(linkWithDirection.to.longitude)))
    }
  }
  
  
  case class PlanLinkData(fromAirportId: Int, toAirportId: Int)
  val planLinkForm = Form(
    mapping(
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
          Created(Json.toJson(link))      
        case None => UnprocessableEntity("Cannot insert link")
      }
    } else {
      BadRequest("Cannot insert link")
    }
  }
 
  def addLinkBlock(request : AuthenticatedRequest[AnyContent, Airline]) : Result = {
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
      if (incomingLink.airline.id != request.user.id) {
        println("airline " + request.user.id + " trying to add link for airline " + incomingLink.airline.id + " ! Error")
        return Forbidden
      }
      
      if (incomingLink.getAssignedAirplanes.isEmpty) {
        return BadRequest("Cannot insert link - no airplane assigned")
      }
      
      val links = LinkSource.loadLinksByCriteria(List(("airline", incomingLink.airline.id), ("from_airport", incomingLink.from.id)), true).filter { _.to.id == incomingLink.to.id }
      if (!links.isEmpty) {
        incomingLink.id = links(0).id
      }
      
      val isNewLink = links.isEmpty
      
      //validate frequency by duration
      val maxFrequency = Computation.calculateMaxFrequency(incomingLink.duration)
      if (maxFrequency * incomingLink.getAssignedAirplanes().size < incomingLink.frequency) { //TODO log error!
        println("max frequecny exceeded, max " + maxFrequency * incomingLink.getAssignedAirplanes().size + " found " +  incomingLink.frequency)
        return BadRequest("Cannot insert link - frequency exceeded limit")  
      }
      //TODO validate slot on airport ....probably rethink how to simplify all these calculation!
      
      val airplanesForThisLink = incomingLink.getAssignedAirplanes
      //validate all airplanes are same model
      val airplaneModels = airplanesForThisLink.foldLeft(Set[Model]())(_ + _.model) //should be just one element
      if (airplaneModels.size != 1) {
        return BadRequest("Cannot insert link - not all airplanes are same model")
      }
      
      //check if the assigned planes are either previously unassigned or assigned to this link
      val occupiedAirplanes = airplanesForThisLink.flatMap { airplaneForThisLink => 
        val assignedLink = AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneForThisLink.id).get._2
        if (assignedLink.isDefined && assignedLink.get.id != incomingLink.id) {
            List(airplaneForThisLink)
        } else {
            List.empty
        }
      }
        
      if (!occupiedAirplanes.isEmpty) {
        return BadRequest("Cannot insert link - some airplanes already occupied " + occupiedAirplanes)
      }
      
      println("PUT " + incomingLink)
            
      if (isNewLink) {
        LinkSource.saveLink(incomingLink) match {
          case Some(link) => Created(Json.toJson(link))      
          case None => UnprocessableEntity("Cannot insert link")
        }
      } else {
        LinkSource.updateLink(incomingLink) match {
          case 1 => Accepted(Json.toJson(incomingLink))      
          case _ => UnprocessableEntity("Cannot update link")
        }
      }
    } else {
      BadRequest("Cannot put link")
    }
  }
  
  def addLink(airlineId : Int) = AuthenticatedAirline(airlineId) { request => addLinkBlock(request) }
  
  def getLink(airlineId : Int, linkId : Int) = AuthenticatedAirline(airlineId) { request =>
    LinkSource.loadLinkById(linkId) match {
      case Some(link) =>
        if (link.airline.id == airlineId) {
          Ok(Json.toJson(link))
        } else {
          Forbidden
        }
      case None =>
        NotFound
    }
    
    
  }
  
  def getAllLinks() = Action {
     val links = LinkSource.loadAllLinks()
    Ok(Json.toJson(links))
  }
  
  def getLinks(airlineId : Int, getProfit : Boolean, toAirportId : Int) = Action {
     
    val links = 
      if (toAirportId == -1) {
        LinkSource.loadLinksByAirlineId(airlineId)
      } else {
        LinkSource.loadLinksByCriteria(List(("airline", airlineId), ("to_airport", toAirportId)))
      }
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
    Ok(Json.obj("count" -> count))
  }
  
  def deleteLink(airlineId : Int, linkId: Int) = AuthenticatedAirline(airlineId) {
    //verify the airline indeed has that link
    LinkSource.loadLinkById(linkId) match {
      case Some(link) =>
        if (link.airline.id != airlineId) {
        Forbidden
      } else {
        val count = LinkSource.deleteLink(linkId)  
        Ok(Json.obj("count" -> count))    
      }
      case None =>
        NotFound
    }
  }
  
  def getLinkConsumption(airlineId : Int, linkId : Int) = Action {
    LinkSource.loadLinkById(linkId) match {
      case Some(link) =>
        if (link.airline.id == airlineId) {
          val linkConsumptions = LinkSource.loadLinkConsumptionsByLinkId(linkId) 
          if (linkConsumptions.isEmpty) {
            Ok(Json.obj())  
          } else {
            Ok(Json.toJson(linkConsumptions(0)))
          }     
        } else {
          Forbidden
        }
      case None => NotFound
    }
     
  }
  
  def getAllLinkConsumptions() = Action {
     val linkConsumptions = LinkSource.loadLinkConsumptions()
     Ok(Json.toJson(linkConsumptions))
  }


  
  def planLink(airlineId : Int) = Action { implicit request =>
    val PlanLinkData(fromAirportId, toAirportId) = planLinkForm.bindFromRequest.get
    AirportSource.loadAirportById(fromAirportId, true) match {
      case Some(fromAirport) =>
        AirportSource.loadAirportById(toAirportId, true) match {
          case Some(toAirport) =>
            var existingLink : Option[Link] = None 
            
            val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
            val (maxFrequencyFromAirport, maxFrequencyToAirport) = getMaxFrequencyByAirports(fromAirport, toAirport, Airline.fromId(airlineId))
            
            val airplanesWithAssignedLinks : List[(Airplane, Option[Link])] = AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
            val freeAirplanes = airplanesWithAssignedLinks.filter {
              case (_ , Some(_)) => false
              case (airplane, None) => 
                airplane.model.range >= distance
            }.map(_._1)
            val assignedToThisLinkAirplanes = airplanesWithAssignedLinks.filter {
              case (airplane , Some(link)) if (link.from.id == fromAirportId && link.to.id == toAirportId) =>
                existingLink = Some(link)
                true
              case _ => false
            }.map(_._1) 
            
            //group airplanes by model, also add boolean to indicated whether the airplane is assigned to this link
            val availableAirplanesByModel = Map[Model, ListBuffer[(Airplane, Boolean)]]()
            var assignedModel : Option[Model] = existingLink match {
              case Some(link) => link.getAssignedModel()
              case None => None
            }
            
            freeAirplanes.foreach { freeAirplane => 
              availableAirplanesByModel.getOrElseUpdate(freeAirplane.model, ListBuffer[(Airplane, Boolean)]()).append((freeAirplane, false)) 
            }
            assignedToThisLinkAirplanes.foreach { assignedAirplane => 
              availableAirplanesByModel.getOrElseUpdate(assignedAirplane.model, ListBuffer[(Airplane, Boolean)]()).append((assignedAirplane, true))
            }
            val planLinkInfoByModel = ListBuffer[ModelPlanLinkInfo]()
            
            availableAirplanesByModel.foreach { 
              case(model, airplaneList) => 
                val duration = Computation.calculateDuration(model, distance)
                val existingSlotsUsedByThisModel= if (assignedModel.isDefined && assignedModel.get.id == model.id) { existingLink.get.frequency } else { 0 } 
                val maxFrequencyByModel : Int = Computation.calculateMaxFrequency(duration)
                
                planLinkInfoByModel.append(ModelPlanLinkInfo(model, duration, maxFrequencyByModel, assignedModel.isDefined && assignedModel.get.id == model.id, airplaneList.toList))
            }
            
            var resultObject = Json.obj("distance" -> distance, 
                                        "suggestedPrice" -> Pricing.computeStandardPrice(distance), 
                                        "maxFrequencyFromAirport" -> maxFrequencyFromAirport, 
                                        "maxFrequencyToAirport" -> maxFrequencyToAirport) + ("modelPlanLinkInfo", Json.toJson(planLinkInfoByModel.toList))
             
            if (existingLink.isDefined) {
              resultObject = resultObject + ("existingLink", Json.toJson(existingLink))
            }
            
            Ok(resultObject)
          case None => BadRequest("unknown toAirport")
        }
        case None => BadRequest("unknown toAirport")
    }
  }
  
  def getVipRoutes() = Action {
    Ok(Json.toJson(RouteHistorySource.loadVipRoutes()))
  }

  
  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
  //case class AirplaneWithPlanRouteInfo(airplane : Airplane, duration : Int, maxFrequency : Int, limitingFactor : String, isAssigned : Boolean)
  case class ModelPlanLinkInfo(model: Model, duration : Int, maxFrequency : Int, isAssigned : Boolean, airplanes : List[(Airplane, Boolean)])
  
  private def getMaxFrequencyByAirports(fromAirport : Airport, toAirport : Airport, airline : Airline) : (Int, Int) =  {
    val airlineId = airline.id
    val links = LinkSource.loadLinksByCriteria(List(("airline", airlineId), ("from_airport", fromAirport.id)), true).filter { _.to.id == toAirport.id }
    val existingLink : Option[Link] = if (links.size == 1) Some(links(0)) else None
    
    val existingSlotsByThisLink = existingLink.fold(0)(_.frequency)
    val maxFrequencyFromAirport : Int = fromAirport.getMaxSlotAssignment(airlineId) - fromAirport.getAirlineSlotAssignment(airlineId) + existingSlotsByThisLink 
    val maxFrequencyToAirport : Int = toAirport.getMaxSlotAssignment(airlineId) - toAirport.getAirlineSlotAssignment(airlineId) + existingSlotsByThisLink
    
    (maxFrequencyFromAirport, maxFrequencyToAirport)
  }
}
