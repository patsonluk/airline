package com.patson.model

import com.patson.PassengerSimulation.LINK_COST_TOLERANCE_FACTOR
import com.patson.model.airplane._
import com.patson.data.{AirlineSource, AirplaneSource, AirportAssetSource, AirportSource, AllianceSource, BankSource, CountrySource, CycleSource, OilSource}
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

  lazy val MAX_VALUES = getMaxValues()
  lazy val MODEL_AIRPORT_POWER = MAX_VALUES._1
  lazy val MAX_INCOME_LEVEL = MAX_VALUES._2
  lazy val MAX_POPULATION = MAX_VALUES._3
  lazy val MAX_INCOME = MAX_VALUES._4

  val MAX_COMPUTED_DISTANCE = 20000
  lazy val standardFlightDurationCache : Array[Int] = {
    val result = new Array[Int](MAX_COMPUTED_DISTANCE + 1)
    for (i <- 0 to MAX_COMPUTED_DISTANCE) { //should cover everything...
      result(i) =  Computation.internalComputeStandardFlightDuration(i)
    }
    result
  }

  def getMaxValues(): (Long, Double, Long, Long) = {
    val allAirports = AirportSource.loadAllAirports()
    //take note that below should NOT use boosted values, should use base, otherwise it will incorrectly load some lazy vals of the Airport that is MAX
    (allAirports.maxBy(_.basePower).basePower, allAirports.maxBy(_.baseIncomeLevel).baseIncomeLevel, allAirports.maxBy(_.basePopulation).basePopulation, allAirports.maxBy(_.baseIncome).baseIncome)
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
  def getIncomeLevel(income : Int) : Double = {
    val incomeLevel = (Math.log(income.toDouble / 500) / Math.log(1.1))
    if (incomeLevel < 1) {
      1
    } else {
      incomeLevel
    }
  }
  def fromIncomeLevel(incomeLevel : Double) : Int = {
    (Math.pow(Math.E, incomeLevel * Math.log(1.1)) * 500).toInt
  }

  def computeIncomeLevelBoostFromPercentage(baseIncome : Int, minIncomeBoost : Int, boostPercentage : Int) = {
    val incomeIncrement = baseIncome * boostPercentage / 100
    val incomeBoost = Math.max(minIncomeBoost, incomeIncrement)

    //10% would always be 1, but cannot make assumption of our income level calculation tho...
    val baseIncomeLevel = getIncomeLevel(baseIncome)
    BigDecimal(Computation.getIncomeLevel(baseIncome + incomeBoost) - baseIncomeLevel).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  /**
    * For low income base, use the boost level (which is MAX boost). For higher income base, down adjust it to certain
    * percentage
    * @param baseIncome
    * @param boostLevel
    * @return
    */
  def computeIncomeLevelBoostFromLevel(baseIncome : Int, boostLevel : Double) = {
    val newIncomeLevel = getIncomeLevel(baseIncome) + boostLevel
    val incomeIncrement = fromIncomeLevel(newIncomeLevel) - baseIncome
    val maxIncomeBoost = (boostLevel * 10_000).toInt //a bit arbitrary
    val minIncomeBoost = (boostLevel * 2_500).toInt
    val finalBoostLevel =
      if (incomeIncrement < minIncomeBoost) {
        getIncomeLevel(baseIncome + minIncomeBoost) - getIncomeLevel(baseIncome)
      } else if (incomeIncrement <= maxIncomeBoost) {
        boostLevel
      } else {
        getIncomeLevel(baseIncome + maxIncomeBoost) - getIncomeLevel(baseIncome)
      }

    BigDecimal(finalBoostLevel).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
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

  val REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD = 40 //airline with service level below this will pay less compensation
  
  def computeCompensation(link : Link) : Int = {
    if (link.majorDelayCount > 0 || link.minorDelayCount > 0 || link.cancellationCount > 0 ) {
      val soldSeatsPerFlight = link.soldSeats / link.frequency
      val halfCapacityPerFlight = link.capacity / link.frequency * 0.5
      
      val affectedSeatsPerFlight = if (soldSeatsPerFlight.total > halfCapacityPerFlight.total) soldSeatsPerFlight else halfCapacityPerFlight //if less than 50% LF, considered that as 50% LF
      var compensation = (affectedSeatsPerFlight * link.cancellationCount * 0.5 * link.price).total  //50% of ticket price, as there's some penalty for that already
      compensation = compensation + (affectedSeatsPerFlight * link.majorDelayCount * 0.3 * link.price).total //30% of ticket price
      compensation = compensation + (affectedSeatsPerFlight * link.minorDelayCount * 0.05 * link.price).total //5% of ticket price

      if (link.airline.getCurrentServiceQuality() < REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD) { //down to only 20%
        val ratio = 0.2 + 0.8 * link.airline.getCurrentServiceQuality() / REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD
        (compensation * ratio).toInt
      } else {
        compensation.toInt
      }
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
    val amountFromAssets = AirportAssetSource.loadAirportAssetsByAirline(airlineId).map(_.sellValue).sum
    val amountFromLoans = BankSource.loadLoansByAirline(airlineId).map(_.earlyRepayment(currentCycle) * -1).sum //repay all loans now
    val amountFromOilContracts = OilSource.loadOilContractsByAirline(airlineId).map(_.contractTerminationPenalty(currentCycle) * -1).sum //termination penalty
    val existingBalance = AirlineCache.getAirline(airlineId).get.airlineInfo.balance
    
    ResetAmountInfo(amountFromAirplanes, amountFromBases, amountFromAssets, amountFromLoans, amountFromOilContracts, existingBalance)
  }
  
  case class ResetAmountInfo(airplanes : Long, bases : Long, assets : Long, loans : Long, oilContracts : Long, existingBalance : Long) {
    val overall = airplanes + bases + assets + loans + oilContracts + existingBalance
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

  val computeStandardFlightDuration = (distance: Int) => {
    if (distance <= MAX_COMPUTED_DISTANCE) {
      standardFlightDurationCache(distance)
    } else {
      println(s"Unexpected distance $distance")
      internalComputeStandardFlightDuration(distance) //just in case
    }
  }
  private def internalComputeStandardFlightDuration(distance : Int) = {
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