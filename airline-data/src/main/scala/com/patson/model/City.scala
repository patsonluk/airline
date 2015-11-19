package com.patson.model

case class City(name : String, latitude : Double, longitude : Double, countryCode : String, population: Int, income : Int, var id : Int = 0) extends IdObject