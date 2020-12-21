package controllers

import com.patson.AirportSimulation
import com.patson.data.LoyalistSource
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
  val INTENSITY_SQRT_BASE = 1.0 / 2
  val MAX_INTENSITY = Math.pow(MAX_INTENSITY_LOYALIST, INTENSITY_SQRT_BASE)

  def getHeatmapData(airlineId : Int, heatmapType : String, cycleDelta : Int) = Action { request =>
    heatmapType match {
      case "loyalist" =>
        val cycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, cycleDelta)
        val heatmapPoints = LoyalistSource.loadLoyalistHistoryByCycleAndAirline(cycle, airlineId).map { history =>
          val intensity = Math.pow(history.entry.amount, INTENSITY_SQRT_BASE)
          //println(s"$intensity vs $MAX_INTENSITY")
          HeatmapPoint(history.entry.airport.latitude, history.entry.airport.longitude, intensity)
        }

        Ok(Json.obj("maxIntensity" -> MAX_INTENSITY,
          "points" -> heatmapPoints,
          "minDeltaCount" -> AirportSimulation.LOYALIST_HISTORY_ENTRY_MAX * -1
          ))
      case _ => BadRequest(s"Unknown heatmap type $heatmapType")
    }
  }

}
