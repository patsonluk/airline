

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
import scala.collection.immutable.Map
import akka.actor.Actor
import akka.actor.Props
import java.util.concurrent.TimeUnit
import com.patson.stream.SimulationEventStream
import com.patson.stream.CycleCompleted
import com.patson.stream.CycleStart

object MainSimulation extends App {
  val CYCLE_DURATION : Int = 4 * 60
  var currentWeek: Int = 0
//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    import actorSystem.dispatcher
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(CYCLE_DURATION, TimeUnit.SECONDS), actor, Start)
  }

  
  def startCycle(cycle : Int) = {
      val cycleStart = System.currentTimeMillis()
      println("cycle " + cycle + " starting!")
      SimulationEventStream.publish(CycleStart(cycle), None)
      val linkResult : List[LinkConsumptionDetails] = LinkSimulation.linkSimulation(cycle)
      AirportSimulation.airportSimulation(cycle, linkResult)
      val airplanes = AirplaneSimulation.airplaneSimulation(cycle)
      AirlineSimulation.airlineSimulation(cycle, linkResult, airplanes)
      //notify the websockets via EventStream
      SimulationEventStream.publish(CycleCompleted(cycle), None)
      val cycleEnd = System.currentTimeMillis()
      
      println("cycle " + cycle + " spent " + (cycleEnd - cycleStart) / 1000 + " secs")
  }
  
  
  
 
  
  class MainSimulationActor extends Actor {
    currentWeek = CycleSource.loadCycle()
    def receive = {
      case Start =>
        startCycle(currentWeek)
        currentWeek += 1
        CycleSource.setCycle(currentWeek)
    }
  }
   
  
  case class Start()

  
}

