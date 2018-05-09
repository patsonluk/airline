package com.patson.init

import com.patson.model._
import com.patson.data._
import com.patson.Util
import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map

object AirportLinkPatcher {
  private[this] val AIRPORT_LINK_RANGE = 150
  def patchAirportLinks() = {
    val localAuthority = AirlineSource.loadAirlineById(1) match {
      case Some(airline) => airline
      case None =>
        val newAirline = Airline("Local Authority")
        AirlineSource.saveAirlines(List(newAirline))
        newAirline
    }
    
    val allAirports = AirportSource.loadAllAirports(true)
    val airportById = allAirports.map { airport => (airport.id, airport) }.toMap
    
    val airportLinkIds = Map[(Int, Int), Int]() //pair of airport, smaller id always go first, this is to avoid double links
    airportById.foreach { 
      case (id, airport) =>
        if (airport.size >= 3) { //scan surrounding 150 km for other airport
          val boundaryLongitude = GeoDataGenerator.calculateLongitudeBoundary(airport.latitude, airport.longitude, AIRPORT_LINK_RANGE)
          for (targetAirport <- allAirports) {
            if (targetAirport.id != id && 
                targetAirport.size > 0 &&
                targetAirport.longitude >= boundaryLongitude._1 && targetAirport.longitude <= boundaryLongitude._2) {
              val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude)
              if (AIRPORT_LINK_RANGE >= distance) {
                  //println(city.name + " => " + airport.name)
                 airportLinkIds += (if (id > targetAirport.id) { (id, targetAirport.id) -> distance.toInt } else { (targetAirport.id, id) -> distance.toInt }) 
              }
            }
          }
        }
    }
    
    val airportLinks = airportLinkIds.map {
      case ((fromId, toId), distance) =>
        val fromAirport = airportById(fromId)
        val toAirport = airportById(toId)
        val minSize = Math.min(fromAirport.size, toAirport.size)
        val capacity = minSize * minSize * 100 
        val price = LinkClassValues.getInstance((10 * ECONOMY.priceMultiplier).toInt, (10 * BUSINESS.priceMultiplier).toInt, (10 * FIRST.priceMultiplier).toInt)
        
        Link(fromAirport, toAirport, airline = localAuthority, price = price, distance = distance, capacity = LinkClassValues.getInstance(capacity, capacity, capacity), rawQuality = 50, duration = distance / 60, frequency = 100) 
    }
    
    println(airportLinks.size)
    LinkSource.saveLinks(airportLinks.toList)
    
    //AirportSource.updateAirportFeatures(updatingAirports.toList)
  }
}  
  