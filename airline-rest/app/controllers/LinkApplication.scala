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
      val rawQuality = json.\("quality").as[Int]
      
      val link = Link(fromAirport, toAirport, airline, price, distance.toInt, capacity, rawQuality, distance.toInt * 60 / 800, 1)
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
      val frequency = json.\("frequency").as[Int]
      val modelId = json.\("model").as[Int]
      val capacity = frequency * ModelSource.loadModelById(modelId).fold(0)(_.capacity) 
      val duration = ModelSource.loadModelById(modelId).fold(Integer.MAX_VALUE)(Computation.calculateDuration(_, distance))
       
      val airplanes = airplaneIds.foldRight(List[Airplane]()) { (airplaneId, foldList) =>
        AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId) match { 
          case Some((airplane, Some(link))) if (link.from.id != fromAirport.id || link.to.id != toAirport.id) =>
            throw new IllegalStateException("Airplane with id " + airplaneId + " is assigned to other link")
          case Some((airplane, _)) =>
            airplane :: foldList
          case None => 
            throw new IllegalStateException("Airplane with id " + airplaneId + " does not exist")
        }
      }
      var rawQuality =  json.\("rawQuality").as[Int]
      if (rawQuality > Link.MAX_RAW_QUALITY) {
        rawQuality = Link.MAX_RAW_QUALITY
      } else if (rawQuality < 0) {
        rawQuality = 0
      }
         
      val link = Link(fromAirport, toAirport, airline, price, distance, capacity, rawQuality, duration, frequency)
      link.setAssignedAirplanes(airplanes)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
    
    def writes(link: Link): JsValue = JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportId" -> JsNumber(link.from.id),
      "toAirportId" -> JsNumber(link.to.id),
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
      "rawQuality" -> JsNumber(link.rawQuality),
      "computedQuality" -> JsNumber(link.computedQuality),
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
 
  def addLinkBlock(request : Request[AnyContent]) : Result = {
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
      if (incomingLink.getAssignedAirplanes.isEmpty) {
        return BadRequest("Cannot insert link - no airplane assigned").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
      
      val links = LinkSource.loadLinksByCriteria(List(("airline", incomingLink.airline.id), ("from_airport", incomingLink.from.id)), true).filter { _.to.id == incomingLink.to.id }
      if (!links.isEmpty) {
        incomingLink.id = links(0).id
      }
      
      val isNewLink = links.isEmpty
      
      //validate frequency by duration
      val maxFrequency = Computation.calculateMaxFrequency(incomingLink.duration)
      if (maxFrequency * incomingLink.getAssignedAirplanes().size < incomingLink.frequency) { //TODO log error!
        println("max frequecny exceeded, max " + maxFrequency + " found " +  incomingLink.frequency)
        return BadRequest("Cannot insert link - frequency exceeded limit").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")  
      }
      //TODO validate slot on airport ....probably rethink how to simplify all these calculation!
      
      val airplanesForThisLink = incomingLink.getAssignedAirplanes
      //validate all airplanes are same model
      val airplaneModels = airplanesForThisLink.foldLeft(Set[Model]())(_ + _.model) //should be just one element
      if (airplaneModels.size != 1) {
        return BadRequest("Cannot insert link - not all airplanes are same model").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
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
        return BadRequest("Cannot insert link - some airplanes already occupied " + occupiedAirplanes).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
      
      println("PUT " + incomingLink)
            
      if (isNewLink) {
        LinkSource.saveLink(incomingLink) match {
          case Some(link) => Created(Json.toJson(link)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
          case None => UnprocessableEntity("Cannot insert link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
      } else {
        LinkSource.updateLink(incomingLink) match {
          case 1 => Accepted(Json.toJson(incomingLink)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")      
          case _ => UnprocessableEntity("Cannot update link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
      }
    } else {
      BadRequest("Cannot put link").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  
  def addLink() = Action { request => addLinkBlock(request) }
  
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
            
            Ok(resultObject).withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
            )
          case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
        case None => BadRequest("unknown toAirport").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
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
