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
      var isolationLevel : Int = 0

      val boundaryLongitude = GeoDataGenerator.calculateLongitudeBoundary(airport.latitude, airport.longitude, HUB_RANGE_BRACKETS.last)
      for (i <- 0 until HUB_RANGE_BRACKETS.size) {
        val threshold = HUB_RANGE_BRACKETS(i)
        val countOfHubWithinRange = allAirports.count { targetAirport =>
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
          targetAirport.population >= HUB_MIN_POP && distance < threshold && targetAirport.longitude >= boundaryLongitude._1 && targetAirport.longitude <= boundaryLongitude._2
        }
        if (0 == countOfHubWithinRange) { //very isolated
          isolationLevel += 3
        } else if (3 >= countOfHubWithinRange) {
          isolationLevel += 2
        } else if (5 >= countOfHubWithinRange) { //kinda isolated
          isolationLevel += 1
        }
      }
      isolationLevel = (Math.floor( isolationLevel / 2 )).toInt
      if (isolationLevel > 0) {
        isolationByAirport.put(airport, isolationLevel)
      }
    }

    isolationByAirport.foreach {
      case (airport,isolationLevel) =>
        val existingFeatures = airport.getFeatures().filter(_.featureType != AirportFeatureType.ISOLATED_TOWN)
        val newFeatures = existingFeatures :+ AirportFeature(AirportFeatureType.ISOLATED_TOWN, isolationLevel)
        //airport.initFeatures(newFeatures) //CANNOT init features here, features can only be init once.
        AirportSource.updateAirportFeatures(airport.id, newFeatures)
        println(s"$airport isolation level $isolationLevel features ${airport.getFeatures()}")
    }



  }
}  
  