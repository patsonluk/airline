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
import play.api.mvc.Security.AuthenticatedRequest
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.DemandGenerator
import com.patson.data.ConsumptionHistorySource
import com.patson.data.CountrySource
import scala.collection.SortedMap
import scala.collection.immutable.ListMap
import com.patson.data.CycleSource
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.patson.model.LinkConsumptionHistory
import com.patson.model.FlightPreferenceType

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
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
      val rawQuality = json.\("quality").as[Int]
      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
      
      val link = Link(fromAirport, toAirport, airline, LinkClassValues.getInstance(price), distance, LinkClassValues.getInstance(capacity), rawQuality, distance.toInt * 60 / 800, 1, flightType)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
  }
  
  
  
  
  implicit object LinkConsumptionFormat extends Writes[LinkConsumptionDetails] {
    def writes(linkConsumption: LinkConsumptionDetails): JsValue = {
//      val fromAirport = AirportSource.loadAirportById(linkConsumption.fromAirportId)
//      val toAirport = AirportSource.loadAirportById(linkConsumption.toAirportId)
//      val airline = AirlineSource.loadAirlineById(linkConsumption.airlineId)
          JsObject(List(
      "linkId" -> JsNumber(linkConsumption.link.id),
//      "fromAirportCode" -> JsString(fromAirport.map(_.iata).getOrElse("XXX")),
//      "fromAirportName" -> JsString(fromAirport.map(_.name).getOrElse("<unknown>")),
//      "toAirportCode" -> JsString(toAirport.map(_.iata).getOrElse("XXX")),
//      "toAirportName" -> JsString(toAirport.map(_.name).getOrElse("<unknown>")),
//      "airlineName" -> JsString(airline.map(_.name).getOrElse("<unknown>")),
      "fromAirportId" -> JsNumber(linkConsumption.link.from.id),
      "toAirportId" -> JsNumber(linkConsumption.link.to.id),
      "airlineId" -> JsNumber(linkConsumption.link.airline.id),
      "price" -> Json.toJson(linkConsumption.link.price),
      "distance" -> JsNumber(linkConsumption.link.distance),
      "profit" -> JsNumber(linkConsumption.profit),
      "revenue" -> JsNumber(linkConsumption.revenue),
      "fuelCost" -> JsNumber(linkConsumption.fuelCost),
      "crewCost" -> JsNumber(linkConsumption.crewCost),
      "airportFees" -> JsNumber(linkConsumption.airportFees),
      "delayCompensation" -> JsNumber(linkConsumption.delayCompensation),
      "maintenanceCost" -> JsNumber(linkConsumption.maintenanceCost),
      "inflightCost" -> JsNumber(linkConsumption.inflightCost),
      "loungeCost" -> JsNumber(linkConsumption.loungeCost),
      "depreciation" -> JsNumber(linkConsumption.depreciation),
      "capacity" -> Json.toJson(linkConsumption.link.capacity),
      "soldSeats" -> Json.toJson(linkConsumption.link.soldSeats),
      "cancelledSeats" -> Json.toJson(linkConsumption.link.cancelledSeats),
      "minorDelayCount" -> JsNumber(linkConsumption.link.minorDelayCount),
      "majorDelayCount" -> JsNumber(linkConsumption.link.majorDelayCount),
      "cancellationCount" -> JsNumber(linkConsumption.link.cancellationCount),
      
      "cycle" -> JsNumber(linkConsumption.cycle)))
      
    }
  }
  
  implicit object LinkHistoryWrites extends Writes[LinkHistory] {
    def writes(linkHistory: LinkHistory): JsValue = {
          JsObject(List(
      "watchedLinkId" -> JsNumber(linkHistory.watchedLinkId),
      "relatedLinks" -> Json.toJson(linkHistory.relatedLinks),
      "invertedRelatedLinks" -> Json.toJson(linkHistory.invertedRelatedLinks)))
    }
  }
  
  implicit object RelatedLinkWrites extends Writes[RelatedLink] {
    def writes(relatedLink : RelatedLink): JsValue = {
          JsObject(List(
      "linkId" -> JsNumber(relatedLink.relatedLinkId),
      "fromAirportId" -> JsNumber(relatedLink.fromAirport.id),
      "fromAirportCode" -> JsString(relatedLink.fromAirport.iata),
      "fromAirportName" -> JsString(relatedLink.fromAirport.name),
      "toAirportId" -> JsNumber(relatedLink.toAirport.id),
      "toAirportCode" -> JsString(relatedLink.toAirport.iata),
      "toAirportName" -> JsString(relatedLink.toAirport.name),
      "fromAirportCity" -> JsString(relatedLink.fromAirport.city),
      "toAirportCity" -> JsString(relatedLink.toAirport.city),
      "fromLatitude" -> JsNumber(relatedLink.fromAirport.latitude),
      "fromLongitude" -> JsNumber(relatedLink.fromAirport.longitude),
      "toLatitude" -> JsNumber(relatedLink.toAirport.latitude),
      "toLongitude" -> JsNumber(relatedLink.toAirport.longitude),
      "airlineId" -> JsNumber(relatedLink.airline.id),
      "airlineName" -> JsString(relatedLink.airline.name),
      "passenger" -> JsNumber(relatedLink.passengers)))
    }
  }
  
  implicit object ModelPlanLinkInfoWrites extends Writes[ModelPlanLinkInfo] {
    def writes(modelPlanLinkInfo : ModelPlanLinkInfo): JsValue = {
      val jsObject = JsObject(List(
      "modelId" -> JsNumber(modelPlanLinkInfo.model.id), 
      "modelName" -> JsString(modelPlanLinkInfo.model.name),
      "badConditionThreshold" -> JsNumber(Airplane.BAD_CONDITION),
      "criticalConditionThreshold" -> JsNumber(Airplane.CRITICAL_CONDITION),
      "capacity" -> JsNumber(modelPlanLinkInfo.model.capacity),
      "duration" -> JsNumber(modelPlanLinkInfo.duration), 
      "maxFrequency" -> JsNumber(modelPlanLinkInfo.maxFrequency),
      "isAssigned" -> JsBoolean(modelPlanLinkInfo.isAssigned)))
      
      var airplaneArray = JsArray()
      modelPlanLinkInfo.airplanes.foreach {
        case(airplane, isAssigned) => 
          airplaneArray = airplaneArray.append(JsObject(List("airplaneId" -> JsNumber(airplane.id), "isAssigned" -> JsBoolean(isAssigned), "condition" -> JsNumber(airplane.condition))))
      }
      jsObject + ("airplanes" -> airplaneArray)
    }
    
  }
  
  implicit object LinkWithProfitWrites extends Writes[(Link, Int, Int, LinkClassValues)] {
    def writes(linkWithProfit: (Link, Int, Int, LinkClassValues)): JsValue = { 
      val link = linkWithProfit._1
      val profit = linkWithProfit._2
      val revenue = linkWithProfit._3
      val passengers = linkWithProfit._4;
      Json.toJson(link).asInstanceOf[JsObject] + ("profit" -> JsNumber(profit)) + ("revenue" -> JsNumber(revenue)) + ("passengers" -> Json.toJson(passengers))
    }
  }
  
  implicit object RouteWrites extends Writes[Route] {
    def writes(route : Route): JsValue = { 
      Json.toJson(route.links)
    }
  }
  implicit object LinkWithDirectionWrites extends Writes[LinkConsideration] {
    def writes(linkWithDirection : LinkConsideration): JsValue = {
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
  
  val countryByCode = CountrySource.loadAllCountries.map(country => (country.countryCode, country)).toMap
  
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
      val airlineId = incomingLink.airline.id
      
      if (airlineId != request.user.id) {
        println("airline " + request.user.id + " trying to add link for airline " + airlineId + " ! Error")
        return Forbidden
      }
      
      if (incomingLink.getAssignedAirplanes.isEmpty) {
        return BadRequest("Cannot insert link - no airplane assigned")
      }
      
      var existingLink : Option[Link] = LinkSource.loadLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId, LinkSource.ID_LOAD)
      
      if (existingLink.isDefined) {
        incomingLink.id = existingLink.get.id
      }

      
      //validate frequency by duration
      val maxFrequency = incomingLink.getAssignedModel().fold(0)(assignedModel => Computation.calculateMaxFrequency(assignedModel, incomingLink.distance, incomingLink.getAssignedAirplanes().size))
      if (maxFrequency < incomingLink.frequency) {
        println("max frequency exceeded, max " + maxFrequency +  " found " +  incomingLink.frequency + " airline " + request.user)
        return BadRequest("Cannot insert link - frequency exceeded limit")  
      }

      //validate slots      
      val existingFrequency = existingLink.fold(0)(_.frequency)
      val frequencyChange = incomingLink.frequency - existingFrequency
      if ((incomingLink.from.getAirlineSlotAssignment(airlineId) + frequencyChange) > incomingLink.from.getMaxSlotAssignment(airlineId)) {
        println("max slot exceeded, tried to add " + frequencyChange + " but from airport slot at " + incomingLink.from.getAirlineSlotAssignment(airlineId) + "/" + incomingLink.from.getMaxSlotAssignment(airlineId))
        return BadRequest("Cannot insert link - frequency exceeded limit - from airport does not have enough slots")
      }
      if ((incomingLink.to.getAirlineSlotAssignment(airlineId) + frequencyChange) > incomingLink.to.getMaxSlotAssignment(airlineId)) {
        println("max slot exceeded, tried to add " + frequencyChange + " but to airport slot at " + incomingLink.to.getAirlineSlotAssignment(airlineId) + "/" + incomingLink.to.getMaxSlotAssignment(airlineId))
        return BadRequest("Cannot insert link - frequency exceeded limit - to airport does not have enough slots")
      }
      
      val maxFrequencyAbsolute = Computation.getMaxFrequencyAbsolute(request.user)
      if (incomingLink.frequency > maxFrequencyAbsolute) {
        return BadRequest("Cannot insert link - frequency exceeded absolute limit - " + maxFrequencyAbsolute)
      }
      
      if (incomingLink.frequency == 0) {
        return BadRequest("Cannot insert link - frequency is 0")
      }
      
      val airplanesForThisLink = incomingLink.getAssignedAirplanes
      //validate all airplanes are same model
      val airplaneModels = airplanesForThisLink.foldLeft(Set[Model]())(_ + _.model) //should be just one element
      if (airplaneModels.size != 1) {
        return BadRequest("Cannot insert link - not all airplanes are same model")
      }
      
      //validate the model has the range
      val model = airplaneModels.toList(0)
      if (model.range < incomingLink.distance) {
        return BadRequest("Cannot insert link - model cannot reach that distance")
      }
      
      //validate the model is allowed for airport sizes
      if (!incomingLink.from.allowsModel(model) || !incomingLink.to.allowsModel(model)) {
        return BadRequest("Cannot insert link - airport size does not allow that!")
      }
      
      val currentCycle = CycleSource.loadCycle()
      
      //check if the assigned planes are either previously unassigned or assigned to this link
      val violatingAirplanes = airplanesForThisLink.flatMap { airplaneForThisLink => 
        val (airplane, assignedLink) = AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneForThisLink.id, AirplaneSource.LINK_SIMPLE_LOAD).get
        if ((assignedLink.isDefined && assignedLink.get.id != incomingLink.id) || !airplane.isReady(currentCycle)) {
            List(airplaneForThisLink)
        } else {
            List.empty
        }
      }
        
      if (!violatingAirplanes.isEmpty) {
        return BadRequest("Cannot insert link - some airplanes are either occupied with other routes or not ready for deployment " + violatingAirplanes)
      }
      
      //validate configuration is valid
      if ((incomingLink.capacity(ECONOMY) * ECONOMY.spaceMultiplier + 
           incomingLink.capacity(BUSINESS) * BUSINESS.spaceMultiplier + 
           incomingLink.capacity(FIRST) * FIRST.spaceMultiplier) > incomingLink.frequency * model.capacity) {
        return BadRequest("Requested capacity exceed the allowed limit, invalid configuration!")
      }
      
      if (incomingLink.from.id == incomingLink.to.id) {
        return BadRequest("Same from and to airport!")
      }
      //validate price
      if (incomingLink.price(ECONOMY) < 0 || 
           incomingLink.price(BUSINESS) < 0 || 
           incomingLink.price(FIRST) < 0) {
        return BadRequest("negative ticket price not allowed")
      }
      
      
      //validate based on existing user parameters
      val rejectionReason = getRejectionReason(request.user, fromAirport = incomingLink.from, toAirport = incomingLink.to, existingLink.isEmpty)
      if (rejectionReason.isDefined) {
        return BadRequest("Link is rejected: " + rejectionReason.get);
      }
      
      if (existingLink.isEmpty) {
        incomingLink.flightNumber = LinkApplication.getNextAvailableFlightNumber(request.user)
      } else {
        incomingLink.flightNumber = existingLink.get.flightNumber
      }
      
      println("PUT " + incomingLink)
            
      if (existingLink.isEmpty) {
        LinkSource.saveLink(incomingLink) match {
          case Some(link) =>  {
            val cost = Computation.getLinkCreationCost(incomingLink.from, incomingLink.to)
            AirlineSource.adjustAirlineBalance(request.user.id, cost * -1)
            AirlineSource.saveCashFlowItem(AirlineCashFlowItem(request.user.id, CashFlowType.CREATE_LINK, cost * -1))
            
            val toAirport = incomingLink.to
            if (toAirport.getAirlineAwareness(airlineId) < 5) { //update to 5 for link creation
               toAirport.setAirlineAwareness(airlineId, 5)
               AirportSource.updateAirlineAppeal(List(toAirport))
            }
            
            
            Created(Json.toJson(link))
          }
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
    LinkSource.loadLinkById(linkId, LinkSource.FULL_LOAD) match {
      case Some(link) =>
        if (link.airline.id == airlineId) {
          val (maxFrequencyFromAirport, maxFrequencyToAirport) = getMaxFrequencyByAirports(link.from, link.to, link.airline, Some(link))
          Ok(Json.toJson(link).asInstanceOf[JsObject] + 
             ("maxFrequencyFromAirport" -> JsNumber(maxFrequencyFromAirport)) +
             ("maxFrequencyToAirport" -> JsNumber(maxFrequencyToAirport)))
        } else {
          Forbidden
        }
      case None =>
        NotFound
    }
  }
  
  def getExpectedQuality(airlineId : Int, fromAirportId : Int, toAirportId : Int, queryAirportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirportSource.loadAirportById(fromAirportId) match {
      case Some(fromAirport) =>
        AirportSource.loadAirportById(toAirportId) match {
          case Some(toAirport) =>
            val flightType = Computation.getFlightType(fromAirport, toAirport, Computation.calculateDistance(fromAirport, toAirport))
            val airport = if (fromAirportId == queryAirportId) fromAirport else toAirport
            var result = Json.obj()
            LinkClass.values.foreach { linkClass : LinkClass =>
              result += (linkClass.code -> JsNumber(airport.expectedQuality(flightType, linkClass)))
            }
            Ok(result)
          case None =>
          NotFound
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
        foldMap + (linkConsumptionDetails.link.id -> linkConsumptionDetails)
      }
      val linksWithProfit = links.map { link =>  
        (link, consumptions.get(link.id).fold(0)(_.profit), consumptions.get(link.id).fold(0)(_.revenue), consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.soldSeats))  
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
  
  def deleteLink(airlineId : Int, linkId: Int) = AuthenticatedAirline(airlineId) { request =>
    //verify the airline indeed has that link
    LinkSource.loadLinkById(linkId) match {
      case Some(link) =>
        if (link.airline.id != request.user.id) {
          Forbidden
        } else {
          getDeleteLinkRejection(link, request.user) match {
            case Some(reason) => {
              println("cannot delete this link: " + reason)
              BadRequest(reason)
            }
            case None => {
              val count = LinkSource.deleteLink(linkId)  
              Ok(Json.obj("count" -> count))
            }
          }

        }
      case None =>
        NotFound
    }
  }
  
  def getLinkConsumption(airlineId : Int, linkId : Int, cycleCount : Int) = Action {
    LinkSource.loadLinkById(linkId) match {
      case Some(link) =>
        if (link.airline.id == airlineId) {
          val linkConsumptions = LinkSource.loadLinkConsumptionsByLinkId(linkId, cycleCount) 
          if (linkConsumptions.isEmpty) {
            Ok(Json.obj())  
          } else {
            Ok(Json.toJson(linkConsumptions.take(cycleCount)))
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

  def planLink(airlineId : Int, fromAirportId : Int, toAirportId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    loadAirports(fromAirportId, toAirportId) match {
      case Some((fromAirport, toAirport)) =>
          val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
          val countryRelationships = CountrySource.getCountryRelationshipsByAirline(request.user)
          
          val availableModelsByRange = ModelSource.loadModelsWithinRange(distance).filter { model =>
            fromAirport.allowsModel(model) && toAirport.allowsModel(model) && model.purchasable(countryRelationships.getOrElse(model.countryCode, 0)) 
          }.sortBy(_.range)
          
          //find max frequency allowed per model
          val airplanesAllowed = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId, LinkSource.FULL_LOAD) match {
            case Some(existingLink) => existingLink.getAssignedAirplanes().length + 1 //if it already has X airplanes, allow X + 1
            case None => 1
          }
          
          var modelsJson = Json.arr()
          
          availableModelsByRange.foreach { model =>
            val maxFrequency = Computation.calculateMaxFrequencyDouble(model, distance)
            modelsJson = modelsJson.append(Json.toJson(model).as[JsObject] + ("maxFrequency" -> JsNumber(maxFrequency))) //maxFrequency per airplane for this model 
          }
          
          var result = Json.obj("models" -> modelsJson, "maxAirplanesAllowed" -> airplanesAllowed, "maxFrequecny" -> Computation.getMaxFrequencyAbsolute(request.user))
          
          
          Ok(result)
      case None => BadRequest("unknown Airport")
    }
  }
  
  def evaluateLink(airlineId : Int, fromAirportId : Int, toAirportId : Int, airplaneModelId : Int, frequency : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
      BadRequest("unknown Airport")
  }
  
    
  /**
   * Loads a pair of aiports, only return Some(fromAirport, toAirport) if BOTH airports are found
   */
  def loadAirports(fromAirportId : Int, toAirportId : Int, fullLoad : Boolean = false) : Option[(Airport, Airport)] = {
    AirportSource.loadAirportById(fromAirportId, fullLoad) match {
      case Some(fromAirport) =>
        AirportSource.loadAirportById(toAirportId, fullLoad) match {
          case Some(toAirport) =>
            Some(fromAirport, toAirport)
          case None => None
        }
        case None => None
    }
  }
  
  def planLink(airlineId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val PlanLinkData(fromAirportId, toAirportId) = planLinkForm.bindFromRequest.get
    AirportSource.loadAirportById(fromAirportId, true) match {
      case Some(fromAirport) =>
        AirportSource.loadAirportById(toAirportId, true) match {
          case Some(toAirport) =>
            val airline = request.user
            //var existingLinkOption : Option[Link] = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)
            
            val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
            //val (maxFrequencyFromAirport, maxFrequencyToAirport) = getMaxFrequencyByAirports(fromAirport, toAirport, Airline.fromId(airlineId), existingLink)
            val airplanesAllowed = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId, LinkSource.FULL_LOAD) match {
              case Some(existingLink) => existingLink.getAssignedAirplanes().length + 1
              case None => 1
            }
            
            val maxFrequencyByAirplanes = Computation.calculateMaxFrequency(assignedModel, incomingLink.distance, incomingLink.getAssignedAirplanes().size)
            
            
            
            //check relationship
            //val airlineCountryCode = request.user.getCountryCode().get
            
            
            val maxFrequency = getLinkOptions(airline, fromAirport, toAirport, existingLinks)
            
            val rejectionReason = getRejectionReason(request.user, fromAirport, toAirport, existingLink.isEmpty)
              
            
            //group airplanes by model, also add boolean to indicated whether the airplane is assigned to this link
            val availableAirplanesByModel = Map[Model, ListBuffer[(Airplane, Boolean)]]()
            
            val modelsWithinRange : List[Model] = ModelSource.loadModelsWithinRange(distance)
            modelsWithinRange.foreach {
              availableAirplanesByModel.put(_, ListBuffer[(Airplane, Boolean)]())
            }
            
            
            val airplanesWithAssignedLinks : List[(Airplane, Option[Link])] = AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
            
            val currentCycle = CycleSource.loadCycle
            //val airplaneModelsWithinRange = AirplaneSource.load
            val availableAirplanes = airplanesWithAssignedLinks.filter(_._1.isReady(currentCycle)).filter {
              case (_ , Some(_)) => false
              case (airplane, None) => 
                airplane.model.range >= distance
            }.map(_._1)
            
            val assignedToThisLinkAirplanes = existingLink match {
              case Some(link) => airplanesWithAssignedLinks.filter {
                case (airplane, Some(assignedLink)) if (link.id == assignedLink.id) => true
                case _ => false
              }.map(_._1)
              case _ => List.empty 
            }               
               
            
            var assignedModel : Option[Model] = existingLink match {
              case Some(link) => link.getAssignedModel()
              case None => None
            }
            
            availableAirplanes.foreach { availableAirplane => 
              availableAirplanesByModel(availableAirplane.model).append((availableAirplane, false)) 
            }
            assignedToThisLinkAirplanes.foreach { assignedAirplane => 
              availableAirplanesByModel(assignedAirplane.model).append((assignedAirplane, true))
            }
            val planLinkInfoByModel = ListBuffer[ModelPlanLinkInfo]()
            
            val sortedAirplanesByModel = ListMap(availableAirplanesByModel.filter{
              case (model, _) => fromAirport.allowsModel(model) && toAirport.allowsModel(model) 
            }.toSeq.sortBy(_._1.range):_*)
            
            sortedAirplanesByModel.foreach {
              case(model, airplaneList) => 
                val duration = Computation.calculateDuration(model, distance)
                val existingSlotsUsedByThisModel= if (assignedModel.isDefined && assignedModel.get.id == model.id) { existingLink.get.frequency } else { 0 } 
                val maxFrequencyByModel : Double = Computation.calculateMaxFrequencyDouble(model, distance)
                
                planLinkInfoByModel.append(ModelPlanLinkInfo(model, duration, maxFrequencyByModel, assignedModel.isDefined && assignedModel.get.id == model.id, airplaneList.toList))
            }
            
            
            var suggestedPrice : LinkClassValues = LinkClassValues.getInstance(Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), ECONOMY),
                                                                   Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), BUSINESS),
                                                                   Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), FIRST))
                                                                                                                                      
            //adjust suggestedPrice with Lounge
            toAirport.getLounge(airline.id, airline.getAllianceId, activeOnly = true).foreach { lounge =>
              suggestedPrice = LinkClassValues.getInstance(suggestedPrice(ECONOMY), 
                                                       (suggestedPrice(BUSINESS) / lounge.getPriceReduceFactor(distance)).toInt,
                                                       (suggestedPrice(FIRST) / lounge.getPriceReduceFactor(distance)).toInt)
                                                           
            }
            
            fromAirport.getLounge(airline.id, airline.getAllianceId, activeOnly = true).foreach { lounge =>
              suggestedPrice = LinkClassValues.getInstance(suggestedPrice(ECONOMY), 
                                                       (suggestedPrice(BUSINESS) / lounge.getPriceReduceFactor(distance)).toInt,
                                                       (suggestedPrice(FIRST) / lounge.getPriceReduceFactor(distance)).toInt)
            }
            val relationship = CountrySource.getCountryMutualRelationship(fromAirport.countryCode, toAirport.countryCode)
            val directBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS) + DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, relationship, PassengerType.BUSINESS)
            val directTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST) + DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, relationship, PassengerType.TOURIST)
            
            val directDemand = directBusinessDemand + directTouristDemand
            //val airportLinkCapacity = LinkSource.loadLinksByToAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum + LinkSource.loadLinksByFromAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum 
            
            val cost = if (existingLink.isEmpty) Computation.getLinkCreationCost(fromAirport, toAirport) else 0
            val flightNumber = if (existingLink.isEmpty) LinkApplication.getNextAvailableFlightNumber(request.user) else existingLink.get.flightNumber
            val flightCode = LinkApplication.getFlightCode(request.user, flightNumber)
            val maxFrequencyAbsolute = Computation.getMaxFrequencyAbsolute(request.user)
            
            var resultObject = Json.obj("fromAirportId" -> fromAirport.id,
                                        "fromAirportName" -> fromAirport.name,
                                        "fromAirportCity" -> fromAirport.city,
                                        "fromAirportLatitude" -> fromAirport.latitude,
                                        "fromAirportLongitude" -> fromAirport.longitude,
                                        "fromCountryCode" -> fromAirport.countryCode,
                                        "toAirportId" -> toAirport.id,
                                        "toAirportName" -> toAirport.name,
                                        "toAirportCity" -> toAirport.city,
                                        "toAirportLatitude" -> toAirport.latitude,
                                        "toAirportLongitude" -> toAirport.longitude,
                                        "toCountryCode" -> toAirport.countryCode,
                                        "flightCode" -> flightCode,
                                        "mutualRelationship" -> relationship,
                                        "distance" -> distance, 
                                        "suggestedPrice" -> suggestedPrice,
                                        "economySpaceMultiplier" -> ECONOMY.spaceMultiplier, 
                                        "businessSpaceMultiplier" -> BUSINESS.spaceMultiplier,
                                        "firstSpaceMultiplier" -> FIRST.spaceMultiplier,
                                        "maxFrequencyFromAirport" -> maxFrequencyFromAirport, 
                                        "maxFrequencyToAirport" -> maxFrequencyToAirport,
                                        "maxFrequencyAbsolute" -> maxFrequencyAbsolute,
                                        "directDemand" -> directDemand,
                                        "businessPassengers" -> directBusinessDemand.total,
                                        "touristPassengers" -> directTouristDemand.total,
                                        "cost" -> cost).+("modelPlanLinkInfo", Json.toJson(planLinkInfoByModel.toList))
                                                  
            
            val competitorLinkConsumptions = (LinkSource.loadLinksByAirports(fromAirportId, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByAirports(toAirportId, fromAirportId, LinkSource.ID_LOAD)).flatMap { link =>
              LinkSource.loadLinkConsumptionsByLinkId(link.id, 1)
            }
            var otherLinkArray = Json.toJson(competitorLinkConsumptions.filter( _.link.capacity.total > 0).map { linkConsumption => Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite) }.toSeq)
            resultObject = resultObject + ("otherLinks", otherLinkArray)
                                        
            if (existingLink.isDefined) {
              resultObject = resultObject + ("existingLink", Json.toJson(existingLink))
              val deleteRejection = getDeleteLinkRejection(existingLink.get, request.user)
              if (deleteRejection.isDefined) {
                resultObject = resultObject + ("deleteRejection", Json.toJson(deleteRejection.get))
              }
            }
            
            if (rejectionReason.isDefined) {
              resultObject = resultObject + ("rejection", Json.toJson(rejectionReason.get))
            }
            
            Ok(resultObject)
          case None => BadRequest("unknown toAirport")
        }
        case None => BadRequest("unknown toAirport")
    }
  }
  
  def getDeleteLinkRejection(link : Link, airline : Airline) : Option[String] = {
    if (airline.getBases().map { _.airport.id}.contains(link.to.id)) {
      //then make sure there's still some link other then this pointing to the target
      if (LinkSource.loadLinksByAirlineId(airline.id).filter(_.to.id == link.to.id).size == 1) {
        Some("Cannot delete this route as this flies to a base. Must remove the base before this can be deleted")
      } else { //ok, more than 1 link
        None
      }
    } else {
      None
    }
  }
  
  def getRejectionReason(airline : Airline, fromAirport: Airport, toAirport : Airport, newLink : Boolean) : Option[String]= {
    val airlineCountryCode = airline.getCountryCode match {
      case Some(countryCode) => countryCode
      case None => return Some("Airline has no HQ!")
    }
    val toCountryCode = toAirport.countryCode
   
    if (newLink) { //only check new links for now
      //validate from airport is a base
      val base = fromAirport.getAirlineBase(airline.id) match {
        case None => return Some("Cannot fly from this airport, this is not a base!")
        case Some(base) => base
      } 
      
      //check mutualRelationship
      val mutalRelationshipToAirlineCountry = CountrySource.getCountryMutualRelationship(airlineCountryCode, toCountryCode)
      if (mutalRelationshipToAirlineCountry <= Country.HOSTILE_RELATIONSHIP_THRESHOLD) {
        return Some("This country has bad relationship with your home country and banned your airline from operating to any of their airports")
      } else if (toCountryCode != airlineCountryCode && CountrySource.loadCountryByCode(toCountryCode).get.openness + mutalRelationshipToAirlineCountry < Country.INTERNATIONAL_INBOUND_MIN_OPENNESS) {
        return Some("This country does not want to open their airports to your country") 
      }
      
      
      //check airline grade limit
      val existingFlightCategoryCounts : scala.collection.immutable.Map[FlightCategory.Value, Int] = LinkSource.loadLinksByAirlineId(airline.id).map(link => Computation.getFlightCategory(link.from, link.to)).groupBy(category => category).mapValues(_.size)
      val flightCategory = Computation.getFlightCategory(fromAirport, toAirport)
      airline.getLinkLimit(flightCategory).foreach { limit =>//if there's limit
        if (limit <= existingFlightCategoryCounts.getOrElse(flightCategory, 0)) {
          return Some("Cannot create more route of category " + flightCategory + " until your airline reaches next grade")  
        }  
      }
      
      
      //check distance
      val distance = Computation.calculateDistance(fromAirport, toAirport)
      if (distance <= DemandGenerator.MIN_DISTANCE) {
        return Some("Route must be longer than " + DemandGenerator.MIN_DISTANCE + " km")
      }
      
      //check balance
      val cost = Computation.getLinkCreationCost(fromAirport, toAirport)
      if (airline.getBalance() < cost) {
        return Some("Not enough cash to establish this route")
      }
    }  
    
    return None
  }
  
