package controllers

import com.patson.data.{AirlineSource, LinkSource}
import com.patson.model._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import models.Consideration
import play.api.libs.json._
import play.api.mvc._

import java.util.Calendar
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
    * Remaining cooldown (in seconds) before the HSR can be reconfigured again. Measured from link last_update
    */
  private[this] def remainingCooldownSeconds(railIds : List[Int]) : Map[Int, Long] = {
    val now = System.currentTimeMillis()
    LinkSource.loadLinkLastUpdates(railIds).map {
      case (railId, lastUpdate : Calendar) =>
        val elapsedSeconds = (now - lastUpdate.getTimeInMillis) / 1000
        (railId, Math.max(0L, HighSpeedRail.RECONFIGURE_COOLDOWN_SECONDS - elapsedSeconds))
    }
  }

  private[this] def cooldownWeeks(remainingSeconds : Long) : Long = Math.ceil(remainingSeconds.toDouble / HighSpeedRail.CYCLE_DURATION_SECONDS).toLong

  def getBuildConsideration(airline : Airline, fromAirport : Airport, toAirport : Airport, cars : Map[LinkClass, Int]) : Consideration[HighSpeedRail] = {
    val distance = Computation.calculateDistance(fromAirport, toAirport)
    val rail = HighSpeedRail(fromAirport, toAirport, airline, distance, HighSpeedRail.capacityByCars(cars), HighSpeedRail.FREQUENCY)
    val cost = HighSpeedRail.buildCost(distance, fromAirport)

    val rejection : Option[String] = {
      val base = AirlineSource.loadAirlineBaseByAirlineAndAirport(airline.id, fromAirport.id)
      if (base.isEmpty || base.get.scale < HighSpeedRail.MIN_BASE_SCALE) {
        Some(s"Requires a base of level ${HighSpeedRail.MIN_BASE_SCALE} or above at ${fromAirport.displayText}")
      } else if (fromAirport.id == toAirport.id) {
        Some("Departure and destination airports cannot be the same")
      } else if (distance <= 0 || distance > HighSpeedRail.MAX_DISTANCE) {
        Some(s"High speed rail can only be built up to ${HighSpeedRail.MAX_DISTANCE} km. This is $distance km")
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
          val remainingSeconds = remainingCooldownSeconds(List(existingRail.id)).getOrElse(existingRail.id, 0L)
          if (remainingSeconds > 0) {
            Some(s"Can only reconfigure in ${cooldownWeeks(remainingSeconds)} week(s)")
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
              Ok(Json.toJson(savedRail)(HighSpeedRailWrites))
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
          LinkSource.updateLink(consideration.newFacility) //also resets last_update, which restarts the cooldown
          Ok(Json.toJson(consideration.newFacility)(HighSpeedRailWrites))
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
    val cooldowns = remainingCooldownSeconds(rails.map(_.id))
    Ok(JsArray(rails.map { rail =>
      val remainingSeconds = cooldowns.getOrElse(rail.id, 0L)
      Json.toJson(rail)(HighSpeedRailWrites).as[JsObject] + ("reconfigureCooldown" -> JsNumber(cooldownWeeks(remainingSeconds)))
    }))
  }

  def getAirportHighSpeedRails(airportId : Int) = Action { request =>
    val rails = loadHighSpeedRails(List(("from_airport", airportId))) ++ loadHighSpeedRails(List(("to_airport", airportId)))
    Ok(JsArray(rails.map(rail => Json.toJson(rail)(HighSpeedRailWrites))))
  }
}
