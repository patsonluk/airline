package com.patson.init

import com.patson.data.AirportSource
import com.patson.model.Runway

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirportRunwayPatcher extends App {

  //implicit val materializer = FlowMaterializer()

  mainFlow
  
  def mainFlow() {
    val runways: Map[String, List[Runway]] = Await.result(GeoDataGenerator.getRunway(), Duration.Inf)
    val airports = AirportSource.loadAllAirports(true)

    GeoDataGenerator.setAirportRunwayDetails(airports, runways)

    AirportSource.updateAirports(airports)

    Await.result(actorSystem.terminate(), Duration.Inf)
  }
}