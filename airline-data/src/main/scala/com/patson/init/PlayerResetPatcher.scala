package com.patson.init

import com.patson.data.{AirlineSource, AirportSource, CitySource, CountrySource}
import com.patson.init.GeoDataGenerator.{CsvAirport, getCity, getIncomeInfo}
import com.patson.model._

import scala.collection.mutable.ListBuffer
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
object PlayerResetPatcher extends App {
  //implicit val materializer = FlowMaterializer()

  mainFlow

  val NEW_BALANCE = 500000000

  def mainFlow() {
    val airlines = AirlineSource.loadAllAirlines()
    airlines.foreach { airline =>
      Airline.resetAirline(airline.id, 500000000, true)
    }

    Await.result(actorSystem.terminate(), Duration.Inf)
  }



}