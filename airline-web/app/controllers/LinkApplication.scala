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
import scala.collection.mutable.LinkedHashSet
import models.AirplaneWithReadyStatus

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
      JsObject(List(
      "modelId" -> JsNumber(modelPlanLinkInfo.model.id), 
      "modelName" -> JsString(modelPlanLinkInfo.model.name),
      "badConditionThreshold" -> JsNumber(Airplane.BAD_CONDITION),
      "criticalConditionThreshold" -> JsNumber(Airplane.CRITICAL_CONDITION),
      "capacity" -> JsNumber(modelPlanLinkInfo.model.capacity),
      "duration" -> JsNumber(modelPlanLinkInfo.duration), 
      "constructionTime" -> JsNumber(modelPlanLinkInfo.model.constructionTime),
      "maxFrequency" -> JsNumber(modelPlanLinkInfo.maxFrequency),
      "isAssigned" -> JsBoolean(modelPlanLinkInfo.isAssigned)))
    }
  }
  
  implicit object LinkAirplaneCompositionWrites extends Writes[LinkAirplaneComposition] {
    def writes(composition : LinkAirplaneComposition): JsValue = {
      JsObject(List(
      "existingAssignedAirplanes" -> Json.toJson(composition.existingAssignedAirplanes.map(airplane => AirplaneWithReadyStatus(airplane, airplane.isReady(composition.currentCycle)))), 
      "newAssignedAirplanes" -> Json.toJson(composition.newAssignedAirplanes.map(airplane => AirplaneWithReadyStatus(airplane, airplane.isReady(composition.currentCycle)))),
      "purchasingAirplanes" -> JsNumber(composition.purchasingAirplanes),
      "model" -> Json.toJson(composition.model)))
      
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
  
  implicit object UpdateLinkReads extends Reads[UpdateLinkData] {
    def reads(json: JsValue): JsResult[UpdateLinkData] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      //val capacity = json.\("capacity").as[Int]
      val price = LinkClassValues.getInstance(json.\("price").\("economy").as[Int], json.\("price").\("business").as[Int], json.\("price").\("first").as[Int])
      val frequency = json.\("frequency").as[Int]
      val modelId = json.\("model").as[Int]
      
      val capacity = LinkClassValues.getInstance(frequency * json.\("configuration").\("economy").as[Int],
                                              frequency * json.\("configuration").\("business").as[Int],
                                              frequency * json.\("configuration").\("first").as[Int])
                                
      var rawQuality =  json.\("rawQuality").as[Int]
      if (rawQuality > Link.MAX_QUALITY) {
        rawQuality = Link.MAX_QUALITY
      } else if (rawQuality < 0) {
        rawQuality = 0
      }
         
      //airlineId : Int, fromAirportId : Int, toAirportId : Int, modelId: Int, frequency : Int, capacity : LinkClassValues, price : LinkClassValues
      JsSuccess(UpdateLinkData(airlineId, fromAirportId, toAirportId, modelId, frequency, rawQuality, capacity, price))
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
  
  case class UpdateLinkData(airlineId : Int, fromAirportId : Int, toAirportId : Int, modelId: Int, frequency : Int, rawQuality : Int, capacity : LinkClassValues, price : LinkClassValues)
  
  def addLinkBlock(request : AuthenticatedRequest[AnyContent, Airline]) : Result = {
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[UpdateLinkData]
      val airlineId = incomingLink.airlineId
      
      if (airlineId != request.user.id) {
        println("airline " + request.user.id + " trying to add link for airline " + airlineId + " ! Error")
        return Forbidden
      }
      val airline = request.user
      
//      if (incomingLink.getAssignedAirplanes.isEmpty) {
//        return BadRequest("Cannot insert link - no airplane assigned")
//      }
      
      val existingLinkOption : Option[Link] = LinkSource.loadLinkByAirportsAndAirline(incomingLink.fromAirportId, incomingLink.toAirportId, airlineId, LinkSource.FULL_LOAD)
      
//      if (existingLink.isDefined) {
//        incomingLink.id = existingLink.get.id
//      }
      
      val fromAirport = AirportSource.loadAirportById(incomingLink.fromAirportId, true).getOrElse(return BadRequest("From airport not found"))
      val toAirport = AirportSource.loadAirportById(incomingLink.toAirportId, true).getOrElse(return BadRequest("To airport not found"))
      
      val distance = Computation.calculateDistance(fromAirport, toAirport)
      
      //validate frequency by duration
      val model = ModelSource.loadModelById(incomingLink.modelId).getOrElse(return BadRequest("Model not found"))
      
      
      val maxFrequencyAbsolute = Computation.getMaxFrequencyAbsolute(request.user)
      if (incomingLink.frequency > maxFrequencyAbsolute) {
        return BadRequest("Cannot insert link - frequency exceeded absolute limit - " + maxFrequencyAbsolute)
      }
      
      if (incomingLink.frequency == 0) {
        return BadRequest("Cannot insert link - frequency is 0")
      }
      

      
      //validate the model has the range
      if (model.range < distance) {
        return BadRequest("Cannot insert link - model cannot reach that distance")
      }
      
      //validate the model is allowed for airport sizes
      if (!fromAirport.allowsModel(model) || !toAirport.allowsModel(model)) {
        return BadRequest("Cannot insert link - airport size does not allow that!")
      }
      
      
      val currentCycle = CycleSource.loadCycle()
      
      
      
      //validate configuration is valid
      if ((incomingLink.capacity(ECONOMY) * ECONOMY.spaceMultiplier + 
           incomingLink.capacity(BUSINESS) * BUSINESS.spaceMultiplier + 
           incomingLink.capacity(FIRST) * FIRST.spaceMultiplier) > incomingLink.frequency * model.capacity) {
        return BadRequest("Requested capacity exceed the allowed limit, invalid configuration!")
      }
      
      if (incomingLink.fromAirportId == incomingLink.toAirportId) {
        return BadRequest("Same from and to airport!")
      }
      //validate price
      if (incomingLink.price(ECONOMY) < 0 || 
           incomingLink.price(BUSINESS) < 0 || 
           incomingLink.price(FIRST) < 0) {
        return BadRequest("negative ticket price not allowed")
      }
      
      
      //validate based on existing user parameters
      val rejectionReason = getRejectionReason(request.user, fromAirport = fromAirport, toAirport = toAirport, existingLinkOption.isEmpty)
      if (rejectionReason.isDefined) {
        return BadRequest("Link is rejected: " + rejectionReason.get);
      }
      
      val flightNumber = existingLinkOption match {
        case Some(link) => link.flightNumber 
        case None =>  LinkApplication.getNextAvailableFlightNumber(request.user)
      }
      
      val airplaneComposition = getLinkAirplaneComposition(airline, distance, existingLinkOption, model, incomingLink.frequency)
      val updatingAirplanes = ListBuffer[Airplane]()
      updatingAirplanes ++= airplaneComposition.existingAssignedAirplanes
      updatingAirplanes ++= airplaneComposition.newAssignedAirplanes
      
      
      if (airplaneComposition.purchasingAirplanes > 0) { //need to purchase airlines first
        AirplaneUtil.buyNewAirplanes(airline, model, airplaneComposition.purchasingAirplanes) match {
          case Left(rejection) => return BadRequest(rejection)
          case Right(newAirplanes) => updatingAirplanes ++= newAirplanes
        }
      }
      
      
      val cost = Computation.getLinkCreationCost(fromAirport, toAirport)
      //validate balance
      
      val negotiationInfo = NegotiationUtil.getLinkNegotationInfo(existingLinkOption, fromAirport, toAirport, incomingLink.capacity, incomingLink.frequency)
      val negotiationResultOption = 
        if (negotiationInfo.requiredPoints > 0) { //then negotiation is required
          Some(NegotiationUtil.negotiate(negotiationInfo))
        } else {
          None
        }
      
      println("PUT " + incomingLink)
      
      
      if (negotiationResultOption.isDefined && !negotiationResultOption.get.isSuccessful) {
        return Ok(Json.obj("negotiationResult" -> negotiationResultOption.get))
      }
        
      val updatingLink : Link = existingLinkOption match {
        case Some(existingLink) => 
          existingLink.copy(frequency = incomingLink.frequency, price = incomingLink.price, capacity = incomingLink.capacity, rawQuality = incomingLink.rawQuality)
        case None => 
          val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
          val duration = Computation.calculateDuration(model, distance)
          Link(fromAirport, toAirport, airline, price = incomingLink.price, distance = distance, capacity = incomingLink.capacity, rawQuality = incomingLink.rawQuality, duration = duration, frequency = incomingLink.frequency, flightType = flightType, flightNumber = flightNumber)
      }
      
      updatingLink.setAssignedAirplanes(updatingAirplanes.toList)
      if (existingLinkOption.isEmpty) {
        LinkSource.saveLink(updatingLink) match {
          case Some(link) =>  {
            AirlineSource.adjustAirlineBalance(request.user.id, cost * -1)
            AirlineSource.saveCashFlowItem(AirlineCashFlowItem(request.user.id, CashFlowType.CREATE_LINK, cost * -1))
            
            if (toAirport.getAirlineAwareness(airlineId) < 5) { //update to 5 for link creation
               toAirport.setAirlineAwareness(airlineId, 5)
               AirportSource.updateAirlineAppeal(List(toAirport))
            }
            var result : JsObject = Json.toJson(link).asInstanceOf[JsObject]
            negotiationResultOption.foreach { negotiationResult =>
              result = result + ("negotiationResult" -> Json.toJson(negotiationResult))
            }
            
            Created(result)
          }
          case None => UnprocessableEntity("Cannot insert link")
        }
      } else {
        LinkSource.updateLink(updatingLink) match {
          case 1 => 
            var result : JsObject = Json.toJson(updatingLink).asInstanceOf[JsObject]
            negotiationResultOption.foreach { negotiationResult =>
              result = result + ("negotiationResult" -> Json.toJson(negotiationResult))
            }
            Accepted(result)      
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

  /**
   * Evaluate the link (or changes). Give details about price and negotiation cost/odds
   */
  def previewAirplaneComposition(airlineId : Int, fromAirportId : Int, toAirportId : Int, airplaneModelId : Int, frequency : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val airline = request.user
    loadAirports(fromAirportId, toAirportId) match {
      case Some((fromAirport, toAirport)) =>
          ModelSource.loadModelById(airplaneModelId) match {
            case Some(model) =>
              val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
              val existingLinkOption = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)
              val linkAirplaneComposition = getLinkAirplaneComposition(airline, distance, existingLinkOption, model, frequency)
              
              var result = Json.obj("airplaneComposition" -> 
                                    (Json.toJson(linkAirplaneComposition).asInstanceOf[JsObject] + 
                                    ("economySpaceMultiplier" -> JsNumber(ECONOMY.spaceMultiplier)) + 
                                    ("businessSpaceMultiplier" -> JsNumber(BUSINESS.spaceMultiplier)) +
                                    ("firstSpaceMultiplier" -> JsNumber(FIRST.spaceMultiplier)))) 
              
              existingLinkOption match  { 
                case Some(link) => 
                  //result = result + ("existingLink" -> Json.toJson(link))
                   //if it already has X airplanes, allow X + 1
                  result = result + ("possibleAirplanes" -> JsNumber(link.getAssignedAirplanes().length + 1))    
                case None =>
                  result = result + ("possibleAirplanes" -> JsNumber(1))
              }
              
              Ok(result)  
            case None => BadRequest("unknown Airplane Model")
          }
      case None => BadRequest("unknown Airport")
    }
  }
  
  def previewLinkNegotiation(airlineId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val airline = request.user
    
    val fromAirportId = request.body.asInstanceOf[AnyContentAsJson].json.\("fromAirportId").as[Int]
    val toAirportId = request.body.asInstanceOf[AnyContentAsJson].json.\("toAirportId").as[Int]
    val frequency = request.body.asInstanceOf[AnyContentAsJson].json.\("frequency").as[Int]
    val configuration = request.body.asInstanceOf[AnyContentAsJson].json.\("configuration").as[LinkClassValues]
    val airplaneModelId = request.body.asInstanceOf[AnyContentAsJson].json.\("airplaneModelId").as[Int]
    
    loadAirports(fromAirportId, toAirportId) match {
      case Some((fromAirport, toAirport)) =>
          ModelSource.loadModelById(airplaneModelId) match {
            case Some(model) =>
              val capacity = configuration * frequency
              val existingLinkOption = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)
              val negotiationInfo = NegotiationUtil.getLinkNegotationInfo(existingLinkOption, fromAirport, toAirport, capacity, frequency)
              
              Ok(Json.toJson(negotiationInfo))  
            case None => BadRequest("unknown Airplane Model")
          }
      case None => BadRequest("unknown Airport")
    }
    
  }
  
  
  
  
  private[this] def getLinkAirplaneComposition(airline : Airline, distance : Int, existingLinkOption : Option[Link], model : Model, newFrequency : Int) : LinkAirplaneComposition = {
    val maxFrequencyPerAirplane = Computation.calculateMaxFrequencyDouble(model, distance)
    val airplanesRequired = Math.ceil(newFrequency / maxFrequencyPerAirplane).toInt
//              val existingAssignedAirplanes = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId, LinkSource.FULL_LOAD) match {
//                case Some(existingLink) => existingLink.getAssignedAirplanes().length
//                case None => 0
//              }
    

    val currentCycle = CycleSource.loadCycle()
    val airplanesWithAssignedLinks : List[(Airplane, Option[Link])] = AirplaneSource.loadAirplanesWithAssignedLinkByCriteria(List(("owner", airline.id), ("model", model.id)))
    val availableAirplanes = airplanesWithAssignedLinks.filter {
      case (airplane, assignedLinkOption) => assignedLinkOption.isEmpty 
    }.map(_._1) //ok if some airplanes is not ready yet
    
    
    var existingAssignedAirplanes = airplanesWithAssignedLinks.filter {
      case (airplane, assignedLinkOption) => existingLinkOption.isDefined && assignedLinkOption.isDefined && existingLinkOption.get.id == assignedLinkOption.get.id 
    }.map(_._1).sortBy(_.condition).sortBy(_.isReady(currentCycle)).reverse
    
    
    var shortCount = airplanesRequired - existingAssignedAirplanes.length
    val newAssignedReadyAirplanes = ListBuffer[Airplane]()
    val newAssignedBuildingAirplanes = ListBuffer[Airplane]()
    
    
    if (shortCount < 0) { //have excessive airplanse
      existingAssignedAirplanes = existingAssignedAirplanes.take(airplanesRequired)
    }
    
    if (shortCount > 0) { //need more airplanes
      //check available ones first
      val readyAirplanes = availableAirplanes.filter(_.isReady(currentCycle)).sortBy(_.condition)
      if (readyAirplanes.length >= shortCount) {
        newAssignedReadyAirplanes ++= readyAirplanes.takeRight(shortCount)
      } else {
        newAssignedReadyAirplanes ++= readyAirplanes                  
      }
      
      shortCount -= newAssignedReadyAirplanes.length
    }
    if (shortCount > 0) { //still need more
      //check building ones
      val buildingAirplanes = availableAirplanes.filter(!_.isReady(currentCycle)).sortBy(_.condition)
      if (buildingAirplanes.length >= shortCount) {
        newAssignedBuildingAirplanes ++= buildingAirplanes.takeRight(shortCount)
      } else {
        newAssignedBuildingAirplanes ++= buildingAirplanes                  
      } 
      
      shortCount -= newAssignedBuildingAirplanes.length
    }
    val purchasingAirplanes = shortCount
    
    LinkAirplaneComposition(model, existingAssignedAirplanes, newAssignedReadyAirplanes.toList ++ newAssignedBuildingAirplanes.toList, purchasingAirplanes, currentCycle)
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
    		    var existingLinkOption : Option[Link] = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)
            
            val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
            //val (maxFrequencyFromAirport, maxFrequencyToAirport) = getMaxFrequencyByAirports(fromAirport, toAirport, Airline.fromId(airlineId), existingLinkOption)
            
            //check relationship
            val airlineCountryCode = request.user.getCountryCode().get
            val rejectionReason = getRejectionReason(request.user, fromAirport, toAirport, existingLinkOption.isEmpty)
              
            
            
            val assignedModel = existingLinkOption.flatMap(_.getAssignedModel()) 
            val airlineCountryRelationships = CountrySource.getCountryRelationshipsByAirline(request.user)
            val availableModels = ModelSource.loadModelsWithinRange(distance).filter { model =>
              fromAirport.allowsModel(model) && toAirport.allowsModel(model) && model.purchasable(airlineCountryRelationships.getOrElse(model.countryCode, 0))
            }
                      
            val planLinkInfoByModel = ListBuffer[ModelPlanLinkInfo]()
            
            availableModels.foreach { model =>
                val duration = Computation.calculateDuration(model, distance)
                val maxFrequencyByModel : Double = Computation.calculateMaxFrequencyDouble(model, distance)
                
                planLinkInfoByModel.append(ModelPlanLinkInfo(model, duration, maxFrequencyByModel, assignedModel.isDefined && assignedModel.get.id == model.id))
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
            
            val cost = if (existingLinkOption.isEmpty) Computation.getLinkCreationCost(fromAirport, toAirport) else 0
            val flightNumber = if (existingLinkOption.isEmpty) LinkApplication.getNextAvailableFlightNumber(request.user) else existingLinkOption.get.flightNumber
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
//                                        "maxFrequencyFromAirport" -> maxFrequencyFromAirport, 
//                                        "maxFrequencyToAirport" -> maxFrequencyToAirport,
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
                                        
            if (existingLinkOption.isDefined) {
              val currentCycle = CycleSource.loadCycle
              val sortedAssignedAirplanes = existingLinkOption.get.getAssignedAirplanes.sortBy(_.condition).sortBy(_.isReady(currentCycle)).reverse
              val airplaneCompositionJson = 
                Json.toJson(LinkAirplaneComposition(assignedModel.get, existingAssignedAirplanes = sortedAssignedAirplanes, newAssignedAirplanes = List.empty, purchasingAirplanes = 0, currentCycle = currentCycle)).asInstanceOf[JsObject] +
                ("economySpaceMultiplier" -> JsNumber(ECONOMY.spaceMultiplier)) + 
                ("businessSpaceMultiplier" -> JsNumber(BUSINESS.spaceMultiplier)) +
                ("firstSpaceMultiplier" -> JsNumber(FIRST.spaceMultiplier)) 
                
                
              val existingLinkJson = Json.toJson(existingLinkOption.get).asInstanceOf[JsObject] + ("airplaneComposition" -> airplaneCompositionJson) 
              
                
              
              resultObject = resultObject + ("existingLink", existingLinkJson)
              val deleteRejection = getDeleteLinkRejection(existingLinkOption.get, request.user)
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
  case class ModelPlanLinkInfo(model: Model, duration : Int, maxFrequency : Double, isAssigned : Boolean)
  case class LinkAirplaneComposition(model : Model, existingAssignedAirplanes : List[Airplane], newAssignedAirplanes : List[Airplane], purchasingAirplanes : Int, currentCycle : Int)
  
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
