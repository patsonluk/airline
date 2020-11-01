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
import com.patson.data.CountrySource
import com.patson.data.AirportSource
import com.patson.util.{ChampionUtil, CountryCache}
import javax.inject.Inject


class CountryApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getAllCountries(homeCountryCode : Option[String]) = Action {
    val countries = CountrySource.loadAllCountries()
    
    homeCountryCode match {
      case None => Ok(Json.toJson(countries))
      case Some(homeCountryCode) => {
        val mutualRelationships = CountrySource.getCountryMutualRelationships(homeCountryCode)
        val countriesWithMutualRelationship : List[(Country, Int)] = countries.map { country =>
            (country, mutualRelationships.get(country.countryCode).getOrElse(0))
        }
    
        Ok(Json.toJson(countriesWithMutualRelationship))
      }
        
    }
    
  }
  
  
  def getCountry(countryCode : String) = Action {
    CountryCache.getCountry(countryCode) match {
      case Some(country) =>
        var jsonObject : JsObject = Json.toJson(country).asInstanceOf[JsObject]
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
          val champions = ChampionUtil.getChampionInfoByCountryCode(countryCode).sortBy(_.ranking)
          var championsJson = Json.toJson(champions)
  //        var x = 0
  //        for (x <- 0 until champions.size) {
  //          val airline = allAirlines(champions(x)._1)
  //          val passengerCount = champions(x)._2
  //          val ranking = x + 1
  //          val reputationBoost = Computation.computeReputationBoost(country, ranking) 
  //          championsJson = championsJson :+ Json.obj("airline" -> Json.toJson(airline), "passengerCount" -> JsNumber(passengerCount), "ranking" -> JsNumber(ranking), "reputationBoost" -> JsNumber(reputationBoost))
  //        }
          
          jsonObject = jsonObject.asInstanceOf[JsObject] + ("champions" -> championsJson)

          var nationalAirlinesJson = Json.arr()
          var parternedAirlinesJson = Json.arr()
          CountrySource.loadCountryAirlineTitlesByCountryCode(countryCode).foreach {
            case countryAirlineTitle =>
              val CountryAirlineTitle(country, airline, title) = countryAirlineTitle
              val share : Long = marketShares.airlineShares.getOrElse(airline.id, 0L)
              title match {
                case Title.NATIONAL_AIRLINE =>
                  nationalAirlinesJson = nationalAirlinesJson.append(Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "passengerCount" -> share, "loyaltyBonus" -> countryAirlineTitle.loyaltyBonus))
                case Title.PARTNERED_AIRLINE =>
                  parternedAirlinesJson = parternedAirlinesJson.append(Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "passengerCount" -> share, "loyaltyBonus" -> countryAirlineTitle.loyaltyBonus))
              }
          }

          jsonObject = jsonObject + ("nationalAirlines" -> nationalAirlinesJson) + ("partneredAirlines" -> parternedAirlinesJson)
          
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
