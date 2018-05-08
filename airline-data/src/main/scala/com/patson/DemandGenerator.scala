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
import com.patson.model.PassengerType


object DemandGenerator {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()
  private[this] val FIRST_CLASS_INCOME_MIN = 25000
  private[this] val FIRST_CLASS_INCOME_MAX = 100000
  private[this] val FIRST_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.08, PassengerType.TOURIST -> 0.02) //max 8% first (Business passenger), 2% first (Tourist)
  private[this] val BUSINESS_CLASS_INCOME_MIN = 10000
  private[this] val BUSINESS_CLASS_INCOME_MAX = 100000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.30, PassengerType.TOURIST -> 0.10) //max 30% business (Business passenger), 10% business (Tourist) 
  
  
  val defaultTotalWorldPower = {
    AirportSource.loadAllAirports(false).filter { _.iata != ""  }.map { _.power }.sum
  }
//  mainFlow
//  
//  def mainFlow() = {
//    Await.ready(computeDemand(), Duration.Inf)
//    
//    actorSystem.shutdown()
//  }
  
  def computeDemand() = {
    println("Loading airports")
    //val allAirports = AirportSource.loadAllAirports(true)
    val airports = AirportSource.loadAllAirports(true).filter { _.iata != ""  }
    println("Loaded " + airports.size + " airports")
    
    val airportSource = Source(airports)
	  
	  val computeFlow: Flow[Airport, (Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])] = Flow[Airport].filter { _.power > 0 }.mapAsync { 
	    fromAirport : Airport => {
	      Future {
	        val demandList = ListBuffer[(Airport, (PassengerType.Value, LinkClassValues))]() 
	        airports.foreach { toAirport => 
	          val businessDemand = computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.BUSINESS)
	          val touristDemand = computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.TOURIST)
	          
//	          if (businessDemand.total > 0 && touristDemand.total > 0) {
//	            println(businessDemand + " : " + touristDemand)
//	          }
//	          
	          if (businessDemand.total > 0) {
	            demandList.append((toAirport, (PassengerType.BUSINESS, businessDemand)))
	          } 
	          if (touristDemand.total > 0) {
	            demandList.append((toAirport, (PassengerType.TOURIST, touristDemand)))
	          }
	        } 
	        (fromAirport, demandList.toList)
	      }
	    }
	  }
	  
	  val demandChunkSize = 5
	  val toPassengerGroupFlow: Flow[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))]), List[(PassengerGroup, Airport, Int)]] = Flow[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])].map {
	    case (fromAirport, toAirportsWithDemand) =>
	      val passangerGroupDemand = ListBuffer[(PassengerGroup, Airport, Int)]() 
        //for each city generate different preferences
        val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

        val demandListFromThisAiport = toAirportsWithDemand.foreach {
          case (toAirport, (passengerType, demand)) =>
            LinkClass.values.foreach { linkClass =>
              if (demand(linkClass) > 0) {
                var remainingDemand = demand(linkClass)
                while (remainingDemand > demandChunkSize) {
                  passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass), passengerType), toAirport, demandChunkSize))
                  remainingDemand -= demandChunkSize
                }
                passangerGroupDemand.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass), passengerType), toAirport, remainingDemand)) // don't forget the last chunk
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
  
  def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, passengerType : PassengerType.Value) : LinkClassValues = {
    if (fromAirport == toAirport) {
      LinkClassValues.getInstance(0, 0, 0)
    } else {
      import FlightType._
      val flightType = Computation.getFlightType(fromAirport, toAirport)
      
      var multiplier = flightType match {
        case SHORT_HAUL_DOMESTIC => 6
        case LONG_HAUL_DOMESTIC => 3
        case SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL => if (passengerType == PassengerType.BUSINESS) 1.5 else 2.0
        case LONG_HAUL_INTERNATIONAL | LONG_HAUL_INTERCONTINENTAL => 1
        case ULTRA_LONG_HAUL_INTERCONTINENTAL  => if (passengerType == PassengerType.BUSINESS) 0.5 else 0.3
      }
      
      if (passengerType == PassengerType.TOURIST) {
        multiplier *= 0.3 //generate very small number of tourist, as it should be mainly driven by features
      }
      
      //adjustment : extra bonus to tourist supply for rich airports, up to double at every 15 income level increment
      val incomeLevel = Computation.getIncomeLevel(fromAirport.income)
      if (passengerType == PassengerType.TOURIST && incomeLevel > 25) { 
        multiplier *= (((incomeLevel - 25).toDouble / 15) * 2)       
      }
      
      //adjustments : these zones do not have good ground transport
      if (fromAirport.zone == toAirport.zone) {
        if (fromAirport.zone == "OC" || fromAirport.zone == "AF") {
          multiplier *= 4
        } else if (fromAirport.zone == "SA" || fromAirport.zone == "NA") {
          multiplier *= 3
        }
      }
      
      val rawDemand = (fromAirport.power.doubleValue() / 20000000000L * toAirport.power / 20000000000L * multiplier).toInt
      
      var totalDemand = rawDemand
      //adjust by features
      fromAirport.getFeatures().foreach { feature =>
         val adjustment = feature.demandAdjustment(rawDemand, passengerType, fromAirport.id, fromAirport, toAirport)
         totalDemand += adjustment
      }
      toAirport.getFeatures().foreach { feature => 
          val adjustment = feature.demandAdjustment(rawDemand, passengerType, toAirport.id, fromAirport, toAirport)
         totalDemand += adjustment
      }
      
      //compute demand composition. depends on from airport income
      val income = fromAirport.income

      val firstClassPercentage : Double = 
        if (flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_INTERCONTINENTAL || flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL) {
          if (income <= FIRST_CLASS_INCOME_MIN) {
            0 
          } else if (income >= FIRST_CLASS_INCOME_MAX) {
            FIRST_CLASS_PERCENTAGE_MAX(passengerType) 
          } else { 
            FIRST_CLASS_PERCENTAGE_MAX(passengerType) * (income - FIRST_CLASS_INCOME_MIN) / (FIRST_CLASS_INCOME_MAX - FIRST_CLASS_INCOME_MIN)
          }
        } else {
         0 
        }
      val businessClassPercentage : Double =
        if (flightType != SHORT_HAUL_DOMESTIC) {
          if (income <= BUSINESS_CLASS_INCOME_MIN) {
            0 
          } else if (income >= BUSINESS_CLASS_INCOME_MAX) {
            BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) 
          } else { 
            BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * (income - BUSINESS_CLASS_INCOME_MIN) / (BUSINESS_CLASS_INCOME_MAX - BUSINESS_CLASS_INCOME_MIN)
          }
        } else {
         0 
        }
      val firstClassDemand = (totalDemand * firstClassPercentage).toInt
      val businessClassDemand = (totalDemand * businessClassPercentage).toInt
      val economyClassDemand = totalDemand - firstClassDemand - businessClassDemand
      LinkClassValues.getInstance(economyClassDemand, businessClassDemand, firstClassDemand)
    }
  }
  
  def getFlightPreferencePoolOnAirport(fromAirport : Airport) : FlightPreferencePool = {
    //for now 5 simple preferences per airport
    val simplePreferenceCount = 5;


    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    for (i <- 0 until simplePreferenceCount) { 
      flightPreferences.append((SimplePreference(i, simplePreferenceCount - 1, ECONOMY), 1))
    }
        
    //for now 5 * 3 loyalty preferences per airport
    val loyaltyPreferenceCount = 5;
    for (i <- 0 until loyaltyPreferenceCount) {
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), ECONOMY), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), BUSINESS), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), FIRST), 1))
    }
    
    
    new FlightPreferencePool(flightPreferences.toList)
  }
  
  sealed case class Demand(businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}