package com.patson.init

import scala.collection.mutable.Set
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

import scala.collection.mutable.ArrayBuffer
import com.patson.util.LogoGenerator
import java.awt.Color

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object AirlineGenerator extends App {
  mainFlow
  
  def mainFlow() = {
    generateAirlines(250)
    
    println("DONE Creating airlines")

    Await.result(actorSystem.terminate(), Duration.Inf)
  }
  
  def generateAirlines(count: Int) : Unit = {
    //val airlines = scala.collection.mutable.Map[Airline, AirlineBase]()
    //val airlineBases = ListBuffer[AirlineBase]()
    val airports = AirportSource.loadAllAirports(true).sortBy { _.power }
    val topAirports = airports.takeRight(count)
    val models = ModelSource.loadAllModels()
    
    val airportsByZone = airports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = baseAirport.iata, email = "", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0)
      UserSource.saveUser(user)
      Authentication.createUserSecret(baseAirport.iata, "1234")
      
      val newAirline = Airline("Air " + baseAirport.city + " - " + baseAirport.iata, isGenerated = true)
      newAirline.setBalance(0)
      newAirline.setMaintenanceQuality(100)
      newAirline.setTargetServiceQuality(30)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 1, 1, true)
      //airlines.put(newAirline, airlineBase)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      AirlineSource.saveLogo(newAirline.id, LogoGenerator.generateRandomLogo())
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      
      AirportSource.updateAirlineAppeal(baseAirport.id, newAirline.id, AirlineAppeal(0, 100))
      
      println(i + " generated user " + user.userName)
                  
      //generate Local Links
      generateLinks(newAirline, baseAirport, airportsByZone(baseAirport.zone).filter { _.id != baseAirport.id }, 150, 50, models)
      //generate Inter-zone links
      generateLinks(newAirline, baseAirport, airports.filter { airport => airport.zone != baseAirport.zone }, 50, 5, models)
    }
    
    Patchers.patchFlightNumber()
  }
  
  def generateLinks(airline : Airline, fromAirport : Airport,  toAirports : List[Airport], poolSize: Int, linkCount : Int, airplaneModels : List[Model]) {
    //only try to goto the top poolSize of airprots
    val topToAirports = toAirports.takeRight(poolSize)
    
    val pickedToAirports = drawFromPool(topToAirports.reverse, linkCount) 
//      if (topToAirports.length <= linkCount) {
//        topToAirports
//      } else {
//        Random.shuffle(topToAirports).take(linkCount)
//      }
    val airplaneModelsByRange = airplaneModels.sortBy { _.range }
    val airplaneModelsByCapacity = airplaneModels.sortBy { _.capacity }
    val newLinks = ListBuffer[Link]()
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    pickedToAirports.foreach { toAirport =>
      val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
      val estimatedOneWayDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS) + DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
      val targetSeats = estimatedOneWayDemand(ECONOMY)
      
      if (targetSeats > 0) {
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        var pickedModel = airplaneModelsByCapacity.find { model => model.capacity * Computation.calculateMaxFrequency(model, distance) >= targetSeats && model.range >= distance} //find smallest model that can cover all demand
        
        if (pickedModel.isEmpty) { //find the one with lowest range that can cover it
          pickedModel = airplaneModelsByRange.find { model => model.range >= distance}
        }
        
        
        pickedModel match { //find the biggest airplane that does NOT meet the targetSeats
          case Some(model) =>
            var frequency = targetSeats / model.capacity
            if (frequency == 0) {
              frequency = 1
            }
//            val availableSlots = Math.min(fromAirport.availableSlots, toAirport.availableSlots) //don't care about slots, as reputation/loyalty is too low might always be 1
//            frequency = Math.min(frequency, availableSlots)

            val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(model, distance)
            if (frequency > 0) {
              val assignedAirplanes = mutable.HashMap[Airplane, LinkAssignment]()
              var airplanesRequired = frequency / maxFrequencyPerAirplane
              if (frequency % maxFrequencyPerAirplane > 0) {
                airplanesRequired += 1
              }

              val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
              //make airplanes :)
              var remainingFrequency = frequency
              for (i <- 0 until airplanesRequired) {
                val newAirplane = Airplane(model = model, owner = airline, constructedCycle = 0 , purchasedCycle = 0, condition =  Airplane.MAX_CONDITION, depreciationRate = 0, value = model.price)
                AirplaneSource.saveAirplanes(List(newAirplane))
                newAirplane.assignDefaultConfiguration()
                val frequencyForThis = if (remainingFrequency > maxFrequencyPerAirplane) maxFrequencyPerAirplane else remainingFrequency
                val flightMinutesForThis = frequencyForThis * flightMinutesRequired
                assignedAirplanes.put(newAirplane, LinkAssignment(frequencyForThis, flightMinutesForThis))
                remainingFrequency -= frequencyForThis
              }
              
              val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
              val price = Pricing.computeStandardPrice(distance, flightType, ECONOMY)
              val capacity = frequency * model.capacity
              val duration = Computation.calculateDuration(model, distance)
              val newLink = Link(fromAirport, toAirport, airline, LinkClassValues.getInstance(price), distance, LinkClassValues.getInstance(capacity), rawQuality = 40, duration = duration, frequency = frequency, flightType = flightType)
              
              newLink.setAssignedAirplanes(assignedAirplanes.toMap)
              newLinks += newLink
              
              
            } else {
              println("Cannot generate link from " + fromAirport.iata + " to " + toAirport.iata + " frequency is 0")
            }
              
          case None => println("Too far? : demand: " + targetSeats + " Distance " + distance)
        }
      }
    }
    
    LinkSource.saveLinks(newLinks.toList)
    //newLinks.foreach { link => LinkSource.saveLink(link) }
  }
  
  def drawFromPool(poolTopFirst : Seq[Airport], drawSize : Int) : Seq[Airport] = {
    if (drawSize >= poolTopFirst.length) {
      poolTopFirst
    } else {
      var walker = 0
      val probably = 0.3
      val result = Set[Airport]() 
      while (result.size < drawSize) {
        if (Random.nextDouble() <= 0.3) {
          result += poolTopFirst(walker)
        }
        walker = (walker + 1) % poolTopFirst.size
      }
      result.toSeq
    }
  }
}