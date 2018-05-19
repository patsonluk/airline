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
  val AWARENESS_INCREMENT_MAX_WITH_HQ = 50 //how much awareness will increment to just because of being a HQ
  val AWARENESS_INCREMENT_MAX_WITH_BASE = 30 //how much awareness will increment to just because of being a HQ
  val LOYALTY_DECAY = 0.01
  val LOYALTY_AUTO_INCREMENT_WITH_HQ = 0.05
  val LOYALTY_AUTO_INCREMENT_WITH_BASE = 0.02
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ = 30 //how much loyalty will increment to just because of being a HQ
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE = 15 //how much loyalty will increment to just because of being a HQ
  
  private val MAX_LOYALTY_ADJUSTMENT = 0.5
  
  
  def airportSimulation(cycle: Int, linkConsumptions : List[LinkConsumptionDetails]) = {
    println("starting airport simulation")
    println("loading all airports")
    //do decay
    val allAirports = AirportSource.loadAllAirports(true)
    val allAirportsMap = allAirports.map( airport => airport.id -> airport).toMap
    println("finished loading all airports")
    
    
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
      airport.getAirlineBases().values.foreach { base =>
        var newAwareness : Double = airport.getAirlineAwareness(base.airline.id)
        var newLoyalty : Double = airport.getAirlineLoyalty(base.airline.id)
        if (base.headquarter) {
          if (newAwareness < AWARENESS_INCREMENT_MAX_WITH_HQ) {
            newAwareness += AWARENESS_INCREMENT_WITH_HQ  
          }
          if (newLoyalty < LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ) {
            newLoyalty += LOYALTY_AUTO_INCREMENT_WITH_HQ
          }
        } else {
          if (newAwareness < AWARENESS_INCREMENT_MAX_WITH_BASE) {
            newAwareness += AWARENESS_INCREMENT_WITH_BASE  
          }
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
      
    //AirportSource.updateAirlineAppeal(allAirports)
        
    //increment of awareness
    val links = LinkSource.loadAllLinks() 
    
    val airportWithLinks = Map[(Int, Int), Set[Link]]() //(airportId, airlineId)
    links.foreach { link =>
      val airlinesFlyiesFromThisAirport = airportWithLinks.getOrElseUpdate((link.from.id, link.airline.id), Set[Link]())
      airlinesFlyiesFromThisAirport.add(link)
      
      val airlinesFlyiesToThisAirport = airportWithLinks.getOrElseUpdate((link.to.id, link.airline.id), Set[Link]())
      airlinesFlyiesToThisAirport.add(link)
    }
    
    //add awareness based on airline with some links to/from an airport
    airportWithLinks.keySet.foreach {
      case(airportId, airlineId) =>
        val airport = allAirportsMap(airportId)
        val existingAwareness = airport.getAirlineAwareness(airlineId)
        val newAwareness = 
          if ((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) >= AirlineAppeal.MAX_AWARENESS) {
            AirlineAppeal.MAX_AWARENESS   
          } else {
            existingAwareness + AWARENESS_INCREMENT_WITH_LINKS
          }
        airport.setAirlineAwareness(airlineId, newAwareness)
    }
    
    
    //update the loyalty on airports based on link consumption
    println("start updating loyalty")
    val toAirportSoldLinks = linkConsumptions.groupBy { _.toAirportId } //Map[(airportId, airlineId), links] //cannot use Airport instance directly as they are not the same instance 
    val fromAirportSoldLinks = linkConsumptions.groupBy { _.fromAirportId }
    val airportSoldLinks: scala.collection.immutable.Map[Int, Seq[LinkConsumptionDetails]] = 
      (toAirportSoldLinks.toSeq ++ fromAirportSoldLinks.toSeq).groupBy(_._1).mapValues { linkConsumptions =>
        linkConsumptions.map { 
          case (_, linkConsumptionsByDirection) => linkConsumptionsByDirection
        }.flatten
    }
    
    airportSoldLinks.foreach {
      case (airportId, soldLinks) =>
        updateAirportBySoldLinks(allAirportsMap(airportId), soldLinks)
    }
    
    AirportSource.updateAirlineAppeal(allAirports)
    
    airportProjectSimulation(allAirports)
  }
  
  def airportProjectSimulation(allAirports : List[Airport]) = {
    import ProjectStatus._
    println("simulating airport projects")
    
    val inProgressProjects = AirportSource.loadAllAirportProjects().filter { _.status != COMPLETED }
    
    
    
  }
  
  def updateAirportBySoldLinks(airport : Airport, soldLinks : Seq[LinkConsumptionDetails]) = {
    soldLinks.groupBy { _.airlineId }.foreach {
      case(airlineId, soldLinksByAirline) => {
        val totalTransportedPassengers = soldLinksByAirline.map { _.soldSeats.total }.sum 
        val totalQualityProduct = soldLinksByAirline.map { soldLink => soldLink.soldSeats.total.toLong * soldLink.quality }.sum
        val averageQuality = if (totalTransportedPassengers == 0) 0 else totalQualityProduct / totalTransportedPassengers
        
        var loyaltyAdjustment = totalTransportedPassengers * 1000.toDouble / airport.population  
        if (loyaltyAdjustment > MAX_LOYALTY_ADJUSTMENT) {
          loyaltyAdjustment = MAX_LOYALTY_ADJUSTMENT
        }
        val existingLoyalty = airport.getAirlineLoyalty(airlineId)
        if (existingLoyalty < averageQuality) {
            if (existingLoyalty + loyaltyAdjustment >= averageQuality) {
              airport.setAirlineLoyalty(airlineId, averageQuality)
            } else {
              airport.setAirlineLoyalty(airlineId, existingLoyalty + loyaltyAdjustment)
            }
        } else {
          if (existingLoyalty - loyaltyAdjustment <= averageQuality) {
            airport.setAirlineLoyalty(airlineId, averageQuality)
          } else {
            airport.setAirlineLoyalty(airlineId, existingLoyalty - loyaltyAdjustment)
          }
        }
        //println("airport " + airport.name + " airline " + airlineId + " loyalty updating from " + existingLoyalty + " to " + airport.getAirlineLoyalty(airlineId))
      }
    }
  }
}