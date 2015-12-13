

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

object MainSimulation extends App {
  
   
  
//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    import actorSystem.dispatcher
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(1, TimeUnit.MINUTES), actor, Start)
  }
  
  def startCycle(cycle : Int) = {
      AirportSimulation.airportSimulation(cycle) 
      val linkResult : Map[Int, ListBuffer[LinkConsumptionDetails]] = LinkSimulation.linkSimulation(cycle)
      AirlineSimulation.airlineSimulation(linkResult, cycle)
  }
  
  
  
 
  
  class MainSimulationActor extends Actor {
    var currentWeek = CycleSource.loadCycle()
    def receive = {
      case Start =>
        startCycle(currentWeek)
        currentWeek += 1
        CycleSource.setCycle(currentWeek)
    }
  }
  
  case class Start()

  
}

