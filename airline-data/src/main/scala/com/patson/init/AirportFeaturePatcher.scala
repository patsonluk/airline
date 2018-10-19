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
        "NRT" -> 65,
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
        "BKK" -> 70, //bangkok
        "CDG" -> 65, //paris
        "MCO" -> 58, //Orlando
        "MLE" -> 55, //Maldives
        "JTR" -> 46, //Santorini
        "PEK" -> 44, //Beijing
        "DPS" -> 43, //Bali
        "NAN" -> 41, //Fiji
        "VCE" -> 40, //venice
        "ATH" -> 40, //Athens
        "DXB" -> 39, //Dubai
        "MLA" -> 39, //Malta
        "PRG" -> 38, //Prague
        "LPA" -> 38, //Gran Canaria 
        "IST" -> 35, //Istanbul
        "TFS" -> 34, //Tenerife
        "PPT" -> 34, //Pape'ete
        "HER" -> 33, //Crete
        "CNS" -> 33, //Cairns Australia
        "PUJ" -> 33, //Puna cana
        "OKA" -> 32, //Okinawa
        "CUN" -> 32, //Cancun
        "GUM" -> 31, //Guam
        "FCO" -> 38, //Rome
        "KIX" -> 30,
        "LAS" -> 30, //Vegas
        "MIA" -> 30,
        "NRT" -> 28, //Tokyo
        "HND" -> 28, //Tokyo
        "VIE" -> 28, //vienna
        "BCN" -> 28, //Barcelona
        "MRS" -> 27,
        "MXP" -> 27,
        "SJD" -> 26, //Los Cabos
        "GIG" -> 26, //Rio
        "KOA" -> 25, //Kailua Hawaii
        "NAS" -> 25, //Bahamas (nassau)
        "KEF" -> 24, //Reykjavik
        "FLL" -> 22, //Fort Lauderdale
        "AMS" -> 21, //Amsterdam
        "TLV" -> 21,
        "CAI" -> 20,
        "NCE" -> 22,
        "LHR" -> 20,
        "BOM" -> 20,
        "LAX" -> 20,
        "YVR" -> 20,
        "IAD" -> 20, //washington
        "SJU" -> 20, //San Juan
        "GRU" -> 20, //Sao Paulo
        "BNE" -> 20, //brisbane
        "AKL" -> 20, //auckland
        "SIN" -> 29, //Singapore
        "HKG" -> 28, //hong kong
        "BUD" -> 24, //Budapest
        "PSA" -> 22,
        "OSL" -> 22, //Oslo
        "SFO" -> 20,
        "BWN" -> 19, //Brunei
        "MFM" -> 19, //Macau
        "CTS" -> 19, //Chitose Sapporo
        "HKT" -> 19, //Phuket
        "PMI" -> 19, //Palma De Mallorca
        "OGG" -> 18, //	Kahului, Hawaii
        "KUL" -> 19, //	Kuala Lumpur
        "LIS" -> 19, //Lisbon
        "SYX" -> 18, //Sanya China
        "XIY" -> 18, //Xian china
        "PVG" -> 18, //Shanghai
        "PER" -> 18, //Perth
        "FLN" -> 18,
        "NAP" -> 18,
        "KRK" -> 18,
        "HEL" -> 17, //Helsinki 
        "LED" -> 17, //St Petersburg
        "CJU" -> 17, //Jeju
        "HGH" -> 17, //Hangzhou china
        "SJO" -> 17, //Costa Rica
        "LIH" -> 16, //Lihue, Hawaii
        "CEB" -> 16, //Cebu
        "ITO" -> 15, //Hilo hawaii
        "WAW" -> 15, //Warsaw
        "KWL" -> 15, //Guilin China
        "DRW" -> 14, //Darwin Australia
        "MBJ" -> 13, //Montego bay
        "LXA" -> 12, //Lhasa China
        "MYR" -> 12,
        "AUA" -> 11, //Aruba
        "BGI" -> 11, //Barbados
        "IBZ" -> 11, //Ibiza
        "TAO" -> 10, //Qingdao China
        "GEA" -> 10,
        "YZF" -> 10, //Yellowknife
        "CUR" -> 10 //Curacao
        
        
      ),
    FINANCIAL_HUB -> Map[String, Int](
        "LHR" -> 80,
        "JFK" -> 80,
        "HND" -> 75,
        "HKG" -> 70,
        "SIN" -> 70,
        "ICN" -> 50,
        "PVG" -> 50,
        "ZRH" -> 40,
        "BOS" -> 39,
        "SFO" -> 38,
        "DFW" -> 37,
        "ORD" -> 35,
        "DEN" -> 34,
        "EWR" -> 33,
        "SHA" -> 33, //Shanghai Hongqiao
        "ITM" -> 28,
        "CLT" -> 28,
        "DXB" -> 25,
        "PHX" -> 22,
        "IAH" -> 21,
        "YYZ" -> 20,
        "MSP" -> 19,
        "LGA" -> 19,
        "DUS" -> 18,
        "CGN" -> 18,
        "PHL" -> 17,
        "MUC" -> 23,
        "SEA" -> 22,
        "CPT" -> 19,
        "CAN" -> 18, //Guangzhou
        "MAA" -> 17, //Chennai
        "SZX" -> 15, //Shenzhen
        "FUK" -> 20,
        "ATL" -> 18,
        "PUS" -> 22, //Busan
        "FLL" -> 15,
        "BSB" -> 18, //Brazilia
        "CDG" -> 38, //paris
        "FRA" -> 54,
        "NGO" -> 18,
        "MAN" -> 18,
        "HAM" -> 18,
        "TXL" -> 16,
        "ARN" -> 17,
        "MNL" -> 13, //Manila
        "KHH" -> 11 //Kaohsiung
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