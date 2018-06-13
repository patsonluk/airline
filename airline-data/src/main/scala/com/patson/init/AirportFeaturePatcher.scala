package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher {
  import AirportFeatureType._
  val featureList = Map(
    INTERNATIONAL_HUB -> Map[String, Int](
        "JFK" -> 80,
        "LHR" -> 80,
        "HKG" -> 70,
        "SIN" -> 70,
        "HND" -> 60,
        "CDG" -> 60,
        "PVG" -> 40,
        "PEK" -> 20,
        "SYD" -> 50,
        "AUH" -> 50,
        "EZE" -> 20,
        "BOM" -> 10,
        "MXP" -> 30,
        "GRU" -> 20,
        "FRA" -> 40,
        "YYZ" -> 30,
        "LAX" -> 40,
        "MAD" -> 20,
        "AMS" -> 20,
        "KUL" -> 20,
        "FCO" -> 30,
        "BRU" -> 10,
        "ICN" -> 30,
        "JNB" -> 20,
        "SFO" -> 40,
        "ZRH" -> 40,
        "IAD" -> 20,
        "MIA" -> 20,
        "YVR" -> 20,
        "BKK" -> 20,
        "BOS" -> 20,
        "CGK" -> 20,
        "TPE" -> 30,
        "KIX" -> 20,
        "ARN" -> 20
      ),
    VACATION_HUB -> Map[String, Int](
        "CDG" -> 50, //paris
        "FCO" -> 30,
        "VCE" -> 40, //venice
        "VIE" -> 20, //vienna
        "CAI" -> 20,
        "IST" -> 20,
        "ATH" -> 40,
        "LHR" -> 20,
        "BOM" -> 20,
        "KIX" -> 30,
        "PEK" -> 20,
        "BKK" -> 20, //bangkok
        "HNL" -> 80, //honolulu
        "LAX" -> 20,
        "YVR" -> 20,
        "LAS" -> 30,
        "IAD" -> 20, //washington
        "MIA" -> 30,
        "MCO" -> 40,
        "HND" -> 30, //Tokyo
        "SJU" -> 20, //San Juan
        "GRU" -> 20, //Sao Paulo
        "BNE" -> 20, //brisbane
        "AKL" -> 20, //auckland
        "PRG" -> 20, //prague
        "GUM" -> 40, //Guam
        "OKA" -> 20, //Okinawa
        "MLE" -> 60, //Maldives
        "SFO" -> 20
      ),
    FINANCIAL_HUB -> Map[String, Int](
        "LHR" -> 80,
        "JFK" -> 80,
        "HKG" -> 70,
        "SIN" -> 70,
        "HND" -> 70,
        "ICN" -> 50,
        "PVG" -> 50,
        "ZRH" -> 40,
        "YYZ" -> 20,
        "SFO" -> 20,
        "CDG" -> 30, //paris
        "FRA" -> 50
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