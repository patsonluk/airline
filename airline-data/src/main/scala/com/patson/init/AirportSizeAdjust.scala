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
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Thailand
        "HKT" -> 5,
        
        
       //below manual adjustment
        
        "BFI" -> 3
      )
      
  
    
}