//  def getVipRoutes() = Action {
//    Ok(Json.toJson(RouteHistorySource.loadVipRoutes()))
//  }
  
  def getRelatedLinkConsumption(airlineId : Int, linkId : Int, selfOnly : Boolean) =  AuthenticatedAirline(airlineId) {
    LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD) match {
      case Some(link) => {
        if (link.airline.id != airlineId) {
          Forbidden(Json.obj())
        } else {
          Ok(Json.toJson(HistoryUtil.loadConsumptionByLink(link, selfOnly)))
        }
      }
      case None => NotFound(Json.obj())
    }
  }
  
  
  
//  def getLinkHistory(airlineId : Int) = AuthenticatedAirline(airlineId) {
//    LinkHistorySource.loadWatchedLinkIdByAirline(airlineId) match {
//      case Some(watchedLinkId) =>
//        LinkHistorySource.loadLinkHistoryByWatchedLinkId(watchedLinkId) match {
//          case Some(linkHistory) => Ok(Json.toJson(linkHistory))
//          case None => Ok(Json.obj())
//        }
//      case None => Ok(Json.obj())
//    }
//  }
  
  def updateServiceFunding(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val serviceFundingTry = Try(request.body.asInstanceOf[AnyContentAsJson].json.\("serviceFunding").as[Int])
      
      serviceFundingTry match {
        case Success(serviceFunding) =>
          if (serviceFunding < 0) {
            BadRequest("Cannot have negative service funding")
          } else {
            val airline = request.user
            airline.setServiceFunding(serviceFunding)
            AirlineSource.saveAirlineInfo(airline, updateBalance = false)
            Ok(Json.obj("serviceFunding" -> JsNumber(serviceFunding)))
          }
        case Failure(_) =>
          BadRequest("Cannot Update service funding")
      }
      
    } else {
      BadRequest("Cannot Update service funding")
    }
  }
  
  def updateMaintenanceQuality(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val maintenanceQualityTry = Try(request.body.asInstanceOf[AnyContentAsJson].json.\("maintenanceQuality").as[Int])
      maintenanceQualityTry match {
        case Success(maintenanceQuality) =>
          val airline = request.user
          airline.setMaintainenceQuality(maintenanceQuality)
          AirlineSource.saveAirlineInfo(airline, updateBalance = false)
          Ok(Json.obj("serviceFunding" -> JsNumber(maintenanceQuality)))
        case Failure(_) =>
          BadRequest("Cannot Update maintenance quality")
      }
      
    } else {
      BadRequest("Cannot Update maintenance quality")
    }
  }
  
  
  def getLinkComposition(airlineId : Int, linkId : Int) =  AuthenticatedAirline(airlineId) {
    val consumptionEntries : List[LinkConsumptionHistory]= ConsumptionHistorySource.loadConsumptionByLinkId(linkId)
    val consumptionByCountry = consumptionEntries.groupBy(_.homeCountryCode).mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPassengerType = consumptionEntries.groupBy(_.passengerType).mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPreferenceType = consumptionEntries.groupBy(_.preferenceType).mapValues(entries => entries.map(_.passengerCount).sum)
    
    
    
    var countryJson = Json.arr()
    consumptionByCountry.foreach {
      case(countryCode, passengerCount) => 
        countryByCode.get(countryCode).foreach { country => //just in case the first turn after patch this will be ""
          countryJson = countryJson.append(Json.obj("countryName" -> country.name, "countryCode" -> countryCode, "passengerCount" -> passengerCount))  
        }
         
    }
    var passengerTypeJson = Json.arr()
    consumptionByPassengerType.foreach {
      case(passengerType, passengerCount) => passengerTypeJson = passengerTypeJson.append(Json.obj("title" -> getPassengerTypeTitle(passengerType), "passengerCount" -> passengerCount)) 
    }
    
    var preferenceTypeJson = Json.arr()
    consumptionByPreferenceType.foreach {
      case(preferenceType, passengerCount) => preferenceTypeJson = preferenceTypeJson.append(Json.obj("title" -> preferenceType.title, "description" -> preferenceType.description, "passengerCount" -> passengerCount)) 
    }
    
    Ok(Json.obj("country" -> countryJson, "passengerType" -> passengerTypeJson, "preferenceType" -> preferenceTypeJson))
  }
  
  val getPassengerTypeTitle = (passengerType : PassengerType.Value) =>  passengerType match {
    case PassengerType.BUSINESS => "Business"
    case PassengerType.TOURIST => "Tourist"
  }
  

  
  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
  //case class AirplaneWithPlanRouteInfo(airplane : Airplane, duration : Int, maxFrequency : Int, limitingFactor : String, isAssigned : Boolean)
  case class ModelPlanLinkInfo(model: Model, duration : Int, maxFrequency : Double, isAssigned : Boolean, airplanes : List[(Airplane, Boolean)])
  
  private def getMaxFrequencyByAirports(fromAirport : Airport, toAirport : Airport, airline : Airline, existingLink : Option[Link]) : (Int, Int) =  {
    val airlineId = airline.id
    
    val existingSlotsByThisLink = existingLink.fold(0)(_.frequency)
    val maxFrequencyFromAirport : Int = fromAirport.getMaxSlotAssignment(airlineId) - fromAirport.getAirlineSlotAssignment(airlineId) + existingSlotsByThisLink 
    val maxFrequencyToAirport : Int = toAirport.getMaxSlotAssignment(airlineId) - toAirport.getAirlineSlotAssignment(airlineId) + existingSlotsByThisLink
    
    (maxFrequencyFromAirport, maxFrequencyToAirport)
  }
  
  
}

object LinkApplication {
  def getFlightCode(airline : Airline, flightNumber : Int) = {
    airline.getAirlineCode + " " + (1000 + flightNumber).toString.substring(1, 4)
  }
  
  
  def getNextAvailableFlightNumber(airline : Airline) : Int = {
    val flightNumbers = LinkSource.loadFlightNumbers(airline.id)
    
    val sortedFlightNumbers = flightNumbers.sorted
    
    var candidate = 1
    sortedFlightNumbers.foreach { existingNumber =>
      if (candidate < existingNumber) {
        return candidate
      }
      candidate = existingNumber + 1
    }
    
    return candidate
  }

  
}
