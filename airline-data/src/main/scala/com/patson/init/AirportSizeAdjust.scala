package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportSizeAdjust {
  //https://en.wikipedia.org/wiki/List_of_busiest_airports_by_passenger_traffic
  val sizeList = Map(
        "ATL" -> 10,
        "PEK" -> 9,
        "DXB" -> 9,
        "HND" -> 9,
        "LAX" -> 9,
        "LHR" -> 9,
        "HKG" -> 9,
        "ORD" -> 9,
        "DFW" -> 9,
        "BKK" -> 9,
        "PVG" -> 9,
        "CDG" -> 9,
        "CAN" -> 9,
        "SIN" -> 9,
        "IST" -> 9,
        "ICN" -> 9,
        "FRA" -> 9,
        "CGK" -> 8,
        "DEN" -> 8,
        "KUL" -> 8,
        "JFK" -> 9,
        "AMS" -> 9,
        "PHX" -> 8,
        "MIA" -> 8,
        "SFO" -> 8,
        "DEL" -> 8,
        "CLT" -> 8,
        "LAS" -> 8,
        "CTU" -> 8,
        "GRU" -> 8,
        "IAH" -> 8,
        "MUC" -> 8,
        "SYD" -> 8,
        "YYZ" -> 8,
        "FCO" -> 8,
        "LGW" -> 7,
        "SHA" -> 7,
        "BCN" -> 7,
        "SEA" -> 7,
        "SZX" -> 7,
        "TPE" -> 7,
        "MCO" -> 7,
        "EWR" -> 7,
        "NRT" -> 7,
        "MSP" -> 7,
        "BOM" -> 7,
        "MEX" -> 7,
        "MNL" -> 7,
        "DME" -> 7,
        "KMG" -> 7,
        "XIY" -> 7,
        "SVO" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_the_United_States
        "DTW" -> 7,
        "PHL" -> 7,
        "FLL" -> 7,
        "BWI" -> 7,
        "DCA" -> 7,
        "SLC" -> 7,
        "MDW" -> 7,
        "IAD" -> 7,
        "SAN" -> 7,
        "HNL" -> 7,
        "TPA" -> 7,
        "PDX" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Germany
        "DUS" -> 7,
        "TXL" -> 7,
        "HAM" -> 7,
        "SXF" -> 7,
        "CGN" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Germany
        "NCE" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Italy
        "MXP" -> 7,
        "VCE" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Spain
        "AGP" -> 7,
        "ALC" -> 7,
        "LPA" -> 7,
        "TFS" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Austria
        "VIE" -> 7,
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
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Mexico
        "CUN" -> 7,
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Russia
        "LED" -> 7,
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
        "JNB" -> 7,
        "CSX" -> 6,
        "KHN" -> 5,
        
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Thailand
        "HKT" -> 5,
        
        //Adjustment on country with 1m pop+ but w/o a level 4 airport
        //https://en.wikipedia.org/wiki/List_of_airports_in_Afghanistan
        "KBL" -> 4,
        "KDH" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Albania
        "TIA" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Angola
        "LAD" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Bangladesh
        "CGP" -> 4,
        "DAC" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Burkina_Faso
        "OUA" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Benin
        "COO" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Bolivia
        "VVI" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Democratic_Republic_of_the_Congo
        "FIH" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Republic_of_the_Congo
        "BZV" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Ivory_Coast
        "ABJ" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Cameroon
        "DLA" -> 4,
        //costa rica
        "SJO" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Guinea
        "CKY" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Guatemala
        "GUA" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Honduras
        "TGU" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Haiti
        "PAP" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Jamaica
        "KIN" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Kyrgyzstan
        "FRU" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_North_Korea
        "FNJ" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Liberia
        "ROB" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Lithuania
        "RIX" -> 4,
        "VNO" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Moldova
        "KIV" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Madagascar
        "TNR" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_the_Republic_of_Macedonia
        "SKP" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mali
        "BKO" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mongolia
        "ULN" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mauritania
        "NKC" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Malawi
        "LLW" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Mozambique
        "MPM" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Niger
        "NIM" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Nicaragua
        "MGA" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Nepal
        "KTM" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Paraguay
        "ASU" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Rwanda
        "KGL" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Sierra_Leone
        "FNA" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Somalia
        "MGQ" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_El_Salvador
        "SAL" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Chad
        "NDJ" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Tajikistan
        "DYU" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Turkmenistan
        "ASB" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Tanzania
        "DAR" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Uganda
        "EBB" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Venezuela
        "CCS" -> 5,
        "MAR" -> 5,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Kosovo
        "PRN" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Yemen
        "SAH" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Zambia
        "LUN" -> 4,
        //https://en.wikipedia.org/wiki/List_of_airports_in_Zimbabwe
        "HRE" -> 4,
       //below manual adjustment
        "KOA" -> 4, //Kailua hawaii
        "MRU" -> 4, //Mauritius
        "IBZ" -> 4, //Ibiza Spain 
        "BFI" -> 3,
        "DRW" -> 5, //Darwin
        "CLO" -> 4,
        "GYE" -> 4,
        "UIO" -> 4,
        "JTR" -> 3, //Santorini
        "YLW" -> 3, //Kelowna
        "MDE" -> 5, //https://en.wikipedia.org/wiki/Jos%C3%A9_Mar%C3%ADa_C%C3%B3rdova_International_Airport around 7 mil passengers
        "BDA" -> 5, // Bermuda
        "DUB" -> 5, // Dublin
        "BHX" -> 5 //https://en.wikipedia.org/wiki/Birmingham_Airport around 13 mil passengers, A380 capable
      )
      
  
    
}