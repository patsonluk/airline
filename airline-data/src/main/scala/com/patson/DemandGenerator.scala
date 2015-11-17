package com.patson

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import com.patson.data.AirportSource
import com.patson.model.Airport
import com.patson.model.AppealPreference
import com.patson.model.FlightPreference
import com.patson.model.FlightPreferencePool
import com.patson.model.FlightPreferencePool
import com.patson.model.PassengerGroup
import com.patson.model.PassengerGroup
import com.patson.model.SimplePreference

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source


object DemandGenerator extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()

  def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, totalWorldPower : Long) = {
    if (fromAirport == toAirport) {
      0
    } else {
      val passengerSupplyPerWeek =  (fromAirport.power / 30000 / 52).toInt //assuming 1 flight per year if PPP is 30k
      (passengerSupplyPerWeek * toAirport.power / totalWorldPower).toInt //TODO only proportional to target airport power for now
    }
  }
  
  mainFlow
  
  def mainFlow() = {
    Await.ready(computeDemand(), Duration.Inf)
    
    actorSystem.shutdown()
  }
  
  def computeDemand() = {
    println("Loading airports")
    //val allAirports = AirportSource.loadAllAirports(true)
    val airports = AirportSource.loadAllAirports(true).filter { _.size >= 2 }
    println("Loaded " + airports.size + " airports")
    val totalWorldPower = airports.foldRight(0L)( _.power + _)
    
	  val airportSource = Source(airports)
	  
	   
	  
	  val computeFlow: Flow[Airport, (Airport, List[(Airport, Int)])] = Flow[Airport].filter { _.power > 0 }.mapAsync { 
	    fromAirport : Airport => {
	      Future {
	        val demandList = ListBuffer[(Airport, Int)]() 
	        airports.foreach { toAirport => 
	          val demand = computeDemandBetweenAirports(fromAirport, toAirport, totalWorldPower)
	          if (demand > 0) {
	            demandList.append((toAirport, demand))
	          } 
	        } 
	        
	        (fromAirport, demandList.toList)
	      }
	    }
	  }
	  
	  val demandChunkSize = 5
	  val toPassengerGroupFlow: Flow[(Airport, List[(Airport, Int)]), List[(PassengerGroup, Airport, Int)]] = Flow[(Airport, List[(Airport, Int)])].map {
	    case (fromAirport, toAirportsWithDemand) =>
	      val passangerGroupDemand = ListBuffer[(PassengerGroup, Airport, Int)]() 
        //for each city generate different preferences
        val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

        val demandListFromThisAiport = toAirportsWithDemand.foreach {
          case (toAirport, demand) =>
            val splitStart = System.nanoTime()
            var remainingDemand = demand
            while (remainingDemand > demandChunkSize) {
              passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw), toAirport, demandChunkSize))
              remainingDemand -= demandChunkSize
            }
            passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw), toAirport, remainingDemand)) // don't forget the last chunk
            val splitEnd = System.nanoTime()
//            println("Split took " + (splitEnd - splitStart))
        }
	      passangerGroupDemand.toList
	  }
	  
	  
    //val resultSink = Sink.foreach { demandInfo : (Airport, Map[Airport, Int]) => println() }
    var counter = 0
    val progressChunk = airports.length / 100
    
//    val resultSink = Sink.fold(List[(PassengerGroup, Airport, Int)]()) {
//      (holder, demandInfo : List[(PassengerGroup, Airport, Int)]) =>
//        val resultFoldStart = System.nanoTime()
//        val newHolder = holder ++ demandInfo 
//        val resultFoldEnd = System.nanoTime()
//        println("Result fold took " + (resultFoldEnd - resultFoldStart))
//        newHolder
//    }
    val resultSink = Sink.fold(ListBuffer[(PassengerGroup, Airport, Int)]()) {
      (holder, demandInfo : List[(PassengerGroup, Airport, Int)]) =>
        counter += 1
        if (progressChunk == 0 || counter % progressChunk == 0) {
          print(".")
        }
        holder.appendAll(demandInfo) 
        holder
    }
    
    val completeFlow = airportSource.via(computeFlow).via(toPassengerGroupFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink).map(_.toList)
  }
  
  def getFlightPreferencePoolOnAirport(fromAirport : Airport) : FlightPreferencePool = {
    //for now 5 simple preferences per airport
    val simplePreferenceCount = 5;

    //TODO
    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    for (i <- 0 until simplePreferenceCount) { 
      flightPreferences.append((SimplePreference(i, simplePreferenceCount - 1), 1))
    }
        
//    fromAirport.airlineLoyalties.foreach {
//      case(airline, strength) => flightPreferences.append((LoyaltyPreference(airline, strength), strength * 10))    
//    }
    
    //for now 100 loyalty preferences per airport
    val loyaltyPreferenceCount = 100;
    for (i <- 0 until loyaltyPreferenceCount) {
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.airlineAppeals.toMap), 1))
    }
    
    
    new FlightPreferencePool(flightPreferences.toList)
  }
}