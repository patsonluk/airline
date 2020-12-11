package controllers

import java.util.Calendar

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import com.patson.Util
import com.patson.data.{AirlineSource, AirplaneSource, AirportSource, AllianceSource, ConsumptionHistorySource, CountrySource, CycleSource, DelegateSource, LinkSource}
import com.patson.model._
import com.patson.model.airplane.{Airplane, LinkAssignments, Model}
import models.{LinkHistory, RelatedLink}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.libs.json._
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc._
import com.patson.data.airplane.ModelSource
import play.api.mvc.Security.AuthenticatedRequest
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.DemandGenerator

import scala.collection.{SortedMap, immutable, mutable}
import scala.collection.immutable.ListMap
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.patson.model.LinkConsumptionHistory
import com.patson.model.FlightPreferenceType
import com.patson.util.{AirlineCache, AirportCache, AllianceCache, CountryCache}
import javax.inject.Inject

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
      val lastUpdate = entry.lastUpdate
      Json.toJson(link).asInstanceOf[JsObject] + ("profit" -> JsNumber(profit)) + ("revenue" -> JsNumber(revenue)) + ("passengers" -> Json.toJson(passengers)) + ("lastUpdate" -> JsNumber(lastUpdate.getTimeInMillis))
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

    val fromAirport = AirportSource.loadAirportById(incomingLink.from.id, true).getOrElse(return BadRequest("From airport not found"))
    val toAirport = AirportSource.loadAirportById(incomingLink.to.id, true).getOrElse(return BadRequest("To airport not found"))


    val existingLink : Option[Link] = LinkSource.loadLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId)

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
    val rejectionReason = getRejectionReason(request.user, fromAirport = incomingLink.from, toAirport = incomingLink.to, existingLink.isEmpty)
    if (rejectionReason.isDefined) {
      return BadRequest("Link is rejected: " + rejectionReason.get);
    }

    if (delegateCount > airline.getDelegateInfo().availableCount) {
      return BadRequest(s"Assigning $delegateCount delegates but not enough available");
    }

    if (existingLink.isEmpty) {
      incomingLink.flightNumber = LinkApplication.getNextAvailableFlightNumber(request.user)
    } else {
      incomingLink.flightNumber = existingLink.get.flightNumber
    }

    val negotiationInfo = NegotiationUtil.getLinkNegotiationInfo(airline, incomingLink, existingLink)
    val negotiationResultOption =
      if (negotiationInfo.finalRequirementValue > 0) { //then negotiation is required
        //update delegate status
        val cycle = CycleSource.loadCycle()
        val task = DelegateTask.linkNegotiation(cycle, fromAirport, toAirport)
        val availableCycle = cycle + task.coolDown

        val busyDelegates = (0 until delegateCount).toList.map { _ =>
          BusyDelegate(airline, task, Some(availableCycle))
        }

        DelegateSource.saveBusyDelegates(busyDelegates)

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
              val existingAppeal = toAirport.getAirlineBaseAppeal(airlineId)
              if (existingAppeal.awareness < 5) { //update to 5 for link creation
                AirportSource.updateAirlineAppeal(toAirport.id, airlineId, AirlineAppeal(existingAppeal.loyalty, 5))
              }
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
    if (negotiationResultOption.isDefined) {
      result = result + ("negotiationResult" -> Json.toJson(negotiationResultOption.get))
    }
    return Ok(result)
  }

  def addLink(airlineId : Int) = AuthenticatedAirline(airlineId) { request => addLinkBlock(request) }

  def getLink(airlineId : Int, linkId : Int) = AuthenticatedAirline(airlineId) { request =>
    LinkSource.loadLinkById(linkId, LinkSource.FULL_LOAD) match {
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
     val links = LinkSource.loadAllLinks()
    Ok(Json.toJson(links))
  }

  def getLinks(airlineId : Int, toAirportId : Int) = Action {

    val links =
      if (toAirportId == -1) {
        LinkSource.loadLinksByAirlineId(airlineId)
      } else {
        LinkSource.loadLinksByCriteria(List(("airline", airlineId), ("to_airport", toAirportId)))
      }
    Ok(Json.toJson(links)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }

  def getLinksDetails(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val links = LinkSource.loadLinksByAirlineId(airlineId)
    val consumptions = LinkSource.loadLinkConsumptionsByAirline(airlineId).foldLeft(immutable.Map[Int, LinkConsumptionDetails]()) { (foldMap, linkConsumptionDetails) =>
      foldMap + (linkConsumptionDetails.link.id -> linkConsumptionDetails)
    }
    val lastUpdates : scala.collection.immutable.Map[Int, Calendar] = LinkSource.loadLinkLastUpdates(links.map(_.id))

    val linksWithProfit: Seq[LinkExtendedInfo] = links.map { link =>
      //(link, consumptions.get(link.id).fold(0)(_.profit), consumptions.get(link.id).fold(0)(_.revenue), consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.soldSeats))
      LinkExtendedInfo(link, consumptions.get(link.id).fold(0)(_.profit), consumptions.get(link.id).fold(0)(_.revenue), consumptions.get(link.id).fold(LinkClassValues.getInstance())(_.link.soldSeats), lastUpdates(link.id))
    }
    Ok(Json.toJson(linksWithProfit)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }

  case class LinkExtendedInfo(link : Link, profit : Int, revenue : Int, soldSeats : LinkClassValues, lastUpdate : Calendar)

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
        var existingLink: Option[Link] = LinkSource.loadLinkByAirportsAndAirline(fromAirportId, toAirportId, airlineId)

        val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt

        val rejectionReason = getRejectionReason(request.user, fromAirport, toAirport, existingLink.isEmpty)

        val warnings = getWarnings(request.user, fromAirport, toAirport, existingLink.isEmpty)

        val modelsWithinRange: List[Model] = ModelSource.loadModelsWithinRange(distance)

        val allManufacturingCountries = modelsWithinRange.map(_.countryCode).toSet

        val countryRelations : immutable.Map[String, AirlineCountryRelationship] = allManufacturingCountries.map { countryCode =>
          (countryCode, AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline))
        }.toMap

        val modelsWithinRangeAndRelationship = modelsWithinRange.filter(model => model.purchasableWithRelationship(countryRelations(model.countryCode).relationship))

        val airplanesAssignedToThisLink = new mutable.HashMap[Int, Int]()

        if (existingLink.isDefined) {
          AirplaneSource.loadAirplaneLinkAssignmentsByLinkId(existingLink.get.id).foreach {
            case (airplaneId, linkAssignment) =>
              if (linkAssignment.frequency > 0) {
                airplanesAssignedToThisLink.put(airplaneId, linkAssignment.frequency)
              }
          }
        }

        val ownedAirplanesByModel = AirplaneSource.loadAirplanesByOwner(airlineId).groupBy(_.model)

        //available airplanes are either the ones that are already assigned to this link or have available flight minutes that is >= required minutes
        //group airplanes by model, also add Int to indicated how many frequency is this airplane currently assigned to this link
        val availableAirplanesByModel : immutable.Map[Model, List[(Airplane, Int)]] = modelsWithinRangeAndRelationship.map { model =>
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

            planLinkInfoByModel.append(ModelPlanLinkInfo(model, duration, flightMinutesRequired, assignedModel.isDefined && assignedModel.get.id == model.id, airplaneList))
        }


        var suggestedPrice: LinkClassValues = LinkClassValues.getInstance(Pricing.computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), ECONOMY),
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
        val countryRelationship = CountrySource.getCountryMutualRelationship(fromAirport.countryCode, toAirport.countryCode)
        val directBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, countryRelationship, PassengerType.BUSINESS) + DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, countryRelationship, PassengerType.BUSINESS)
        val directTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, countryRelationship, PassengerType.TOURIST) + DemandGenerator.computeDemandBetweenAirports(toAirport, fromAirport, countryRelationship, PassengerType.TOURIST)

        val directDemand = directBusinessDemand + directTouristDemand
        //val airportLinkCapacity = LinkSource.loadLinksByToAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum + LinkSource.loadLinksByFromAirport(fromAirport.id, LinkSource.ID_LOAD).map { _.capacity.total }.sum

        val cost = if (existingLink.isEmpty) Computation.getLinkCreationCost(fromAirport, toAirport) else 0
        val flightNumber = if (existingLink.isEmpty) LinkApplication.getNextAvailableFlightNumber(request.user) else existingLink.get.flightNumber
        val flightCode = LinkUtil.getFlightCode(request.user, flightNumber)

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
          "suggestedPrice" -> suggestedPrice,
          "economySpaceMultiplier" -> ECONOMY.spaceMultiplier,
          "businessSpaceMultiplier" -> BUSINESS.spaceMultiplier,
          "firstSpaceMultiplier" -> FIRST.spaceMultiplier,
          "directDemand" -> directDemand,
          "businessPassengers" -> directBusinessDemand.total,
          "touristPassengers" -> directTouristDemand.total,
          "cost" -> cost).+("modelPlanLinkInfo", Json.toJson(planLinkInfoByModel.toList))


        val competitorLinkConsumptions = (LinkSource.loadLinksByAirports(fromAirportId, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByAirports(toAirportId, fromAirportId, LinkSource.ID_LOAD)).flatMap { link =>
          LinkSource.loadLinkConsumptionsByLinkId(link.id, 1)
        }
        var otherLinkArray = Json.toJson(competitorLinkConsumptions.filter(_.link.capacity.total > 0).map { linkConsumption => Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite) }.toSeq)
        resultObject = resultObject + ("otherLinks", otherLinkArray)

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


  object RejectionType extends Enumeration {
    type RejectionType = Value
    val NO_BASE, TITLE_REQUIREMENT, AIRLINE_GRADE, DISTANCE, NO_CASH = Value
  }

  def getRejectionReason(airline : Airline, fromAirport: Airport, toAirport : Airport, newLink : Boolean) : Option[(String, RejectionType.Value)]= {
    import RejectionType._
    if (airline.getCountryCode.isEmpty) {
      return Some(("Airline has no HQ!", NO_BASE))
    }
    val toCountryCode = toAirport.countryCode

    if (newLink) { //only check new links for now
      //validate from airport is a base
      val base = fromAirport.getAirlineBase(airline.id) match {
        case None => return Some(("Cannot fly from this airport, this is not a base!", NO_BASE))
        case Some(base) => base
      }


      val flightCategory = FlightType.getCategory(Computation.getFlightType(fromAirport, toAirport))
      //check title status
      if (flightCategory == FlightCategory.INTERCONTINENTAL) {
        val requiredTitle = if (toAirport.isGateway()) Title.APPROVED_AIRLINE else Title.PRIVILEGED_AIRLINE
        val currentTitle = CountryAirlineTitle.getTitle(toCountryCode, airline)
        val ok =  currentTitle.title.id <= requiredTitle.id //smaller value means higher title

        if (!ok) {
          return Some((s"Cannot fly Intercontinental to this ${if (toAirport.isGateway()) "Gateway" else "Non-gateway"} airport until your airline attain title ${Title.description(requiredTitle)} with ${CountryCache.getCountry(toCountryCode).get.name}", TITLE_REQUIREMENT))
        }
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
            val staffRequired = LinkSource.loadLinksByCriteria(List(("from_airport", base.airport.id), ("airline", airline.id)), LinkSource.SIMPLE_LOAD).map(_.getOfficeStaffRequired).sum
            val staffCapacity = base.getOfficeStaffCapacity
            val newStaffRequired = staffRequired + Link.getOfficeStaffRequired(fromAirport, toAirport)

            val extraCompensation = base.getOvertimeCompensation(staffCapacity, staffRequired) - base.getOvertimeCompensation(staffCapacity, newStaffRequired)

            if (extraCompensation > 0) { //then we should prompt warning of over limit
              warnings.append(s"Exceeding operation capacity of current base. Extra overtime compensation of $$$extraCompensation will be charged per week for this route.")
            }
          case None => //should not be none
      }
    }
    warnings.toList
  }

