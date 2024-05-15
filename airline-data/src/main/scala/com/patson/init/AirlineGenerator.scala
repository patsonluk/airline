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

import java.awt.Color
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object AirlineGenerator extends App {
  mainFlow

  val GENERATED_AIRLINE_ID_MAX = 300
  
  def mainFlow() = {
    deleteAirlines()
    generateAirlines(240)
    generateMegaAirlines(30)
    generateLocalAirlines(30)
    println("DONE Creating airlines")

    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def deleteAirlines() : Unit = {
    println("Deleting airlines...")
    UserSource.deleteGeneratedUsers()
    UserCache.invalidateAll()
    AirlineCache.invalidateAll()
    AirlineSource.deleteAirlinesByCriteria(List(("is_generated", true)))
  }
  
  def generateAirlines(count: Int) : Unit = {
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    val airports = AirportSource.loadAllAirports(true).sortBy { _.popMiddleIncome }
    val airportsWithoutDomestic = airports.filterNot(_.isDomesticAirport())
    val topAirports = airportsWithoutDomestic.takeRight(count)

    val modelsShort = ModelSource.loadAllModels().filter { model => 
      model.family == "Boeing 737"
    }
    val modelsLong = ModelSource.loadAllModels().filter { model => 
      model.family == "Boeing 777"
    }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret(baseAirport.iata, "gidding")
      
      val newAirline = Airline("Rats Global " + baseAirport.iata, isGenerated = true)
      newAirline.setBalance(1000000000)
      newAirline.setTargetServiceQuality(49)
      newAirline.setCurrentServiceQuality(70)
      newAirline.setReputation(80)
      newAirline.setSkipTutorial(true)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())

      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 7, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      
      println(i + " generated user " + user.userName)

      val nearbyAirports = airportsWithoutDomestic.filter(toAirport => {
        toAirport.id != baseAirport.id && countryRelationships.getOrElse((baseAirport.countryCode, toAirport.countryCode), 0) >= 0 && Computation.calculateDistance(baseAirport, toAirport) > 300 && Computation.calculateDistance(baseAirport, toAirport) < 3000
      })
      val nearbyPoolSize = if(nearbyAirports.length < 300) nearbyAirports.length else 300
      val farAwayAirports = airportsWithoutDomestic.filter(toAirport => {
        toAirport.id != baseAirport.id && Computation.calculateDistance(baseAirport, toAirport) >= 3000 && countryRelationships.getOrElse((baseAirport.countryCode, toAirport.countryCode), 0) >= 0
      })
      generateLinks("near 18", newAirline, baseAirport, nearbyAirports, poolSize = nearbyPoolSize, 15, modelsShort, false, 40)
      generateLinks("international 8", newAirline, baseAirport, farAwayAirports, poolSize = 300, 8, modelsLong, false, 60)
    }
    
    Patchers.patchFlightNumber()
  }

  def generateMegaAirlines(count: Int) : Unit = {
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    val groupedAirports: Map[String, List[Airport]] = AirportSource.loadAllAirports(false).groupBy(_.countryCode)
    val uniqueAirportsWithHighestPower: List[Airport] = groupedAirports.values.map { airportList =>
      airportList.maxBy(_.power)
    }.toList
    val sortedAirports = uniqueAirportsWithHighestPower.sortBy(_.power)
    val topAirports = sortedAirports.takeRight(count)
    val models = ModelSource.loadAllModels().filter { model => 
      model.family == "Boeing 747" || model.family == "Comac C929"
    }
    
    val airportsByZone = topAirports.groupBy { _.zone }
    for (i <- 0 until count) {
      val baseAirport = topAirports(i)
      val user = User(userName = "M" + baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret("M" + baseAirport.iata, "gidding")
      
      val newAirline = Airline("Rats Auto MOUP " + baseAirport.countryCode, isGenerated = true)
      newAirline.setBalance(200000000)
      newAirline.setTargetServiceQuality(65)
      newAirline.setCurrentServiceQuality(80)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 9, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 60)
      
      println(i + " generated user " + user.userName)

      val nearbyAirports = sortedAirports.filter(toAirport => {
        toAirport.id != baseAirport.id && countryRelationships.getOrElse((baseAirport.countryCode, toAirport.countryCode), 0) >= 0 && Computation.calculateDistance(baseAirport, toAirport) > 300 && Computation.calculateDistance(baseAirport, toAirport) < 3000
      })
      val nearbyPoolSize = if (nearbyAirports.length < 300) nearbyAirports.length else 300
      val farAwayAirports = sortedAirports.filter(toAirport => {
        toAirport.id != baseAirport.id && Computation.calculateDistance(baseAirport, toAirport) >= 3000 && countryRelationships.getOrElse((baseAirport.countryCode, toAirport.countryCode), 0) >= 0
      })
      generateLinks("near 12", newAirline, baseAirport, nearbyAirports, poolSize = nearbyPoolSize, 12, models, false, 40)
      generateLinks("international 20", newAirline, baseAirport, farAwayAirports, farAwayAirports.length, 20, models, false, 60)
    }
    
    Patchers.patchFlightNumber()
  }

  def generateLocalAirlines(count: Int) : Unit = {
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    val airports = AirportSource.loadAllAirports(true).sortBy { _.popMiddleIncome }
    val baseAirports = airports.filter { airport =>
      airport.countryCode == "US" || airport.countryCode == "MX" || airport.countryCode == "CA"
    }.filter ( _.size <= 4 ).sortBy { _.popMiddleIncome }.takeRight(count)
    val models = ModelSource.loadAllModels().filter { model => 
      model.family == "Dornier 728"
    }
    
    for (i <- 0 until count) {
      val baseAirport = baseAirports(i)
      val user = User(userName = "B" + baseAirport.iata, email = "bot", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret("B" + baseAirport.iata, "gidding")
      
      val newAirline = Airline("Rats Bark Fly " + baseAirport.iata, isGenerated = true)
      newAirline.setBalance(200000000)
      newAirline.setTargetServiceQuality(60)
      newAirline.setCurrentServiceQuality(65)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 6, 1, true)
      
      AirlineSource.saveAirlines(List(newAirline))
      
      UserSource.setUserAirline(user, newAirline)
      AirlineSource.saveAirlineBase(airlineBase)
      
      AirlineSource.saveAirplaneRenewal(newAirline.id, 55)
      
      println(i + " generated user " + user.userName)

      val destinationAirports = airports.filter(toAirport => {
        toAirport.id != baseAirport.id && Computation.calculateDistance(baseAirport, toAirport) <= 3000 && countryRelationships.getOrElse((baseAirport.countryCode, toAirport.countryCode), 0) >= 0
      })
      generateLinks("local 35", newAirline, baseAirport, destinationAirports, destinationAirports.length, 30, models, false, 40)
    }
    
    Patchers.patchFlightNumber()
  }
  
  def generateLinks(descripton: String, airline : Airline, fromAirport : Airport,  toAirports : List[Airport], poolSize: Int, linkCount : Int, airplaneModels : List[Model], legacyConfig : Boolean, rawQuality : Int): Unit = {
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    //only try to goto the top poolSize of airprots
    val topToAirports = toAirports.takeRight(poolSize)

    val pickedToAirports = drawFromPool(topToAirports.reverse, poolSize)
    val airplaneModelsLarge = airplaneModels.sortBy { _.capacity }.reverse
    val airplaneModelsSmall = airplaneModels.sortBy { _.capacity }
    val newLinks = ListBuffer[Link]()
    var i = 0
    while (newLinks.length < linkCount && i < poolSize) {
      val toAirport = pickedToAirports(i)
      i += 1
      val distance = Computation.calculateDistance(fromAirport, toAirport)
      val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
      val affinity = Computation.calculateAffinityValue(fromAirport.zone, toAirport.zone, relationship)
      val demand = DemandGenerator.computeBaseDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
      val targetSeats = demand.travelerDemand.total * 3

      if (targetSeats > 0) {
        var pickedModel = airplaneModelsSmall.find { model => model.capacity * Computation.calculateMaxFrequency(model, distance) >= targetSeats && model.range >= distance} //find smallest model that can cover all demand
        
        if (pickedModel.isEmpty) { //find the largest model with range
          pickedModel = airplaneModelsLarge.find(_.range >= distance)
        }
        
        
        pickedModel match { //find the biggest airplane that does NOT meet the targetSeats
          case Some(model) =>
            val frequency = if(targetSeats / model.capacity > 35) 35 else (targetSeats.toDouble / model.capacity).toInt

            val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(model, distance)
            if (frequency > 0) {
              val assignedAirplanes = mutable.HashMap[Airplane, LinkAssignment]()
              var airplanesRequired = Math.max(1, frequency / maxFrequencyPerAirplane)

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
              
              val flightType = Computation.getFlightType(fromAirport, toAirport, distance, relationship)
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
    if(newLinks.length>0){
      LinkSource.saveLinks(newLinks.toList)
    } else {
      println(s"No links on $descripton from ${fromAirport.iata} !!!")
    }
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