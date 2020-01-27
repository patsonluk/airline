package com.patson.init

import com.patson.Util
import com.patson.data.{AirportSource, CitySource, CountrySource}
import com.patson.model._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object GeoDataPatcher extends App {

  import actorSystem.dispatcher

  //implicit val materializer = FlowMaterializer()

  mainFlow
  
  def mainFlow() {
    val incomeInfo = GeoDataGenerator.getIncomeInfo()
    val getCityFuture = GeoDataGenerator.getCity(incomeInfo)
    
    var cities = Await.result(getCityFuture, Duration.Inf)
    println("FROM " + cities.length)
    cities = cities ++ AdditionalLoader.loadAdditionalCities(incomeInfo)
    println("TO " + cities.length)
    
    //cities.foreach(println)
        
    //make sure cities are saved first as we need the id for airport info
    try {
//      AirportSource.deleteAllAirports()
      CitySource.deleteAllCitites()
      CitySource.saveCities(cities)
    } catch {
      case e : Throwable => e.printStackTrace()
    }
    
    val airports = GeoDataGenerator.buildAirportData(GeoDataGenerator.getAirport(), GeoDataGenerator.getRunway(), cities)

    GeoDataGenerator.buildCountryData(airports)

    Await.result(actorSystem.terminate(), Duration.Inf)
  }
}