package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportSizeAdjust {
  //https://en.wikipedia.org/wiki/List_of_busiest_airports_by_passenger_traffic
  //orderd per 2022 stats
  val sizeList = Map(
        "ATL" -> 10,
        "DFW" -> 10,
        "DEN" -> 10,
        "ORD" -> 10,
        "DXB" -> 10,
        "LAX" -> 9,
        "IST" -> 9,
        "LHR" -> 9,
        "DEL" -> 9,
        "CDG" -> 9,
        "JFK" -> 9,
        "LAS" -> 9,
        "AMS" -> 9,
        "MIA" -> 9,
        "MAD" -> 9, //top 15
        "PEK" -> 9, //bonus addition
        "HND" -> 8,
        "MCO" -> 8,
        "FRA" -> 8,
        "CLT" -> 8,
        "MEX" -> 8,
        "PHX" -> 8,
        "SFO" -> 8,
        "BCN" -> 8,
        "CGK" -> 8,
        "YYZ" -> 8, //top 25, minus some USA airports 
        "DOH" -> 8, 
        "BKK" -> 8,
        "SIN" -> 8,
        "ICN" -> 8,
        "KUL" -> 8,
        "GRU" -> 8,
        "MUC" -> 8,
        "SYD" -> 8,
        "FCO" -> 8,
        "HKG" -> 7,
        "LGW" -> 7,
        "TPE" -> 7,
        "NRT" -> 7,
        "BOM" -> 7,
        "MNL" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_the_United_States
        "IAH" -> 7,
        "SEA" -> 7,
        "DTW" -> 7,
        "EWR" -> 7,        
        "PHL" -> 7,
        "FLL" -> 7,
        "BWI" -> 7,
        "DCA" -> 7,
        "SLC" -> 7,
        "IAD" -> 7,
        "SAN" -> 7,
        "HNL" -> 7,
        "TPA" -> 7,
        "PDX" -> 7,
        "BOS" -> 7,
        "MSP" -> 7,
        //metro NYC
        "LGA" -> 6,
        "SWF" -> 3,
        "ACY" -> 2,
        "HPN" -> 3,
        "BDL" -> 4,
        //metro LA
        "BUR" -> 4,
        "SNA" -> 4,
        "ONT" -> 3,
        "LGB" -> 3,        
        "SBD" -> 3,
        "PMD" -> 2,
        //metro Chicago
        "MDW" -> 7,
        "RFD" -> 2,
        "MSN" -> 5,
        //metro SF
        "STS" -> 3,
        "SMF" -> 6,
        "MHR" -> 2,
        //upgrade established city airports
        "SAT" -> 6,
        "OKC" -> 6,
        "ABQ" -> 6,
        "MKE" -> 6,
        "KOA" -> 5, //Kailua hawaii
        "CHS" -> 5,
        //downgrade secondary USA airports
        "LCK" -> 3,
        "BLV" -> 3,
        "GRK" -> 3,
        "PIA" -> 3,
        "FWA" -> 4,
        "BFI" -> 3,
        "GGG" -> 2,
        "OGD" -> 2,
        "USA" -> 2,
        "MEI" -> 2,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Europe
        "SVO" -> 7,
        "LIS" -> 7,
        "DUB" -> 7,
        "ORY" -> 7,
        "VIE" -> 7,
        "ZRH" -> 7,
        "ATH" -> 7,
        "MAN" -> 7,
        "CPH" -> 7,
        "MXP" -> 7,
        "OSL" -> 7,
        "BER" -> 7,
        "AGP" -> 7,
        "BRU" -> 7,
        "DME" -> 7,
        "ARN" -> 7,
        "LED" -> 7,
        "DUS" -> 7,
        "WAW" -> 7,
        "ALC" -> 7,
        "HEL" -> 7,
        "BUD" -> 7,
        "NCE" -> 7,
        "OTP" -> 7,
        "EDI" -> 7,
        "HAM" -> 7,
        //keeping islands small so they don't spill over other islands
        "LPA" -> 5,
        "TFS" -> 5,
        "PMI" -> 5,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_India
        "BLR" -> 7,
        "MAA" -> 7,
        "CCU" -> 7,
        "HYD" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Japan
        "KIX" -> 7,
        "FUK" -> 7,
        "CTS" -> 7,
        "OKA" -> 7,
        "ITM" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_South_Korea
        "CJU" -> 7,
        "PUS" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Brazil
        "BSB" -> 7,
        "GIG" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_China
        "SZX" -> 8,
        "PVG" -> 8,
        "CAN" -> 8,
        "SHA" -> 7,
        "CKG" -> 7,
        "CTU" -> 7,
        "KMG" -> 7,
        "HGH" -> 7,
        "TFU" -> 7,
        "XIY" -> 7,
        "CSX" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Mexico
        "CUN" -> 7,
        "GDL" -> 7,
        "TIJ" -> 6,
        //downgrade
        "PBC" -> 4,
        "TLC" -> 4,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Turkey
        "SAW" -> 7,
        "AYT" -> 7,
        "ESB" -> 7,
        "ADB" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Canada
        "YVR" -> 7,
        "YUL" -> 7,
        "YYC" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Africa
        "JNB" -> 8,
        "CPT" -> 7,
        "CAI" -> 7,
        "CMN" -> 7,
        "LOS" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Thailand
        //https://en.wikipedia.org/wiki/List_of_airports_in_Albania
        //algeria downgrades
        "TMR" -> 3,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Angola
        //https://en.wikipedia.org/wiki/List_of_airports_in_Bangladesh
        //https://en.wikipedia.org/wiki/List_of_airports_in_Burkina_Faso
        //https://en.wikipedia.org/wiki/List_of_airports_in_Benin
        //https://en.wikipedia.org/wiki/List_of_airports_in_Bolivia
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Democratic_Republic_of_the_Congo
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Republic_of_the_Congo
        //https://en.wikipedia.org/wiki/List_of_airports_in_Ivory_Coast
        //https://en.wikipedia.org/wiki/List_of_airports_in_Cameroon
        //cote d'ivoire
        "BYK" -> 4,
        //costa-rico
        "BYK" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Guinea
        //https://en.wikipedia.org/wiki/List_of_airports_in_Guatemala
        //https://en.wikipedia.org/wiki/List_of_airports_in_Honduras
        //https://en.wikipedia.org/wiki/List_of_airports_in_Haiti
        //https://en.wikipedia.org/wiki/List_of_airports_in_Jamaica
        //https://en.wikipedia.org/wiki/List_of_airports_in_Kyrgyzstan
        //https://en.wikipedia.org/wiki/List_of_airports_in_North_Korea
        "FNJ" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Liberia
        //https://en.wikipedia.org/wiki/List_of_airports_in_Lithuania
        //Libya
        "GHT" -> 2,
        "AKF" -> 2,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Moldova
        //https://en.wikipedia.org/wiki/List_of_airports_in_Madagascar
        "ERS" -> 2,
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Republic_of_Macedonia
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mali
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mongolia
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mauritania
        //https://en.wikipedia.org/wiki/List_of_airports_in_Malawi
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mozambique
        //https://en.wikipedia.org/wiki/List_of_airports_in_Niger
        //https://en.wikipedia.org/wiki/List_of_airports_in_Nicaragua
        //https://en.wikipedia.org/wiki/List_of_airports_in_Nepal
        //https://en.wikipedia.org/wiki/List_of_airports_in_Paraguay
        //https://en.wikipedia.org/wiki/List_of_airports_in_Rwanda
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Russia
        "PYJ" -> 3,
        "DYR" -> 3,
        "NER" -> 3,
        "PKC" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Sierra_Leone
        //https://en.wikipedia.org/wiki/List_of_airports_in_Somalia
        //https://en.wikipedia.org/wiki/List_of_airports_in_El_Salvador
        //https://en.wikipedia.org/wiki/List_of_airports_in_Chad
        //https://en.wikipedia.org/wiki/List_of_airports_in_Tajikistan
        //https://en.wikipedia.org/wiki/List_of_airports_in_Turkmenistan
        //https://en.wikipedia.org/wiki/List_of_airports_in_Tanzania
        "MWZ" -> 3,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Uganda
        //https://en.wikipedia.org/wiki/List_of_airports_in_Venezuela
        //https://en.wikipedia.org/wiki/List_of_airports_in_Kosovo
        //https://en.wikipedia.org/wiki/List_of_airports_in_Yemen
        //https://en.wikipedia.org/wiki/List_of_airports_in_Zambia
        //https://en.wikipedia.org/wiki/List_of_airports_in_Zimbabwe
        
        // Force AU airports to have large range
        "MEL" -> 8,
        "PER" -> 7,
        "BNE" -> 7,
        // "ADL" -> 7,
        // "CNS" -> 7,
        "OOL" -> 4,
        "AKL" -> 7,
        "ZQN" -> 3,
        "WLG" -> 4,
       //below manual adjustment
        "MRU" -> 4, //Mauritius
        "YLW" -> 3, //Kelowna
        "MDE" -> 5, //https://en.wikipedia.org/wiki/Jos%C3%A9_Mar%C3%ADa_C%C3%B3rdova_International_Airport around 7 mil passengers
        "BDA" -> 5, // Bermuda
        "BHX" -> 5 //https://en.wikipedia.org/wiki/Birmingham_Airport around 13 mil passengers, A380 capable
      )
      
  
    
}