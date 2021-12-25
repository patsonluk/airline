package controllers

import com.patson.data.LinkSource
import com.patson.model._
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject
import scala.math.BigDecimal.int2bigDecimal


class GenericTransitApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getGenericTransits(airportId : Int) = Action { request =>
    val genericTransits = (LinkSource.loadLinksByCriteria(List(("from_airport", airportId), ("transport_type", TransportType.GENERIC_TRANSIT.id))) ++
      LinkSource.loadLinksByCriteria(List(("to_airport", airportId), ("transport_type", TransportType.GENERIC_TRANSIT.id)))).map(_.asInstanceOf[GenericTransit])
    var resultJson = Json.arr()
    val consumptionByLinkId = LinkSource.loadLinkConsumptionsByLinksId(genericTransits.map(_.id)).map(entry => (entry.link.id, entry)).toMap
    genericTransits.foreach { transit =>
      val toAirport =
        if (transit.from.id == airportId) {
          transit.to
        } else {
          transit.from
        }

      var transitJson = Json.obj("toAirportId" -> toAirport.id, "toAirportText" -> toAirport.displayText, "toAirportPopulation" -> toAirport.population, "capacity" -> transit.capacity.total, "linkId" -> transit.id)
      consumptionByLinkId.get(transit.id) match {
        case Some(consumption) =>
          transitJson = transitJson + ("passenger" -> JsNumber(consumption.link.getTotalSoldSeats))
        case None =>
          transitJson = transitJson + ("passenger" -> JsNumber(0))
      }
      resultJson = resultJson.append(transitJson)
    }
    Ok(resultJson)

  }

//  def updateShuttles(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
//    val airline : Airline = request.user
//    val inputArray = request.body.asInstanceOf[AnyContentAsJson].json.asInstanceOf[JsArray]
//    val updates = inputArray.value.map { entry =>
//      ShuttleUpdate(entry("linkId").as[Int], entry("capacity").as[Int])
//    }.toList
//
//    val newCapacityByLinkId = updates.map(entry => (entry.linkId, entry.capacity)).toMap
//
//    getRejections(airline, AirportCache.getAirport(airportId).get, updates) match {
//      case Some(rejection) =>
//        BadRequest(rejection)
//      case None =>
//        val updatingLinks = LinkSource.loadLinksByIds(updates.map(_.linkId)).map { shuttle =>
//          shuttle.capacity = LinkClassValues.getInstance(economy = newCapacityByLinkId(shuttle.id))
//          shuttle
//        }
//        LinkSource.updateLinks(updatingLinks)
//        Ok(Json.obj())
//    }
//  }

//  case class ShuttleUpdate(linkId : Int, capacity : Int)
//
//  def getRejections(airline : Airline, airport : Airport, newShuttles : List[ShuttleUpdate]) : Option[String] = {
//    AirlineSource.loadShuttleServiceByAirlineAndAirport(airline.id, airport.id) match {
//      case Some(shuttleService) =>
//        if (newShuttles.map(_.capacity).sum > shuttleService.getCapacity) {
//          return Some(s"New shuttles require capacity ${newShuttles.map(_.capacity).sum} > max ${shuttleService.getCapacity}")
//        }
//        val existingLinks = LinkSource.loadLinksByIds(newShuttles.map(_.linkId))
//        if (existingLinks.size != newShuttles.length) {
//          return Some(s"New shuttles has count ${newShuttles.length} != existing ${existingLinks.size}")
//        }
//
//        newShuttles.foreach {
//          case ShuttleUpdate(linkId, capacity) =>
//            if (capacity < 0 || capacity % 100 != 0) {
//              return Some(s"Invalid shuttle capacity $capacity")
//            }
//            existingLinks.find(_.id == linkId).foreach { existingLink =>
//              if (existingLink.transportType != TransportType.SHUTTLE || existingLink.from.id != airport.id) {
//                return Some(s"Invalid update to shuttle $existingLink")
//              }
//            }
//        }
//        return None
//      case None =>
//        return Some(s"Shuttle service does not exists for ${airport.displayText}")
//    }
//  }

  
}
