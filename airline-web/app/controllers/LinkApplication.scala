package controllers

import java.util.Calendar
import com.patson.data._
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.{Airplane, LinkAssignment, LinkAssignments, Model}
import com.patson.model.history.LinkChange
import com.patson.model.negotiation.LinkNegotiationDiscount
import com.patson.model.event.Olympics
import com.patson.model.{FlightPreferenceType, _}
import com.patson.util.{AirlineCache, AirplaneOwnershipCache, AirportCache, AllianceCache, CountryCache}
import com.patson.{DemandGenerator, Util}
import controllers.AuthenticationObject.AuthenticatedAirline

import javax.inject.Inject
import models.{LinkHistory, RelatedLink}
import play.api.data.Form
import play.api.data.Forms.{mapping, number}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, Json, _}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer
import scala.collection.{MapView, immutable, mutable}
import scala.math.BigDecimal.{RoundingMode, int2bigDecimal}
import scala.util.{Failure, Success, Try}


class LinkApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  object TestLinkReads extends Reads[Link] {
     def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      val capacity = json.\("capacity").as[Int]
      val price = json.\("price").as[Int]
      val fromAirport = AirportCache.getAirport(fromAirportId).get
      val toAirport = AirportCache.getAirport(toAirportId).get
      val airline = AirlineCache.getAirline(airlineId).get
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
//      val fromAirport = AirportCache.getAirport(linkConsumption.fromAirportId)
//      val toAirport = AirportCache.getAirport(linkConsumption.toAirportId)
//      val airline = AirlineCache.getAirline(linkConsumption.airlineId)
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
      "satisfaction" -> JsNumber(linkConsumption.satisfaction),
      "cycle" -> JsNumber(linkConsumption.cycle)))
      
    }
  }

  implicit object RelatedLinkWrites extends Writes[RelatedLink] {
    def writes(relatedLink : RelatedLink): JsValue = {
      JsObject(List(
        "linkId" -> JsNumber(relatedLink.relatedLinkId),
        "fromAirportId" -> JsNumber(relatedLink.fromAirport.id),
        "fromAirportCode" -> JsString(relatedLink.fromAirport.iata),
        "fromAirportName" -> JsString(relatedLink.fromAirport.name),
        "fromCountryCode" -> JsString(relatedLink.fromAirport.countryCode),
        "toAirportId" -> JsNumber(relatedLink.toAirport.id),
        "toAirportCode" -> JsString(relatedLink.toAirport.iata),
        "toAirportName" -> JsString(relatedLink.toAirport.name),
        "fromAirportCity" -> JsString(relatedLink.fromAirport.city),
        "toAirportCity" -> JsString(relatedLink.toAirport.city),
        "toCountryCode" -> JsString(relatedLink.toAirport.countryCode),
        "fromLatitude" -> JsNumber(relatedLink.fromAirport.latitude),
        "fromLongitude" -> JsNumber(relatedLink.fromAirport.longitude),
        "toLatitude" -> JsNumber(relatedLink.toAirport.latitude),
        "toLongitude" -> JsNumber(relatedLink.toAirport.longitude),
        "airlineId" -> JsNumber(relatedLink.airline.id),
        "airlineName" -> JsString(relatedLink.airline.name),
        "passenger" -> JsNumber(relatedLink.passengers)))
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

  implicit object LinkAssignmentsWrites extends Writes[LinkAssignments] {
    def writes(assignments : LinkAssignments): JsValue = {
      var result = Json.obj()

      assignments.assignments.foreach {
        case(linkId, assignment) => result = result + (linkId.toString -> JsNumber(assignment.frequency))
      }
      result
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
      "flightMinutesRequired" -> JsNumber(modelPlanLinkInfo.flightMinutesRequired),
      "maxFrequency" -> JsNumber(modelPlanLinkInfo.maxFrequency),
      "isAssigned" -> JsBoolean(modelPlanLinkInfo.isAssigned)))
      
      var airplaneArray = JsArray()

      modelPlanLinkInfo.airplanes.foreach {
        case(airplane, frequency) =>
          val assignments = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id)
          val airplaneJson = Json.toJson(airplane).asInstanceOf[JsObject] + ("linkAssignments" -> Json.toJson(assignments))
          airplaneArray = airplaneArray.append(JsObject(List("airplane" -> airplaneJson, "frequency" -> JsNumber(frequency))))
      }
      jsObject + ("airplanes" -> airplaneArray)
    }
  }

  
  implicit object LinkExtendedInfoWrites extends Writes[LinkExtendedInfo] {
    def writes(entry: LinkExtendedInfo): JsValue = {
      val link = entry.link
      val profit = entry.profit
      val revenue = entry.revenue
      val passengers = entry.soldSeats
      val capacityHistory = entry.capacityHistory
      val satisfaction = entry.satisfaction
      val lastUpdate = entry.lastUpdate
      Json.toJson(link).asInstanceOf[JsObject] + ("profit" -> JsNumber(profit)) + ("revenue" -> JsNumber(revenue)) + ("passengers" -> Json.toJson(passengers)) + ("capacityHistory" -> Json.toJson(capacityHistory)) + ("satisfaction" -> JsNumber(satisfaction)) + ("lastUpdate" -> JsNumber(lastUpdate.getTimeInMillis))
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

  implicit object RouteWrites extends Writes[Route] {
    def writes(route : Route): JsValue = { 
      Json.toJson(route.links)
    }
  }

  implicit object LinkCommentWrites extends Writes[LinkComment] {
    def writes(entry: LinkComment): JsValue = {
      Json.obj("comment" -> entry.description,
        "category" -> entry.category.toString
      )
    }
  }

  implicit object LinkStaffBreakdownWrites extends Writes[StaffBreakdown] {
    def writes(entry: StaffBreakdown): JsValue = {
      Json.obj("basic" -> entry.basicStaff,
        "frequency" -> entry.frequencyStaff,
        "capacity" -> entry.capacityStaff,
        "modifier" -> entry.modifier,
        "total" -> entry.total
      )
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
    val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
    val delegateCount = request.body.asInstanceOf[AnyContentAsJson].json.\("assignedDelegates").as[Int]

    val airlineId = incomingLink.airline.id

    if (airlineId != request.user.id) {
      println("airline " + request.user.id + " trying to add link for airline " + airlineId + " ! Error")
      return Forbidden
    }

    val airline = request.user

    if (incomingLink.getAssignedAirplanes.isEmpty) {
      return BadRequest("Cannot insert link - no airplane assigned")
    }

    val fromAirport = AirportCache.getAirport(incomingLink.from.id, true).getOrElse(return BadRequest("From airport not found"))
    val toAirport = AirportCache.getAirport(incomingLink.to.id, true).getOrElse(return BadRequest("To airport not found"))


    val existingLink : Option[Link] = LinkSource.loadFlightLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId)

    if (existingLink.isDefined) {
      incomingLink.id = existingLink.get.id
    }

    //validate slots
    val airplanesForThisLink = incomingLink.getAssignedAirplanes
    //validate all airplanes are same model
    val airplaneModels = airplanesForThisLink.foldLeft(Set[Model]())(_ + _._1.model) //should be just one element
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

    val flightMinutesRequiredPerFrequency = Computation.calculateFlightMinutesRequired(model, incomingLink.distance)

    //check if the assigned planes are owned by this airline and have minutes left for this
    incomingLink.getAssignedAirplanes().foreach {
      case(airplane, assignment) =>
        if (airplane.owner.id != airlineId){
          return BadRequest(s"Cannot insert link - airplane $airplane is not owned by ${request.user}")
        }
        if (airplane.home.id != incomingLink.from.id) {
          return BadRequest(s"Cannot insert link - airplane $airplane is not based in ${incomingLink.from}")
        }

        val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id)
        val existingFrequency = linkAssignments.getFrequencyByLink(incomingLink.id)
        val frequencyDelta = assignment.frequency - existingFrequency
        val flightMinutesDelta = flightMinutesRequiredPerFrequency * frequencyDelta
        if (frequencyDelta > 0) {
          if (airplane.availableFlightMinutes < flightMinutesDelta) {
            return BadRequest(s"Cannot insert link - airplane require flight minutes : $flightMinutesDelta, but only have ${airplane.availableFlightMinutes} left")
          }
        }
    }

    //validate the frequency change is valid
    val existingFrequency = existingLink.fold(0)(_.futureFrequency())
    val frequencyChange = incomingLink.futureFrequency() - existingFrequency //use future frequency here

    if (incomingLink.futureFrequency() == 0) {
      return BadRequest("Cannot insert link - future frequency is 0")
    }

    //validate configuration is valid
    if ((incomingLink.futureCapacity()(ECONOMY) * ECONOMY.spaceMultiplier +
         incomingLink.futureCapacity()(BUSINESS) * BUSINESS.spaceMultiplier +
         incomingLink.futureCapacity()(FIRST) * FIRST.spaceMultiplier) > incomingLink.futureFrequency() * model.capacity) {
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
    val rejectionReason = getRejectionReason(request.user, fromAirport = incomingLink.from, toAirport = incomingLink.to, existingLink)
    if (rejectionReason.isDefined) {
      return BadRequest("Link is rejected: " + rejectionReason.get);
    }

    //if assign more delegates then available.
    //However assigning no delegates should be allowed even if negative delegates
    if (delegateCount > 0 && delegateCount > airline.getDelegateInfo().availableCount) {
      return BadRequest(s"Assigning $delegateCount delegates but not enough available");
    }

    if (delegateCount > NegotiationUtil.MAX_ASSIGNED_DELEGATE) {
      return BadRequest(s"Assigning $delegateCount delegates > ${NegotiationUtil.MAX_ASSIGNED_DELEGATE}");
    }

    if (existingLink.isEmpty) {
      incomingLink.flightNumber = LinkApplication.getNextAvailableFlightNumber(request.user)
    } else {
      incomingLink.flightNumber = existingLink.get.flightNumber
    }

    val negotiationInfo = NegotiationUtil.getLinkNegotiationInfo(airline, incomingLink, existingLink)
    val negotiationResultOption =
      if (negotiationInfo.finalRequirementValue > 0) { //then negotiation is required
        getNegotiationRejectionReason(airline, incomingLink.from, incomingLink.to, existingLink) foreach {
          case (reason, rejectionType) =>
            return BadRequest(s"No negotiation : $reason")
        }

        Some(NegotiationUtil.negotiate(negotiationInfo, delegateCount))
      } else {
        None
      }


    println("PUT " + incomingLink)

    val resultLink : Link =
      if (negotiationResultOption.map(_.isSuccessful).getOrElse(true)) { //negotiation successful or no negotiation needed {
        if (existingLink.isEmpty) {
          LinkSource.saveLink(incomingLink) match {
            case Some(link) => {
              val cost = Computation.getLinkCreationCost(incomingLink.from, incomingLink.to)
              AirlineSource.adjustAirlineBalance(request.user.id, cost * -1)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(request.user.id, CashFlowType.CREATE_LINK, cost * -1))

              val toAirport = incomingLink.to
              link
            }
            case None =>
              return UnprocessableEntity("Cannot insert link")
          }
        } else {
          LinkSource.updateLink(incomingLink) match {
            case 1 =>
              //update assignments
              LinkSource.updateAssignedPlanes(incomingLink.id, incomingLink.getAssignedAirplanes())

              incomingLink
            case _ =>
              return UnprocessableEntity("Cannot update link")
          }
        }
      } else { //negotiation failed
        incomingLink
      }

    var result : JsObject = Json.toJson(resultLink).asInstanceOf[JsObject]

    negotiationResultOption.foreach { negotiationResult =>
      //update delegate status
      val cycle = CycleSource.loadCycle()
      val task = DelegateTask.linkNegotiation(cycle, fromAirport, toAirport)
      val coolDown = if (negotiationResult.isSuccessful) task.coolDown else task.coolDown / 2 //half cooldown if it was unsuccessful
      val availableCycle = cycle + coolDown

      val busyDelegates = (0 until delegateCount).toList.map { _ =>
        BusyDelegate(airline, task, Some(availableCycle))
      }

      DelegateSource.saveBusyDelegates(busyDelegates)

      LinkSource.saveNegotiationCoolDown(resultLink.airline, resultLink.from, resultLink.to, cycle + Link.LINK_NEGOTIATION_COOL_DOWN)



      if (negotiationResult.isGreatSuccess) {
        val existingCapacity = existingLink.fold(LinkClassValues.getInstance())(_.futureCapacity())
        val capacityChange = incomingLink.futureCapacity() - existingCapacity //use future capacity here
        val normalizedCapacityChange = capacityChange(ECONOMY) * ECONOMY.spaceMultiplier + capacityChange(BUSINESS) * BUSINESS.spaceMultiplier + capacityChange(FIRST) * FIRST.spaceMultiplier
        val capacityMonetaryBaseValue = (resultLink.standardPrice(ECONOMY) * normalizedCapacityChange).toLong //this could be negative if freq increased but capacity decreased
        val frequencyChange = incomingLink.futureFrequency() - existingFrequency
        val frequencyMonetaryBaseValue = frequencyChange.toLong * resultLink.getAssignedModel().get.capacity * resultLink.standardPrice(ECONOMY)
        val monetaryBaseValue = Math.max(0, Math.max(capacityMonetaryBaseValue, frequencyMonetaryBaseValue))

        val bonus = NegotiationUtil.getLinkBonus(resultLink, monetaryBaseValue, busyDelegates)
        bonus.apply(airline)
        result = result + ("negotiationBonus" -> Json.obj("description" -> JsString(bonus.description), "intensity" -> JsNumber(bonus.intensity)))
      }

      if (negotiationResult.isSuccessful) { //purge all previous discounts if successful
        NegotiationSource.deleteLinkDiscountsByAirlineAndAirport(airline.id, fromAirport.id, toAirport.id)
      }

      NegotiationUtil.getNextNegotiationDiscount(resultLink, negotiationResult).foreach { discount =>
        if (discount.discount > 0) {
          val logMessage =  s"Negotiation Discount of ${(discount.discount * 100).toInt}% for ${fromAirport.displayText} -> ${toAirport.displayText} (expires after ${LinkNegotiationDiscount.DURATION} weeks)"
          LogSource.insertLogs(List(Log(airline, logMessage, LogCategory.NEGOTIATION, LogSeverity.INFO, cycle)))
          NegotiationSource.saveLinkDiscount(discount)
          result = result + ("nextNegotiationDiscount" -> JsString(s"Some progress made. ${(discount.discount * 100).toInt}% Negotiation Discount for the next ${LinkNegotiationDiscount.DURATION} weeks"))
        }
      }

      result = result + ("negotiationResult" -> Json.toJson(negotiationResult))

      val airportAnimationDetails  = AirportAnimationUtil.getAnimation(toAirport, fromAirport) //to airport has higher priority
      var animationJson = Json.obj("url" -> airportAnimationDetails.animation.url)
      airportAnimationDetails.label.foreach { label =>
        animationJson = animationJson + ("label" -> JsString(label))
      }
      result = result + ("airportAnimation" -> animationJson)
    }


    return Ok(result)
  }

  def addLink(airlineId : Int) = AuthenticatedAirline(airlineId) { request => addLinkBlock(request) }

  def getLink(airlineId : Int, linkId : Int) = AuthenticatedAirline(airlineId) { request =>
    LinkSource.loadFlightLinkById(linkId, LinkSource.FULL_LOAD) match {
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

  def getOvertimeCompensation(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
    val existingListOption = LinkSource.loadFlightLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId)

    val airline = request.user
    val staffBreakdown : StaffBreakdown = incomingLink.getFutureOfficeStaffBreakdown
    val staffRequiredByThisLink = staffBreakdown.total
    val extraOvertimeCompensation = airline.getBases().find(_.airport.id == incomingLink.from.id) match {
      case Some(base) =>
        val existingLinks = LinkSource.loadFlightLinksByFromAirportAndAirlineId(base.airport.id, airline.id, LinkSource.SIMPLE_LOAD)
        val existingStaffRequired = existingLinks.map(_.getFutureOfficeStaffRequired).sum
        val newStaffRequired = existingStaffRequired - existingListOption.map(_.getFutureOfficeStaffRequired).getOrElse(0) + staffRequiredByThisLink
        val extraCompensation = base.getOvertimeCompensation(newStaffRequired) - base.getOvertimeCompensation(existingStaffRequired)
        if (extraCompensation > 0) { //then we should prompt warning of over limit
          extraCompensation
        } else {
          0
        }
      case None => 0
    }

    Ok(Json.obj(
      "extraOvertimeCompensation" -> extraOvertimeCompensation,
      "staffBreakdown" -> staffBreakdown,
      "flightType" -> FlightType.label(Computation.getFlightType(incomingLink.from, incomingLink.to, incomingLink.distance))
    ))
  }


  def getExpectedQuality(airlineId : Int, fromAirportId : Int, toAirportId : Int, queryAirportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirportCache.getAirport(fromAirportId) match {
      case Some(fromAirport) =>
        AirportCache.getAirport(toAirportId) match {
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
     val links = LinkSource.loadAllFlightLinks()
    Ok(Json.toJson(links))
  }

  def getLinks(airlineId : Int, toAirportId : Int) = Action {

    val links =
      if (toAirportId == -1) {
        LinkSource.loadFlightLinksByAirlineId(airlineId)
      } else {
        LinkSource.loadFlightLinksByToAirportAndAirlineId(toAirportId, airlineId)
      }
    Ok(Json.toJson(links)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }

  def getLinksDetails(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val links = LinkSource.loadFlightLinksByAirlineId(airlineId)
    val consumptions = LinkSource.loadLinkConsumptionsByAirline(airlineId).foldLeft(immutable.Map[Int, LinkConsumptionDetails]()) { (foldMap, linkConsumptionDetails) =>
      foldMap + (linkConsumptionDetails.link.id -> linkConsumptionDetails)
    }
    val lastUpdates : scala.collection.immutable.Map[Int, Calendar] = LinkSource.loadLinkLastUpdates(links.map(_.id))

    val linksWithProfit: Seq[LinkExtendedInfo] = links.map { link =>
      //(link, consumptions.get(link.id).fold(0)(_.profit), consumptions.get(link.id).fold(0)(_.revenue), consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.soldSeats))
      LinkExtendedInfo(link,
        consumptions.get(link.id).fold(0)(_.profit),
        consumptions.get(link.id).fold(0)(_.revenue),
        consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.soldSeats),
        consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.capacity),
        consumptions.get(link.id).map(_.satisfaction).getOrElse(0),
        lastUpdates(link.id))
    }
    Ok(Json.toJson(linksWithProfit)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }

  case class LinkExtendedInfo(link : Link, profit : Int, revenue : Int, soldSeats : LinkClassValues, capacityHistory : LinkClassValues, satisfaction : Double, lastUpdate : Calendar)

  def deleteLink(airlineId : Int, linkId: Int) = AuthenticatedAirline(airlineId) { request =>
    //verify the airline indeed has that link
    LinkSource.loadFlightLinkById(linkId) match {
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
              if (count == 1) { //update airplane available minutes too
                link.getAssignedAirplanes()
              }
              Ok(Json.obj("count" -> count))
            }
          }

        }
      case None =>
        NotFound
    }
  }

  def getLinkConsumption(airlineId : Int, linkId : Int, cycleCount : Int) = AuthenticatedAirline(airlineId) { request =>
    LinkSource.loadFlightLinkById(linkId) match {
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

//  def getAllLinkConsumptions() = Action {
//     val linkConsumptions = LinkSource.loadLinkConsumptions()
//     Ok(Json.toJson(linkConsumptions))
//  }

  def preparePlanLink(airline : Airline, fromAirportId : Int, toAirportId : Int) : Either[String, (Airport, Airport)] = {
    AirportCache.getAirport(fromAirportId, true) match {
      case Some(fromAirport) =>
        AirportCache.getAirport(toAirportId, true) match {
          case Some(toAirport) =>
            if (airline.getBases().map(_.airport.id).contains(fromAirportId)) { //make sure it has a base for the from Airport
              Right((fromAirport, toAirport))
            } else {
              Left(s"from Airport $fromAirportId is not a base of ${airline.name}")
            }
          case None =>
            Left(s"from Airport $fromAirportId is not found")
        }
      case None =>
        Left(s"to Airport $toAirportId is not found")
    }
  }



  def planLink(airlineId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val PlanLinkData(fromAirportId, toAirportId) = planLinkForm.bindFromRequest.get
    val airline = request.user
    preparePlanLink(airline, fromAirportId, toAirportId) match {
      case Right((fromAirport, toAirport)) => {
        var existingLink: Option[Link] = LinkSource.loadFlightLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)

        val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt

        val rejectionReason = getRejectionReason(request.user, fromAirport, toAirport, existingLink)

        val warnings = getWarnings(request.user, fromAirport, toAirport, existingLink.isEmpty)

        val modelsWithinRange: List[Model] = ModelSource.loadModelsWithinRange(distance)

        val allManufacturingCountries = modelsWithinRange.map(_.countryCode).toSet

        val countryRelations : immutable.Map[String, AirlineCountryRelationship] = allManufacturingCountries.map { countryCode =>
          (countryCode, AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline))
        }.toMap

        val ownedAirplanesByModel = AirplaneSource.loadAirplanesByOwner(airlineId).groupBy(_.model)
        val modelsWithinRangeAndRelationship = modelsWithinRange.filter(model => model.purchasableWithRelationship(countryRelations(model.countryCode).relationship))
        val availableModels = modelsWithinRangeAndRelationship ++ ownedAirplanesByModel.keys.filter(_.range >= distance)

        val airplanesAssignedToThisLink = new mutable.HashMap[Int, Int]()

        if (existingLink.isDefined) {
          AirplaneSource.loadAirplaneLinkAssignmentsByLinkId(existingLink.get.id).foreach {
            case (airplaneId, linkAssignment) =>
              if (linkAssignment.frequency > 0) {
                airplanesAssignedToThisLink.put(airplaneId, linkAssignment.frequency)
              }
          }
        }

        //available airplanes are either the ones that are already assigned to this link or have available flight minutes that is >= required minutes
        //group airplanes by model, also add Int to indicated how many frequency is this airplane currently assigned to this link
        val availableAirplanesByModel : immutable.Map[Model, List[(Airplane, Int)]] = availableModels.map { model =>
          val ownedAirplanesOfThisModel = ownedAirplanesByModel.getOrElse(model, List.empty)
          val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
          val availableAirplanesOfThisModel = ownedAirplanesOfThisModel.filter(airplane => (airplane.home.id == fromAirportId && airplane.availableFlightMinutes >= flightMinutesRequired) || airplanesAssignedToThisLink.isDefinedAt(airplane.id))

          (model, availableAirplanesOfThisModel.map(airplane => (airplane, airplanesAssignedToThisLink.getOrElse(airplane.id, 0))))
        }.toMap

        val assignedModel: Option[Model] = existingLink match {
          case Some(link) => link.getAssignedModel()
          case None => None
        }


        val planLinkInfoByModel = ListBuffer[ModelPlanLinkInfo]()

        val sortedAirplanesByModel = ListMap(availableAirplanesByModel.filter {
          case (model, _) => fromAirport.allowsModel(model) && toAirport.allowsModel(model)
        }.toSeq.sortBy(_._1.range): _*)

        sortedAirplanesByModel.foreach {
          case (model, airplaneList) =>
            val duration = Computation.calculateDuration(model, distance)

            val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
            val maxFrequency = Airplane.MAX_FLIGHT_MINUTES / flightMinutesRequired

            planLinkInfoByModel.append(ModelPlanLinkInfo(model, duration, flightMinutesRequired, assignedModel.isDefined && assignedModel.get.id == model.id, maxFrequency, airplaneList))
        }


        var suggestedPrice: LinkClassValues = LinkClassValues.getInstance(Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), ECONOMY),
          Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), BUSINESS),
          Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), FIRST))

        //adjust suggestedPrice with Lounge
        toAirport.getLounge(airline.id, airline.getAllianceId, activeOnly = true).foreach { lounge =>
          suggestedPrice = LinkClassValues.getInstance(suggestedPrice(ECONOMY),
            (suggestedPrice(BUSINESS) / (1 + lounge.getPriceReduceFactor(distance))).toInt,
            (suggestedPrice(FIRST) / (1 + lounge.getPriceReduceFactor(distance))).toInt)

        }

        fromAirport.getLounge(airline.id, airline.getAllianceId, activeOnly = true).foreach { lounge =>
          suggestedPrice = LinkClassValues.getInstance(suggestedPrice(ECONOMY),
            (suggestedPrice(BUSINESS) / (1 + lounge.getPriceReduceFactor(distance))).toInt,
            (suggestedPrice(FIRST) / (1 + lounge.getPriceReduceFactor(distance))).toInt)
        }
        val countryRelationship = CountrySource.getCountryMutualRelationship(fromAirport.countryCode, toAirport.countryCode)
        val directFromAirportBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, countryRelationship, PassengerType.BUSINESS)
        val directToAirportBusinessDemand = DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, countryRelationship, PassengerType.BUSINESS)
        val directBusinessDemand =  directFromAirportBusinessDemand + directToAirportBusinessDemand

        val directFromAirportTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, countryRelationship, PassengerType.TOURIST)
        val directToAirportTouristDemand = DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, countryRelationship, PassengerType.TOURIST)
        val directTouristDemand = directFromAirportTouristDemand + directToAirportTouristDemand

        val directDemand = directBusinessDemand + directTouristDemand
        //val airportLinkCapacity = LinkSource.loadLinksByToAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum + LinkSource.loadLinksByFromAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum

        val cost = if (existingLink.isEmpty) Computation.getLinkCreationCost(fromAirport, toAirport) else 0
        val flightNumber = if (existingLink.isEmpty) LinkApplication.getNextAvailableFlightNumber(request.user) else existingLink.get.flightNumber
        val flightCode = LinkUtil.getFlightCode(request.user, flightNumber)
        val flightType = Computation.getFlightType(fromAirport, toAirport, distance)

        val estimatedDifficulty : Option[Double] =
          if (existingLink.isEmpty) {
            val mockedLink = Link(fromAirport, toAirport, airline, LinkClassValues.getInstance(), distance, LinkClassValues.getInstance(), 0, 0, frequency = 1, flightType, flightNumber)
            val mockedAirplane = Airplane(Model.fromId(0), airline, 0, 0, 0, 0, 0)
            mockedLink.setAssignedAirplanes(Map(mockedAirplane -> LinkAssignment(1, 0)))
            Some(NegotiationUtil.getLinkNegotiationInfo(airline, mockedLink, None).finalRequirementValue)
          } else {
            None
          }



        var resultObject = Json.obj("fromAirportId" -> fromAirport.id,
          "fromAirportName" -> fromAirport.name,
          "fromAirportCode" -> fromAirport.iata,
          "fromAirportCity" -> fromAirport.city,
          "fromAirportLatitude" -> fromAirport.latitude,
          "fromAirportLongitude" -> fromAirport.longitude,
          "fromCountryCode" -> fromAirport.countryCode,
          "toAirportId" -> toAirport.id,
          "toAirportName" -> toAirport.name,
          "toAirportCode" -> toAirport.iata,
          "toAirportCity" -> toAirport.city,
          "toAirportLatitude" -> toAirport.latitude,
          "toAirportLongitude" -> toAirport.longitude,
          "toCountryCode" -> toAirport.countryCode,
          "flightCode" -> flightCode,
          "mutualRelationship" -> countryRelationship,
          "distance" -> distance,
          "flightType" -> FlightType.label(flightType),
          "suggestedPrice" -> suggestedPrice,
          "economySpaceMultiplier" -> ECONOMY.spaceMultiplier,
          "businessSpaceMultiplier" -> BUSINESS.spaceMultiplier,
          "firstSpaceMultiplier" -> FIRST.spaceMultiplier,
          "directDemand" -> directDemand,
          "fromAirportBusinessDemand" -> directFromAirportBusinessDemand,
          "toAirportBusinessDemand" -> directToAirportBusinessDemand,
          "fromAirportTouristDemand" -> directFromAirportTouristDemand,
          "toAirportTouristDemand" -> directToAirportTouristDemand,
          "cost" -> cost).+("modelPlanLinkInfo", Json.toJson(planLinkInfoByModel.toList))

        estimatedDifficulty.foreach { difficulty => resultObject = resultObject + ("estimatedDifficulty" -> JsNumber(difficulty)) }


        val competitorLinkConsumptions = (LinkSource.loadFlightLinksByAirports(fromAirportId, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadFlightLinksByAirports(toAirportId, fromAirportId, LinkSource.ID_LOAD)).flatMap { link =>
          LinkSource.loadLinkConsumptionsByLinkId(link.id, 1)
        }
        var otherLinkArray = Json.toJson(competitorLinkConsumptions.filter(_.link.capacity.total > 0).map { linkConsumption => Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite) }.toSeq)
        resultObject = resultObject + ("otherLinks", otherLinkArray)

        val nearbyFromAirports = loadGenericTransitAirports(fromAirport)
        val nearbyToAirports = loadGenericTransitAirports(toAirport)
        val competitorViaLocalTransitLinkConsumptions : List[LinkConsumptionDetails] = {
          val viaLocalTransitFlightLinks : List[Link] =
            nearbyFromAirports.flatMap { localTransitAirport =>
              (nearbyToAirports.map(_.id) :+ toAirportId).flatMap { toAirportId =>
                LinkSource.loadFlightLinksByAirports(localTransitAirport.id, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadFlightLinksByAirports(toAirportId, localTransitAirport.id, LinkSource.ID_LOAD)
              }
            } ++
            nearbyToAirports.flatMap { localTransitAirport =>
              (nearbyFromAirports.map(_.id) :+ fromAirportId).flatMap { fromAirportId =>
                LinkSource.loadFlightLinksByAirports(localTransitAirport.id, fromAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadFlightLinksByAirports(fromAirportId, localTransitAirport.id, LinkSource.ID_LOAD)
              }
            }
          LinkSource.loadLinkConsumptionsByLinksId(viaLocalTransitFlightLinks.map(_.id), 1)
        }
        val otherViaLocalTransitLinkArray = Json.toJson(competitorViaLocalTransitLinkConsumptions.filter(_.link.capacity.total > 0).map { linkConsumption => {
          var linkConsumptionJson : JsObject = Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite).as[JsObject]
          if (nearbyFromAirports.contains(linkConsumption.link.to)) {
            linkConsumptionJson = linkConsumptionJson + ("altFrom" -> JsString(linkConsumption.link.to.iata))
          } else if (nearbyFromAirports.contains(linkConsumption.link.from)) {
            linkConsumptionJson = linkConsumptionJson + ("altFrom" -> JsString(linkConsumption.link.from.iata))
          }

          if (nearbyToAirports.contains(linkConsumption.link.to)) {
            linkConsumptionJson = linkConsumptionJson + ("altTo" -> JsString(linkConsumption.link.to.iata))
          } else if (nearbyToAirports.contains(linkConsumption.link.from)) {
            linkConsumptionJson = linkConsumptionJson + ("altTo" -> JsString(linkConsumption.link.from.iata))
          }

          linkConsumptionJson
        } })
        resultObject = resultObject + ("otherViaLocalTransitLinks", otherViaLocalTransitLinkArray)

        if (existingLink.isDefined) {
          resultObject = resultObject + ("existingLink", Json.toJson(existingLink))
          val deleteRejection = getDeleteLinkRejection(existingLink.get, request.user)
          if (deleteRejection.isDefined) {
            resultObject = resultObject + ("deleteRejection", Json.toJson(deleteRejection.get))
          }
        }

        rejectionReason.foreach { reason =>
          val (description, rejectionType) = reason
          resultObject = resultObject + ("rejection", Json.obj("description" -> description, "type" -> rejectionType.toString))
        }

        if (!warnings.isEmpty) {
          resultObject = resultObject + ("warnings", Json.toJson(warnings))
        }

        resultObject = resultObject + ("toCountryRelationship" -> Json.toJson(AirlineCountryRelationship.getAirlineCountryRelationship(toAirport.countryCode, airline)))
        resultObject = resultObject + ("toCountryTitle" -> Json.toJson(CountryAirlineTitle.getTitle(toAirport.countryCode, airline)))

        Ok(resultObject)
      }
      case Left(error) => BadRequest(error)
    }
  }

  def getDeleteLinkRejection(link : Link, airline : Airline) : Option[String] = {
    if (airline.getBases().filterNot(_.headquarter).map { _.airport.id}.contains(link.to.id)) {
      //then make sure there's still some link other then this pointing to the target
      if (LinkSource.loadFlightLinksByAirlineId(airline.id).filter(_.to.id == link.to.id).size == 1) {
        Some("Cannot delete this route as this flies to a base. Must remove the base before this can be deleted")
      } else { //ok, more than 1 link
        None
      }
    } else {
      None
    }
  }


  object RejectionType extends Enumeration {
    type RejectionType = Value
    val NO_BASE, TITLE_REQUIREMENT, AIRLINE_GRADE, DISTANCE, NO_CASH, NEGOTIATION_COOL_DOWN, DUPLICATED_LINK = Value
  }

  def getRejectionReason(airline : Airline, fromAirport: Airport, toAirport : Airport, existingLink : Option[Link]) : Option[(String, RejectionType.Value)]= {
    import RejectionType._
    if (airline.getCountryCode.isEmpty) {
      return Some(("Airline has no HQ!", NO_BASE))
    }
    val toCountryCode = toAirport.countryCode

    existingLink match {
      case None => //new link
        //validate there's no existing link with opposite direction
        LinkSource.loadFlightLinkByAirportsAndAirline(toAirport.id, fromAirport.id, airline.id).foreach { _ =>
          return Some(("Cannot create this route as your airline already has one flying between these 2 airports"), DUPLICATED_LINK)
        }

        //validate from airport is a base
        val base = fromAirport.getAirlineBase(airline.id) match {
          case None => return Some(("Cannot fly from this airport, this is not a base!", NO_BASE))
          case Some(base) => base
        }


        val flightCategory = FlightType.getCategory(Computation.getFlightType(fromAirport, toAirport))
        //check title status
//        if (flightCategory == FlightCategory.INTERCONTINENTAL) {
//          val requiredTitle = if (toAirport.isGateway()) Title.APPROVED_AIRLINE else Title.PRIVILEGED_AIRLINE
//          val currentTitle = CountryAirlineTitle.getTitle(toCountryCode, airline)
//          val ok = currentTitle.title.id <= requiredTitle.id //smaller value means higher title
//
//          if (!ok) {
//            return Some((s"Cannot fly Intercontinental to this ${if (toAirport.isGateway()) "Gateway" else "Non-gateway"} airport until your airline attain title ${Title.description(requiredTitle)} with ${CountryCache.getCountry(toCountryCode).get.name}", TITLE_REQUIREMENT))
//          }
//        }
        if (fromAirport == toAirport) {
          return Some("Departure and Destination airports cannot be the same. Click and select a different destination airport.", DISTANCE)
        }

        //check distance
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        if (distance <= DemandGenerator.MIN_DISTANCE) {
          return Some("Route must be longer than " + DemandGenerator.MIN_DISTANCE + " km", DISTANCE)
        }

        //check balance
        val cost = Computation.getLinkCreationCost(fromAirport, toAirport)
        if (airline.getBalance() < cost) {
          return Some("Not enough cash to establish this route", NO_CASH)
        }
      case Some(existingLink) => //nothing

    }


    return None
  }

  def getNegotiationRejectionReason(airline : Airline, fromAirport: Airport, toAirport : Airport, existingLink : Option[Link]) : Option[(String, RejectionType.Value)]= {
      LinkSource.loadNegotiationCoolDownExpirationCycle(airline, fromAirport, toAirport).foreach { expirationCycle =>
        return Some(s"Can only re-negotiate in ${expirationCycle - CycleSource.loadCycle()} week(s)", RejectionType.NEGOTIATION_COOL_DOWN)
      }

    return None
  }

  def getWarnings(airline : Airline, fromAirport: Airport, toAirport : Airport, newLink : Boolean) : List[String]= {
    val warnings = ListBuffer[String]()
    if (newLink) { //then check the hub capacity
      airline.getBases().find(_.airport.id == fromAirport.id) match {
          case Some(base) =>
            //val linkCount = LinkSource.loadLinksByCriteria(List(("from_airport", base.airport.id), ("airline", airline.id)), LinkSource.ID_LOAD).length
            //val airlineCountryTitleOfFromCountry = CountrySource.loadCountryAirlineTitlesByCountryCode(fromAirport.countryCode).find(_.airline.id == airline.id)
            //val linkLimit = base.getLinkLimit(airlineCountryTitleOfFromCountry.map(_.title))
//            val staffRequired = LinkSource.loadLinksByCriteria(List(("from_airport", base.airport.id), ("airline", airline.id)), LinkSource.SIMPLE_LOAD).map(_.getOfficeStaffRequired).sum
//            val staffCapacity = base.getOfficeStaffCapacity
//            val newStaffRequired = staffRequired + Link.getOfficeStaffRequired(fromAirport, toAirport)
//
//            val extraCompensation = base.getOvertimeCompensation(staffCapacity, staffRequired) - base.getOvertimeCompensation(staffCapacity, newStaffRequired)
//
//            if (extraCompensation > 0) { //then we should prompt warning of over limit
//              warnings.append(s"Exceeding operation capacity of current base. Extra overtime compensation of $$$extraCompensation will be charged per week for this route.")
//            }
          case None => //should not be none
      }
    }
    warnings.toList
  }

