package com.patson.model

import com.patson.PassengerSimulation.LINK_COST_TOLERANCE_FACTOR
import com.patson.model.airplane._
import com.patson.data.{AirlineSource, AirplaneSource, AirportSource, AllianceSource, BankSource, CountrySource, CycleSource, OilSource}
import com.patson.Util
import com.patson.util.{AirlineCache, AllianceRankingUtil}

import scala.collection.mutable.ListBuffer

object Computation {
  val MODEL_COUNTRY_CODE = "US"
  val MODEL_COUNTRY_POWER : Double = CountrySource.loadCountryByCode(MODEL_COUNTRY_CODE) match {
    case Some(country) =>
      country.airportPopulation.toDouble * country.income
    case None =>
      println(s"Cannot find $MODEL_COUNTRY_CODE to compute model power")
      1
  }

  //distance vs max speed
  val speedLimits = List((300, 350), (400, 500), (400, 700))  
  def calculateDuration(airplaneModel: Model, distance : Int) : Int = {
    val speed =
      if (airplaneModel.category == com.patson.model.airplane.Model.Category.SUPERSONIC) {
        (airplaneModel.speed * 1.5).toInt //up adjusted for SST
      } else {
        airplaneModel.speed
      }
    calculateDuration(speed, distance)
  }
  def calculateDuration(airplaneSpeed : Int, distance : Int) = {
    var remainDistance = distance
    var duration = 0;
    for ((distanceBucket, maxSpeed) <- speedLimits if(remainDistance > 0)) {
      val speed = Math.min(maxSpeed, airplaneSpeed)
      if (distanceBucket >= remainDistance) {
        duration += remainDistance * 60 / speed
      } else {
        duration += distanceBucket * 60 / speed
      }
      remainDistance -= distanceBucket
    }

    if (remainDistance > 0) {
      duration += remainDistance * 60 / airplaneSpeed
    }
    duration
  }


  def calculateFlightMinutesRequired(airplaneModel : Model, distance : Int) : Int = {
    val duration = calculateDuration(airplaneModel, distance)
    val roundTripTime = (duration + airplaneModel.turnaroundTime) * 2
    roundTripTime
  }

  def calculateMaxFrequency(airplaneModel : Model, distance : Int) : Int = {
    if (airplaneModel.range < distance) {
      0
    } else {
      val roundTripTime = calculateFlightMinutesRequired(airplaneModel, distance)
      (Airplane.MAX_FLIGHT_MINUTES / roundTripTime).toInt
    }
  }
  

  val SELL_RATE = 0.8
  
  def calculateAirplaneSellValue(airplane : Airplane) : Int = {
    val currentNewMarketPrice = airplane.model.applyDiscount(ModelDiscount.getBlanketModelDiscounts(airplane.model.id)).price
    val value = airplane.value * airplane.purchaseRate * SELL_RATE //airplane.purchase < 1 means it was bought with a discount, selling should be lower price
    if (value < 0) 0 else value.toInt
  }
  
  def calculateDistance(fromAirport : Airport, toAirport : Airport) : Int = {
    Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  }

  def getFlightType(fromAirport : Airport, toAirport : Airport) : FlightType.Value = {
    getFlightType(fromAirport, toAirport, calculateDistance(fromAirport, toAirport))
  }
  
