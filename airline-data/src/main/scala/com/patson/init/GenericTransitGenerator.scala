package com.patson.init

import com.patson.{Authentication, DemandGenerator, Util}
import com.patson.data._
import com.patson.data.airplane._
import com.patson.init.GeoDataGenerator.calculateLongitudeBoundary
import com.patson.model._
import com.patson.model.airplane._

import java.util.Calendar
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Set}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random


object GenericTransitGenerator {

  def main(args : Array[String]) : Unit = {
    generateGenericTransit()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }
  def generateGenericTransit(airportCount : Int = 4000, range : Int = 50) : Unit = {
    //purge existing generic transit
    LinkSource.deleteLinksByCriteria(List(("transport_type", TransportType.GENERIC_TRANSIT.id)))

    val airports = AirportSource.loadAllAirports(true).sortBy { _.power }.takeRight(airportCount)

    var counter = 0;
    var progressCount = 0;

    val processed = mutable.HashSet[(Int, Int)]()
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    for (airport <- airports) {
      //calculate max and min longitude that we should kick off the calculation
      val boundaryLongitude = calculateLongitudeBoundary(airport.latitude, airport.longitude, range)
      val airportsInRange = scala.collection.mutable.ListBuffer[(Airport, Double)]()
      for (targetAirport <- airports) {
        if (airport.id != targetAirport.id &&
            !processed.contains((targetAirport.id, airport.id)) && //check the swap pairs are not processed already to avoid duplicates
            airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2 &&
            countryRelationships.getOrElse((airport.countryCode, targetAirport.countryCode), 0) >= 2) {
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude).toInt
          if (range >= distance) {
            airportsInRange += Tuple2(targetAirport, distance)
          }
        }

        processed.add((airport.id, targetAirport.id))
      }

      airportsInRange.foreach { case (targetAirport, distance) =>
        val minSize = Math.min(airport.size, targetAirport.size)
        val capacity = minSize * 10000 //kinda random
        val genericTransit = GenericTransit(from = airport, to = targetAirport, distance = distance.toInt, capacity = LinkClassValues.getInstance(economy = capacity))
        LinkSource.saveLink(genericTransit)
        println(genericTransit)
      }

      val progressChunk = airports.size / 100
      counter += 1
      if (counter % progressChunk == 0) {
        progressCount += 1;
        print(".")
        if (progressCount % 10 == 0) {
          print(progressCount + "% ")
        }
      }
    }
  }
}