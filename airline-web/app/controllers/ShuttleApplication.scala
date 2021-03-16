package controllers

import com.patson.data.{AirlineSource, AlertSource, CycleSource, LinkSource, LinkStatisticsSource}
import com.patson.model.{ShuttleService, _}
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import models.{AirportFacility, Consideration, FacilityType}
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject
import scala.math.BigDecimal.int2bigDecimal


class ShuttleApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getShuttles(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airport = AirportCache.getAirport(airportId).get
    val shuttles = LinkSource.loadLinksByCriteria(List(("airline", airlineId), ("from_airport", airportId), ("transport_type", TransportType.SHUTTLE.id))).map(_.asInstanceOf[Shuttle])
    if (shuttles.isEmpty) { //generate empty ones
      var shuttlesJson = Json.arr()
      Computation.getDomesticAirportWithinRange(airport, ShuttleService.COVERAGE_RANGE).filter(_.id != airport.id).foreach { airportWithinRange =>
        val shuttleJson = Json.obj(
          "toAirportId"-> airportWithinRange.id,
          "toAirportText" -> airportWithinRange.displayText,
          "toAirportPopulation" -> airportWithinRange.population,
          "capacity" -> 0,
          "passenger" -> 0,
          "unitCost" -> Shuttle.UPKEEP_PER_CAPACITY
        )
        shuttlesJson = shuttlesJson.append(shuttleJson)
      }
      Ok(shuttlesJson)
    } else {
      var shuttlesJson = Json.arr()
      val consumptionByLinkId = LinkSource.loadLinkConsumptionsByLinksId(shuttles.map(_.id)).map(entry => (entry.link.id, entry)).toMap
      shuttles.foreach { shuttle =>
        var shuttleJson = Json.obj("toAirportId" -> shuttle.to.id, "toAirportText" -> shuttle.to.displayText, "toAirportPopulation" -> shuttle.to.population, "capacity" -> shuttle.capacity.total, "linkId" -> shuttle.id, "unitCost" -> Shuttle.UPKEEP_PER_CAPACITY)
        consumptionByLinkId.get(shuttle.id) match {
          case Some(consumption) =>
            shuttleJson = shuttleJson + ("passenger" -> JsNumber(consumption.link.getTotalSoldSeats))
          case None =>
            shuttleJson = shuttleJson + ("passenger" -> JsNumber(0))
        }
        shuttlesJson = shuttlesJson.append(shuttleJson)
      }
      Ok(shuttlesJson)
    }
  }

  def updateShuttles(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    val inputArray = request.body.asInstanceOf[AnyContentAsJson].json.asInstanceOf[JsArray]
    val updates = inputArray.value.map { entry =>
      ShuttleUpdate(entry("linkId").as[Int], entry("capacity").as[Int])
    }.toList

    val newCapacityByLinkId = updates.map(entry => (entry.linkId, entry.capacity)).toMap

    getRejections(airline, AirportCache.getAirport(airportId).get, updates) match {
      case Some(rejection) =>
        BadRequest(rejection)
      case None =>
        val updatingLinks = LinkSource.loadLinksByIds(updates.map(_.linkId)).map { shuttle =>
          shuttle.capacity = LinkClassValues.getInstance(economy = newCapacityByLinkId(shuttle.id))
          shuttle
        }
        LinkSource.updateLinks(updatingLinks)
        Ok(Json.obj())
    }
  }

  case class ShuttleUpdate(linkId : Int, capacity : Int)

  def getRejections(airline : Airline, airport : Airport, newShuttles : List[ShuttleUpdate]) : Option[String] = {
    AirlineSource.loadShuttleServiceByAirlineAndAirport(airline.id, airport.id) match {
      case Some(shuttleService) =>
        if (newShuttles.map(_.capacity).sum > shuttleService.getCapacity) {
          return Some(s"New shuttles require capacity ${newShuttles.map(_.capacity).sum} > max ${shuttleService.getCapacity}")
        }
        val existingLinks = LinkSource.loadLinksByIds(newShuttles.map(_.linkId))
        if (existingLinks.size != newShuttles.length) {
          return Some(s"New shuttles has count ${newShuttles.length} != existing ${existingLinks.size}")
        }

        newShuttles.foreach {
          case ShuttleUpdate(linkId, capacity) =>
            if (capacity < 0 || capacity % 100 != 0) {
              return Some(s"Invalid shuttle capacity $capacity")
            }
            existingLinks.find(_.id == linkId).foreach { existingLink =>
              if (existingLink.transportType != TransportType.SHUTTLE || existingLink.from.id != airport.id) {
                return Some(s"Invalid update to shuttle $existingLink")
              }
            }
        }
        return None
      case None =>
        return Some(s"Shuttle service does not exists for ${airport.displayText}")
    }
  }

  
}