  def getFlightType(fromAirport : Airport, toAirport : Airport, distance : Int) = { 
//    val distance = distanceOption.getOrElse(Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt)
    
    import FlightType._
    if (fromAirport.countryCode == toAirport.countryCode) { //domestic
      if (distance <= 1000) {
        SHORT_HAUL_DOMESTIC
      } else if (distance <= 3000) {
        MEDIUM_HAUL_DOMESTIC
      } else {
        LONG_HAUL_DOMESTIC
      }
    } else if (fromAirport.zone == toAirport.zone) { //international but same continent
      if (distance <= 2000) {
        SHORT_HAUL_INTERNATIONAL
      } else if (distance <= 4000) {
        MEDIUM_HAUL_INTERNATIONAL
      } else {
        LONG_HAUL_INTERNATIONAL
      }
    } else {
      if (distance <= 2000) {
        SHORT_HAUL_INTERCONTINENTAL
      } else if (distance <= 5000) {
        MEDIUM_HAUL_INTERCONTINENTAL
      } else if (distance <= 12000) {
        LONG_HAUL_INTERCONTINENTAL
      } else {
        ULTRA_LONG_HAUL_INTERCONTINENTAL
      }
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
  
//  def computeReputationBoost(country : Country, ranking : Int) : Double = {
//    //US gives boost of (rank : boost)
//    // 1st : 30
//    // 2nd : 24
//    // 3rd : 19
//    // 4th : 16
//    // 5th : 13
//    // 6th : 10
//    // 7th : 8
//    // 8th : 6
//    // 9th : 4
//    // 10th : 2
//
//    val ratioToModelPower = country.airportPopulation * country.income.toDouble / MODEL_COUNTRY_POWER
//
//    val boost = math.log10(ratioToModelPower * 100) / 2 * reputationBoostTop10(ranking)
//
//    if (boost < 1 && ranking <= 3) {
//      1
//    } else if (boost < 0.5) {
//      0.5
//    } else {
//      BigDecimal(boost).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
//    }
//  }
  
  
  def computeCompensation(link : Link) : Int = {
    if (link.majorDelayCount > 0 || link.minorDelayCount > 0 || link.cancellationCount > 0 ) {
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

//  val MAX_FREQUENCY_ABSOLUTE_BASE = 30
//  def getMaxFrequencyThreshold(airline : Airline) : Int = {
//    MAX_FREQUENCY_ABSOLUTE_BASE
//  }

//  def getMaxFrequencyThreshold(airline : Airline) : Int = {
//     AllianceSource.loadAllianceMemberByAirline(airline) match {
//       case Some(allianceMember) => {
//         if (allianceMember.role != AllianceRole.APPLICANT) {
//           AllianceRankingUtil.getRanking(allianceMember.allianceId) match {
//             case Some((ranking, _)) => {
//               val maxFrequencyBonus = Alliance.getMaxFrequencyBonus(ranking)
//               MAX_FREQUENCY_ABSOLUTE_BASE + maxFrequencyBonus
//             }
//             case None => MAX_FREQUENCY_ABSOLUTE_BASE
//           }
//         } else {
//           MAX_FREQUENCY_ABSOLUTE_BASE
//         }
//       }
//       case None => MAX_FREQUENCY_ABSOLUTE_BASE
//     }
//  }
  
  def getResetAmount(airlineId : Int) : ResetAmountInfo = {
    val currentCycle = CycleSource.loadCycle()
    val amountFromAirplanes = AirplaneSource.loadAirplanesByOwner(airlineId, false).map(Computation.calculateAirplaneSellValue(_).toLong).sum
    val amountFromBases = AirlineSource.loadAirlineBasesByAirline(airlineId).map(_.getValue * 0.2).sum.toLong //only get 20% back
    val amountFromLoans = BankSource.loadLoansByAirline(airlineId).map(_.earlyRepayment(currentCycle) * -1).sum //repay all loans now
    val amountFromOilContracts = OilSource.loadOilContractsByAirline(airlineId).map(_.contractTerminationPenalty(currentCycle) * -1).sum //termination penalty
    val existingBalance = AirlineCache.getAirline(airlineId).get.airlineInfo.balance
    
    ResetAmountInfo(amountFromAirplanes, amountFromBases, amountFromLoans, amountFromOilContracts, existingBalance)
  }
  
  case class ResetAmountInfo(airplanes : Long, bases : Long, loans : Long, oilContracts : Long, existingBalance : Long) {
    val overall = airplanes + bases + loans + oilContracts + existingBalance
  }

//  def getAirplaneConstructionTime(model : Model, existingConstruction : Int) : Int = {
//    model.constructionTime + (existingConstruction / 5) * model.constructionTime / 4 
//  }

  val MAX_SATISFACTION_PRICE_RATIO_THRESHOLD = 0.7 //at 100% satisfaction is <= this threshold
  val MIN_SATISFACTION_PRICE_RATIO_THRESHOLD = LINK_COST_TOLERANCE_FACTOR + 0.05 //0% satisfaction >= this threshold ... +0.05 so, there will be at least some satisfaction even at the LINK_COST_TOLERANCE_FACTOR
  /**
    * From 0 (not satisfied at all) to 1 (fully satisfied)
    *
    *
    */
  val computePassengerSatisfaction = (cost: Double, standardPrice : Int) => {
    val ratio = cost / standardPrice
    var satisfaction = (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - ratio) / (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - MAX_SATISFACTION_PRICE_RATIO_THRESHOLD)
    satisfaction = Math.min(1, Math.max(0, satisfaction))
    //println(s"${cost} vs standard price $standardPrice. satisfaction : ${satisfaction}")
    satisfaction
  }

  def computeStandardFlightDuration(distance : Int) = {
    val standardSpeed =
      if (distance <= 1000) {
        400
      } else if (distance <= 2000) {
        600
      } else {
        800
      }
    Computation.calculateDuration(standardSpeed, distance)
  }

  def getDomesticAirportWithinRange(principalAirport : Airport, range : Int) = { //range in km
    val affectedAirports = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(principalAirport, airport) <= range) {
        affectedAirports.append(airport)
      }
    }
    affectedAirports.toList
  }
}