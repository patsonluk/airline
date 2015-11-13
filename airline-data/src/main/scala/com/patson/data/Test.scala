package com.patson.data

import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.data.airplane.ModelSource

object Test extends App {
  AirlineSource.loadAirlinesByCriteria(List.empty, true).foreach {
    airline => 
    println(airline.airlineInfo.balance) 
  }
    
}