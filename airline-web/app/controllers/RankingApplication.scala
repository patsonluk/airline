package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import com.patson.data.CycleSource
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.CountrySource
import com.patson.data.AirportSource


class RankingApplication extends Controller {
  implicit object RankingWrites extends Writes[Ranking] {
    def writes(ranking : Ranking): JsValue = {
      val countryCode : String = ranking.airline.getCountryCode().getOrElse("")
      Json.obj(
        "rank" -> ranking.ranking,
        "airlineName" -> ranking.airline.name,
        "airlineId" -> ranking.airline.id,
        "airlineCountryCode" -> countryCode,
        "rankedValue" -> ranking.rankedValue.toString,
        "movement" ->  ranking.movement
      )
    }
  }
  
  implicit object RankingTypeWrites extends Writes[RankingType.Value] {
    def writes(rankingType : RankingType.Value): JsValue = {
      Json.obj(
        "rankingType" -> rankingType.toString
      )
    }
  }
  
  

  def getRankings() = Action {
     var json = Json.obj()
     RankingUtil.getRankings().foreach {
       case(rankingType, rankings) =>
         json = json + (rankingType.toString, Json.toJson(rankings))
     }
     
     Ok(json)
  }
}
