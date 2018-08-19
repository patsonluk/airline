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


class CountryApplication extends Controller {
  def getAllCountries(homeCountryCode : Option[String]) = Action {
    val countries = CountrySource.loadAllCountries()
    
    homeCountryCode match {
      case None => Ok(Json.toJson(countries))
      case Some(homeCountryCode) => {
        val mutualRelationships = CountrySource.getCountryMutualRelationShips(homeCountryCode)
        val countriesWithMutualRelationship : List[(Country, Int)] = countries.map { country =>
            (country, mutualRelationships.get((homeCountryCode, country.countryCode)).getOrElse(0))
        }
    
        Ok(Json.toJson(countriesWithMutualRelationship))
      }
        
    }
    
  }
  
  
  def getCountry(countryCode : String) = Action {
    CountrySource.loadCountryByCode(countryCode) match {
      case Some(country) =>
        var jsonObject= Json.toJson(country)
        val airports = AirportSource.loadAirportsByCountry(countryCode)
        val smallAirportCount = airports.count { airport => airport.size <= 2 }
        val mediumAirportCount = airports.count { airport => airport.size >= 3 && airport.size <= 4 }
        val largeAirportCount = airports.count { airport => airport.size >= 5 }
        
        val allBases = AirlineSource.loadAirlineBasesByCountryCode(countryCode)
        
        val (headquarters, bases) = allBases.partition { _.headquarter }
        
        jsonObject = jsonObject.asInstanceOf[JsObject] ++ 
          Json.obj("smallAirportCount" -> smallAirportCount,
                   "mediumAirportCount" -> mediumAirportCount,
                   "largeAirportCount" -> largeAirportCount,
                   "headquarters" -> Json.toJson(headquarters),
                   "bases" -> Json.toJson(bases))
        val allAirlines = AirlineSource.loadAllAirlines(false).map(airline => (airline.id, airline)).toMap
        CountrySource.loadMarketSharesByCountryCode(countryCode).foreach { marketShares => //if it has market share data
          
        val champions = marketShares.airlineShares.toList.sortBy(_._2)(Ordering[Long].reverse).take(5)
        var championsJson = Json.arr()
        var x = 0
        for (x <- 0 until champions.size) {
          val airline = allAirlines(champions(x)._1)
          val passengerCount = champions(x)._2
          val ranking = x + 1
          val reputationBoost = Computation.computeReputationBoost(country, ranking) 
          championsJson = championsJson :+ Json.obj("airline" -> Json.toJson(airline), "passengerCount" -> JsNumber(passengerCount), "ranking" -> JsNumber(ranking), "reputationBoost" -> JsNumber(reputationBoost))
        }
        
        jsonObject = jsonObject.asInstanceOf[JsObject] + ("champions" -> championsJson)
        
        jsonObject = jsonObject.asInstanceOf[JsObject] + ("marketShares" -> Json.toJson(marketShares.airlineShares.map { 
            case ((airlineId, passengerCount)) => (allAirlines(airlineId), passengerCount)
          }.toList)(AirlineSharesWrites))
        }
          
        Ok(jsonObject)
      case None => NotFound
    } 
  }
  
  object AirlineSharesWrites extends Writes[List[(Airline, Long)]] {
    def writes(shares: List[(Airline, Long)]): JsValue = {
      var jsonArray = Json.arr()
      shares.foreach { share =>
        jsonArray = jsonArray :+ JsObject(List(
        "airlineId" -> JsNumber(share._1.id),
        "airlineName" -> JsString(share._1.name),
        "passengerCount" -> JsNumber(share._2)
        ))
      }
      jsonArray
    }
  }
}
