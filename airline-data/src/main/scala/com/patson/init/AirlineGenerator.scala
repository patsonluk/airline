package com.patson.init

import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import com.patson.util._
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
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object AirlineGenerator extends App {
  mainFlow
  
  def mainFlow() = {
    deleteAirlines()
    generateAirlines(250)
    generateMegaAirlines(25)
    generateLocalAirlines(10)
    println("DONE Creating airlines")

    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def deleteAirlines() : Unit = {
    println("Deleting airlines...")
    UserSource.deleteGeneratedUsers()
    AirportCache.invalidateAll()
    UserCache.invalidateAll()
    AirlineCache.invalidateAll()
    AirlineSource.deleteAirlinesByCriteria(List(("is_generated", true)))
  }
  
  def generateAirlines(count: Int) : Unit = {
    val airports = AirportSource.loadAllAirports(false).sortBy { _.power }
    val topAirports = airports.takeRight(count)
    val modelsShort = ModelSource.loadAllModels().filter { model => 
      model.family == "Boeing 787"
    }
    val modelsLong = ModelSource.loadAllModels().filter { model => 
      model.family == "Airbus A380"
    }
    val airportsByZone = airports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret(baseAirport.iata, "gidding")
      
      val newAirline = Airline("Rat Wings " + baseAirport.iata, isGenerated = true)
      newAirline.setBalance(100000000)
      newAirline.setMaintenanceQuality(50)
      newAirline.setTargetServiceQuality(49)
      newAirline.setCurrentServiceQuality(70)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 10, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      AirlineSource.saveLogo(newAirline.id, LogoGenerator.generateRandomLogo())
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      
      println(i + " generated user " + user.userName)
                  
      //generate Local Links
      generateLinks(newAirline, baseAirport, airportsByZone(baseAirport.zone).filter { _.id != baseAirport.id }, 150, 24, modelsShort, true, 60)
      //generate Inter-zone links
      generateLinks(newAirline, baseAirport, airports.filter { airport => airport.zone != baseAirport.zone }, 50, 5, modelsLong, false, 80)
    }
    
    Patchers.patchFlightNumber()
  }

  def generateMegaAirlines(count: Int) : Unit = {
    val groupedAirports: Map[String, List[Airport]] = AirportSource.loadAllAirports(false).groupBy(_.countryCode)
    val uniqueAirportsWithHighestPower: List[Airport] = groupedAirports.values.map { airportList =>
      airportList.maxBy(_.power)
    }.toList
    val sortedAirports = uniqueAirportsWithHighestPower.sortBy(_.power)
    val topAirports = sortedAirports.takeRight(count)
    val models = ModelSource.loadAllModels().filter { model => 
      model.family == "Airbus A350" || model.family == "Airbus A320"
    }
    
    val airportsByZone = topAirports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = "K" + baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret("K" + baseAirport.iata, "gidding")
      
      val newAirline = Airline("Koala Air " + baseAirport.countryCode, isGenerated = true)
      newAirline.setBalance(100000000)
      newAirline.setMaintenanceQuality(55)
      newAirline.setTargetServiceQuality(55)
      newAirline.setCurrentServiceQuality(70)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 10, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      AirlineSource.saveLogo(newAirline.id, LogoGenerator.generateRandomLogo())
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      
      println(i + " generated user " + user.userName)
                  
      //generate Local Links
      generateLinks(newAirline, baseAirport, airportsByZone(baseAirport.zone).filter { _.id != baseAirport.id }, 20, 5, models, false, 60)
      //generate Inter-zone links
      generateLinks(newAirline, baseAirport, topAirports.filter { airport => airport.zone != baseAirport.zone }, 70, 12, models, false, 80)
    }
    
    Patchers.patchFlightNumber()
  }

  def generateLocalAirlines(count: Int) : Unit = {
    val airports = AirportSource.loadAllAirports(false).sortBy { _.power }
    val topAirports = airports.takeRight(count)
    val models = ModelSource.loadAllModels().filter { model => 
      model.family == "Airbus A320"
    }
    
    val airportsByZone = topAirports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = "A" + baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret("A" + baseAirport.iata, "gidding")
      
      val newAirline = Airline("Ajwaa Airlines " + baseAirport.countryCode, isGenerated = true)
      newAirline.setBalance(100000000)
      newAirline.setMaintenanceQuality(55)
      newAirline.setTargetServiceQuality(55)
      newAirline.setCurrentServiceQuality(70)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 9, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      AirlineSource.saveLogo(newAirline.id, LogoGenerator.generateRandomLogo())
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      
      println(i + " generated user " + user.userName)
                  
      //generate Local Links
      generateLinks(newAirline, baseAirport, airportsByZone(baseAirport.zone).filter { _.id != baseAirport.id }, 100, 35, models, false, 40)
      //generate Inter-zone links
      generateLinks(newAirline, baseAirport, topAirports.filter { airport => airport.zone != baseAirport.zone }, 10, 2, models, false, 60)
    }
    
    Patchers.patchFlightNumber()
  }
  
  def generateLinks(airline : Airline, fromAirport : Airport,  toAirports : List[Airport], poolSize: Int, linkCount : Int, airplaneModels : List[Model], legacyConfig : Boolean, rawQuality : Int): Unit = {
    //only try to goto the top poolSize of airprots
    val topToAirports = toAirports.takeRight(poolSize)
    
    val pickedToAirports = drawFromPool(topToAirports.reverse, linkCount) 
    val airplaneModelsByRange = airplaneModels.sortBy { _.range }
    val airplaneModelsByCapacity = airplaneModels.sortBy { _.capacity }
    val newLinks = ListBuffer[Link]()
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    pickedToAirports.foreach { toAirport =>
      val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
      val estimatedOneWayDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS) + DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
      val targetSeats = estimatedOneWayDemand(ECONOMY) * 3
      
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

            val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(model, distance)
            if (frequency > 0) {
              val assignedAirplanes = mutable.HashMap[Airplane, LinkAssignment]()
              var airplanesRequired = frequency / maxFrequencyPerAirplane
              if (frequency % maxFrequencyPerAirplane > 0) {
                airplanesRequired += 1
              }

              val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
              var capacity = LinkClassValues(model.capacity, model.capacity, model.capacity)
              //make airplanes :)
              var remainingFrequency = frequency
              for (i <- 0 until airplanesRequired) {
                //case class Airplane(model : Model, var owner : Airline, constructedCycle : Int, var purchasedCycle : Int, condition : Double, depreciationRate : Int, value : Int, var isSold : Boolean = false, var dealerRatio : Double = Airplane.DEFAULT_DEALER_RATIO, var configuration : AirplaneConfiguration = AirplaneConfiguration.empty, var home : Airport = Airport.fromId(0), isReady : Boolean = true, var purchaseRate : Double = 1, version : Int = 0,var id : Int = 0) extends IdObject {
                val newAirplane = Airplane(model = model, owner = airline, constructedCycle = 0 , purchasedCycle = 0, condition =  Airplane.MAX_CONDITION, depreciationRate = 0, value = model.price, isSold = false, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, configuration = AirplaneConfiguration.empty, home = fromAirport)
                newAirplane.assignDefaultConfiguration(legacyConfig)
                AirplaneSource.saveAirplanes(List(newAirplane))
                val frequencyForThis = if (remainingFrequency > maxFrequencyPerAirplane) maxFrequencyPerAirplane else remainingFrequency
                val flightMinutesForThis = frequencyForThis * flightMinutesRequired
                assignedAirplanes.put(newAirplane, LinkAssignment(frequencyForThis, flightMinutesForThis))
                capacity = (LinkClassValues(newAirplane.configuration.economyVal, newAirplane.configuration.businessVal, newAirplane.configuration.firstVal) * frequencyForThis)
                remainingFrequency -= frequencyForThis
              }
              
              val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
              val price = Pricing.computeStandardPriceForAllClass(distance, flightType)
              
              val duration = Computation.calculateDuration(model, distance)
              val newLink = Link(fromAirport, toAirport, airline, price, distance, capacity, rawQuality, duration = duration, frequency = frequency, flightType = flightType)
              
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
        if (ThreadLocalRandom.current().nextDouble() <= 0.3) {
          result += poolTopFirst(walker)
        }
        walker = (walker + 1) % poolTopFirst.size
      }
      result.toSeq
    }
  }
}