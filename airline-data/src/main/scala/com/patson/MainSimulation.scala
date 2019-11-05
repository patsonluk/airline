

package com.patson

import java.util.concurrent.TimeUnit

import akka.actor.Props
import akka.actor.Actor
import com.patson.data._
import com.patson.stream.{CycleCompleted, CycleStart, SimulationEventStream}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration

object MainSimulation extends App {
  val CYCLE_DURATION : Int = 15 * 60
  var currentWeek: Int = 0
//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(CYCLE_DURATION, TimeUnit.SECONDS), actor, Start)
  }

  
  def startCycle(cycle : Int) = {
      val cycleStart = System.currentTimeMillis()
      println("cycle " + cycle + " starting!")
      SimulationEventStream.publish(CycleStart(cycle), None)
      println("Oil simulation")
      OilSimulation.simulate(cycle)
      println("Loading all links")
      val links = LinkSource.loadAllLinks(LinkSource.FULL_LOAD)
      println("Finished loading all links")
      val (linkResult, loungeResult) = LinkSimulation.linkSimulation(cycle, links)
      AirportSimulation.airportSimulation(cycle, linkResult)
      val airplanes = AirplaneSimulation.airplaneSimulation(cycle, links)
      AirlineSimulation.airlineSimulation(cycle, linkResult, loungeResult, airplanes)
      
      //purge log
      LogSource.deleteLogsBeforeCycle(cycle - 100)
      
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

