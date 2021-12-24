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

  def generateGenericTransit(airportCount : Int, range : Int) : Unit = {
    val airports = AirportSource.loadAllAirports(true).sortBy { _.power }.takeRight(airportCount)

    var counter = 0;
    var progressCount = 0;

    val countryRelationships = CountrySource.getCountryMutualRelationships()
    for (airport <- airports) {
      //calculate max and min longitude that we should kick off the calculation
      val boundaryLongitude = calculateLongitudeBoundary(airport.latitude, airport.longitude, range)
      val airportsInRange = scala.collection.mutable.ListBuffer[(Airport, Double)]()
      for (targetAirport <- airports) {
        if (airport.id != targetAirport.id &&
            airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2 &&
            countryRelationships.getOrElse((airport.countryCode, targetAirport.countryCode), 0) >= 3) {
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
          if (range >= distance) {
            airportsInRange += Tuple2(airport, distance)
          }
        }
      }

      airportsInRange.foreach { case (targetAirport, distance) =>
        val minSize = Math.min(airport.size, targetAirport.size)
        val capacity = minSize * 10000 //kinda random, final will be double from -> to , to -> from
        val genericTransit = GenericTransit(from = airport, to = targetAirport, distance = distance.toInt, capacity = LinkClassValues.getInstance(economy = capacity))
        LinkSource.saveLink(genericTransit)

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