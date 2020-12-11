package com.patson.init

import com.patson.data.{AirportSource, CitySource, CountrySource}
import com.patson.init.GeoDataGenerator.{getCity, getIncomeInfo}
import com.patson.model._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirportGeoPatcher extends App {
  //implicit val materializer = FlowMaterializer()

  mainFlow

  def mainFlow() {
    val runways : Map[String, List[Runway]] = Await.result(GeoDataGenerator.getRunway(), Duration.Inf)
    val iataToId : Map[String, Int] = AirportSource.loadAllAirports(false).map(airport => (airport.iata, airport.id)).toMap //just load to get IATA to ID
    val rawAirports = Await.result(GeoDataGenerator.getAirport(), Duration.Inf).map(rawAirport => iataToId.get(rawAirport.iata) match {
      case Some(savedId) => rawAirport.copy(id = savedId)
      case None => rawAirport
    })

    val incomeInfo = getIncomeInfo()
    val getCityFuture = getCity(incomeInfo)

    var cities = Await.result(getCityFuture, Duration.Inf)
    cities = cities ++ AdditionalLoader.loadAdditionalCities(incomeInfo)
    //make sure cities are saved first as we need the id for airport info
    try {
      //      AirportSource.deleteAllAirports()
      CitySource.deleteAllCitites()
      CitySource.saveCities(cities)
    } catch {
      case e : Throwable => e.printStackTrace()
    }

    val adjustedAirports = GeoDataGenerator.generateAirportData(rawAirports, runways, cities)

    println(s"Updating ${adjustedAirports.filter(_.id != 0).length} Airports")

    AirportRunwayPatcher.setRunways(adjustedAirports)

    AirportSource.updateAirports(adjustedAirports)

    AirportFeaturePatcher.patchFeatures()


    val updatingCountries = ListBuffer[Country]()
    adjustedAirports.groupBy(_.countryCode).foreach {
      case (countryCode, airports) =>
        val totalAirportPopulation : Long = airports.map {
          _.population
        }.sum
        val averageIncome = if (totalAirportPopulation == 0) {
          0
        } else {
          airports.map {
            _.power
          }.sum / totalAirportPopulation
       }

        CountrySource.loadCountryByCode(countryCode) match {
          case Some(country) => updatingCountries.append(country.copy(airportPopulation = totalAirportPopulation.toInt))
          case None => println(s"Country $countryCode not found!")
        }

    }
    CountrySource.updateCountries(updatingCountries.toList)


    Await.result(actorSystem.terminate(), Duration.Inf)
  }

}