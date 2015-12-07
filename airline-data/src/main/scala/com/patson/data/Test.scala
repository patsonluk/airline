package com.patson.data

import com.patson.init.GeoDataGenerator
import com.patson.Authentication
import java.util.Calendar
import com.patson.model._


object Test extends App {
  val airport = Airport("", "", "Airport", 0, 0, "", "", 5, 0, 0, slots = 100)
  println(airport.slotFee)
  airport.size = 5
  println(airport.slotFee)
}

case class B(value1 : Int, var value2 : Int) {
  var value3 : Int = 0
}
