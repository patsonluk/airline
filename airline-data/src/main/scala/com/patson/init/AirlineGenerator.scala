package com.patson.init

import scala.collection.mutable.ListBuffer
import com.patson.data._
import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.airplane._
import java.util.Calendar
import com.patson.Authentication
import scala.util.Random
import com.patson.DemandGenerator
import com.patson.data._
import com.patson.data.airplane._


object AirlineGenerator extends App {
  mainFlow
  
  def mainFlow() = {
    val airlines = generateAirlines(100)
    airlines.foreach {
      println
    }
    println("DONE Creating airlines")
    
    actorSystem.shutdown()
  }
  
  def generateAirlines(count: Int) = {
    val airlines = scala.collection.mutable.Map[Airline, AirlineBase]()
    //val airlineBases = ListBuffer[AirlineBase]()
    val airports = AirportSource.loadAllAirports(true).sortBy { _.power }
    val topAirports = airports.takeRight(count)
    val models = ModelSource.loadAllModels()
    
    val airportsByZone = airports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = baseAirport.iata, email = "", Calendar.getInstance, UserStatus.ACTIVE)
      UserSource.saveUser(user)
      Authentication.createUserSecret(baseAirport.iata, "1234")
      
      val newAirline = Airline("Air " + baseAirport.city + " - " + baseAirport.iata)
      newAirline.setBalance(0)
      newAirline.setMaintainenceQuality(100)
      
      val airlineBase = AirlineBase(newAirline, baseAirport, 1, 1, true)
      airlines.put(newAirline, airlineBase)
      
      AirlineSource.saveAirlines(List(newAirline))
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      baseAirport.setAirlineAwareness(newAirline.id, 100)
      baseAirport.setAirlineLoyalty(newAirline.id, 100)
      AirportSource.updateAirlineAppeal(airports)
      
      println(i + " generated user " + user.userName)
      
      //generate Local Links
      generateLinks(newAirline, baseAirport, airportsByZone(baseAirport.zone).filter { _.id != baseAirport.id }, 150, 50, models)
      //generate Inter-zone links
      generateLinks(newAirline, baseAirport, airports.filter { airport => airport.zone != baseAirport.zone }, 50, 5, models)
      
    }
    
    airlines
  }
  
  def generateLinks(airline : Airline, fromAirport : Airport,  toAirports : List[Airport], poolSize: Int, linkCount : Int, airplaneModels : List[Model]) {
    //only try to goto the top linkCount * 3 airports in the zone
    val topToAirports = toAirports.takeRight(poolSize)
    
    val pickedToAirports = 
      if (topToAirports.length <= linkCount) {
        topToAirports
      } else {
        Random.shuffle(topToAirports).take(linkCount)
      }
    val airplaneModelsByRange = airplaneModels.sortBy { _.range }
    val airplaneModelsByCapacity = airplaneModels.sortBy { _.capacity } (Ordering[Int].reverse)
    val newLinks = ListBuffer[Link]()
    pickedToAirports.foreach { toAirport =>
      val estimatedOneWayDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.BUSINESS) + DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.TOURIST)
      val targetSeats = estimatedOneWayDemand(ECONOMY)
      
      if (targetSeats > 0) {
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        var pickedModel = airplaneModelsByCapacity.find { model => model.capacity < targetSeats && model.range >= distance}
        
        if (pickedModel.isEmpty) { //find the one with lowest range that can cover it
          pickedModel = airplaneModelsByRange.find { model => model.range >= distance}
        }
        
        
        pickedModel match { //find the biggest airplane that does NOT meet the targetSeats
          case Some(model) =>
            var frequency = targetSeats / model.capacity
            if (frequency == 0) {
              frequency = 1
            }
            val availableSlots = Math.min(fromAirport.availableSlots, toAirport.availableSlots)
            frequency = Math.min(frequency, availableSlots)
            
            if (frequency > 0) {
              val airplanes = ListBuffer[Airplane]()
              var airplanesRequired = frequency / Computation.calculateMaxFrequency(model, distance)
              if (frequency % Computation.calculateMaxFrequency(model, distance) > 0) {
                airplanesRequired += 1
              }
              
              //make airplanes :)
              for (i <- 0 until airplanesRequired) {
                val newAirplane = Airplane(model = model, owner = airline, constructedCycle = 0 , condition =  Airplane.MAX_CONDITION, depreciationRate = 0, value = model.price)
                AirplaneSource.saveAirplanes(List(newAirplane))
                airplanes += newAirplane
              }
              
              val flightType = Computation.getFlightType(fromAirport, toAirport)
              val price = Pricing.computeStandardPrice(distance, flightType, ECONOMY)
              val capacity = frequency * model.capacity
              val duration = Computation.calculateDuration(model, distance)
              val newLink = Link(fromAirport, toAirport, airline, LinkClassValues.getInstance(price), distance, LinkClassValues.getInstance(capacity), rawQuality = 40, duration = duration, frequency = frequency)
              
              newLink.setAssignedAirplanes(airplanes.toList)
              newLinks += newLink
              
              
            } else {
              println("Cannot generate link from " + fromAirport.iata + " to " + toAirport.iata + " frequency is 0")
            }
              
          case None => println("Too far? : demand: " + targetSeats + " Distance " + distance)
        }
      }
    }
    
    newLinks.foreach { link => LinkSource.saveLink(link) }
  }
}