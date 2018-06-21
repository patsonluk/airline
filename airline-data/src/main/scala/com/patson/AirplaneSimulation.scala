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
import com.patson.model.airplane._
import scala.collection.mutable.Map
import akka.actor.Actor
import akka.actor.Props
import java.util.concurrent.TimeUnit

object AirplaneSimulation {
  val LIFE_SPAN = 35 * 52 //in unit of weeks, lifespan with best maintenance
  val MAX_DECAY = 200.toDouble / LIFE_SPAN //assume decay to double with worse maintenance
  val MIN_DECAY = 100.toDouble / LIFE_SPAN 
  
  def airplaneSimulation(cycle: Int) : List[Airplane] = {
    println("starting airplane simulation")
    println("loading all airplanes")
    //do decay
    val allAirplanes = AirplaneSource.loadAirplanesWithAssignedLinkByCriteria(List.empty)
    
    println("finished loading all airplanes")
    
    val updatingAirplanes = ListBuffer[Airplane]()
    allAirplanes.groupBy { _._1.owner }.foreach {
      case (owner, airplanes) => {
        AirlineSource.loadAirlineById(owner.id, true) match {
          case Some(airline) =>
            updatingAirplanes ++= decayAirplanesByAirline(airplanes, airline)
          case None => println("airline " + owner.id + " has airplanes but the airline cannot be loaded!")//invalid airline?
        }
      }
    }
    
    AirplaneSource.updateAirplanes(updatingAirplanes.toList)
    println("Finished updating all airplanes")
    
    updatingAirplanes.toList
  }
  
  def computeDepreciationRate(model : Model, decayRate : Double) = {
    val depreciationRate = (model.price * (decayRate / 100)).toInt
    depreciationRate
  }
  
  def decayAirplanesByAirline(airplanesWithAssignedLink : List[(Airplane, Option[Link])], airline : Airline) : List[Airplane] = {
    val updatingAirplanes = ListBuffer[Airplane]()
    val baseDecayRate = MAX_DECAY - (MAX_DECAY - MIN_DECAY) * (airline.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY)
    airplanesWithAssignedLink.foreach { 
      case(airplane, assignedLink) =>
        var decayRate =
          if (assignedLink.isEmpty) { //not assigned to any links, decay slower
            baseDecayRate / 3 
          } else {
            baseDecayRate
          }
        if (decayRate > airplane.condition) {
          decayRate = airplane.condition
        }
        
        val newCondition = airplane.condition - decayRate
        val depreciationRate = computeDepreciationRate(airplane.model, decayRate)
        val newValue = airplane.value - depreciationRate 
        updatingAirplanes.append(airplane.copy(condition = newCondition, depreciationRate = depreciationRate, value = newValue))
    }
    updatingAirplanes.toList
  }
}