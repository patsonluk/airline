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
  implicit object CountryWrites extends Writes[Country] {
    def writes(country : Country): JsValue = {
      Json.obj(
        "countryCode" -> country.countryCode,
        "name" -> country.name,
        "aiportPopulation" -> country.airportPopulation,
        "incomeLevel" -> Computation.getIncomeLevel(country.income),
        "openness" ->  country.openness
      )
    }
  }

  def getAllCountries() = Action {
    val countries = CountrySource.loadAllCountries()
    Ok(Json.toJson(countries))
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
        
        val headquartersJson =  
        
        jsonObject = jsonObject.asInstanceOf[JsObject] ++ 
          Json.obj("smallAirportCount" -> smallAirportCount,
                   "mediumAirportCount" -> mediumAirportCount,
                   "largeAirportCount" -> largeAirportCount,
                   "headquarters" -> Json.toJson(headquarters),
                   "bases" -> Json.toJson(bases))
                   
        Ok(jsonObject)
      case None => NotFound
    } 
  }
}
