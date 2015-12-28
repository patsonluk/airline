package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher {
  import AirportFeatureType._
  val featureList = Map(
    INTERNATION_HUB -> Map[String, Int](
        "JFK" -> 8,
        "LHR" -> 8,
        "HKG" -> 7,
        "SIN" -> 7,
        "HND" -> 6,
        "CDG" -> 6,
        "PVG" -> 4,
        "PEK" -> 2,
        "SYD" -> 5,
        "AUH" -> 5,
        "EZE" -> 2,
        "BOM" -> 1,
        "MXP" -> 3,
        "GRU" -> 2,
        "FRA" -> 4,
        "YYZ" -> 3,
        "LAX" -> 4,
        "MAD" -> 2,
        "AMS" -> 2,
        "KUL" -> 2,
        "FCO" -> 3,
        "BRU" -> 1,
        "ICN" -> 3,
        "JNB" -> 2,
        "SFO" -> 4,
        "ZRH" -> 4,
        "IAD" -> 2,
        "MIA" -> 2,
        "YVR" -> 2,
        "DMK" -> 2,
        "BOS" -> 2,
        "CGK" -> 2,
        "TPE" -> 3,
        "KIX" -> 2,
        "ARN" -> 2
      ),
    VACATION_HUB -> Map[String, Int](
        "CDG" -> 5, //paris
        "FCO" -> 3,
        "VCE" -> 4, //venice
        "VIE" -> 2, //vienna
        "CAI" -> 2,
        "IST" -> 2,
        "ATH" -> 4,
        "LHR" -> 2,
        "BOM" -> 2,
        "KIX" -> 3,
        "PEK" -> 2,
        "DMK" -> 2,
        "HNL" -> 8, //honolulu
        "LAX" -> 2,
        "YVR" -> 2,
        "LAS" -> 3,
        "IAD" -> 2, //washington
        "MIA" -> 3,
        "MCO" -> 4,
        "HND" -> 3, //Tokyo
        "SJU" -> 2, //San Juan
        "GRU" -> 2, //Sao Paulo
        "BNE" -> 2, //brisbane
        "AKL" -> 2, //auckland
        "PRG" -> 2, //prague
        "GUM" -> 4, //Guam
        "OKA" -> 2, //Okinawa
        "MLE" -> 6, //Maldives
        "SFO" -> 2
      ),
    FINANCIAL_HUB -> Map[String, Int](
        "LHR" -> 9,
        "JFK" -> 8,
        "HKG" -> 7,
        "SIN" -> 7,
        "HND" -> 7,
        "ICN" -> 5,
        "PVG" -> 5,
        "ZRH" -> 4,
        "YYZ" -> 2,
        "SFO" -> 2,
        "CDG" -> 3, //paris
        "FRA" -> 5
      ), //frankfrut
    DOMESTIC_AIRPORT -> Map[String, Int]()
  )
  
  def patchFeatures() = {
    val airportFeatures = scala.collection.mutable.Map[String, ListBuffer[AirportFeature]]()
    featureList.foreach {
      case(featureType , airportMap) =>
        airportMap.foreach {
          case (airportIata, featureStrength) =>
            val featuresForThisAirport = airportFeatures.getOrElseUpdate(airportIata, ListBuffer[AirportFeature]())
            featuresForThisAirport += AirportFeature(featureType, featureStrength)
        }
    }
    
    val updatingAirports = 
      airportFeatures.map {
        case (iata, features) =>
          val airport = AirportSource.loadAirportByIata(iata).get
          airport.initFeatures(features.toList)
          airport
      }
    
    AirportSource.updateAirportFeatures(updatingAirports.toList)
  }
}