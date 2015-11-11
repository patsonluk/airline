package com.patson.model

case class City(name : String, latitude : Double, longitude : Double, countryCode : String, population: Int, power : Long) extends IdObject