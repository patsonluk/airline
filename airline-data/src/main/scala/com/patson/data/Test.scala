package com.patson.data

import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.data.airplane.ModelSource
import com.patson.init.GeoDataGenerator
import scala.concurrent._
import scala.concurrent.duration.Duration
import com.patson.model.Runway
import scala.collection.mutable.Map

object Test extends App {
//  println(Math.log(1000 / 1000) / Math.log(1.5))
//  println(Math.log(5000 / 1000) / Math.log(1.5))
//  println(Math.log(10000 / 1000) / Math.log(1.5))
//  println(Math.log(82763.3576508157 / 1000) / Math.log(1.5))
   val runways = Await.result(GeoDataGenerator.getRunway(), Duration.Inf)
   val airportUpgrades = Map[String, Int]()
   runways.foreach { 
     case (icao, runways) =>
       var longRunway = 0 
       var veryLongRunway = 0
       var megaRunway = 0
       runways.foreach { runway =>
         if (runway.length >= 10000) {
           megaRunway += 1
         } else if (runway.length >= 9000) {
           veryLongRunway += 1
         } else if (runway.length >= 7000) {
           longRunway += 1
         }
       }
       if (megaRunway > 0) {
         airportUpgrades.put(icao, 5 + megaRunway) //at least size 6
       } else if (veryLongRunway > 0) {
         airportUpgrades.put(icao, 5) //size 5
       } else if (longRunway > 3) {
         airportUpgrades.put(icao, 4) //size 4
       }
   }
   
   println(airportUpgrades)
}