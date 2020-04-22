package controllers

import com.patson.data.{ConsumptionHistorySource, CountrySource, CycleSource, EventSource}
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import com.patson.model.Scheduling.TimeSlot
import com.patson.model._


class SearchApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object SearhResultWrites extends Writes[SearchResultRoute] {
    def writes(result: SearchResultRoute): JsValue = {
//      val currentCycle = CycleSource.loadCycle()
//      val remainingDuration =
//        if (currentCycle > olympics.startCycle + olympics.duration) {
//          0
//        } else {
//          olympics.startCycle + olympics.duration - currentCycle
//        }
//
//      val currentYear = olympics.currentYear(currentCycle)
//      val status =
//        if (remainingDuration > 0) {
//          currentYear match {
//            case 1 => "Voting for Host City"
//            case 2 => "Host City Voted"
//            case 3 => "Preparation for the Games"
//            case 4 =>
//              val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - olympics.currentWeek(currentCycle)
//              if (weeksBeforeGames > 0) {
//                s"$weeksBeforeGames week(s) before the Games"
//              } else {
//                "Olympic Games in Progress"
//              }
//            case _ => "Unknown"
//          }
//        } else {
//          "Concluded"
//        }
//      val votingActive = remainingDuration > 0 && currentYear == 1
//      var result = JsObject(List(
//        "id" -> JsNumber(olympics.id),
//        "startCycle" -> JsNumber(olympics.startCycle),
//        "votingActive" -> JsBoolean(votingActive),
//        "remainingDuration" -> JsNumber(remainingDuration),
//        "status" -> JsString(status)))
//
//      Olympics.getSelectedAirport(olympics.id).foreach { airport =>
//        result = result + ("hostCity" -> JsString(airport.city)) + ("hostCountryCode" -> JsString(airport.countryCode))
//      }
//
//      if (remainingDuration > 0) {
//        result = result + ("currentYear" -> JsNumber(currentYear))
//      }
//
//      result
      ???
    }
  }


  def search(fromAirportId : Int, toAirportId : Int) = Action {
    ConsumptionHistorySource.loadConsumptionsByAirportPair(fromAirportId, toAirportId).toList.sortBy(_._2._2).map {
      case ((route, (passengerType, passengerCount))) =>


    }

    Ok(???)
  }

  def generateDepartureTime(link : Link, after : TimeSlot) : TimeSlot = {
    Scheduling.getLinkSchedule(link)
    ???
  }

  case class SearchResultRoute(linkDetails : List[LinkDetail], passengerCount: Int)
  case class LinkDetail(link : Link, timeslot : TimeSlot)

}


