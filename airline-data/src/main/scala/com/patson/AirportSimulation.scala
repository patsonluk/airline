package com.patson

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import scala.util.Random
import scala.concurrent.Future
import com.patson.data._
import com.patson.model._
import scala.collection.mutable.Map
import akka.actor.Actor
import akka.actor.Props
import java.util.concurrent.TimeUnit

object AirportSimulation {
  val AWARENESS_DECAY = 0.1
  val AWARENESS_INCREMENT_WITH_LINKS = 0.2
  val AWARENESS_INCREMENT_WITH_HQ = 0.3
  val AWARENESS_INCREMENT_WITH_BASE = 0.1
  val LOYALTY_DECAY = 0.01
  val LOYALTY_AUTO_INCREMENT_WITH_HQ = 0.05
  val LOYALTY_AUTO_INCREMENT_WITH_BASE = 0.02
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ = 30 //how much loyalty will increment to just because of being a HQ
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE = 15 //how much loyalty will increment to just because of being a HQ
  
  def airportSimulation(cycle: Int) = {
    
    //do decay
    val allAirports = AirportSource.loadAllAirports(true)
    
    
    val basesByAirport : Map[Int, List[AirlineBase]] = AirlineSource.loadAirlineBasesByCriteria(List.empty).foldLeft(Map[Int, List[AirlineBase]]()) { (foldMap, airlineBase) =>
      val bases = foldMap.getOrElse(airlineBase.airport.id, List[AirlineBase]())
      foldMap + (airlineBase.airport.id -> (airlineBase :: bases))
    }
    
    //decay awareness and loyalty
    allAirports.foreach { airport =>
      airport.getAirlineAppeals().foreach { 
        case(airline, AirlineAppeal(loyalty, awareness)) =>
          //decay    
          val newLoyalty = if (loyalty - LOYALTY_DECAY <= 0) 0 else loyalty - LOYALTY_DECAY
          val newAwareness = if (awareness - AWARENESS_DECAY <= 0) 0 else awareness - AWARENESS_DECAY
          
          airport.setAirlineLoyalty(airline, newLoyalty)
          airport.setAirlineAwareness(airline, newAwareness)
      }
       //add base on bases
      basesByAirport.get(airport.id).foreach { _.foreach { base =>
          var newAwareness : Double = airport.getAirlineAwareness(base.airline.id)
          var newLoyalty : Double = airport.getAirlineLoyalty(base.airline.id)
          if (base.headquarter) {
            newAwareness += AWARENESS_INCREMENT_WITH_HQ
            if (newLoyalty < LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ) {
              newLoyalty += LOYALTY_AUTO_INCREMENT_WITH_HQ
            }
          } else {
             newAwareness += AWARENESS_INCREMENT_WITH_BASE
             if (newLoyalty < LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE) {
               newLoyalty += LOYALTY_AUTO_INCREMENT_WITH_BASE
             }
          }
          if (newAwareness > AirlineAppeal.MAX_AWARENESS) {
            newAwareness = AirlineAppeal.MAX_AWARENESS
          }
          if (newLoyalty > AirlineAppeal.MAX_LOYALTY) {
            newLoyalty = AirlineAppeal.MAX_LOYALTY
          }
          airport.setAirlineAwareness(base.airline.id, newAwareness)
          airport.setAirlineLoyalty(base.airline.id, newLoyalty)
        }
      }
    }
      
    AirportSource.updateAirlineAppeal(allAirports)
        
    //increment of awareness
    val links = LinkSource.loadAllLinks(true) 
    
    val airportWithLinks = Map[(Int, Int), Set[Link]]() //(airportId, airlineId)
    links.foreach { link =>
      val airlinesFlyiesFromThisAirport = airportWithLinks.getOrElseUpdate((link.from.id, link.airline.id), Set[Link]())
      airlinesFlyiesFromThisAirport.add(link)
      
      val airlinesFlyiesToThisAirport = airportWithLinks.getOrElseUpdate((link.to.id, link.airline.id), Set[Link]())
      airlinesFlyiesToThisAirport.add(link)
    }
    
    val updatingAirports = Map[Int, Airport]()
    //add awareness based on airline with some links to/from an airport
    airportWithLinks.keySet.foreach {
      case(airportId, airlineId) =>
        val airport = updatingAirports.getOrElseUpdate(airportId, AirportSource.loadAirportById(airportId, true).get)
        val existingAwareness = airport.getAirlineAwareness(airlineId)
        val newAwareness = 
          if ((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) >= AirlineAppeal.MAX_AWARENESS) {
            AirlineAppeal.MAX_AWARENESS   
          } else {
            existingAwareness + AWARENESS_INCREMENT_WITH_LINKS
          }
        airport.setAirlineAwareness(airlineId, newAwareness)
    }
    
    AirportSource.updateAirlineAppeal(updatingAirports.values.toList);
  }
}