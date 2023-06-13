package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher extends App {

  import AirportFeatureType._

  lazy val featureList = Map(
    INTERNATIONAL_HUB -> Map[String, Int](
      "JFK" -> 80, //New York
      "LHR" -> 80, //London Heathrow
      "HKG" -> 70, //Hong Kong
      "SIN" -> 70, //Singaport
      "NRT" -> 65, //Tokyo Narita
      "CDG" -> 60, //Paris Charles de Gaulle
      "GVA" -> 58, //Geneva
      "SYD" -> 50, //Sydney
      "AUH" -> 50, //Abu Dhabi
      "PVG" -> 40, //Shanghai
      "SFO" -> 40, //San Francisco
      "ZRH" -> 40, //Zurich
      "FRA" -> 40, //Frankfurt
      "LAX" -> 40, //Los Angeles
      "TPE" -> 30, //Taipei
      "MXP" -> 30, //Milan
      "YYZ" -> 30, //Toronto
      "FCO" -> 30, //Rome
      "ICN" -> 30, //Seoul Incheon
      "PEK" -> 20, //Beijing
      "EZE" -> 20, //Buenos Aires
      "MAD" -> 20, //Madrid
      "AMS" -> 20, //Amsterdam
      "KUL" -> 20, //Kuala Lumpur
      "JNB" -> 20, //Johannesburg
      "GRU" -> 20, //Sao Paulo
      "IAD" -> 20, //Washington Dulles
      "MIA" -> 20, //Miami
      "YVR" -> 20, //Vancouver
      "BKK" -> 20, //Bangkok
      "BOS" -> 20, //Boston
      "CGK" -> 20, //Jakarta (Indonesia)
      "KIX" -> 20, //Osaka
      "ARN" -> 20, //Stockholm
      "MEX" -> 18, //Mexico City
      "BRU" -> 18, //Brussels
      "DOH" -> 15, //Doha
      "WAW" -> 15, //Warsaw
      "LUX" -> 11, //Luxembourg
      "BOM" -> 10 //Mumbai
    ),
    VACATION_HUB -> Map[String, Int](
      "HNL" -> 80, //Honolulu
      "BKK" -> 70, //Bangkok
      "CDG" -> 65, //Paris
      "MCO" -> 58, //Orlando
      "MLE" -> 55, //Maldives
      "JTR" -> 46, //Santorini
      "PEK" -> 44, //Beijing
      "DPS" -> 43, //Bali
      "NAN" -> 41, //Fiji
      "VCE" -> 40, //Venice
      "ATH" -> 40, //Athens
      "DXB" -> 39, //Dubai
      "MLA" -> 39, //Malta
      "PRG" -> 38, //Prague
      "LPA" -> 38, //Gran Canaria
      "FCO" -> 38, //Rome
      "IST" -> 35, //Istanbul
      "DMK" -> 35, //Bangkok Don Mueang
      "TFS" -> 34, //Tenerife
      "PPT" -> 34, //Pape'ete
      "HER" -> 33, //Crete
      "CNS" -> 33, //Cairns (Australia)
      "PUJ" -> 33, //Punta Cana (Dominican Republic)
      "OKA" -> 32, //Okinawa
      "CUN" -> 32, //Cancun
      "ORY" -> 32, //Paris Orly
      "JED" -> 32, //Jeddah/Mecca
      "GIG" -> 32, //Rio de Janeiro
      "GUM" -> 31, //Guam
      "KIX" -> 30, //Osaka
      "LAS" -> 30, //Vegas
      "MIA" -> 30, //Miami
      "SIN" -> 29, //Singapore
      "HKG" -> 28, //Hong Kong
      "NRT" -> 28, //Tokyo Narita
      "HND" -> 28, //Tokyo Haneda
      "VIE" -> 28, //Vienna
      "BCN" -> 28, //Barcelona
      "HAV" -> 27, //Havana
      "MRS" -> 27, //Marseille
      "MXP" -> 27, //Milan
      "SJD" -> 26, //Los Cabos
      "UTP" -> 26, //Pattaya
      "KOA" -> 25, //Kailua Hawaii
      "NAS" -> 25, //Bahamas (Nassau)
      "DBV" -> 25, //Dubrovnik (Croatia)
      "FLN" -> 25, //Florianopolis (Brazil)
      "CNX" -> 25, //Chiang Mai
      "KEF" -> 24, //Reykjavik
      "BUD" -> 24, //Budapest
      "EZE" -> 23, //Buenos Aires
      "FLL" -> 22, //Fort Lauderdale
      "NCE" -> 22, //Nice
      "PSA" -> 22, //Pisa
      "OSL" -> 22, //Oslo
      "ACA" -> 22, //Acapulco
      "AMS" -> 21, //Amsterdam
      "TLV" -> 21, //Tel Aviv
      "CPH" -> 21, //Copenhagen
      "CAI" -> 20, //Cairo
      "LHR" -> 20, //London Heathrow
      "BOM" -> 20, //Mumbai
      "LAX" -> 20, //Los Angeles
      "YVR" -> 20, //Vancouver
      "IAD" -> 20, //Washington Dulles
      "SJU" -> 20, //San Juan
      "GRU" -> 20, //Sao Paulo
      "BNE" -> 20, //Brisbane
      "AKL" -> 20, //Auckland
      "SFO" -> 20, //San Francisco
      "OOL" -> 20, //Gold Coast (Australia)
      "CMB" -> 20, //Colombo (Sri Lanka)
      "MRU" -> 20, //Mauritius
      "MHD" -> 20, //Mashhad (Iran)
      "CZM" -> 20, //Cozumel
      "KIN" -> 19, //Kingston (Jamaica)
      "BWN" -> 19, //Brunei
      "MFM" -> 19, //Macau
      "CTS" -> 19, //Chitose Sapporo
      "HKT" -> 19, //Phuket
      "PMI" -> 19, //Palma De Mallorca
      "KUL" -> 19, //Kuala Lumpur
      "LIS" -> 19, //Lisbon
      "BAH" -> 19, //Bahrein
      "TUN" -> 19, //Tunis
      "AGP" -> 19, //Málaga
      "OGG" -> 18, //Kahului (Hawaii)
      "SYX" -> 18, //Sanya (China)
      "XIY" -> 18, //Xian (China)
      "PVG" -> 18, //Shanghai
      "PER" -> 18, //Perth (Australia)
      "NAP" -> 18, //Naples
      "KRK" -> 18, //Krakow
      "VRA" -> 18, //Varadero Cuba
      "SXR" -> 18, //Srinagar (India)
      "AYT" -> 18, //Antaya (Turkey)
      "HEL" -> 17, //Helsinki
      "LED" -> 17, //St Petersburg
      "CJU" -> 17, //Jeju
      "HGH" -> 17, //Hangzhou (China)
      "SJO" -> 17, //Costa Rica
      "IGU" -> 17, //Foz Do Iguaçu
      "SCL" -> 17, //Santiago
      "NBO" -> 17, //Nairobi
      "LIH" -> 16, //Lihue (Hawaii)
      "CEB" -> 16, //Cebu
      "HAK" -> 16, //Haikou
      "ITO" -> 15, //Hilo (Hawaii)
      "WAW" -> 15, //Warsaw
      "SAW" -> 15, //Istanbul Sabiha Gökçen
      "KWL" -> 15, //Guilin (China)
      "DRW" -> 14, //Darwin (Australia)
      "STX" -> 14, //Christiansted (US Virgin Islands)
      "RAK" -> 14, //Marrakesh (Morocco)
      "MBJ" -> 13, //Montego Bay (Jamaica)
      "LXA" -> 12, //Lhasa (Tibet/China)
      "MYR" -> 12, //Myrtle Beach
      "KBV" -> 12, //Krabi (Thailand)
      "VFA" -> 12, //Victoria Fall (Zimbabwe)
      "AUA" -> 11, //Aruba
      "BGI" -> 11, //Barbados
      "IBZ" -> 11, //Ibiza
      "TAO" -> 10, //Qingdao (China)
      "NOU" -> 10, //Nouméa
      "YZF" -> 10, //Yellowknife (Canada)
      "CUR" -> 10 //Curacao
    ),
    FINANCIAL_HUB -> Map[String, Int](
      "LHR" -> 80, //London Heathrow
      "JFK" -> 80, //New York
      "HND" -> 75, //Tokyo Haneda
      "HKG" -> 70, //Hong Kong
      "SIN" -> 70, //Singapore
      "FRA" -> 54, //Frankfurt
      "ICN" -> 50, //Seoul Incheon
      "PVG" -> 50, //Shanghai
      "DXB" -> 44, //Dubai
      "ZRH" -> 40, //Zurich
      "BOS" -> 39, //Boston
      "SFO" -> 38, //San Francisco
      "CDG" -> 38, //Paris Charles de Gaulle
      "DFW" -> 37, //Dallas Fort-Worth
      "ORD" -> 35, //Chicago O'Hare
      "DEN" -> 34, //Denver
      "EWR" -> 33, //Newark
      "SHA" -> 33, //Shanghai Hongqiao
      "LGW" -> 32, //London Gatwick
      "CAN" -> 32, //Guangzhou
      "SZX" -> 31, //Shenzhen
      "IST" -> 30, //Istanbul
      "MEX" -> 30, //Mexico City
      "ITM" -> 28, //Osaka Itami
      "CLT" -> 28, //Charlotte
      "ATL" -> 27, //Atlanta
      "ORY" -> 25, //Paris Orly
      "GMP" -> 24, //Seoul Gimpo
      "TSN" -> 23, //Tianjin (China)
      "MUC" -> 23, //Munich
      "SEA" -> 22, //Seattle
      "BHX" -> 22, //Birmingham
      "DEL" -> 22, //New Delhi
      "PHX" -> 22, //Phoenix
      "PUS" -> 22, //Busan
      "MEL" -> 22, //Melbourne
      "KHH" -> 22, //Kaohsiung (Taiwan
      "IAH" -> 21, //Houston
      "YYZ" -> 20, //Toronto
      "FUK" -> 20, //Fukuoka (Japan)
      "MSP" -> 19, //Minneapolis
      "LGA" -> 19, //New York La Guardia
      "CPT" -> 19, //Cape Town
      "DMK" -> 19, //Bangkok Don Mueang
      "CPH" -> 19, //Copenhagen
      "DCA" -> 19, //Washington Ronald Reagan
      "SVO" -> 19, //Moscow Sheremetyevo
      "DME" -> 19, //Moscow Domodedovo
      "NGO" -> 18, //Nagoya (Japan)
      "MAN" -> 18, //Manchester
      "HAM" -> 18, //Hamburg
      "BWI" -> 18, //Baltimore
      "DUS" -> 18, //Dusseldorf
      "CGN" -> 18, //Cologne
      "OTP" -> 18, //Bucharest
      "BSB" -> 18, //Brazilia
      "YUL" -> 18, //Montreal
      "PHL" -> 17, //Philadelphia
      "JED" -> 17, //Jeddah/Mecca
      "MAA" -> 17, //Chennai
      "CKG" -> 17, //Chongqing (China)
      "ARN" -> 17, //Stockholm
      "BER" -> 16, //Berline
      "SCL" -> 16, //Santiago (Chile)
      "FLL" -> 15, //Fort Lauderdale
      "BOG" -> 15, //Bogota
      "RUH" -> 15, //Riyadh
      "CTU" -> 15, //Chengdu
      "WUH" -> 14, //Wuhan
      "NKG" -> 14, //Nanjing
      "CMN" -> 14, //Casablanca
      "DUB" -> 13, //Dublin
      "LOS" -> 13, //Lagos
      "MNL" -> 13, //Manila
      "LIM" -> 12, //Lima
      "ALG" -> 12, //Algeria
      "NBO" -> 11, //Nairobi
      "SGN" -> 11 //Ho Chi Minh City
    ), 
    DOMESTIC_AIRPORT -> Map[String, Int]()
  ) + (GATEWAY_AIRPORT -> getGatewayAirports().map(iata => (iata, 0)).toMap)

  patchFeatures()

  def patchFeatures() = {
    val airportFeatures = scala.collection.mutable.Map[String, ListBuffer[AirportFeature]]()
    featureList.foreach {
      case (featureType, airportMap) =>
        airportMap.foreach {
          case (airportIata, featureStrength) =>
            val featuresForThisAirport = airportFeatures.getOrElseUpdate(airportIata, ListBuffer[AirportFeature]())
            featuresForThisAirport += AirportFeature(featureType, featureStrength)
        }
    }


    airportFeatures.toList.foreach {
        case (iata, features) =>
          AirportSource.loadAirportByIata(iata) match {
            case Some(airport) =>
              AirportSource.updateAirportFeatures(airport.id, features.toList)
            case None =>
              println(s"Cannot find airport with iata $iata to patch $features")
          }
      }
    IsolatedAirportPatcher.patchIsolatedAirports()
  }

  def getGatewayAirports() : List[String] = {
    //The most powerful airport of every country
    val airportsByCountry = AirportSource.loadAllAirports().groupBy(_.countryCode).filter(_._2.length > 0)
    val topAirportByCountry = airportsByCountry.view.mapValues(_.sortBy(_.power).last)

    val baseList = topAirportByCountry.values.map(_.iata).toList

    val list: mutable.ListBuffer[String] = collection.mutable.ListBuffer(baseList:_*)

    list -= "HND" //not haneda, instead it should be NRT

    //now add extra ones for bigger countries
    //from top to bottom by pop coverage, so we wouldnt miss any
    list.appendAll(List(
      "CAN", //China
      "PVG",
      "PEK",
      "JFK", //US
      "LAX",
      "SFO",
      "MIA",
      "BOM", //India
      "GIG", //Brazil
      "GRU",
      "NRT", //Japan
      "KIX",
      "SVO", //Russia
      "LED",
      "FCO", //Italy
      "MXP",
      "FRA", //Germany
      "MUC",
      "SYD", //Australia
      "MEL",
      "YVR", //Canada
      "YYZ"))
    list.toList
  }
}
