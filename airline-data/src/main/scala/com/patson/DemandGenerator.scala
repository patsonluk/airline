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
import scala.util.Random
import com.patson.data.CountrySource
import java.util.ArrayList
import java.util.Collections


object DemandGenerator {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()
  private[this] val FIRST_CLASS_INCOME_MIN = 15000
  private[this] val FIRST_CLASS_INCOME_MAX = 100000
  private[this] val FIRST_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.08, PassengerType.TOURIST -> 0.02) //max 8% first (Business passenger), 2% first (Tourist)
  private[this] val BUSINESS_CLASS_INCOME_MIN = 5000
  private[this] val BUSINESS_CLASS_INCOME_MAX = 100000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.30, PassengerType.TOURIST -> 0.10) //max 30% business (Business passenger), 10% business (Tourist) 
  val MIN_DISTANCE = 50
  
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
  import scala.collection.JavaConverters._
  
  def computeDemand() : java.util.List[(PassengerGroup, Airport, Int)] = {
    println("Loading airports")
    //val allAirports = AirportSource.loadAllAirports(true)
    val airports = AirportSource.loadAllAirports(true).filter { airport => airport.iata != "" && airport.power > 0 }
    println("Loaded " + airports.size + " airports")
    
    val allDemands = new ArrayList[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
	  
	  val countryRelationships = CountrySource.getCountryMutualRelationShips()
	  airports.foreach {  fromAirport =>
	    val demandList = Collections.synchronizedList(new ArrayList[(Airport, (PassengerType.Value, LinkClassValues))]())
	    airports.par.foreach { toAirport =>
//	      if (fromAirport != toAirport) {
          val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
          val businessDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
          val touristDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
    	          
          if (businessDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.BUSINESS, businessDemand)))
          } 
          if (touristDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.TOURIST, touristDemand)))
          }
