package controllers

import com.patson.data.{ConsumptionHistorySource, CountrySource, CycleSource, EventSource}
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.{PassengerType, _}


class SearchApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object SearhResultWrites extends Writes[(SimpleRoute, Int)] {
    def writes(route: (SimpleRoute, Int)): JsValue = {
      var result = Json.obj("passenger" -> route._2)
      var routeJson = Json.arr()
      route._1.links.foreach {
        case(link, linkClass, inverted) => {
          val (from, to) = if (!inverted) (link.from, link.to) else (link.to, link.from)
          routeJson = routeJson.append(Json.obj(
            "airlineName" -> link.airline.name,
            "airlineId" -> link.airline.id,
            "flightCode" -> (link.airline.getAirlineCode() + link.flightNumber),
            "linkClass" -> linkClass.label,
            "fromAirportId" -> from.id,
            "fromAirportName" -> from.name,
            "fromAirportIata" -> from.iata,
            "fromAirportCity" -> from.city,
            "fromAirportCountryCode" -> from.countryCode,
            "toAirportId" -> to.id,
            "toAirportName" -> to.name,
            "toAirportIata" -> to.iata,
            "toAirportCity" -> to.city,
            "toAirportCountryCode" -> to.countryCode,
            "price" -> link.price(linkClass)))
        }
      }
      result = result + ("route" -> routeJson)
      result
    }
  }

  implicit object AirportSearchResultWrites extends Writes[AirportSearchResult] {
    def writes(airportSearchResult : AirportSearchResult) : JsValue = {
      Json.obj(
        "airportId" -> airportSearchResult.getId,
        "airportName" -> airportSearchResult.getName,
        "airportIata" -> airportSearchResult.getIata,
        "airportCity" -> airportSearchResult.getCity,
        "countryCode" -> airportSearchResult.getCountryCode,
        "score" -> airportSearchResult.getScore)

    }
  }


  def searchRoute(fromAirportId : Int, toAirportId : Int) = Action {
    val routes: List[(SimpleRoute, PassengerType.Value, Int)] = ConsumptionHistorySource.loadConsumptionsByAirportPair(fromAirportId, toAirportId).toList.sortBy(_._2._2).map {
      case ((route, (passengerType, passengerCount))) =>
        (SimpleRoute(route.links.map(linkConsideration => (linkConsideration.link, linkConsideration.linkClass, linkConsideration.inverted))), passengerType, passengerCount)
    }

    val reverseRoutes : List[(SimpleRoute, PassengerType.Value, Int)] = ConsumptionHistorySource.loadConsumptionsByAirportPair(toAirportId, fromAirportId).toList.sortBy(_._2._2).map {
      case ((route, (passengerType, passengerCount))) =>
        (SimpleRoute(route.links.reverse.map(linkConsideration => (linkConsideration.link, linkConsideration.linkClass, !linkConsideration.inverted))), passengerType, passengerCount)
    }

//    println(s"from ${routes.length}")
//    println(routes.groupBy(_._1).size)

    val sortedRoutes: List[(SimpleRoute, Int)] = (routes ++ reverseRoutes).groupBy(_._1).view.mapValues( _.map(_._3).sum).toList.sortBy(_._2).reverse

    sortedRoutes.foreach(println)
    Ok(Json.toJson(sortedRoutes))
  }

  import collection.JavaConverters._
  def searchAirport(input : String) = Action {
    if (input.length < 3) {
      Ok(Json.obj("message" -> "Search with at least 3 characters"))
    } else {
      val result: List[AirportSearchResult] = SearchUtil.search(input).asScala.toList
      if (result.isEmpty) {
        Ok(Json.obj("message" -> "No match"))
      } else {
        Ok(Json.obj("airports" -> Json.toJson(result)))
      }
    }
  }

  case class SimpleRoute(links : List[(Link, LinkClass, Boolean)])


  def generateDepartureTime(link : Link, after : TimeSlot) : TimeSlot = {
    Scheduling.getLinkSchedule(link)
    ???
  }

  case class SearchResultRoute(linkDetails : List[LinkDetail], passengerCount: Int)
  case class LinkDetail(link : Link, timeslot : TimeSlot)

}


