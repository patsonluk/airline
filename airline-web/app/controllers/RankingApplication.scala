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
import javax.inject.Inject

class RankingApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val MAX_ENTRY = 20
  implicit object RankingWrites extends Writes[Ranking] {
    def writes(ranking : Ranking): JsValue = {
      //val countryCode : String = ranking.airline.getCountryCode().getOrElse("")
      var result = Json.obj(
        "rank" -> ranking.ranking,
        //"airlineCountryCode" -> countryCode,
        "rankedValue" -> ranking.rankedValue.toString,
        "movement" ->  ranking.movement
      )
      
      ranking.entry match {
        case airline : Airline =>
          result = result + ("airlineName" -> JsString(airline.name)) + ("airlineId" -> JsNumber(airline.id)) + ("airlineSlogan" -> JsString(airline.slogan.getOrElse("")))
        case link : Link =>
          val fromJson = Json.toJson(link.from)(SimpleAirportWrites)
          val toJson = Json.toJson(link.to)(SimpleAirportWrites)
          result = result + ("airlineName" -> JsString(link.airline.name)) + ("airlineId" -> JsNumber(link.airline.id)) ++ Json.obj("rankInfo" -> Json.obj("from" -> fromJson, "to" -> toJson))
        case lounge : Lounge =>
          result = result + ("airlineName" -> JsString(lounge.airline.name)) + ("airlineId" -> JsNumber(lounge.airline.id)) + ("rankInfo" -> JsString(getLoungeDescription(lounge)))
        case airport : Airport =>
          result = result + ("airportName" -> JsString(airport.name)) + ("airportId" -> JsNumber(airport.id)) + ("iata" -> JsString(airport.iata)) + ("countryCode" -> JsString(airport.countryCode))
        case (airport1 : Airport, airport2 : Airport) =>
          result = result ++ Json.obj("airport1" -> Json.toJson(airport1)(SimpleAirportWrites), "airport2" -> Json.toJson(airport2)(SimpleAirportWrites))
        case _ =>
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
  

  def getLoungeDescription(lounge : Lounge) = {
    lounge.name + " at " + lounge.airport.city + "(" + lounge.airport.iata + ")" 
  }
  
  

  def getRankings() = Action {
     var json = Json.obj()
     RankingUtil.getRankings().foreach {
       case(rankingType, rankings) =>
         json = json + (rankingType.toString, Json.toJson(rankings.take(MAX_ENTRY)))
     }
     
     Ok(json)
  }
  
  
  def getRankingsWithAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    var json = Json.obj()
    RankingUtil.getRankings().foreach {
      case(rankingType, rankings) =>
        val (topRankings, nonTopRankings) = rankings.splitAt(MAX_ENTRY)
         
        var truncatedRankings : List[Ranking] = topRankings
        nonTopRankings.find { ranking =>
          val entry = ranking.entry 
          
          if (entry.isInstanceOf[Airline]) { 
            val airline = entry.asInstanceOf[Airline]
            airline.id == airlineId
          } else if (entry.isInstanceOf[Link]) {
            val link = entry.asInstanceOf[Link]
            link.airline.id == airlineId
          } else if (entry.isInstanceOf[Lounge]) {
            val lounge = entry.asInstanceOf[Lounge]
            lounge.airline.id == airlineId
          } else if (entry.isInstanceOf[Airport]) { 
            false
          } else {
            false
          }
        }.foreach { selfRanking =>
          truncatedRankings = truncatedRankings :+ selfRanking
        }
        
        
        
        json = json + (rankingType.toString, Json.toJson(truncatedRankings))
    }
     
     Ok(json)
  }
}
