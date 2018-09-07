package com.patson.model

import com.patson.model.airplane._
import com.patson.data.CycleSource
import com.patson.Util
import com.patson.data.AllianceSource

object Computation {
  //distance vs max speed
  val speedLimits = List((300, 350), (400, 500), (400, 700))  
  def calculateDuration(airplaneModel: Model, distance : Int) = {
    var remainDistance = distance
    var duration = 0;
    for ((distanceBucket, maxSpeed) <- speedLimits if(remainDistance > 0)) {
      val speed = Math.min(maxSpeed, airplaneModel.speed)
      if (distanceBucket >= remainDistance) {
        duration += remainDistance * 60 / speed
      } else {
        duration += distanceBucket * 60 / speed
      }
      remainDistance -= distanceBucket
    }
    
    if (remainDistance > 0) {
      duration += remainDistance * 60 / airplaneModel.speed
    }
    duration
  }

  def calculateMaxFrequency(airplaneModel: Model, distance : Int) : Int = {
    if (airplaneModel.range < distance) {
      0
    } else {
      val duration = calculateDuration(airplaneModel, distance)
      val roundTripTime = (duration + airplaneModel.turnoverTime) * 2
      val availableFlightTimePerWeek = (3.5 * 24 * 60).toInt //assume per week only 3 days are "flyable"
      //println(airplaneModel + " distance " + distance + " freq: " + availableFlightTimePerWeek / roundTripTime + " times")
      availableFlightTimePerWeek / roundTripTime
    }
  }
  
  def calculateAge(fromCycle : Int) = {
    val currentCycle = CycleSource.loadCycle()
    currentCycle - fromCycle 
  }
  
  val SELL_RATE = 0.8
  
  def calculateAirplaneSellValue(airplane : Airplane) : Int = {
    //80% off
    val value = airplane.value * SELL_RATE
    if (value < 0) 0 else value.toInt
  }
  
  def calculateDistance(fromAirport : Airport, toAirport : Airport) : Int = {
    Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  }
  
  def getFlightType(fromAirport : Airport, toAirport : Airport, distance : Int) = { 
//    val distance = distanceOption.getOrElse(Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt)
    
    import FlightType._
    if (fromAirport.countryCode == toAirport.countryCode) { //domestic
      if (distance <= 1000) {
        SHORT_HAUL_DOMESTIC
      } else {
        LONG_HAUL_DOMESTIC
      }
    } else if (fromAirport.zone == toAirport.zone) { //international but same continent
      if (distance <= 2000) {
        SHORT_HAUL_INTERNATIONAL
      } else {
        LONG_HAUL_INTERNATIONAL
      }
    } else {
      if (distance <= 4000) {
        SHORT_HAUL_INTERCONTINENTAL
      } else if (distance <= 12000) {
        LONG_HAUL_INTERCONTINENTAL
      } else {
        ULTRA_LONG_HAUL_INTERCONTINENTAL
      }
    }
  }
  
  import FlightCategory._
  def getFlightCategory(fromAirport : Airport, toAirport : Airport) : FlightCategory.Value = {
    val distance = calculateDistance(fromAirport, toAirport)
    if (fromAirport.countryCode == toAirport.countryCode) {
      DOMESTIC
    } else if (fromAirport.zone == toAirport.zone || distance <= 1000) {
      REGIONAL
    } else {
      INTERCONTINENTAL
    }
  }

  /**
   * Returns a normalized income level, should be greater than 0
   */
  def getIncomeLevel(income : Int) : Int = {
    val incomeLevel = (Math.log(income.toDouble / 500) / Math.log(1.1)).toInt
    if (incomeLevel < 1) {
      1
    } else {
      incomeLevel
    }
  }
  
  def getLinkCreationCost(from : Airport, to : Airport) : Int = {
    
    val baseCost = 100000 + (from.income + to.income)
      
    val minAirportSize = Math.min(from.size, to.size) //encourage links for smaller airport
    
    val airportSizeMultiplier = Math.pow(1.5, minAirportSize) 
    val distance = calculateDistance(from, to)
    val distanceMultiplier = distance.toDouble / 5000
    val internationalMultiplier = if (from.countryCode == to.countryCode) 1 else 3
    
    (baseCost * airportSizeMultiplier * distanceMultiplier * internationalMultiplier).toInt 
  }
  
  def computeReputationBoost(country : Country, ranking : Int) : Double = {
    //US gives 30 boost if 1st rank, 20 if 2nd and 10 if 3rd  pop 97499995 income 54629
    
    val modelPower = 97499995L * 54629L
    val ratioToModelPower = country.airportPopulation * country.income.toDouble / modelPower
    
    val boost = 
      if (ranking <= 3) { //top 3
        math.log10(ratioToModelPower * 100) / 2 * 10 * (4 - ranking)
      } else {
        math.log10(ratioToModelPower * 100) / 2 * 5 / (ranking - 3)
      }
    
    if (boost < 1 && ranking <= 3) {
      1
    } else if (boost < 0.5) {
      0.5
    } else {
      BigDecimal(boost).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    }
  }
  
  
  def computeCompensation(link : Link) : Int = {
    if (link.majorDelayCount > 0 || link.minorDelayCount > 0) {
      val soldSeatsPerFlight = link.soldSeats / link.frequency
      val halfCapacityPerFlight = link.capacity / link.frequency * 0.5
      
      val affectedSeatsPerFlight = if (soldSeatsPerFlight.total > halfCapacityPerFlight.total) soldSeatsPerFlight else halfCapacityPerFlight //if less than 50% LF, considered that as 50% LF
      var compensation = (affectedSeatsPerFlight * link.cancellationCount * 0.5 * link.price).total  //50% of ticket price, as there's some penalty for that already
      compensation = compensation + (affectedSeatsPerFlight * link.majorDelayCount * 0.3 * link.price).total //30% of ticket price
      compensation = compensation + (affectedSeatsPerFlight * link.minorDelayCount * 0.05 * link.price).total //5% of ticket price
      
      compensation.toInt
    } else {
      0
    }
  }
  
  val MAX_FREQUENCY_ABSOLUTE_BASE = 30
  def getMaxFrequencyAbsolute(airline : Airline) : Int = {
     AllianceSource.loadAllianceMemberByAirline(airline) match {
       case Some(allianceMember) => {
         if (allianceMember.role != AllianceRole.APPLICANT) {
           val alliances = AllianceSource.loadAllAlliances(false)
           val alliance = alliances.find( allianceMember.allianceId == _.id).get
           Alliance.getRankings(alliances).get(alliance) match {
             case Some((ranking, _)) => {
               val maxFrequencyBonus = Alliance.getMaxFrequencyBonus(ranking)
               MAX_FREQUENCY_ABSOLUTE_BASE + maxFrequencyBonus
             }
             case None => MAX_FREQUENCY_ABSOLUTE_BASE
           }
         } else {
           MAX_FREQUENCY_ABSOLUTE_BASE
         }
       }
       case None => MAX_FREQUENCY_ABSOLUTE_BASE
     }
  }
  
//  def getAirplaneConstructionTime(model : Model, existingConstruction : Int) : Int = {
//    model.constructionTime + (existingConstruction / 5) * model.constructionTime / 4 
//  }
}