//  def getVipRoutes() = Action {
//    Ok(Json.toJson(RouteHistorySource.loadVipRoutes()))
//  }
  
  def getRelatedLinkConsumption(airlineId : Int, linkId : Int, cycleDelta : Int, selfOnly : Boolean) =  AuthenticatedAirline(airlineId) {
    LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD) match {
      case Some(link) => {
        if (link.airline.id != airlineId) {
          Forbidden(Json.obj())
        } else {
          Ok(Json.toJson(HistoryUtil.loadConsumptionByLink(link, cycleDelta, selfOnly)))
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


  def getLinkComposition(airlineId : Int, linkId : Int) =  AuthenticatedAirline(airlineId) {
    val consumptionEntries : List[LinkConsumptionHistory]= ConsumptionHistorySource.loadConsumptionByLinkId(linkId)
    val consumptionByCountry = consumptionEntries.groupBy(_.homeCountryCode).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPassengerType = consumptionEntries.groupBy(_.passengerType).view.mapValues(entries => entries.map(_.passengerCount).sum)
    val consumptionByPreferenceType = consumptionEntries.groupBy(_.preferenceType).view.mapValues(entries => entries.map(_.passengerCount).sum)

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

  def getLinkRivalHistory(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) {
    var result = Json.obj()
    //get competitor history
    LinkSource.loadLinkById(linkId).foreach { link =>
      //find all link with same from and to
      val overlappingLinks = LinkSource.loadLinksByAirports(link.from.id, link.to.id) ++ LinkSource.loadLinksByAirports(link.to.id, link.from.id)
      val rivals = scala.collection.mutable.HashSet[Airline]()

      var overlappingLinksJson = Json.arr()
      overlappingLinks.filter(_.capacity.total > 0).foreach { overlappingLink => //only work on links that have capacity
        overlappingLinksJson = overlappingLinksJson.append(Json.toJson(LinkSource.loadLinkConsumptionsByLinkId(overlappingLink.id, cycleCount))(Writes.traversableWrites(MinimumLinkConsumptionWrite)))
        rivals += overlappingLink.airline
      }

      result = result + ("overlappingLinks" -> overlappingLinksJson)
    }

    Ok(result)
  }

  def getLinkRivalDetails(airlineId : Int, linkId : Int, cycleCount : Int) =  AuthenticatedAirline(airlineId) {
    var result = Json.obj()
    //get competitor history
    LinkSource.loadLinkById(linkId).foreach { link =>
      //find all link with same from and to
      val overlappingLinks = LinkSource.loadLinksByAirports(link.from.id, link.to.id) ++ LinkSource.loadLinksByAirports(link.to.id, link.from.id)
      val rivals = scala.collection.mutable.HashSet[Airline]()

      overlappingLinks.filter(_.capacity.total > 0).foreach { overlappingLink => //only work on links that have capacity
        rivals += overlappingLink.airline
      }

      val fromAirportLinks = LinkSource.loadLinksByFromAirport(link.from.id) ++ LinkSource.loadLinksByToAirport(link.from.id)
      val toAirportLinks =  LinkSource.loadLinksByFromAirport(link.to.id) ++ LinkSource.loadLinksByToAirport(link.to.id)

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

        fromAirportInfo = fromAirportInfo.append(Json.obj("airline" -> rival, "network" -> fromAirportAllianceNetworkCapacity, "awareness" -> fromAirport.getAirlineAwareness(rival.id), "loyalty" -> fromAirport.getAirlineLoyalty(rival.id)))
        toAirportInfo = toAirportInfo.append(Json.obj("airline" -> rival, "network" -> toAirportAllianceNetworkCapacity, "awareness" -> toAirport.getAirlineAwareness(rival.id), "loyalty" -> toAirport.getAirlineLoyalty(rival.id)))
      }

      result = result ++ Json.obj("fromAirport" -> fromAirportInfo, "fromAirportCode" -> fromAirport.iata, "fromCity" -> fromAirport.city)
      result = result ++ Json.obj("toAirport" -> toAirportInfo, "toAirportCode" -> toAirport.iata, "toCity" -> toAirport.city)
    }

    Ok(result)
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

  def getLinkNegotiation(airlineId : Int) = AuthenticatedAirline(airlineId)  { implicit request =>
    val incomingLink = request.body.asInstanceOf[AnyContentAsJson].json.as[Link]
    //val delegatesCount = request.body.asInstanceOf[AnyContentAsJson].json.\("assignedDelegates").as[Int]
    val existingLinkOption = LinkSource.loadLinkByAirportsAndAirline(incomingLink.from.id, incomingLink.to.id, airlineId)
    val negotiationInfo = NegotiationUtil.getLinkNegotiationInfo(request.user, incomingLink, existingLinkOption)

    Ok(Json.obj("negotiationInfo" -> Json.toJson(negotiationInfo)(NegotiationInfoWrites(incomingLink)),
    "delegateInfo" -> Json.toJson(request.user.getDelegateInfo),
    "toAirport" -> Json.toJson(incomingLink.to),
    "fromAirport" -> Json.toJson(incomingLink.from)))
  }


  val getPassengerTypeTitle = (passengerType : PassengerType.Value) =>  passengerType match {
    case PassengerType.BUSINESS => "Business"
    case PassengerType.TOURIST => "Tourist"
    case PassengerType.OLYMPICS => "Olympics"
  }



  class PlanLinkResult(distance : Double, availableAirplanes : List[Airplane])
  //case class AirplaneWithPlanRouteInfo(airplane : Airplane, duration : Int, maxFrequency : Int, limitingFactor : String, isAssigned : Boolean)
  case class ModelPlanLinkInfo(model: Model, duration : Int, flightMinutesRequired : Int, isAssigned : Boolean, airplanes : List[(Airplane, Int)])
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
