package controllers

import com.patson.data.{AirlineSource, CountrySource, CycleSource, LinkSource}
import com.patson.model._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import models.Consideration
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject


class HighSpeedRailApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  case class HighSpeedRailInput(fromAirportId : Int, toAirportId : Int, cars : Map[LinkClass, Int], linkId : Option[Int])

  private[this] def readInput(json : JsValue) : HighSpeedRailInput = {
    HighSpeedRailInput(
      (json \ "fromAirportId").as[Int],
      (json \ "toAirportId").as[Int],
      Map(
        ECONOMY -> (json \ "economyCars").asOpt[Int].getOrElse(0),
        BUSINESS -> (json \ "businessCars").asOpt[Int].getOrElse(0),
        FIRST -> (json \ "firstCars").asOpt[Int].getOrElse(0)),
      (json \ "linkId").asOpt[Int])
  }

  private[this] def loadHighSpeedRails(criteria : List[(String, Any)]) : List[HighSpeedRail] = {
    LinkSource.loadLinksByCriteria(criteria :+ ("transport_type", TransportType.HIGH_SPEED_RAIL.id)).map(_.asInstanceOf[HighSpeedRail])
  }

  private[this] def loadHighSpeedRail(linkId : Int) : Option[HighSpeedRail] = loadHighSpeedRails(List(("id", linkId))).headOption

  /**
    * Remaining cooldown (in cycles) before the HSR can be reconfigured again. No record means no cooldown
    */
  private[this] def reconfigureCooldown(railId : Int, currentCycle : Int) : Int = {
    LinkSource.loadHighSpeedRailUpdateCycle(railId).map(HighSpeedRail.reconfigureCooldown(_, currentCycle)).getOrElse(0)
  }

  def getBuildConsideration(airline : Airline, fromAirport : Airport, toAirport : Airport, cars : Map[LinkClass, Int]) : Consideration[HighSpeedRail] = {
    val distance = Computation.calculateDistance(fromAirport, toAirport)
    val rail = HighSpeedRail(fromAirport, toAirport, airline, distance, HighSpeedRail.capacityByCars(cars), HighSpeedRail.WEEKLY_FREQUENCY, fundedCycle = CycleSource.loadCycle())
    val cost = HighSpeedRail.buildCost(distance, fromAirport)

    val rejection : Option[String] = {
      val base = AirlineSource.loadAirlineBaseByAirlineAndAirport(airline.id, fromAirport.id)
      if (base.isEmpty || base.get.scale < HighSpeedRail.MIN_BASE_SCALE) {
        Some(s"Requires a base of level ${HighSpeedRail.MIN_BASE_SCALE} or above at ${fromAirport.displayText}")
      } else if (fromAirport.id == toAirport.id) {
        Some("Departure and destination airports cannot be the same")
      } else if (distance <= 0 || distance > HighSpeedRail.MAX_DISTANCE) {
        Some(s"High speed rail can only be built up to ${HighSpeedRail.MAX_DISTANCE} km. This is $distance km")
      } else if (CountrySource.getCountryMutualRelationship(fromAirport.countryCode, toAirport.countryCode) < HighSpeedRail.MIN_COUNTRY_RELATIONSHIP) {
        Some(s"High speed rail can only be built between countries with a mutual relationship of at least ${HighSpeedRail.MIN_COUNTRY_RELATIONSHIP}")
      } else if (loadHighSpeedRails(List(("from_airport", fromAirport.id), ("airline", airline.id))).nonEmpty) {
        Some(s"Your airline already operates a high speed rail from ${fromAirport.displayText}")
      } else if (loadHighSpeedRails(List(("from_airport", fromAirport.id), ("to_airport", toAirport.id))).nonEmpty) {
        Some(s"There is already a high speed rail from ${fromAirport.displayText} to ${toAirport.displayText}")
      } else {
        HighSpeedRail.carsRejection(cars, HighSpeedRail.carCount(fromAirport)).orElse {
          if (airline.getBalance() < cost) {
            Some("Not enough cash to build this high speed rail")
          } else {
            None
          }
        }
      }
    }

    Consideration(cost, rail, rejection)
  }

  def getReconfigureConsideration(airline : Airline, existingRail : HighSpeedRail, cars : Map[LinkClass, Int]) : Consideration[HighSpeedRail] = {
    val updatedRail = existingRail.copy(capacity = HighSpeedRail.capacityByCars(cars))
    val rejection : Option[String] =
      if (existingRail.airline.id != airline.id) {
        Some("This high speed rail is not operated by your airline")
      } else {
        HighSpeedRail.carsRejection(cars, HighSpeedRail.carCount(existingRail.from)).orElse {
          val cooldown = reconfigureCooldown(existingRail.id, CycleSource.loadCycle())
          if (cooldown > 0) {
            Some(s"Can only reconfigure in $cooldown week(s)")
          } else {
            None
          }
        }
      }

    Consideration(0, updatedRail, rejection) //reconfigure is free
  }

  def getConsideration(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val input = readInput(request.body.asInstanceOf[AnyContentAsJson].json)
    (AirportCache.getAirport(input.fromAirportId), AirportCache.getAirport(input.toAirportId)) match {
      case (Some(fromAirport), Some(toAirport)) =>
        val consideration = input.linkId match {
          case Some(linkId) =>
            loadHighSpeedRail(linkId) match {
              case Some(existingRail) => Some(getReconfigureConsideration(airline, existingRail, input.cars))
              case None => None
            }
          case None => Some(getBuildConsideration(airline, fromAirport, toAirport, input.cars))
        }

        consideration match {
          case Some(consideration) =>
            val rail = consideration.newFacility
            val capacityFromAirport = rail.from
            var result = Json.obj(
              "cost" -> JsNumber(consideration.cost),
              "distance" -> JsNumber(rail.distance),
              "carCount" -> JsNumber(HighSpeedRail.carCount(capacityFromAirport)),
              "maxCapacity" -> JsNumber(HighSpeedRail.maxCapacity(capacityFromAirport)),
              "usedCapacity" -> JsNumber(input.cars.values.sum * HighSpeedRail.CAR_CAPACITY),
              "capacity" -> Json.toJson(rail.capacity))
            if (consideration.isRejected) {
              result = result + ("rejection" -> JsString(consideration.rejectionReason))
            }
            Ok(result)
          case None => NotFound
        }
      case _ => NotFound
    }
  }

  def buildHighSpeedRail(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val input = readInput(request.body.asInstanceOf[AnyContentAsJson].json)
    (AirportCache.getAirport(input.fromAirportId), AirportCache.getAirport(input.toAirportId)) match {
      case (Some(fromAirport), Some(toAirport)) =>
        val consideration = getBuildConsideration(airline, fromAirport, toAirport, input.cars)
        if (consideration.isRejected) {
          BadRequest(consideration.rejectionReason)
        } else {
          LinkSource.saveLink(consideration.newFacility) match {
            case Some(savedRail) =>
              AirlineSource.adjustAirlineBalance(airline.id, -1 * consideration.cost)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airline.id, CashFlowType.FACILITY_CONSTRUCTION, -1 * consideration.cost))
              Ok(Json.toJson(savedRail)(new HighSpeedRailWrites(savedRail.fundedCycle))) //saving the link also starts the reconfigure cooldown
            case None =>
              BadRequest("Failed to build high speed rail")
          }
        }
      case _ => NotFound
    }
  }

  def reconfigureHighSpeedRail(airlineId : Int, linkId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val input = readInput(request.body.asInstanceOf[AnyContentAsJson].json)
    loadHighSpeedRail(linkId) match {
      case Some(existingRail) =>
        val consideration = getReconfigureConsideration(airline, existingRail, input.cars)
        if (consideration.isRejected) {
          BadRequest(consideration.rejectionReason)
        } else {
          LinkSource.updateLink(consideration.newFacility)
          LinkSource.updateHighSpeedRailCycle(linkId, CycleSource.loadCycle()) //restarts the reconfigure cooldown
          Ok(Json.toJson(consideration.newFacility)(new HighSpeedRailWrites(CycleSource.loadCycle())))
        }
      case None => NotFound
    }
  }

  def deleteHighSpeedRail(airlineId : Int, linkId : Int) = AuthenticatedAirline(airlineId) { request =>
    loadHighSpeedRail(linkId) match {
      case Some(rail) =>
        if (rail.airline.id != request.user.id) {
          Forbidden("This high speed rail is not operated by your airline")
        } else {
          val count = LinkSource.deleteLink(linkId) //no refund
          Ok(Json.obj("count" -> count))
        }
      case None => NotFound
    }
  }

  def getAirlineHighSpeedRails(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val rails = loadHighSpeedRails(List(("airline", airlineId)))
    val currentCycle = CycleSource.loadCycle()
    Ok(JsArray(rails.map { rail =>
      Json.toJson(rail)(new HighSpeedRailWrites(currentCycle)).as[JsObject] + ("reconfigureCooldown" -> JsNumber(reconfigureCooldown(rail.id, currentCycle)))
    }))
  }

  /**
    * Airports within HSR range of the airport and with enough country mutual relationship, sorted by size (biggest first)
    */
  private[this] def candidateAirports(airport : Airport) : List[Airport] = {
    val relationships = CountrySource.getCountryMutualRelationships(airport.countryCode) //same country is 5
    Computation.getAirportsWithinRange(airport, HighSpeedRail.MAX_DISTANCE, cachedAirportsByLatitude)
      .filter(candidate => relationships.getOrElse(candidate.countryCode, 0) >= HighSpeedRail.MIN_COUNTRY_RELATIONSHIP)
      .sortBy(candidate => (-candidate.size, -candidate.population))
  }

  /**
    * Candidate destination airports of HSR from the fromAirport, with the existing HSR (if any) from fromAirport to each of them
    */
  def getHighSpeedRailOptions(airlineId : Int, fromAirportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirportCache.getAirport(fromAirportId) match {
      case Some(fromAirport) =>
        val existingRailsByToAirportId = loadHighSpeedRails(List(("from_airport", fromAirportId))).map(rail => (rail.to.id, rail)).toMap
        val currentCycle = CycleSource.loadCycle()
        val candidates = candidateAirports(fromAirport)
        val options = candidates.map { candidate =>
          val distance = Computation.calculateDistance(fromAirport, candidate)
          var optionJson = Json.obj("airport" -> Json.toJson(candidate)(SimpleAirportWrites), "distance" -> distance, "buildCost" -> HighSpeedRail.buildCost(distance, fromAirport))
          existingRailsByToAirportId.get(candidate.id).foreach { rail =>
            optionJson = optionJson + ("rail" -> (Json.toJson(rail)(new HighSpeedRailWrites(currentCycle)).as[JsObject] + ("reconfigureCooldown" -> JsNumber(reconfigureCooldown(rail.id, currentCycle)))))
          }
          optionJson
        }

        Ok(Json.obj(
          "fromAirport" -> Json.toJson(fromAirport)(SimpleAirportWrites),
          "carCount" -> HighSpeedRail.carCount(fromAirport),
          "seatsPerCar" -> Json.obj(
            "economy" -> HighSpeedRail.seatsPerCar(ECONOMY),
            "business" -> HighSpeedRail.seatsPerCar(BUSINESS),
            "first" -> HighSpeedRail.seatsPerCar(FIRST)),
          "options" -> JsArray(options)))
      case None => NotFound
    }
  }

  /**
    * For airport details. HSR from and to this airport, with their ridership
    */
  def getAirportHighSpeedRails(airportId : Int) = Action { request =>
      val rails = loadHighSpeedRails(List(("from_airport", airportId))) ++ loadHighSpeedRails(List(("to_airport", airportId)))
      val passengersByLinkId : Map[Int, LinkClassValues] = LinkSource.loadLinkConsumptionsByLinksId(rails.map(_.id)).map(consumption => (consumption.link.id, consumption.link.soldSeats)).toMap
      val currentCycle = CycleSource.loadCycle()

      val railsWithPaxStats = rails.map { rail =>
        Json.toJson(rail)(new HighSpeedRailWrites(currentCycle)).as[JsObject] ++ Json.obj(
          "toAirportPopulation" -> rail.to.population,
          "passenger" -> Json.toJson(passengersByLinkId.getOrElse(rail.id, LinkClassValues.getInstance())))
      }

      Ok(JsArray(railsWithPaxStats))
  }
}
