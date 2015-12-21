package com.patson

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.patson.data.AirportSource
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.patson.model._



object DemandGenerator extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()
  private val FIRST_CLASS_INCOME_MIN = 50000
  private val FIRST_CLASS_INCOME_MAX = 200000
  private val FIRST_CLASS_PERCENTAGE_MAX = 0.05
  private val BUSINESS_CLASS_INCOME_MIN = 20000
  private val BUSINESS_CLASS_INCOME_MAX = 100000
  private val BUSINESS_CLASS_PERCENTAGE_MAX = 0.15
  
  mainFlow
  
  def mainFlow() = {
    Await.ready(computeDemand(), Duration.Inf)
    
    actorSystem.shutdown()
  }
  
  def computeDemand() = {
    println("Loading airports")
    //val allAirports = AirportSource.loadAllAirports(true)
    val airports = AirportSource.loadAllAirports(true).filter { _.iata != ""  }
    println("Loaded " + airports.size + " airports")
    val totalWorldPower = airports.foldRight(0L)( _.power + _)
    
	  val airportSource = Source(airports)
	  
	  val computeFlow: Flow[Airport, (Airport, List[(Airport, FlightDemand)])] = Flow[Airport].filter { _.power > 0 }.mapAsync { 
	    fromAirport : Airport => {
	      Future {
	        val demandList = ListBuffer[(Airport, FlightDemand)]() 
	        airports.foreach { toAirport => 
	          val demand = computeDemandBetweenAirports(fromAirport, toAirport, totalWorldPower)
	          
	          if (demand.hasDemand) {
	            demandList.append((toAirport, demand))
	          } 
	        } 
	        
	        (fromAirport, demandList.toList)
	      }
	    }
	  }
	  
	  val demandChunkSize = 5
	  val toPassengerGroupFlow: Flow[(Airport, List[(Airport, FlightDemand)]), List[(PassengerGroup, Airport, Int)]] = Flow[(Airport, List[(Airport, FlightDemand)])].map {
	    case (fromAirport, toAirportsWithDemand) =>
	      val passangerGroupDemand = ListBuffer[(PassengerGroup, Airport, Int)]() 
        //for each city generate different preferences
        val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

        val demandListFromThisAiport = toAirportsWithDemand.foreach {
          case (toAirport, demand) =>
            LinkClass.values().foreach { linkClass =>
              if (demand(linkClass) > 0) {
                var remainingDemand = demand(linkClass)
                while (remainingDemand > demandChunkSize) {
                  passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass)), toAirport, demandChunkSize))
                  remainingDemand -= demandChunkSize
                }
                passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass)), toAirport, remainingDemand)) // don't forget the last chunk}
              }
            }
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
  
  private[patson] def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, totalWorldPower : Long) : FlightDemand = {
    if (fromAirport == toAirport) {
      new FlightDemand(0, 0, 0)
    } else {
      val passengerSupplyPerWeek =  (fromAirport.power / 30000 / 52).toInt //assuming 1 flight per year if income per capita is 30k
      import FlightType._
      val flightType = Computation.getFlightType(fromAirport, toAirport)
      val multiplier = flightType match {
        case SHORT_HAUL_DOMESTIC => 4
        case LONG_HAUL_DOMESTIC => 2
        case SHORT_HAUL_INTERNATIONAL => 1.5
        case LONG_HAUL_INTERNATIONAL => 1
        case ULTRA_LONG_HAUL_INTERNATIONAL => 0.5
      }
      val totalDemand = (passengerSupplyPerWeek * multiplier * toAirport.power / totalWorldPower).toInt
      
      //compute demand composition
      val totalIncome = fromAirport.income + toAirport.income

      val firstClassPercentage = 
        if (flightType == LONG_HAUL_INTERNATIONAL || flightType == ULTRA_LONG_HAUL_INTERNATIONAL) {
          if (totalIncome <= FIRST_CLASS_INCOME_MIN) {
            0 
          } else if (totalIncome >= FIRST_CLASS_INCOME_MAX) {
            FIRST_CLASS_PERCENTAGE_MAX 
          } else { 
            FIRST_CLASS_PERCENTAGE_MAX * (totalIncome - FIRST_CLASS_INCOME_MIN) / (FIRST_CLASS_INCOME_MAX - FIRST_CLASS_INCOME_MIN)
          }
        } else {
         0 
        }
      val businessClassPercentage =
        if (flightType != SHORT_HAUL_DOMESTIC) {
          if (totalIncome <= BUSINESS_CLASS_INCOME_MIN) {
            0 
          } else if (totalIncome >= BUSINESS_CLASS_INCOME_MAX) {
            BUSINESS_CLASS_PERCENTAGE_MAX 
          } else { 
            BUSINESS_CLASS_PERCENTAGE_MAX * (totalIncome - BUSINESS_CLASS_INCOME_MIN) / (BUSINESS_CLASS_INCOME_MAX - BUSINESS_CLASS_INCOME_MIN)
          }
        } else {
         0 
        }
      val firstClassDemand = (totalDemand * firstClassPercentage).toInt
      val businessClassDemand = (totalDemand * businessClassPercentage).toInt
      val economyClassDemand = totalDemand - firstClassDemand - businessClassDemand
      new FlightDemand(economyClassDemand, businessClassDemand, firstClassDemand)
    }
  }
  
  def getFlightPreferencePoolOnAirport(fromAirport : Airport) : FlightPreferencePool = {
    //for now 5 simple preferences per airport
    val simplePreferenceCount = 10;


    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    for (i <- 0 until simplePreferenceCount) { 
      flightPreferences.append((SimplePreference(i, simplePreferenceCount - 1, ECONOMY), 1))
    }
        
    //for now 50 * 3 loyalty preferences per airport
    val loyaltyPreferenceCount = 50;
    for (i <- 0 until loyaltyPreferenceCount) {
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), ECONOMY), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), BUSINESS), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), FIRST), 1))
    }
    
    
    new FlightPreferencePool(flightPreferences.toList)
  }
  
  sealed private[patson] class FlightDemand(economy : Int, business : Int, first : Int) {
    val hasDemand = economy > 0 || business > 0 || first > 0
    private val demandMap : Map[LinkClass, Int] = Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first)
    def apply(linkClass : LinkClass) = demandMap(linkClass)
  }
  
}