//  def getVipRoutes() = Action {
//    Ok(Json.toJson(RouteHistorySource.loadVipRoutes()))
//  }
  
  def getRelatedLinkConsumption(airlineId : Int, linkId : Int, cycleDelta : Int, selfOnly : Boolean, economy : Boolean, business : Boolean, first : Boolean) =  AuthenticatedAirline(airlineId) {
    LinkSource.loadFlightLinkById(linkId, LinkSource.SIMPLE_LOAD) match {
      case Some(link) => {
        if (link.airline.id != airlineId) {
          Forbidden(Json.obj())
        } else {
          val filter = (linkConsideration : LinkConsideration) => {
            if (economy && business && first) {
              true
            } else {
              (linkConsideration.linkClass == ECONOMY && economy) || (linkConsideration.linkClass == BUSINESS && business) || (linkConsideration.linkClass == FIRST && first)
            }
          }
          Ok(Json.toJson(HistoryUtil.loadConsumptionByLink(link, filter, cycleDelta, selfOnly)))
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

  def setTargetServiceQuality(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      Try(request.body.asInstanceOf[AnyContentAsJson].json.\("targetServiceQuality").as[Int]) match {
        case Success(targetServiceQuality) =>
          if (targetServiceQuality < 0) {
            BadRequest("Cannot have negative targetServiceQuality")
          } else if (targetServiceQuality > 100) {
            BadRequest(s"Cannot have targetServiceQuality $targetServiceQuality")
          } else {
            val airline = request.user
            airline.setTargetServiceQuality(targetServiceQuality)
            AirlineSource.saveAirlineInfo(airline, updateBalance = false)
            Ok(Json.obj("targetServiceQuality" -> JsNumber(targetServiceQuality)))
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
          airline.setMaintenanceQuality(maintenanceQuality)
          AirlineSource.saveAirlineInfo(airline, updateBalance = false)
          Ok(Json.obj("serviceFunding" -> JsNumber(maintenanceQuality)))
        case Failure(_) =>
          BadRequest("Cannot Update maintenance quality")
      }

    } else {
      BadRequest("Cannot Update maintenance quality")
    }
  }

  val TOP_COMMENT_COUNT = 5
  def getLinkComposition(airlineId : Int, linkId : Int) =  AuthenticatedAirline(airlineId) { request =>
    val consumptionEntries : List[LinkConsumptionHistory] = ConsumptionHistorySource.loadConsumptionByLinkId(linkId)
    val consumptionByCountry = consumptionEntries.groupBy(_.homeAirport.countryCode).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByHomeAirport = consumptionEntries.groupBy(_.homeAirport).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByDestinationAirport = consumptionEntries.groupBy(_.destinationAirport).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPassengerType = consumptionEntries.groupBy(_.passengerType).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPreferenceType = consumptionEntries.groupBy(_.preferenceType).view.mapValues(entries => entries.map(_.passengerCount).sum)

    var countryJson = Json.arr()
    consumptionByCountry.foreach {
      case (countryCode, passengerCount) =>
        countryByCode.get(countryCode).foreach { country => //just in case the first turn after patch this will be ""
          countryJson = countryJson.append(Json.obj("countryName" -> country.name, "countryCode" -> countryCode, "passengerCount" -> passengerCount))
        }

    }

    var homeAirportsJson = Json.arr()
    consumptionByHomeAirport.toList.sortBy(_._2)(Ordering[Int].reverse).take(30).foreach {
      case (airport, passengerCount) =>
        homeAirportsJson = homeAirportsJson.append(Json.obj("airport" -> airport.displayText, "countryCode" -> airport.countryCode, "passengerCount" -> passengerCount))
    }

    var destinationAirportsJson = Json.arr()
    consumptionByDestinationAirport.toList.sortBy(_._2)(Ordering[Int].reverse).take(30).foreach {
      case (airport, passengerCount) =>
        destinationAirportsJson = destinationAirportsJson.append(Json.obj("airport" -> airport.displayText, "countryCode" -> airport.countryCode, "passengerCount" -> passengerCount))
    }

    var passengerTypeJson = Json.arr()
    consumptionByPassengerType.foreach {
      case (passengerType, passengerCount) => passengerTypeJson = passengerTypeJson.append(Json.obj("title" -> getPassengerTypeTitle(passengerType), "passengerCount" -> passengerCount))
    }

    var preferenceTypeJson = Json.arr()
    consumptionByPreferenceType.foreach {
      case (preferenceType, passengerCount) => preferenceTypeJson = preferenceTypeJson.append(Json.obj("title" -> preferenceType.title, "description" -> preferenceType.description, "passengerCount" -> passengerCount))
    }

    var satisfactionByClassJson = Json.arr()
    var satisfactionByPreferenceJson = Json.arr()
    var topPositiveCommentsJson = Json.arr()
    var topNegativeCommentsJson = Json.arr()
    var topPositiveCommentsByClassJson = Json.obj()
    var topNegativeCommentsByClassJson = Json.obj()
    var topPositiveCommentsByPreferenceJson = Json.obj()
    var topNegativeCommentsByPreferenceJson = Json.obj()


    LinkSource.loadFlightLinkById(linkId).foreach { link =>
      val consumptionByLinkClass = consumptionEntries.groupBy(_.linkClass)
      val satisfactionByLinkClass = consumptionByLinkClass.view.mapValues(entries => (entries.map( entry => entry.satisfaction * entry.passengerCount)).sum / entries.map(_.passengerCount).sum)

      satisfactionByLinkClass.foreach {
        case (linkClass, satisfaction) => satisfactionByClassJson = satisfactionByClassJson.append(Json.obj("title" -> linkClass.label, "level" -> linkClass.level, "satisfaction" -> BigDecimal(satisfaction).setScale(2, RoundingMode.HALF_UP)))
      }

      //value is List[(PassengerCount, Satisfaction)], group by actual link class taken, but computation satisfaction based on the preferred link class
      val consumptionSatisfactionByPreferenceAndLinkClass : MapView[(FlightPreferenceType.Value, LinkClass), List[(Int, Double)]] = consumptionEntries.groupBy(entry => (entry.preferenceType, entry.linkClass)).view.mapValues(_.map { consumption => (consumption.passengerCount, consumption.satisfaction)})
      //flatten key to remove linkClass to become Map[FlightPreferenceType, List[(Int, Double)]]
      val consumptionSatisfactionByPreference = mutable.Map[FlightPreferenceType.Value, ListBuffer[(Int, Double)]]()
      consumptionSatisfactionByPreferenceAndLinkClass.foreach {
        case((preference, linkClass), passengerCountWithSatisfaction) => consumptionSatisfactionByPreference.getOrElseUpdate(preference, ListBuffer()).appendAll(passengerCountWithSatisfaction)
      }
      val averageSatisfactionByPreference : MapView[FlightPreferenceType.Value, Double] = consumptionSatisfactionByPreference.view.mapValues { entries =>
        entries.map(entry => entry._1 * entry._2).sum / entries.map(_._1).sum
      }

      averageSatisfactionByPreference.foreach {
        case (preferenceType, satisfaction) => satisfactionByPreferenceJson = satisfactionByPreferenceJson.append(Json.obj("title" -> preferenceType.title, "id" -> preferenceType.id, "description" -> preferenceType.description, "satisfaction" -> BigDecimal(satisfaction).setScale(2, RoundingMode.HALF_UP)))
      }


      val comments : Predef.Map[(LinkClass, FlightPreferenceType.Value), LinkCommentSummary] = LinkCommentUtil.simulateComments(consumptionEntries, request.user, link)
      val sampleSizeByClass : MapView[LinkClass, Int] = comments.toList.groupBy(_._1._1).view.mapValues(_.map { _._2.sampleSize }.sum)
      val sampleSizeByPreference : MapView[FlightPreferenceType.Value, Int] = comments.toList.groupBy(_._1._2).view.mapValues(_.map { _._2.sampleSize }.sum)
      val sampleSize = comments.values.map(_.sampleSize).sum

      val overallCommentStats : MapView[LinkComment, Double] = comments.values.flatMap(_.comments).groupBy(x => x).view.mapValues(comments => comments.size.toDouble / sampleSize)
      val topPositiveStats = overallCommentStats.toList.filter(_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT)
      val topNegativeStats = overallCommentStats.toList.filter(!_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT)
      topPositiveCommentsJson = Json.toJson(topPositiveStats).asInstanceOf[JsArray]
      topNegativeCommentsJson = Json.toJson(topNegativeStats).asInstanceOf[JsArray]

      val commentByClass : MapView[LinkClass, List[LinkComment]] = comments.toList.groupBy(_._1._1).view.mapValues(_.map { _._2.comments }.flatten)
      val commentStatsByClass : immutable.Map[LinkClass, MapView[LinkComment, Double]] = commentByClass.map {
        case((linkClass, comments)) =>
          val ratio = comments.groupBy(x => x).view.mapValues(_.length.toDouble / sampleSizeByClass(linkClass))
          (linkClass, ratio)
      }.toMap
      val topPositiveStatsByClass = commentStatsByClass.view.mapValues(stats => stats.toList.filter(_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT))
      val topNegativeStatsByClass = commentStatsByClass.view.mapValues( stats => stats.toList.filter(!_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT))
      topPositiveStatsByClass.foreach {
        case (linkClass, stats) =>  topPositiveCommentsByClassJson = topPositiveCommentsByClassJson + (linkClass.level.toString -> Json.toJson(stats))
      }
      topNegativeStatsByClass.foreach {
        case (linkClass, stats) =>  topNegativeCommentsByClassJson = topNegativeCommentsByClassJson + (linkClass.level.toString -> Json.toJson(stats))
      }


      val commentByPreference : MapView[FlightPreferenceType.Value, List[LinkComment]] = comments.toList.groupBy(_._1._2).view.mapValues(_.map { _._2.comments }.flatten)
      val commentStatsByPreference : immutable.Map[FlightPreferenceType.Value, MapView[LinkComment, Double]] = commentByPreference.map {
        case((preference, comments)) =>
          val ratio = comments.groupBy(x => x).view.mapValues(_.length.toDouble / sampleSizeByPreference(preference))
          (preference, ratio)
      }.toMap
      val topPositiveStatsByPreference = commentStatsByPreference.view.mapValues( stats => stats.toList.filter(_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT))
      val topNegativeStatsByPreference = commentStatsByPreference.view.mapValues( stats => stats.toList.filter(!_._1.positive).sortBy(_._2).reverse.take(TOP_COMMENT_COUNT))
      topPositiveStatsByPreference.foreach {
        case (preference, stats) =>  topPositiveCommentsByPreferenceJson = topPositiveCommentsByPreferenceJson + (preference.id.toString -> Json.toJson(stats))
      }
      topNegativeStatsByPreference.foreach {
        case (preference, stats) =>  topNegativeCommentsByPreferenceJson = topNegativeCommentsByPreferenceJson + (preference.id.toString -> Json.toJson(stats))
      }

//      println(topPositiveStatsByClass.toMap)
//      println(topNegativeStatsByClass.toMap)
//      println(topPositiveStatsByPreference.toMap)
//      println(topNegativeStatsByPreference.toMap)
    }



    Ok(Json.obj("country" -> countryJson,
      "homeAirports" -> homeAirportsJson,
      "destinationAirports" -> destinationAirportsJson,
      "passengerType" -> passengerTypeJson,
      "preferenceType" -> preferenceTypeJson,
      "linkClassSatisfaction" -> satisfactionByClassJson,
      "preferenceSatisfaction" -> satisfactionByPreferenceJson,
      "topPositiveComments" -> topPositiveCommentsJson,
      "topPositiveCommentsByClass" -> topPositiveCommentsByClassJson,
      "topPositiveCommentsByPreference" -> topPositiveCommentsByPreferenceJson,
      "topNegativeComments" -> topNegativeCommentsJson,
      "topNegativeCommentsByClass" -> topNegativeCommentsByClassJson,
      "topNegativeCommentsByPreference" -> topNegativeCommentsByPreferenceJson
    ))
  }





  def getLinkRivalHistory(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) {
    var result = Json.obj()
    //get competitor history
    LinkSource.loadFlightLinkById(linkId).foreach { link =>
      //find all link with same from and to
      val overlappingLinks = LinkSource.loadFlightLinksByAirports(link.from.id, link.to.id) ++ LinkSource.loadFlightLinksByAirports(link.to.id, link.from.id)
      val rivals = scala.collection.mutable.HashSet[Airline]()

      var overlappingLinksJson = Json.arr()
      overlappingLinks.filter(_.capacity.total > 0).foreach { overlappingLink => //only work on links that have capacity
        overlappingLinksJson = overlappingLinksJson.append(Json.toJson(LinkSource.loadLinkConsumptionsByLinkId(overlappingLink.id, cycleCount))(Writes.list(MinimumLinkConsumptionWrite)))
        rivals += overlappingLink.airline
      }

      result = result + ("overlappingLinks" -> overlappingLinksJson)
    }

    Ok(result)
  }

  def getLinkRivalDetails(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) {
    var result = Json.obj()
    //get competitor history
    LinkSource.loadFlightLinkById(linkId).foreach { link =>
      //find all link with same from and to
      val overlappingLinks = LinkSource.loadFlightLinksByAirports(link.from.id, link.to.id) ++ LinkSource.loadFlightLinksByAirports(link.to.id, link.from.id)
      val rivals = scala.collection.mutable.HashSet[Airline]()

      overlappingLinks.filter(_.capacity.total > 0).foreach { overlappingLink => //only work on links that have capacity
        rivals += overlappingLink.airline
      }

      val fromAirportLinks = LinkSource.loadFlightLinksByFromAirport(link.from.id) ++ LinkSource.loadFlightLinksByToAirport(link.from.id)
      val toAirportLinks =  LinkSource.loadFlightLinksByFromAirport(link.to.id) ++ LinkSource.loadFlightLinksByToAirport(link.to.id)

      val fromAirport = AirportCache.getAirport(link.from.id, fullLoad = true).get
      val toAirport = AirportCache.getAirport(link.to.id, fullLoad = true).get

      //check the network capacity of rival (including self here)
      var fromAirportInfo = Json.arr()
      var toAirportInfo = Json.arr()
      rivals.foreach { rival => //check alliance
        val networkAirlineIds : List[Int] =
          (AllianceSource.loadAllianceMemberByAirline(rival) match {
            case Some(allianceMember) => AllianceCache.getAlliance(allianceMember.allianceId).get.members.map(_.airline)
            case None => List(rival)
          }).map(_.id)

        //now check network capacity on the from Airport
        val fromAirportAllianceLinks = fromAirportLinks.filter(fromAirportLink => networkAirlineIds.contains(fromAirportLink.airline.id) && fromAirportLink.id != link.id)
        val toAirportAllianceLinks = toAirportLinks.filter(toAirportLink => networkAirlineIds.contains(toAirportLink.airline.id) && toAirportLink.id != link.id)

        val fromAirportAllianceNetworkCapacity = fromAirportAllianceLinks.foldLeft(LinkClassValues.getInstance()) { (container, link) =>
          container + link.capacity
        }
        val toAirportAllianceNetworkCapacity = toAirportAllianceLinks.foldLeft(LinkClassValues.getInstance()) { (container, link) =>
          container + link.capacity
        }

        var fromAirportJson = Json.obj("airline" -> rival, "network" -> fromAirportAllianceNetworkCapacity, "loyalty" -> fromAirport.getAirlineLoyalty(rival.id))
        var toAirportJson = Json.obj("airline" -> rival, "network" -> toAirportAllianceNetworkCapacity, "loyalty" -> toAirport.getAirlineLoyalty(rival.id))

        //check lounges
        getLoungeJson(rival, fromAirport).foreach { loungeJson =>
          fromAirportJson = fromAirportJson + ("lounge" -> loungeJson)
        }
        getLoungeJson(rival, toAirport).foreach { loungeJson =>
          toAirportJson = toAirportJson + ("lounge" -> loungeJson)
        }
        fromAirportInfo = fromAirportInfo.append(fromAirportJson)
        toAirportInfo = toAirportInfo.append(toAirportJson)
      }

      result = result ++ Json.obj("fromAirport" -> fromAirportInfo, "fromAirportCode" -> fromAirport.iata, "fromCity" -> fromAirport.city)
      result = result ++ Json.obj("toAirport" -> toAirportInfo, "toAirportCode" -> toAirport.iata, "toCity" -> toAirport.city)
    }

    Ok(result)
  }

  def getLoungeJson(airline : Airline, airport : Airport) : Option[JsValue] = {
    airport.getLoungeByAirline(airline.id, true) match {
      case Some(lounge) =>
        return Some(Json.toJson(lounge))
      case None =>
        airline.getAllianceId().foreach { allianceId =>
          airport.getLoungeByAlliance(allianceId, true).foreach { lounge =>
            return Some(Json.toJson(lounge))
          }
        }
    }
    return None
  }



  class LinkChangeToEventWrites(currentCycle : Int, originalLink : Link) extends Writes[LinkChange] {
    def writes(linkChange : LinkChange) : JsValue = {
      var priceJson = Json.obj()
      var priceDeltaJson = Json.obj()
      var capacityDeltaJson = Json.obj()

      var hasCapacityChange = false
      var hasPriceChange = false
      LinkClass.values.foreach { linkClass =>
        if (linkChange.capacity(linkClass) > 0) { //do not report price for link class that has no capacity
          priceJson = priceJson + (linkClass.label -> JsNumber(linkChange.price(linkClass)))
          if (linkChange.priceDelta(linkClass) != 0) { //do not put delta if price has not been changed
            hasPriceChange = true
            priceDeltaJson = priceDeltaJson + (linkClass.label -> JsNumber(linkChange.priceDelta(linkClass)))
          }
        }
        if (linkChange.capacityDelta(linkClass) != 0) {
          hasCapacityChange = true
          capacityDeltaJson = capacityDeltaJson + (linkClass.label -> JsNumber(linkChange.capacityDelta(linkClass)))
        }
      }

      capacityDeltaJson = capacityDeltaJson + ("total" -> JsNumber(linkChange.capacityDelta.total))


      val description = s"Flight ${linkChange.fromAirport.displayText} -> ${linkChange.toAirport.displayText}"


      val effectiveCycle = linkChange.cycle + 1 //effect is seen 1 cycle after...

      val matchFrom =
        originalLink.from.id == linkChange.fromAirport.id || originalLink.from.id == linkChange.toAirport.id
      val matchTo =
        originalLink.to.id == linkChange.fromAirport.id || originalLink.to.id == linkChange.toAirport.id

      Json.obj(
        "airlineId" -> linkChange.airline.id,
        "airlineName" -> linkChange.airline.name,
        "linkId" -> linkChange.linkId,
        "description" -> description,
        "price" -> priceJson,
        "priceDelta" -> priceDeltaJson,
        "capacity" -> Json.toJson(linkChange.capacity),
        "capacityTotal" -> JsNumber(linkChange.capacity.total),
        "capacityDelta" -> capacityDeltaJson,
        "capacityDeltaTotal" -> JsNumber(linkChange.capacityDelta.total),
        "frequency" -> linkChange.frequency,
        "flightCode" -> LinkUtil.getFlightCode(linkChange.airline, linkChange.flightNumber),
        "matchFrom" -> matchFrom,
        "matchTo" -> matchTo,
        "cycle" -> effectiveCycle,
        "cycleDelta" -> (effectiveCycle - currentCycle))
    }
  }

  class AllianceHistoryToEventWrites(currentCycle : Int) extends Writes[AllianceHistory] {
    def writes(entry : AllianceHistory) : JsValue = {
      val event = AllianceUtil.getAllianceEventText(entry.event)


      val description = s"${entry.airline.name} $event ${entry.allianceName}"
      val effectiveCycle = entry.cycle + 1 //effect is seen 1 cycle after...

      Json.obj(
        "airlineId" -> entry.airline.id,
        "airlineName" -> entry.airline.name,
        "description" -> description,
        "cycle" -> effectiveCycle,
        "cycleDelta" -> (effectiveCycle - currentCycle))
    }
  }


  /**
   * Loads various events related to this link, this could be rival changes, connection changes, olympics, airline resets etc
   * @param airlineId
   * @param linkId
   * @param cycleCount
   * @return
   */
  def getLinkRelatedEventHistory(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) { implicit request =>
    var result = Json.arr()

    val currentCycle = CycleSource.loadCycle()
    val fromCycle = currentCycle - cycleCount
    LinkSource.loadFlightLinkById(linkId).foreach { link =>
      //load change history of this airport pair
      var linkChanges = loadLinkChangeHistory(link.from, fromCycle, currentCycle)
      val fromLinkIds = linkChanges.map(_.linkId)
      linkChanges = linkChanges ++ loadLinkChangeHistory(link.to, fromCycle, currentCycle).filter(entry => !fromLinkIds.contains(entry.linkId))

      linkChanges = linkChanges.filter { entry =>
        val perfectMatch = entry.linkId == linkId ||
        (entry.toAirport.id == link.to.id && entry.fromAirport.id == link.from.id) ||
        (entry.toAirport.id == link.from.id && entry.fromAirport.id == link.to.id)
        if (!perfectMatch) {
          val matchingAirport =
            if (entry.toAirport.id == link.to.id || entry.fromAirport.id == link.to.id) {
              link.to
            } else {
              link.from
            }

          //see if the capacity matter enough. for now only check the airport pop ratio. Not perfect, but want something simple for now
          entry.capacity.total >= matchingAirport.population / 2500 || Math.abs(entry.capacityDelta.total) >= matchingAirport.population / 5000
        } else {
          true
        }
      }

      val linkChangesJson = Json.toJson(linkChanges)(Writes.list(new LinkChangeToEventWrites(currentCycle, link))).as[JsArray]
      result = result ++ linkChangesJson
    }

    //self alliance history
    result = result ++ Json.toJson(AllianceSource.loadAllianceHistoryByAirline(airlineId).filter(_.cycle >= fromCycle).filter(entry => isRelevantAllianceHistoryEvent(entry.event)))(Writes.list(new AllianceHistoryToEventWrites(currentCycle))).as[JsArray]
    //other airline alliance history (complicate if current airline left/joined during the period, for now just get the history of current alliance
    request.user.getAllianceId().foreach { allianceId =>
      AllianceSource.loadAllianceById(allianceId).foreach { alliance =>
        result = result ++
          Json.toJson(AllianceSource.loadAllianceHistoryByAllianceName(alliance.name).filter {
            entry => entry.cycle >= fromCycle && entry.airline.id != airlineId && isRelevantAllianceHistoryEvent(entry.event)
          })(Writes.list(new AllianceHistoryToEventWrites(currentCycle))).as[JsArray]
      }
    }

    //events such as olympics
    result = result ++ getEventHistoryJsArray(currentCycle, fromCycle)
    result = result ++ getAirlineMajorRestructureJsArray(currentCycle, fromCycle)

    Ok(result)
  }

  /**
   * Loads rival (and self) consumption history for comparison
   * @param airlineId
   * @param linkId
   * @param cycleCount
   * @return
   */
  def getLinkRelatedRivalHistory(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) { implicit request =>
    var result = Json.obj()

    LinkSource.loadFlightLinkById(linkId).foreach { link =>
      //load change history of this airport pair
      val relatedLinks = LinkSource.loadFlightLinksByAirports(link.from.id, link.to.id) ++ LinkSource.loadFlightLinksByAirports(link.to.id, link.from.id)
      val consumptionsByCycle = LinkSource.loadLinkConsumptionsByLinksId(relatedLinks.map(_.id), cycleCount).groupBy(_.cycle).toList.sortBy(_._1)
      consumptionsByCycle.foreach {
        case(cycle, consumptions) =>
          var cycleJson = Json.obj()
          consumptions.sortBy(_.link.airline.id).foreach { consumption =>
            cycleJson = cycleJson + (consumption.link.airline.id.toString -> Json.obj(
              "airlineName" -> consumption.link.airline.name,
              "passenger"-> consumption.link.getTotalSoldSeats
            ))
          }
          result = result + (cycle.toString -> cycleJson)
      }

      result
    }



    Ok(result)
  }

  var eventHistoryJsArrayCache = Json.arr()
  val eventHistoryLoadCycle = new AtomicInteger(-1)

  def getEventHistoryJsArray(currentCycle : Int, fromCycle : Int) : JsArray = {
    //see if it's cached
    eventHistoryLoadCycle.synchronized {
      if (eventHistoryLoadCycle.get() != currentCycle) {
        var result = Json.arr()
        EventSource.loadEvents().filter(event => event.startCycle + event.duration >= fromCycle).foreach { event =>
          event match {
            case olympics : Olympics =>
              //meh kinda ugly to just simulate status by iteration...
              var walkerStatus = olympics.status(fromCycle)
              for (cycle <- (olympics.startCycle + 1) to currentCycle) {
                if (olympics.status(cycle) != walkerStatus) {
                  if (cycle >= fromCycle) {
                    Olympics.getSelectedAirport(event.id).foreach { selectedAirport =>
                      val description = s"${selectedAirport.city} Olympics : ${OlympicsUtil.getStatusText(olympics, cycle)}"
                      result = result :+ Json.obj(
                        "description" -> description,
                        "descriptionCountryCode" -> selectedAirport.countryCode,
                        "cycle" -> cycle,
                        "cycleDelta" -> (cycle - currentCycle))
                    }
                  }
                  walkerStatus = olympics.status(cycle)
                }
              }
            case _ =>
          }
        }

        eventHistoryJsArrayCache = result
        eventHistoryLoadCycle.set(currentCycle)
      }
    }
    eventHistoryJsArrayCache
  }

  var airlineMajorRestructureJsArrayCache = Json.arr()
  val airlineMajorRestructureLoadCycle = new AtomicInteger(-1)
  def getAirlineMajorRestructureJsArray(currentCycle : Int, fromCycle : Int) : JsArray = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    //see if it's cached
    airlineMajorRestructureLoadCycle.synchronized {
      if (airlineMajorRestructureLoadCycle.get() != currentCycle) {
        var result = Json.arr()
        val query = s"SELECT * FROM ${Constants.LINK_CHANGE_HISTORY_TABLE} WHERE cycle >= ? AND cycle <> ? AND capacity_delta <= ?" //exclude current cycle as it does not affect anything until next cycle...

        val linkCutHistory = ChangeHistorySource.loadLinkChangeByQueryString(query, List(fromCycle, currentCycle, -1000))

        linkCutHistory.groupBy(_.airline).foreach {
          case(airline, linkCuts) => linkCuts.groupBy(_.cycle).foreach {
            case(cycle, cutsThisCycle) =>
              val totalCapacityChange = cutsThisCycle.map(_.capacityDelta.total).sum
              if (totalCapacityChange <= -5000) { //if total delta <= -5000 consider a major restructure
                val description = s"Major capacity cut of ${formatter.format(totalCapacityChange * -1)} on ${linkCuts.length} route(s)"
                val effectiveCycle = cycle + 1
                result = result :+ Json.obj(
                  "description" -> description,
                  "airlineId" -> airline.id,
                  "airlineName" -> airline.name,
                  "cycle" -> effectiveCycle,
                  "cycleDelta" -> (effectiveCycle - currentCycle))
              }
          }
        }

        airlineMajorRestructureJsArrayCache = result
        airlineMajorRestructureLoadCycle.set(currentCycle)
      }
    }
    airlineMajorRestructureJsArrayCache
  }


  def isRelevantAllianceHistoryEvent(event : AllianceEvent.Value)  = {
    event match {
      case com.patson.model.AllianceEvent.JOIN_ALLIANCE => true
      case com.patson.model.AllianceEvent.LEAVE_ALLIANCE => true
      case com.patson.model.AllianceEvent.BOOT_ALLIANCE => true
      case _ => false
    }
  }


  def loadLinkChangeHistory(airport : Airport, fromCycle : Int, currentCycle : Int): List[LinkChange] = {
    val query = s"SELECT * FROM ${Constants.LINK_CHANGE_HISTORY_TABLE} WHERE (from_airport = ? OR to_airport = ?) AND cycle >= ? AND cycle <> ?" //exclude current cycle as it does not affect anything until next cycle...

    ChangeHistorySource.loadLinkChangeByQueryString(query, List(airport.id, airport.id, fromCycle, currentCycle))
  }


  /**
    * Loads a pair of aiports, only return Some(fromAirport, toAirport) if BOTH airports are found
    */
  def loadAirports(fromAirportId : Int, toAirportId : Int, fullLoad : Boolean = false) : Option[(Airport, Airport)] = {
    AirportCache.getAirport(fromAirportId, fullLoad) match {
      case Some(fromAirport) =>
        AirportCache.getAirport(toAirportId, fullLoad) match {
          case Some(toAirport) =>
            Some(fromAirport, toAirport)
          case None => None
        }
      case None => None
    }
  }

  def loadGenericTransitAirports(principalAirport : Airport) : List[Airport] = {
    LinkSource.loadLinksByCriteria(List(("from_airport", principalAirport.id), ("transport_type", TransportType.GENERIC_TRANSIT.id))).map(_.to) ++
    LinkSource.loadLinksByCriteria(List(("to_airport", principalAirport.id), ("transport_type", TransportType.GENERIC_TRANSIT.id))).map(_.from)
  }


  def getLinkNegotiation(airlineId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
    //val delegatesCount = request.body.asInstanceOf[AnyContentAsJson].json.\("assignedDelegates").as[Int]
    val existingLinkOption = LinkSource.loadFlightLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId)
    val negotiationInfo = NegotiationUtil.getLinkNegotiationInfo(request.user, incomingLink, existingLinkOption)


    var result = Json.obj("negotiationInfo" -> Json.toJson(negotiationInfo)(NegotiationInfoWrites(incomingLink)),
    "delegateInfo" -> Json.toJson(request.user.getDelegateInfo),
    "toAirport" -> Json.toJson(incomingLink.to),
    "fromAirport" -> Json.toJson(incomingLink.from))

    getNegotiationRejectionReason(request.user, incomingLink.from, incomingLink.to, existingLinkOption).foreach {
      case (reason, rejectionType) => result = result + ("rejection" -> JsString(reason))
    }

    Ok(result)

  }


  val getPassengerTypeTitle = (passengerType : PassengerType.Value) =>  passengerType match {
    case PassengerType.BUSINESS => "Business"
    case PassengerType.TOURIST => "Tourist"
    case PassengerType.OLYMPICS => "Olympics"
  }



  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
  //case class AirplaneWithPlanRouteInfo(airplane : Airplane, duration : Int, maxFrequency : Int, limitingFactor : String, isAssigned : Boolean)
  case class ModelPlanLinkInfo(model: Model, duration : Int, flightMinutesRequired : Int, isAssigned : Boolean, maxFrequency : Int, airplanes : List[(Airplane, Int)])
}

object LinkApplication {

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
