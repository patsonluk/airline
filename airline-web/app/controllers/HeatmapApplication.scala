package controllers

import com.patson.AirportSimulation
import com.patson.data.LoyalistSource
import com.patson.model.LoyalistHistory
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import websocket.MyWebSocketActor

class HeatmapApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object HeatmapPointWrites extends Writes[HeatmapPoint] {
    def writes(point : HeatmapPoint): JsValue = {
      //val countryCode : String = ranking.airline.getCountryCode().getOrElse("")
      Json.obj(
        "lat" -> point.lat,
        //"airlineCountryCode" -> countryCode,
        "lng" -> point.lng,
        "weight" ->  point.weight
      )
    }
  }

  case class HeatmapPoint(lat : Double, lng : Double, weight : Double)


  val MAX_INTENSITY_LOYALIST = 1000000
  val MAX_INTENSITY_TREND = 100
  val INTENSITY_SQRT_BASE = 1.0 / 2


  def getHeatmapData(airlineId : Int, heatmapType : String, cycleDelta : Int) = Action { request =>
    heatmapType match {
      case "loyalistImpact" =>
        val cycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, cycleDelta)
        val heatmapPoints = LoyalistSource.loadLoyalistHistoryByCycleAndAirline(cycle, airlineId).map { history =>
          val intensity = Math.pow(history.entry.amount, INTENSITY_SQRT_BASE)
          //println(s"$intensity vs $MAX_INTENSITY")
          HeatmapPoint(history.entry.airport.latitude, history.entry.airport.longitude, intensity)
        }

        val maxIntensity = Math.pow(MAX_INTENSITY_LOYALIST, INTENSITY_SQRT_BASE)
        Ok(Json.obj("maxIntensity" -> maxIntensity,
          "points" -> heatmapPoints,
          "minDeltaCount" -> AirportSimulation.LOYALIST_HISTORY_ENTRY_MAX * -1
          ))
      case "loyalistTrend" =>
        val targetCycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, cycleDelta)
        val previousCycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, cycleDelta - 1)
        val targetHistoryByAirportId : Map[Int, LoyalistHistory] = LoyalistSource.loadLoyalistHistoryByCycleAndAirline(targetCycle, airlineId).map(entry => (entry.entry.airport.id, entry)).toMap
        val previousHistoryByAirportId : Map[Int, LoyalistHistory] = LoyalistSource.loadLoyalistHistoryByCycleAndAirline(previousCycle, airlineId).map(entry => (entry.entry.airport.id, entry)).toMap
        val allAirportIds : List[Int] = (targetHistoryByAirportId.keys ++ previousHistoryByAirportId.keys).toList

        val points = allAirportIds.map { airportId =>
          val delta = targetHistoryByAirportId.get(airportId).map(_.entry.amount).getOrElse(0) - previousHistoryByAirportId.get(airportId).map(_.entry.amount).getOrElse(0)
          val intensity = Math.min(MAX_INTENSITY_TREND, Math.pow(Math.abs(delta), INTENSITY_SQRT_BASE)) * (if (delta >= 0) 1 else -1)
          //println(s"$intensity vs $MAX_INTENSITY")
          val airport = AirportCache.getAirport(airportId).get
          HeatmapPoint(airport.latitude, airport.longitude, intensity)
        }

        val maxIntensity = MAX_INTENSITY_TREND
        Ok(Json.obj("maxIntensity" -> maxIntensity,
          "points" -> points,
          "minDeltaCount" -> AirportSimulation.LOYALIST_HISTORY_ENTRY_MAX * -1
        ))
      case _ => BadRequest(s"Unknown heatmap type $heatmapType")
    }
  }





}
