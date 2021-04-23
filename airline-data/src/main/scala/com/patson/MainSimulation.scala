

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
      if (cycle == 0) { //initialize it
        OilSimulation.simulate(0)
        LoanInterestRateSimulation.simulate(0)
      }

      SimulationEventStream.publish(CycleStart(cycle, cycleStartTime), None)
      invalidateCaches()

      UserSimulation.simulate(cycle)
      println("Event simulation")
      EventSimulation.simulate(cycle)

      val (flightLinkResult, loungeResult, linkRidershipDetails) = LinkSimulation.linkSimulation(cycle)
      println("Airport simulation")
      AirportSimulation.airportSimulation(cycle, flightLinkResult, linkRidershipDetails)
      println("Airplane simulation")
      val airplanes = AirplaneSimulation.airplaneSimulation(cycle)
      println("Airline simulation")
      AirlineSimulation.airlineSimulation(cycle, flightLinkResult, loungeResult, airplanes)
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

      val cycleEnd = System.currentTimeMillis()
      
      println("cycle " + cycle + " spent " + (cycleEnd - cycleStartTime) / 1000 + " secs")
      cycleEnd
  }

  def postCycle(currentCycle : Int) = {
    println("Oil simulation")
    OilSimulation.simulate(currentCycle) //simulate at the beginning of a new cycle
    println("Loan simulation")
    LoanInterestRateSimulation.simulate(currentCycle) //simulate at the beginning of a new cycle
    //refresh delegates
    println("Delegate simulation")
    DelegateSimulation.simulate(currentCycle)

    println("Post cycle link simulation")
    LinkSimulation.simulatePostCycle(currentCycle)

    println(s"Post cycle done $currentCycle")
  }


  /**
    * The simulation can be seen like this:
    * On week(cycle) n. It starts the long simulation (pax simulation) at the "END of the week"
    * when it finishes computing the pax of the past week. It sets the current week to next week (which indicates a beginning of week n + 1)
    * It then runs some postCycle task (these tasks should be short and can be regarded as things to do at the Beginning of a week)
    *
    */
  class MainSimulationActor extends Actor {
    currentWeek = CycleSource.loadCycle()
    def receive = {
      case Start =>
        status = SimulationStatus.IN_PROGRESS
        val endTime = startCycle(currentWeek)

        currentWeek += 1
        CycleSource.setCycle(currentWeek)
        status = SimulationStatus.WAITING_CYCLE_START
        postCycle(currentWeek) //post cycle do some quick updates, no long simulation

        //notify the websockets via EventStream
        println("Publish Cycle Complete message")
        SimulationEventStream.publish(CycleCompleted(currentWeek - 1, endTime), None)
    }
  }
   
  
  case class Start()

  var status : SimulationStatus.Value = SimulationStatus.WAITING_CYCLE_START
  object SimulationStatus extends Enumeration {
    type DelegateTaskType = Value
    val IN_PROGRESS, WAITING_CYCLE_START = Value
  }

  
}

