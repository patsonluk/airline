package com.patson.init

import com.patson.data.{AirportSource, CitySource, CountrySource}
import com.patson.init.GeoDataGenerator.{CsvAirport, getCity, getIncomeInfo}
import com.patson.model._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Regenerate ALL airport data (pops, runway, power etc) without wiping the existing airport DB
  *
  * It will attempt to update the airport if it's already existed and insert airport otherwise
  *
  * it will NOT purge airports that no longer in the CSV file tho
  *
  */
object AirportGeoPatcher extends App {
  //implicit val materializer = FlowMaterializer()

  mainFlow

  def mainFlow() {
    val runways : Map[Int, List[Runway]] = Await.result(GeoDataGenerator.getRunway(), Duration.Inf)
    val existingAirports = AirportSource.loadAllAirports(false)
    val iataToGeneratedId : Map[String, Int] = existingAirports.map(airport => (airport.iata, airport.id)).toMap //just load to get IATA to our generated ID

    val csvAirports : List[CsvAirport] = Await.result(GeoDataGenerator.getAirport(), Duration.Inf).map { csvAirport =>
      val rawAirport = csvAirport.airport
      val csvAirportId = csvAirport.csvAirportId
      val scheduleService = csvAirport.scheduledService

      iataToGeneratedId.get(rawAirport.iata) match {
        case Some(savedId) => CsvAirport(rawAirport.copy(id = savedId), csvAirportId, scheduleService)
        case None => csvAirport
      }
    }

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

    val computedAirports = GeoDataGenerator.generateAirportData(csvAirports, runways, cities)

    val newAirports = computedAirports.filter(_.id == 0)
    val updatingAirports = computedAirports.filter(_.id > 0)

    GeoDataGenerator.setAirportRunwayDetails(csvAirports, runways)
    println(s"Creating ${newAirports.length} Airports")
    AirportSource.saveAirports(newAirports)

    println(s"Updating ${updatingAirports.length} Airports")
    AirportSource.updateAirports(updatingAirports)

    val deletingAirportIds = existingAirports.map(_.id).diff(computedAirports.map(_.id))
    println(s"Deleting ${deletingAirportIds.length} Airports")
    AirportSource.deleteAirports(deletingAirportIds)


    AirportFeaturePatcher.patchFeatures()


    val updatingCountries = ListBuffer[Country]()
    computedAirports.groupBy(_.countryCode).foreach {
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