//	      }
	    }
	    allDemands.add((fromAirport, demandList.asScala.toList))
    }
	  
	  val baseDemandChunkSize = 20
	  
	  
	  val allDemandChunks = new ArrayList[(PassengerGroup, Airport, Int)]()
	  for (i <- 0 until allDemands.size()) {
	    val (fromAirport, toAirportsWithDemand) = allDemands.get(i)
      //for each city generate different preferences
      val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

      val demandListFromThisAiport = toAirportsWithDemand.foreach {
        case (toAirport, (passengerType, demand)) =>
          LinkClass.values.foreach { linkClass =>
            if (demand(linkClass) > 0) {
              var remainingDemand = demand(linkClass)
              var demandChunkSize = baseDemandChunkSize + Random.nextInt(baseDemandChunkSize) 
              while (remainingDemand > demandChunkSize) {
                allDemandChunks.add((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass), passengerType), toAirport, demandChunkSize))
                remainingDemand -= demandChunkSize
                demandChunkSize = baseDemandChunkSize + Random.nextInt(baseDemandChunkSize)
              }
              allDemandChunks.add((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass), passengerType), toAirport, remainingDemand)) // don't forget the last chunk
            }
          }
      }
	      
	  }
	  
	  
    allDemandChunks
  }
  
  def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, relationship : Int, passengerType : PassengerType.Value) : LinkClassValues = {
    val distance = Computation.calculateDistance(fromAirport, toAirport)
    if (fromAirport == toAirport || fromAirport.population == 0 || toAirport.population == 0 || distance <= MIN_DISTANCE) {
      LinkClassValues.getInstance(0, 0, 0)
    } else {
      import FlightType._
      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
      
      //assumption - 1 passenger each week from airport with 1 million pop and 50k income will want to travel to an airport with 1 million pop at income level 25 for business
      //             0.3 passenger in same condition for sightseeing (very low as it should be mainly driven by feature)
      //we are using income level for to airport as destination income difference should have less impact on demand compared to origination airport (and income level is log(income))
      val toAirportIncomeLevel = Computation.getIncomeLevel(toAirport.income)
      
      val fromAirportIncome = fromAirport.power / fromAirport.population
      val fromAirportAdjustedPower = if (fromAirportIncome < 50000) fromAirport.power else fromAirport.population * 50000 //to make high income airport a little bit less overpowered for base
      
      var baseDemand = (fromAirportAdjustedPower.doubleValue() / 1000000 / 50000) * (toAirport.population.doubleValue() / 1000000 * toAirportIncomeLevel / 10) * (passengerType match {
        case PassengerType.BUSINESS => 6
        case PassengerType.TOURIST => 1
      })
      
      if (fromAirport.countryCode != toAirport.countryCode) {
        //baseDemand = baseDemand *
        val mutliplier = 
            if (relationship <= -3) 0 
            else if (relationship == -2) 0.1
            else if (relationship == -1) 0.2
            else if (relationship == 0) 0.5
            else if (relationship == 1) 0.8
            else if (relationship == 2) 1
            else if (relationship == 3) 1.5
            else 2 // >= 4
        baseDemand = baseDemand * mutliplier
      }
          
      
      
      var adjustedDemand = baseDemand
      
      //bonus for domestic and short-haul flight
      adjustedDemand += baseDemand * (flightType match {
        case SHORT_HAUL_DOMESTIC => 4 //people would just drive or take other transit
        case LONG_HAUL_DOMESTIC => 7 
        case SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL => 0
        case LONG_HAUL_INTERNATIONAL | LONG_HAUL_INTERCONTINENTAL => -0.5
        case ULTRA_LONG_HAUL_INTERCONTINENTAL => -0.75
      })
      
      
      //adjustment : extra bonus to tourist supply for rich airports, up to double at every 10 income level increment
      val incomeLevel = Computation.getIncomeLevel(fromAirport.income)
      if (passengerType == PassengerType.TOURIST && incomeLevel > 25) { 
        adjustedDemand += baseDemand * (((incomeLevel - 25).toDouble / 10) * 2)       
      }
      
      //adjustments : these zones do not have good ground transport
      if (fromAirport.zone == toAirport.zone) {
        if (fromAirport.zone == "AF") {
          adjustedDemand +=  baseDemand * 2
        } else if (fromAirport.zone == "SA") {
          adjustedDemand +=  baseDemand * 1
        } else if (fromAirport.zone == "OC" || fromAirport.zone == "NA") {
          adjustedDemand +=  baseDemand * 0.5
        }
      }
      
      //adjust by features
      fromAirport.getFeatures().foreach { feature =>
        val adjustment = feature.demandAdjustment(baseDemand, passengerType, fromAirport.id, fromAirport, toAirport, flightType)
        adjustedDemand += adjustment
      }
      toAirport.getFeatures().foreach { feature => 
        val adjustment = feature.demandAdjustment(baseDemand, passengerType, toAirport.id, fromAirport, toAirport, flightType)
        adjustedDemand += adjustment
      }
      
      //compute demand composition. depends on from airport income
      val income = fromAirport.income

      val firstClassPercentage : Double = 
        if (flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_INTERCONTINENTAL || flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL || flightType == LONG_HAUL_DOMESTIC || flightType == SHORT_HAUL_INTERNATIONAL) {
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
      val firstClassDemand = (adjustedDemand * firstClassPercentage).toInt
      val businessClassDemand = (adjustedDemand * businessClassPercentage).toInt
      val economyClassDemand = adjustedDemand.toInt - firstClassDemand - businessClassDemand
      LinkClassValues.getInstance(economyClassDemand, businessClassDemand, firstClassDemand)
    }
  }
  
  def getFlightPreferencePoolOnAirport(fromAirport : Airport) : FlightPreferencePool = {
    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    flightPreferences.append((SimplePreference(0.7, ECONOMY), 1)) //someone that does not care much
    flightPreferences.append((SimplePreference(0.9, ECONOMY), 3))
    flightPreferences.append((SimplePreference(1, ECONOMY), 4)) //average sensitivity
    flightPreferences.append((SimplePreference(2, ECONOMY), 4)) //quite sensitive to price
    flightPreferences.append((SimplePreference(5, ECONOMY), 2)) //very sensitive to price

        
    //for now 5 * 3 loyalty preferences per airport
    val loyaltyPreferenceCount = 5;
    for (i <- 0 until loyaltyPreferenceCount) {
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), ECONOMY), 15)) 
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), BUSINESS), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(fromAirport.getAirlineAppeals(), FIRST), 1))
    }
    
    
    new FlightPreferencePool(flightPreferences.toList)
  }
  
  sealed case class Demand(businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}