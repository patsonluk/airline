package controllers

import com.patson.data.{ChangeHistorySource, CycleSource}
import com.patson.model.history.LinkChange
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable.ListBuffer


class ChangeHistoryApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  var currentCycle : Int = 0
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

  implicit object LinkChangeWrites extends Writes[LinkChange] {
    def writes(linkChange : LinkChange) : JsValue = {
      Json.obj(
        "linkId" -> linkChange.linkId,
        "fromAirportName" -> linkChange.fromAirport.name,
        "fromAirportIata" -> linkChange.fromAirport.iata,
        "fromAirportCity" -> linkChange.fromAirport.city,
        "toAirportName" -> linkChange.toAirport.name,
        "toAirportIata" -> linkChange.toAirport.iata,
        "toAirportCity" -> linkChange.toAirport.city,
        "fromCountryCode" -> linkChange.fromCountry.countryCode,
        "toCountryCode" -> linkChange.toCountry.countryCode,
        "price" -> Json.toJson(linkChange.price),
        "priceDelta" -> Json.toJson(linkChange.priceDelta),
        "capacity" -> Json.toJson(linkChange.capacity),
        "capacityDelta" -> Json.toJson(linkChange.capacityDelta),
        "airplaneModelName" -> linkChange.airplaneModel.name,
        "frequency" -> linkChange.frequency,
        "rawQuality" -> linkChange.rawQuality,
        "flightCode" -> LinkUtil.getFlightCode(linkChange.airline, linkChange.flightNumber),
        "cycle" -> linkChange.cycle,
        "cycleDelta" -> (linkChange.cycle - currentCycle))
    }
  }


  def searchLinkHistory = Action { implicit request =>
    currentCycle = CycleSource.loadCycle()
    val json = request.body.asInstanceOf[AnyContentAsJson].json
    val capacityDelta = json.\("capacityDelta").asOpt[Int]
    val capacity = json.\("capacity").asOpt[Int]
    val fromAirportId = json.\("fromAirportId").asOpt[Int]
    val toAirportId = json.\("toAirportId").asOpt[Int]
    val fromCountryCode = json.\("fromCountryCode").asOpt[String]
    val toCountryCode = json.\("toCountryCode").asOpt[String]
    val fromZone = json.\("fromZone").asOpt[String]
    val toZone = json.\("toZone").asOpt[String]
    val airlineId = json.\("airlineId").asOpt[Int]

    val baseCriteria = ListBuffer[(String, String, Any)]()
    capacityDelta.map( value => baseCriteria.append(("capacity_delta", ">=", value)))
    capacity.map( value => baseCriteria.append(("capacity", ">=", value)))
    fromAirportId.map( value => baseCriteria.append(("from_airport", "=", value)))
    toAirportId.map( value => baseCriteria.append(("to_airport", "=", value)))
    fromCountryCode.map( value => baseCriteria.append(("from_country", "=", value)))
    toCountryCode.map( value => baseCriteria.append(("to_country", "=", value)))
    fromZone.map( value => baseCriteria.append(("from_zone", "=", value)))
    toZone.map( value => baseCriteria.append(("to_zone", "=", value)))
    airlineId.map( value => baseCriteria.append(("airline", "=", value)))

    //TODO destination swap

    val entries = ChangeHistorySource.loadLinkChangeByCriteria(baseCriteria.toList)
    Ok(Json.toJson(entries))
  }


}


