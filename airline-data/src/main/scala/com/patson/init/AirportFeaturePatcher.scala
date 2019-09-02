package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher extends App {
  import AirportFeatureType._
  val featureList = Map(
    INTERNATIONAL_HUB -> Map[String, Int](
        "JFK" -> 80,
        "LHR" -> 80,
        "HKG" -> 70,
        "SIN" -> 70,
        "NRT" -> 65,
        "CDG" -> 60,
        "GVA" -> 58, //Geneva
        "PVG" -> 40,
        "PEK" -> 20,
        "SYD" -> 50,
        "AUH" -> 50,
        "EZE" -> 20,
        "MXP" -> 30,
        "GRU" -> 20,
        "FRA" -> 40,
        "YYZ" -> 30,
        "LAX" -> 40,
        "MAD" -> 20,
        "AMS" -> 20,
        "KUL" -> 20,
        "FCO" -> 30,
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
        "ARN" -> 20,
        "MEX" -> 18,
        "BRU" -> 18,
        "WAW" -> 15,
        "LUX" -> 11,
        "BOM" -> 10
        "PTY" -> 30
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
        "DMK" -> 35, //Bangkok other airport
        "TFS" -> 34, //Tenerife
        "PPT" -> 34, //Pape'ete
        "HER" -> 33, //Crete
        "CNS" -> 33, //Cairns Australia
        "PUJ" -> 33, //Punta cana
        "OKA" -> 32, //Okinawa
        "CUN" -> 32, //Cancun
        "ORY" -> 32, //Paris Orly
        "GUM" -> 31, //Guam
        "FCO" -> 38, //Rome
        "PTY" -> 33,
        "KIX" -> 30,
        "LAS" -> 30, //Vegas
        "MIA" -> 30,
        "NRT" -> 28, //Tokyo
        "HND" -> 28, //Tokyo
        "VIE" -> 28, //vienna
        "BCN" -> 28, //Barcelona
        "HAV" -> 27, //Havana
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
        "CPH" -> 21, //Copenhagen
        "SFO" -> 20,
        "OOL" -> 20, //Gold Coast Aus
        "CMB" -> 20, //Colombo, Sri Lanka
        "MRU" -> 20, //Mauritius
        "KIN" -> 19, //Kingston Jamaica
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
        "VRA" -> 18, //Varadero Cuba
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
        "STX" -> 14, //US Virgin Island
        "MBJ" -> 13, //Montego bay Jamaica
        "LXA" -> 12, //Lhasa China
        "MYR" -> 12, //Myrtle Beach
        "KBV" -> 12, //Krab Thailand
        "AUA" -> 11, //Aruba
        "BGI" -> 11, //Barbados
        "IBZ" -> 11, //Ibiza
        "TAO" -> 10, //Qingdao China
        "GEA" -> 10,
        "YZF" -> 10, //Yellowknife
        "CUR" -> 10, //Curacao
        "IKU" -> 10, //Tamchy
        "GPS" -> 16 //Galapagos
      ),
    FINANCIAL_HUB -> Map[String, Int](
        "LHR" -> 80,
        "JFK" -> 80,
        "HND" -> 75,
        "HKG" -> 70,
        "SIN" -> 70,
        "ICN" -> 50,
        "PVG" -> 50,
        "DXB" -> 44,
        "ZRH" -> 40,
        "BOS" -> 39,
        "SFO" -> 38,
        "DFW" -> 37,
        "ORD" -> 35,
        "DEN" -> 34,
        "EWR" -> 33,
        "SHA" -> 33, //Shanghai Hongqiao
        "LGW" -> 32, //London Gatwick
        "ITM" -> 28,
        "CLT" -> 28,
        "ATL" -> 27,
        "ORY" -> 25, //Paris Orly
        "CAN" -> 25, //Guangzhou
        "SZX" -> 25, //Shenzhen
        "GMP" -> 24, //Seoul Gimpo
        "TSN" -> 23, //Tianjin
        "BHX" -> 22, //Birmingham
        "DEL" -> 22, //New Delhi
        "PHX" -> 22,
        "IAH" -> 21,
        "YYZ" -> 20,
        "MSP" -> 19,
        "LGA" -> 19,
        "DUS" -> 18,
        "CGN" -> 18,
        "MEX" -> 18,
        "PHL" -> 17,
        "MUC" -> 23,
        "SEA" -> 22,
        "CPT" -> 19,
        "BWI" -> 18, //Baltimore
        "MAA" -> 17, //Chennai
        "FUK" -> 20,
        "PUS" -> 22, //Busan
        "FLL" -> 15,
        "BSB" -> 18, //Brazilia
        "CDG" -> 38, //paris
        "FRA" -> 54,
        "DMK" -> 19, //Bangkok other airport
        "CPH" -> 19, //Copenhagen
        "NGO" -> 18,
        "MAN" -> 18,
        "HAM" -> 18,
        "TXL" -> 16,
        "BOG" -> 15, //Bogota alpha-
        "YUL" -> 18, //Montreal
        "RUH" -> 15,
        "SCL" -> 16, //Santiago alpha-
        "DCA" -> 19,
        "ARN" -> 17,
        "SVO" -> 19, //Moscow
        "DME" -> 19, //Moscow
        "DUB" -> 13,  //Dublin
        "MNL" -> 13, //Manila
        "CKG" -> 10, //Chongqing
        "WUH" -> 10, //Wuhan
        "CTU" -> 10, //Chengdu
        "KHH" -> 11, //Kaohsiung
        "PTY" -> 23 //Panama
      ), //frankfrut
    DOMESTIC_AIRPORT -> Map[String, Int]()
  )

    patchFeatures()
  
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
