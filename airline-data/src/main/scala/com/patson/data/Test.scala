package com.patson.data

import com.patson.init.GeoDataGenerator
import com.patson.Authentication
import java.util.Calendar
import com.patson.model._
import com.patson.LinkSimulation
import com.patson.model.airplane._
import com.patson.init.AirportFeaturePatcher
import com.patson.init.AirportLinkPatcher
import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer
import com.patson.RouteFinder


object Test extends App {
  //GeoDataGenerator.buildCountryData(AirportSource.loadAllAirports(false))
//  val bases = AirlineSource.loadAirlineBasesByCriteria(List.empty)
//  bases.foreach { base =>
//    AirlineSource.saveAirlineBase(base.copy(countryCode = base.airport.countryCode))  
//  }
  val link = Link.fromId(1)
  link.availableSeats = LinkClassValues.getInstance(10, 20, 35)
//  println(link.availableSeatsAtOrBelowClass(FIRST))
//  println(link.availableSeatsAtOrBelowClass(BUSINESS))
//  println(link.availableSeatsAtOrBelowClass(ECONOMY))
}




