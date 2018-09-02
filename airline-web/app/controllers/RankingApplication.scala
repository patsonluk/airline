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
      //val countryCode : String = ranking.airline.getCountryCode().getOrElse("")
      var result = Json.obj(
        "rank" -> ranking.ranking,
        //"airlineCountryCode" -> countryCode,
        "rankedValue" -> ranking.rankedValue.toString,
        "movement" ->  ranking.movement
      )
      
      if (ranking.entry.isInstanceOf[Airline]) { 
        val airline = ranking.entry.asInstanceOf[Airline]
        result = result + ("airlineName" -> JsString(airline.name)) + ("airlineId" -> JsNumber(airline.id))
      } else if (ranking.entry.isInstanceOf[Link]) {
        val link = ranking.entry.asInstanceOf[Link]
        result = result + ("airlineName" -> JsString(link.airline.name)) + ("airlineId" -> JsNumber(link.airline.id)) + ("rankInfo" -> JsString(getLinkDescription(link)))
      }
      
      result
    }
  }
  
  implicit object RankingTypeWrites extends Writes[RankingType.Value] {
    def writes(rankingType : RankingType.Value): JsValue = {
      Json.obj(
        "rankingType" -> rankingType.toString
      )
    }
  }
  
  def getLinkDescription(link : Link) = {
    link.from.city + "(" + link.from.iata + ") <=> " + link.to.city + "(" + link.to.iata + ")" 
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
