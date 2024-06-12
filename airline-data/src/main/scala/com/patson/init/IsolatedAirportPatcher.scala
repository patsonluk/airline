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
    val ISOLATED_ISLAND_AIRPORTS = Array(
      "KOI", "SYY", "BEB", "TRE", "ILY", "CAL", "ISC", "GCI", "JER",
      //carribean
      "PVA", "ADZ", "CYB", "RTB", "UII", "GJA",
      //europe
      "IDY", "ACI", "ISC", "OUI",
      "EGH", //Tresco Heliport
      "BYR", //DK
      "HGL", //DE
      "IOR","INQ","IIA", //IE
      //USA
      "FRD", "ESD", "ACK", "MVY", "BID",
      //oceania
      "WSZ"
      //add more greek, usa, japan, australia?
    )
    val ISOLATED_COUNTRIES = Array("FO", "BS", "KY", "TC", "VC", "GD", "DM", "AG", "MS", "BQ", "BL", "MF", "SX", "AI", "VI", "VG", "MU", "MV", "CC", "CK") //always add 1 level, because island countries and islands are inherently isolated


    allAirports.foreach { airport =>
      var isolationLevel : Int = 0

      val boundaryLongitude = GeoDataGenerator.calculateLongitudeBoundary(airport.latitude, airport.longitude, HUB_RANGE_BRACKETS.last)
      for (i <- 0 until HUB_RANGE_BRACKETS.size) {
        val threshold = HUB_RANGE_BRACKETS(i)
        val populationWithinRange = allAirports.filter { targetAirport =>
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
          distance < threshold && targetAirport.longitude >= boundaryLongitude._1 && targetAirport.longitude <= boundaryLongitude._2
        }.map(_.population).sum
        if (populationWithinRange < 100000) { //very isolated
          isolationLevel += 3
        } else if (populationWithinRange < 500000) {
          isolationLevel += 2
        } else if (populationWithinRange < 2500000) { //kinda isolated
          isolationLevel += 1
        }
      }
      isolationLevel = (Math.floor( isolationLevel / 2 )).toInt
      if (ISOLATED_COUNTRIES.contains(airport.countryCode) && airport.size <= 4 || ISOLATED_ISLAND_AIRPORTS.contains(airport.iata)) {
        isolationLevel += 1
      }
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
  