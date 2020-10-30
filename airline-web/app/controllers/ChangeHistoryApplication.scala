package controllers

import com.patson.data.{ChangeHistorySource, Constants, CycleSource}
import com.patson.model.LinkClass
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
      var priceJson = Json.obj()
      var priceDeltaJson = Json.obj()
      var capacityDeltaJson = Json.obj()

      LinkClass.values.foreach { linkClass =>
        if (linkChange.capacity(linkClass) > 0) { //do not report price for link class that has no capacity
          priceJson = priceJson + (linkClass.label -> JsNumber(linkChange.price(linkClass)))
          if (linkChange.priceDelta(linkClass) != 0) { //do not put delta if price has not been changed
            priceDeltaJson = priceDeltaJson + (linkClass.label -> JsNumber(linkChange.priceDelta(linkClass)))
          }
        }
        if (linkChange.capacityDelta(linkClass) != 0) {
          capacityDeltaJson = capacityDeltaJson + (linkClass.label -> JsNumber(linkChange.capacityDelta(linkClass)))
        }
      }

      capacityDeltaJson = capacityDeltaJson + ("total" -> JsNumber(linkChange.capacityDelta.total))



      Json.obj(
        "airlineId" -> linkChange.airline.id,
        "airlineName" -> linkChange.airline.name,
        "linkId" -> linkChange.linkId,
        "fromAirportName" -> linkChange.fromAirport.name,
        "fromAirportIata" -> linkChange.fromAirport.iata,
        "fromAirportCity" -> linkChange.fromAirport.city,
        "toAirportName" -> linkChange.toAirport.name,
        "toAirportIata" -> linkChange.toAirport.iata,
        "toAirportCity" -> linkChange.toAirport.city,
        "fromCountryCode" -> linkChange.fromCountry.countryCode,
        "toCountryCode" -> linkChange.toCountry.countryCode,
        "price" -> priceJson,
        "priceDelta" -> priceDeltaJson,
        "capacity" -> Json.toJson(linkChange.capacity),
        "capacityTotal" -> JsNumber(linkChange.capacity.total),
        "capacityDelta" -> capacityDeltaJson,
        "capacityDeltaTotal" -> JsNumber(linkChange.capacityDelta.total),
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
    val allianceId = json.\("allianceId").asOpt[Int]


    val criteria = ListBuffer[String]()
    val parameters = ListBuffer[Any]()

    //val baseCriteria = ListBuffer[(String, String, Any)]()
    capacityDelta.map( value => {
      criteria += "capacity_delta >= ? OR capacity_delta <= ?"
      parameters += value
      parameters += (value * -1)
    })
    capacity.map( value => {
      criteria += "capacity >= ? OR capacity <= ?"
      parameters += value
      parameters += (value * -1)
    })

    //no reverse search for now. it gets complicated when from and to are in the same value
    fromAirportId.map( value =>  {
      criteria += "from_airport = ?"
      parameters += value
    })
    toAirportId.map( value =>  {
      criteria += "to_airport = ?"
      parameters += value
    })
    fromCountryCode.map( value =>  {
      criteria += "from_country = ?"
      parameters += value
    })
    toCountryCode.map( value =>  {
      criteria += "to_country = ?"
      parameters += value
    })
    fromZone.map( value =>  {
      criteria += "from_zone = ?"
      parameters += value
    })
    toZone.map( value =>  {
      criteria += "to_zone = ?"
      parameters += value
    })
    airlineId.map( value =>  {
      criteria += "airline = ?"
      parameters += value
    })
    allianceId.map( value =>  {
      criteria += "alliance = ?"
      parameters += value
    })

    var query = "SELECT * FROM " + Constants.LINK_CHANGE_HISTORY_TABLE
    if (!parameters.isEmpty) {
      query += " WHERE "
      query += criteria.map("(" + _ + ")").toArray.mkString(" AND ")
    }

    val entries = ChangeHistorySource.loadLinkChangeByQueryString(query, parameters.toList)
    Ok(Json.toJson(entries))
  }


}


