package com.patson.data

import com.patson.init.GeoDataGenerator
import com.patson.Authentication
import java.util.Calendar
import com.patson.model._


object Test extends App {
  println(B(1,1) == B(1,1))
  println(B(1,1) == B(1,2))
  val b1 = B(1,1)
  val b2 = b1.copy()
  println(b1 == b2)
  b2.value3 = 1
  println(b1 == b2)
  b2.value2 = 3
  println(b1 == b2)
}

case class B(value1 : Int, var value2 : Int) {
  var value3 : Int = 0
}
