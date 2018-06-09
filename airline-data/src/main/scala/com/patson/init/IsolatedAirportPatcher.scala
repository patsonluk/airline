package com.patson.init

import com.patson.model._
import com.patson.data._
import com.patson.Util
import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

object IsolatedAirportPatcher {
   
  private[this] val ISOLATION_MAX_POP = 50000 //MAX pop to be consider isolated. Otherwise it has the right mass to be alone
  private[this] val HUB_MIN_POP = 100000 //Not considered as isolated if there's a HUB within HUB_RANGE
  private[this] val HUB_RANGE = 300 //if couldn't find a major airport with
  
  
  def patchIsolatedAirports() = {
    val allAirports = AirportSource.loadAllAirports(true)
    val isolatedAirports = ListBuffer[Airport]()
    allAirports.foreach { airport =>
      if (airport.population <= ISOLATION_MAX_POP) { //then a valid candidate to consider as isolated
        val boundaryLongitude = GeoDataGenerator.calculateLongitudeBoundary(airport.latitude, airport.longitude, HUB_RANGE)
        val hubWithinRange = allAirports.find { targetAirport =>
          if (targetAirport.population >= HUB_MIN_POP && targetAirport.countryCode == airport.countryCode) {
            targetAirport.longitude >= boundaryLongitude._1 && targetAirport.longitude <= boundaryLongitude._2
            val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
            distance <= HUB_RANGE
          } else {
            false
          }
        }
        
        if (hubWithinRange.isEmpty) { //then it is indeed isolated
          val existingFeatures = airport.getFeatures()
          airport.initFeatures(existingFeatures :+ AirportFeature(AirportFeatureType.ISOLATED_TOWN, 1))
          isolatedAirports += airport
        }
      }
    }
    
    isolatedAirports.foreach(println)
    AirportSource.updateAirportFeatures(isolatedAirports.toList)
  }
}  
  