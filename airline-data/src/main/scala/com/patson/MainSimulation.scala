

package com.patson

import java.util.concurrent.TimeUnit

import akka.actor.Props
import akka.actor.Actor
import com.patson.data._
import com.patson.stream.{CycleCompleted, CycleStart, SimulationEventStream}
import com.patson.util.{AirlineCache, AirportCache}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object MainSimulation extends App {
  val CYCLE_DURATION : Int = 30 * 60
  var currentWeek: Int = 0

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(CYCLE_DURATION, TimeUnit.SECONDS), actor, Start)
  }


  def invalidateCaches() = {
    AirlineCache.invalidateAll()
    AirportCache.invalidateAll()
  }

  def startCycle(cycle : Int) = {
      val cycleStartTime = System.currentTimeMillis()
      println("cycle " + cycle + " starting!")
      SimulationEventStream.publish(CycleStart(cycle, cycleStartTime), None)
      invalidateCaches()

      UserSimulation.simulate(cycle)
      println("Oil simulation")
      OilSimulation.simulate(cycle)
      println("Loan simulation")
      LoanInterestRateSimulation.simulate(cycle)
      println("Event simulation")
      EventSimulation.simulate(cycle)

      val (linkResult, loungeResult) = LinkSimulation.linkSimulation(cycle)
      println("Airport simulation")
      AirportSimulation.airportSimulation(cycle, linkResult)
      println("Airplane simulation")
      val airplanes = AirplaneSimulation.airplaneSimulation(cycle)
      println("Airline simulation")
      AirlineSimulation.airlineSimulation(cycle, linkResult, loungeResult, airplanes)
      println("Country simulation")
      CountrySimulation.simulate(cycle)
      println("Airplane model simulation")
      AirplaneModelSimulation.simulate(cycle)
      
      //purge log
      println("Purging logs")
      LogSource.deleteLogsBeforeCycle(cycle - 100)

      //purge history
      println("Purging link history")
      ChangeHistorySource.deleteLinkChangeByCriteria(List(("cycle", "<", cycle - 100)))

      //refresh delegates
      DelegateSource.deleteBusyDelegateByCriteria(List(("available_cycle", "<=", cycle)))
      
      //notify the websockets via EventStream
      println("Publish Cycle Complete message")
      SimulationEventStream.publish(CycleCompleted(cycle), None)
      val cycleEnd = System.currentTimeMillis()
      
      println("cycle " + cycle + " spent " + (cycleEnd - cycleStartTime) / 1000 + " secs")
  }

  def postCycle() = {
    //now update the link capacity if necessary
    LinkSimulation.refreshLinksPostCycle()
  }
  
  
  
 
  
  class MainSimulationActor extends Actor {
    currentWeek = CycleSource.loadCycle()
    def receive = {
      case Start =>
        startCycle(currentWeek)
        currentWeek += 1
        CycleSource.setCycle(currentWeek)
        postCycle()
    }
  }
   
  
  case class Start()

  
}

