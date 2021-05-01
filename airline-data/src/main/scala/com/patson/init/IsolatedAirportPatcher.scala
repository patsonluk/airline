package com.patson.init

import com.patson.model._
import com.patson.data._
import com.patson.Util
import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

object IsolatedAirportPatcher {
   

  import IsolatedTownFeature._
  
  def patchIsolatedAirports() = {
    val allAirports = AirportSource.loadAllAirports(true)
    val isolationByAirport = Map[Airport, Int]()

    allAirports.foreach { airport =>
      if (airport.population <= ISOLATION_MAX_POP) { //then a valid candidate to consider as isolated
        var isolationLevel = 0

        val boundaryLongitude = GeoDataGenerator.calculateLongitudeBoundary(airport.latitude, airport.longitude, HUB_RANGE_BRACKETS.last)
        for (i <- 0 until HUB_RANGE_BRACKETS.size) {
          val threshold = HUB_RANGE_BRACKETS(i)
          val hubWithinRange = allAirports.find { targetAirport =>
            if (targetAirport.population >= HUB_MIN_POP && targetAirport.longitude >= boundaryLongitude._1 && targetAirport.longitude <= boundaryLongitude._2) {
              val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
              distance <= threshold
            } else {
              false
            }
          }

          if (hubWithinRange.isEmpty) { //then it is indeed isolated
            isolationLevel = i + 1
          }
        }
        if (isolationLevel > 0) {
          isolationByAirport.put(airport, isolationLevel)
        }
      }
    }

    val updatingAirports = isolationByAirport.map {
      case (airport,isolationLevel) =>
        val existingFeatures = airport.getFeatures().filter(_.featureType != AirportFeatureType.ISOLATED_TOWN)
        airport.initFeatures(existingFeatures :+ AirportFeature(AirportFeatureType.ISOLATED_TOWN, isolationLevel))
        println(s"$airport isolation level $isolationLevel")
        airport
    }.toList


    AirportSource.updateAirportFeatures(updatingAirports)
  }
}  
  