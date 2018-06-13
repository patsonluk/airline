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
        "HNL" -> 80, //honolulu
        "CDG" -> 65, //paris
        "MCO" -> 58, //Orlando
        "BKK" -> 56, //bangkok
        "PEK" -> 44, //Beijin
        "VCE" -> 40, //venice
        "ATH" -> 40, //Athens
        "MLE" -> 38, //Maldives
        "OKA" -> 32, //Okinawa
        "FCO" -> 30, //Rome
        "KIX" -> 30,
        "LAS" -> 30, //Vegas
        "MIA" -> 30,
        "HND" -> 30, //Tokyo
        "GUM" -> 28, //Guam
        "KOA" -> 25, //Kailua Hawaii
        "VIE" -> 20, //vienna
        "CAI" -> 20,
        "IST" -> 20,
        "LHR" -> 20,
        "BOM" -> 20,
        
        "LAX" -> 20,
        "YVR" -> 20,
        "IAD" -> 20, //washington
        "SJU" -> 20, //San Juan
        "GRU" -> 20, //Sao Paulo
        "BNE" -> 20, //brisbane
        "AKL" -> 20, //auckland
        "PRG" -> 20, //prague
        "HKG" -> 28, //hong kong
        "SFO" -> 20,
        "OGG" -> 18, //	Kahului, Hawaii
        "SYX" -> 18, //Sanya China
        "XIY" -> 18, //Xian china
        "PVG" -> 18, //Shanghai
        "HGH" -> 17, //Hangzhou china
        "LIH" -> 16, //Lihue, Hawaii
        "ITO" -> 15, //Hilo hawaii
        "KWL" -> 15, //Guilin China
        "LXA" -> 12, //Lhasa China
        "TAO" -> 10 //Qingdao China
        
        
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