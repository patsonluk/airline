package controllers

import com.patson.data.{ConsumptionHistorySource, CountrySource, CycleSource, EventSource, LinkSource}
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.{PassengerType, _}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


class SearchApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object SearhResultWrites extends Writes[(SimpleRoute, Int)] {
    def writes(route: (SimpleRoute, Int)): JsValue = {
      var result = Json.obj("passenger" -> route._2)
      var routeJson = Json.arr()
      var index = 0
      var previousArrivalMinutes = 0
      val schedule : Map[Link, TimeSlot] = generateRouteSchedule(route._1.links.map(_._1)).toMap
      val detailedLinkCache : mutable.HashMap[Int, Option[Link]] = mutable.HashMap()
      route._1.links.foreach {
        case(link, linkClass, inverted) => {
          val (from, to) = if (!inverted) (link.from, link.to) else (link.to, link.from)
          val departureMinutes = adjustMinutes(schedule(link).totalMinutes, previousArrivalMinutes)
          val arrivalMinutes = departureMinutes + link.duration
          var linkJson = Json.obj(
            "airlineName" -> link.airline.name,
            "airlineId" -> link.airline.id,
            "flightCode" -> LinkUtil.getFlightCode(link.airline, link.flightNumber),
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
            "duration" -> link.duration,
            "toAirportCountryCode" -> to.countryCode,
            "price" -> link.price(linkClass),
            "departure" -> departureMinutes,
            "arrival" -> arrivalMinutes
           )

          detailedLinkCache.getOrElseUpdate(link.id, LinkSource.loadLinkById(link.id)).foreach { detailedLink =>
            linkJson = linkJson + ("computedQuality" -> JsNumber(detailedLink.computedQuality))
            detailedLink.getAssignedModel().foreach { model =>
              linkJson = linkJson + ("airplaneModelName" -> JsString(model.name))
            }
            linkJson = linkJson + ("features" -> Json.toJson(getLinkFeatures(detailedLink).map(_.toString)))
          }

          previousArrivalMinutes = arrivalMinutes

          routeJson = routeJson.append(linkJson)
          index += 1
        }
      }

      result = result + ("route" -> routeJson) + ("passenger" -> JsNumber(route._2))
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

    val sortedRoutes: List[(SimpleRoute, Int)] = (routes ++ reverseRoutes).groupBy(_._1).view.mapValues( _.map(_._3).sum).toList.sortBy(_._1.totalPrice)

    sortedRoutes.foreach(println)

    Ok(Json.toJson(sortedRoutes))
  }

  import scala.jdk.CollectionConverters._
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

  case class SimpleRoute(links : List[(Link, LinkClass, Boolean)]) {
    val totalPrice = links.map {
      case (link, linkClass, _) => link.price(linkClass)
    }.sum
  }

//
//  def computeLayover(previousLink : Link, currentLink : Link) = {
//    val frequency = previousLink.frequency + currentLink.frequency
//    val random = new Random(previousLink.id)
//    (((random.nextDouble() + 2) * 24 * 60) / frequency).toInt + 15 //some randomness
//  }

  def generateRouteSchedule(links : List[Link]) : List[(Link, TimeSlot)] = {
    val scheduleOptions : ListBuffer[List[(Link, TimeSlot)]] = ListBuffer()
    val random = new Random()
    for (i <- 0 until 7) {
      var previousLinkArrivalTime : TimeSlot = TimeSlot(i, random.nextInt(24), random.nextInt(60))
      val scheduleOption = links.map { link =>
        val departureTime = generateDepartureTime(link, previousLinkArrivalTime.increment(30)) //at least 30 minutes after
        previousLinkArrivalTime = departureTime.increment(link.duration)
        (link, departureTime)
      }
      scheduleOptions.append(scheduleOption)
    }
    val sortedScheduleOptions: mutable.Seq[List[(Link, TimeSlot)]] = scheduleOptions.sortBy {
      case (scheduleOption) =>  {
        var previousArrivalMinutes = 0
        scheduleOption.foreach {
          case (link, departureTimeSlot) => {
            previousArrivalMinutes = adjustMinutes(departureTimeSlot.totalMinutes, previousArrivalMinutes) + link.duration
          }
        }
        //previousArrivalMinutes should contain the minutes of arrival of last leg
        val totalDuration = previousArrivalMinutes - scheduleOption(0)._2.totalMinutes
        totalDuration
      }
    }
    sortedScheduleOptions(0)//find the one with least total duration
  }

  def generateDepartureTime(link : Link, after : TimeSlot) : TimeSlot = {
    val availableSlots = Scheduling.getLinkSchedule(link).sortBy(_.totalMinutes)
    availableSlots.find(_.compare(after) > 0).getOrElse(availableSlots(0)) //find the first slot that is right after the "after option"
  }

  /**
    * Adjust currentMinutes so it's always after the referenceMinutes. This is to handle week rollover on TimeSlot
    * @param currentMinutes
    * @param referenceMinutes
    */
  def adjustMinutes(currentMinutes : Int, referenceMinutes : Int) = {
    var result = currentMinutes
    while (result < referenceMinutes) {
      result += 7 * 24 * 60
    }
    result
  }

  def getLinkFeatures(link : Link) = {
    val features = ListBuffer[LinkFeature.Value]()

    import LinkFeature._
    val airlineServiceQuality = link.airline.getCurrentServiceQuality
    if (link.duration <= 60) { //very short flight
      if (link.rawQuality >= 80) {
        features += BEVERAGE_SERVICE
      }
      if (airlineServiceQuality >= 80) {
        features += POWER_OUTLET
      }
    } else if (link.duration <= 120) {
      if (link.rawQuality >= 60) {
        features += BEVERAGE_SERVICE
      }
      if (link.rawQuality >= 80) {
        features += HOT_MEAL_SERVICE
      }
      if (airlineServiceQuality >= 60) {
        features += POWER_OUTLET
      }
    } else {
      if (link.rawQuality >= 40) {
        features += BEVERAGE_SERVICE
      }
      if (link.rawQuality >= 60) {
        features += HOT_MEAL_SERVICE
      }
      if (airlineServiceQuality >= 60) {
        features += POWER_OUTLET
      }
    }

    if (link.rawQuality == 100 && airlineServiceQuality >= 70) {
      features += PREMIUM_DRINK_SERVICE
    }

    if (link.rawQuality == 100 && airlineServiceQuality >= 85) {
      features += POSH
    }

    if (airlineServiceQuality >= 50) {
      features += IFE
    }

    if (airlineServiceQuality >= 70) {
      features += WIFI
    }

    if (airlineServiceQuality >= 80) {
      features += GAME
    }
    features.toList
  }

  object LinkFeature extends Enumeration {
    type LinkFeature = Value
    val WIFI, BEVERAGE_SERVICE, HOT_MEAL_SERVICE, PREMIUM_DRINK_SERVICE, IFE, POWER_OUTLET, GAME, POSH = Value
  }

  case class SearchResultRoute(linkDetails : List[LinkDetail], passengerCount: Int)
  case class LinkDetail(link : Link, timeslot : TimeSlot)

}


