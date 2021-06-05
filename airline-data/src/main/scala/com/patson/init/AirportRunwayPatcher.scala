package com.patson.init

import com.patson.data.AirportSource
import com.patson.init.GeoDataGenerator.CsvAirport
import com.patson.model._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Use {@link AirportGeoPatcher} instead
  */
@deprecated
object AirportRunwayPatcher extends App {
//
//  //implicit val materializer = FlowMaterializer()
//
//  mainFlow
//
//  def mainFlow() {
//    val airports = AirportSource.loadAllAirports(true)
//    setRunways(airports)
//    AirportSource.updateAirports(airports)
//
//    Await.result(actorSystem.terminate(), Duration.Inf)
//
//  }


}