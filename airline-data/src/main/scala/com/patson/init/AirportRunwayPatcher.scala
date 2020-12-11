package com.patson.init

import com.patson.data.AirportSource
import com.patson.model._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirportRunwayPatcher extends App {

  //implicit val materializer = FlowMaterializer()

  mainFlow
  
  def mainFlow() {
    val airports = AirportSource.loadAllAirports(true)
    setRunways(airports)
    AirportSource.updateAirports(airports)

    Await.result(actorSystem.terminate(), Duration.Inf)

  }

  def setRunways(airports : List[Airport]) = {
    val runways: Map[String, List[Runway]] = Await.result(GeoDataGenerator.getRunway(), Duration.Inf)
    GeoDataGenerator.setAirportRunwayDetails(airports, runways)
  }
}