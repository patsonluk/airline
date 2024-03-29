package com.patson

import java.util.{ArrayList, Collections}
import com.patson.data.{AirportSource, CountrySource, EventSource}
import com.patson.model.event.{EventType, Olympics}
import com.patson.model.{PassengerType, _}
import com.patson.model.AirportFeatureType.{AirportFeatureType, DOMESTIC_AIRPORT, FINANCIAL_HUB, GATEWAY_AIRPORT, INTERNATIONAL_HUB, ISOLATED_TOWN, OLYMPICS_IN_PROGRESS, OLYMPICS_PREPARATIONS, UNKNOWN, VACATION_HUB}

import java.util.concurrent.ThreadLocalRandom
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters._
import scala.util.Random


object DemandGenerator {

  private[this] val FIRST_CLASS_INCOME_MIN = 15000
  private[this] val FIRST_CLASS_INCOME_MAX = 100_000
  private[this] val FIRST_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.08, PassengerType.TOURIST -> 0.02, PassengerType.OLYMPICS -> 0.03) //max 8% first (Business passenger), 2% first (Tourist)
  private[this] val BUSINESS_CLASS_INCOME_MIN = 5000
  private[this] val BUSINESS_CLASS_INCOME_MAX = 100_000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.30, PassengerType.TOURIST -> 0.10, PassengerType.OLYMPICS -> 0.15) //max 30% business (Business passenger), 10% business (Tourist)
  val MIN_DISTANCE = 50
  
  import scala.collection.JavaConverters._



  def computeDemand(cycle: Int) = {
    println("Loading airports")
    //val allAirports = AirportSource.loadAllAirports(true)
    val airports: List[Airport] = AirportSource.loadAllAirports(true).filter { airport => (airport.iata != "" || airport.icao != "") && airport.power > 0 }
    println("Loaded " + airports.size + " airports")
    
    val allDemands = new ArrayList[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
	  
	  val countryRelationships = CountrySource.getCountryMutualRelationships()
	  airports.foreach {  fromAirport =>
      //computer Elite destinations & demand
	    val demandList = Collections.synchronizedList(new ArrayList[(Airport, (PassengerType.Value, LinkClassValues))]())

      airports.par.foreach { toAirport =>
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        if (fromAirport != toAirport && fromAirport.population != 0 && toAirport.population != 0 && distance >= MIN_DISTANCE) {
          val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
          val affinity = Computation.calculateAffinityValue(fromAirport.zone, toAirport.zone, relationship)
          val demand = computeBaseDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
          //return list of paxType demands
          //add each to demandList
          val businessDemand = computeClassDemandBetweenAirports(fromAirport, toAirport, relationship, distance, PassengerType.BUSINESS, (demand * 0.5).toInt)
          val touristDemand = computeClassDemandBetweenAirports(fromAirport, toAirport, relationship, distance, PassengerType.TOURIST, (demand * 0.5).toInt)

          if (businessDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.BUSINESS, businessDemand)))
          }
          if (touristDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.TOURIST, touristDemand)))
          }
        }
      }
//      	    airports.par.foreach { toAirport =>
//          val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
//          val businessDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
//          val touristDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
//
//          if (businessDemand.total > 0) {
//            demandList.add((toAirport, (PassengerType.BUSINESS, businessDemand)))
//          }
//          if (touristDemand.total > 0) {
//            demandList.add((toAirport, (PassengerType.TOURIST, touristDemand)))
//          }
//	    }
	    allDemands.add((fromAirport, demandList.asScala.toList))
  }

    val allDemandsAsScala = allDemands.asScala

    allDemandsAsScala.appendAll(generateEventDemand(cycle, airports))

	  val baseDemandChunkSize = 10
	  
	  val allDemandChunks = ListBuffer[(PassengerGroup, Airport, Int)]()
    var oneCount = 0
	  allDemandsAsScala.foreach {
	    case (fromAirport, toAirportsWithDemand) =>
        //for each city generate different preferences
        val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

        toAirportsWithDemand.foreach {
          case (toAirport, (passengerType, demand)) =>
            LinkClass.values.foreach { linkClass =>
              if (demand(linkClass) > 0) {
                var remainingDemand = demand(linkClass)
                var demandChunkSize = baseDemandChunkSize + ThreadLocalRandom.current().nextInt(baseDemandChunkSize)
                while (remainingDemand > demandChunkSize) {
                  allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass, fromAirport, toAirport), passengerType), toAirport, demandChunkSize))
                  remainingDemand -= demandChunkSize
                  demandChunkSize = baseDemandChunkSize + ThreadLocalRandom.current().nextInt(baseDemandChunkSize)
                }
                allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass, fromAirport, toAirport), passengerType), toAirport, remainingDemand)) // don't forget the last chunk
              }
            }
        }

	  }


    allDemandChunks.toList
  }

  //new demand formula
  def computeBaseDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, affinity : Int, distance : Int ) : Int = {
    //roughly adjusting for PPP, ignoring some top countries where nominal ~= PPP
//    val normalizedIncome = if (!Set("LU", "IS", "BM", "QA", "FK", "GI", "CK", "AU", "US", "CA", "MO").contains(fromAirport.countryCode)) {
//      fromAirport.income + 4000
//    } else {
//      fromAirport.income
//    }
//    val iata = fromAirport.iata
//    import java.text.NumberFormat
    val fromPopIncomeAdjusted = if (fromAirport.popMiddleIncome > 0) fromAirport.popMiddleIncome else 1.0

    val incomeRatio = math.min(1.5, (toAirport.income.toDouble / fromAirport.income.toDouble + 1) / 2.0)

    //modeling provincial travel dynamics
    val populationRatio = if (distance < 2500 && toAirport.population > fromPopIncomeAdjusted) {
      math.min(2.0, math.pow(toAirport.population / fromPopIncomeAdjusted.toDouble, .125))
    } else {
      1.0
    }

    val distanceReducerExponent: Double =
      if (distance < 350) {
        distance.toDouble / 350
      } else if (distance > 5000) { //about long-distance or longer
        1.01 - distance.toDouble / (15000 * (affinity + 2)) //affinity affects perceived distance
      } else if (distance > 2000) { //bit less than medium-distance
        1.11 - distance.toDouble / 20000 //i.e 0.1, and then adding a .01 boost
      } else 1

    //domestic/foreign relation multiplier
    val countryRelationMutliplier : Double =
      if (affinity == 5) 1.0 //domestic
      else if (affinity >= 4) 0.4
      else if (affinity == 3) 0.35
      else if (affinity == 2) 0.3
      else if (affinity == 1) 0.2
      else if (affinity == 0) 0.1
      else 0.05


    val specialCountryModifier =
      if (fromAirport.countryCode == "AU" || fromAirport.countryCode == "NZ") {
        16.0 //they travel a lot
      } else if (fromAirport.countryCode == "ZA" || fromAirport.countryCode == "BW" || fromAirport.countryCode == "SZ") {
        5.0
      } else if (toAirport.countryCode == "ID") {
        2.0 //island nation with top10 domestic routes & big tourism destination
      } else if (fromAirport.countryCode == "KR" && toAirport.countryCode == "KR") {
        4.0 //top 10 domestic routes
      } else if (fromAirport.countryCode == "CN" && toAirport.countryCode == "CN" && distance < 1100) {
        0.6 //China has a very extensive highspeed rail network (1100km is Beijing to Shanghai)
      } else if (toAirport.countryCode == "IN") {
        0.5 //toAiport pops are huge even tho > 80% aren't middle income, so cut pops by 50%
      } else if (fromAirport.countryCode == "JP" && toAirport.countryCode == "JP" && distance < 500) {
        0.4 //also interconnected by HSR / intercity rail
      } else if (fromAirport.countryCode == "FR" && distance < 550 && toAirport.countryCode != "GB") {
        0.3
      } else if (fromAirport.countryCode == "IT" && distance < 500) {
        0.5
      } else if (fromAirport.zone.contains("EU") && distance < 260) {
        0.6
      } else 1.0

    val baseDemand: Double = specialCountryModifier * countryRelationMutliplier * incomeRatio * populationRatio * fromPopIncomeAdjusted * toAirport.population.toDouble / 250_000 / 250_000
    if (baseDemand <= 1){
      0
    } else {
      (Math.pow(baseDemand, distanceReducerExponent)).toInt
    }
  }

  def computeClassDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, relationship : Int, distance : Int, passengerType : PassengerType.Value, baseDemand : Int ) : LinkClassValues = {
    import FlightType._
    val flightType = Computation.getFlightType(fromAirport, toAirport, distance, relationship)
    var adjustedDemand : Double = baseDemand.toDouble

    //adjust by features
//    fromAirport.getFeatures().foreach { feature =>
//      val adjustment = feature.demandAdjustment(baseDemand, passengerType, fromAirport.id, fromAirport, toAirport, flightType, relationship)
//      adjustedDemand += adjustment
//    }
//    toAirport.getFeatures().foreach { feature =>
//      val adjustment = feature.demandAdjustment(baseDemand, passengerType, toAirport.id, fromAirport, toAirport, flightType, relationship)
//      adjustedDemand += adjustment
//    }
    
    //more business and first going to international hubs      
    val internationalHubPercentBonus = {
      if (toAirport.hasFeature(AirportFeatureType.INTERNATIONAL_HUB)) 0.01
      else 0
    }

    val income = fromAirport.income

    val firstClassPercentage : Double = 
      if (flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL || flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_DOMESTIC ||  flightType == MEDIUM_HAUL_INTERNATIONAL) {
        if (income <= FIRST_CLASS_INCOME_MIN) {
          internationalHubPercentBonus
        } else if (income >= FIRST_CLASS_INCOME_MAX) {
          internationalHubPercentBonus * 2 + FIRST_CLASS_PERCENTAGE_MAX(passengerType)
        } else {
          internationalHubPercentBonus + FIRST_CLASS_PERCENTAGE_MAX(passengerType) * (income - FIRST_CLASS_INCOME_MIN) / (FIRST_CLASS_INCOME_MAX - FIRST_CLASS_INCOME_MIN)
        }
      } else {
        internationalHubPercentBonus
      }
    val businessClassPercentage : Double =
      if (income <= BUSINESS_CLASS_INCOME_MIN) {
        internationalHubPercentBonus
      } else if (income >= BUSINESS_CLASS_INCOME_MAX) {
        internationalHubPercentBonus * 3 + BUSINESS_CLASS_PERCENTAGE_MAX(passengerType)
      } else {
        internationalHubPercentBonus * 2 + BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * (income - BUSINESS_CLASS_INCOME_MIN) / (BUSINESS_CLASS_INCOME_MAX - BUSINESS_CLASS_INCOME_MIN)
      }
    val firstClassDemand = (adjustedDemand * firstClassPercentage).toInt
    val businessClassDemand = (adjustedDemand * businessClassPercentage).toInt
    val economyClassDemand = (adjustedDemand - firstClassDemand - businessClassDemand).toInt

//    LinkClassValues.getInstance(economyClassDemand, businessClassDemand, firstClassDemand)
    LinkClassValues.getInstance(baseDemand, 0, 0)
  }

//  def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, relationship : Int, passengerType : PassengerType.Value) : LinkClassValues = {
//    val distance = Computation.calculateDistance(fromAirport, toAirport)
//    if (fromAirport == toAirport || fromAirport.population == 0 || toAirport.population == 0 || distance <= MIN_DISTANCE) {
//      LinkClassValues.getInstance(0, 0, 0)
//    } else {
//      import FlightType._
//      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
//
//      //assumption - 1 passenger each week from airport with 1 million pop and 50k income will want to travel to an airport with 1 million pop at income level 25 for business
//      //             0.3 passenger in same condition for sightseeing (very low as it should be mainly driven by feature)
//      //we are using income level for to airport as destination income difference should have less impact on demand compared to origination airport (and income level is log(income))
//      val toAirportIncomeLevel = toAirport.incomeLevel
//
//      val lowIncomeThreshold = Country.LOW_INCOME_THRESHOLD + 10_000 //due to a bug in v2, we need to increase this a bit to avoid demand collapse in low income countries
//
//      val fromAirportAdjustedIncome : Double = if (fromAirport.income > Country.HIGH_INCOME_THRESHOLD) { //to make high income airport a little bit less overpowered
//        Country.HIGH_INCOME_THRESHOLD + (fromAirport.income - Country.HIGH_INCOME_THRESHOLD) / 3
//      } else if (fromAirport.income < lowIncomeThreshold) { //to make low income airport a bit stronger
//        val delta = lowIncomeThreshold - fromAirport.income
//        lowIncomeThreshold - delta * 0.3 //so a 0 income country will be boosted to 21000, a 10000 income country will be boosted to 24000
//      } else {
//        fromAirport.income
//      }
//
//      val fromAirportAdjustedPower =
//	if (fromAirport.population > 50000) fromAirportAdjustedIncome * fromAirport.population
//	else fromAirportAdjustedIncome * 50000
//
//      val ADJUST_FACTOR = 0.35
//
//      val population_adjusted =
//	if (toAirport.population.doubleValue > 50000) toAirport.population.doubleValue
//	else 50000
//
//      var baseDemand: Double = (fromAirportAdjustedPower.doubleValue() / 1000000 / 50000) * (population_adjusted / 1000000 * toAirportIncomeLevel / 10) * (passengerType match {
//      case PassengerType.BUSINESS => 6
//      case PassengerType.TOURIST | PassengerType.OLYMPICS => 1
//      }) * ADJUST_FACTOR
//
//      if (fromAirport.countryCode != toAirport.countryCode) {
//        //baseDemand = baseDemand *
//        val mutliplier =
//            if (relationship <= -3) 0
//            else if (relationship == -2) 0.1
//            else if (relationship == -1) 0.2
//            else if (relationship == 0) 0.5
//            else if (relationship == 1) 0.8
//            else if (relationship == 2) 1
//            else if (relationship == 3) 1.5
//            else 2 // >= 4
//        baseDemand = baseDemand * mutliplier
//      }
//
//
//
//      var adjustedDemand = baseDemand
//
//      //bonus for domestic and short-haul flight
//      adjustedDemand += baseDemand * (flightType match {
//        case SHORT_HAUL_DOMESTIC => 7.0
//        case MEDIUM_HAUL_DOMESTIC => 9.0
//        case LONG_HAUL_DOMESTIC => 7.0
//        case SHORT_HAUL_INTERNATIONAL => 1.5
//        case MEDIUM_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL => 0
//        case LONG_HAUL_INTERNATIONAL | MEDIUM_HAUL_INTERCONTINENTAL => -1.1
//        case LONG_HAUL_INTERCONTINENTAL => -1.4
//        case ULTRA_LONG_HAUL_INTERCONTINENTAL => -2.5
//      })
//
//      //adjustment : extra bonus to tourist supply for rich airports, up to double at every 10 income level increment
//      if ((passengerType == PassengerType.TOURIST || passengerType == PassengerType.OLYMPICS) && fromAirport.incomeLevel > 25) {
//        adjustedDemand += baseDemand * (((fromAirport.incomeLevel - 25).toDouble / 10) * 2)
//      }
//
//      //adjustments : these zones do not have good ground transport
//      if (fromAirport.zone == toAirport.zone) {
//        if (fromAirport.zone == "AF") {
//          adjustedDemand +=  baseDemand * 2
//        } else if (fromAirport.zone == "SA") {
//          adjustedDemand +=  baseDemand * 1
//        } else if (fromAirport.zone == "NA") {
//          adjustedDemand += baseDemand * 0.5
//        }
//      }
//
//      //they travel a lot
//      if (fromAirport.countryCode == "AU" || fromAirport.countryCode == "NZ") {
//        adjustedDemand += baseDemand * 1
//      }
//
//      //adjustments : China has very extensive highspeed rail network (1100km is Beijing to Shanghai)
//      if (fromAirport.countryCode == "CN" && toAirport.countryCode == "CN" && distance < 1100) {
//        adjustedDemand *= 0.6
//      }
//      //also interconnected by HSR / intercity rail
//      if (fromAirport.countryCode == "FR" || fromAirport.countryCode == "LU" || fromAirport.countryCode == "BE" || fromAirport.countryCode == "NL" || fromAirport.countryCode == "CH"){
//        if (toAirport.countryCode == "FR" || toAirport.countryCode == "LU" || toAirport.countryCode == "BE" || toAirport.countryCode == "NL" || toAirport.countryCode == "CH"){
//          adjustedDemand *= 0.3
//        }
//      }
//      if (fromAirport.countryCode == "DE" || fromAirport.countryCode == "AT" || fromAirport.countryCode == "CZ" || fromAirport.countryCode == "NL" || fromAirport.countryCode == "CH"){
//        if (toAirport.countryCode == "DE" || toAirport.countryCode == "AT" || toAirport.countryCode == "CZ" || toAirport.countryCode == "NL" || toAirport.countryCode == "CH"){
//          adjustedDemand *= 0.5
//        }
//      }
//      if (fromAirport.countryCode == "IT" && toAirport.countryCode == "IT" && distance < 500) {
//        adjustedDemand *= 0.2
//      }
//      if (fromAirport.countryCode == "ES" && toAirport.countryCode == "ES" && distance < 500) {
//        adjustedDemand *= 0.2
//      }
//      if (fromAirport.countryCode == "JP" && toAirport.countryCode == "JP" && distance < 500) {
//        adjustedDemand *= 0.4
//      }
//
//      //adjust by features
//      fromAirport.getFeatures().foreach { feature =>
//        val adjustment = feature.demandAdjustment(baseDemand, passengerType, fromAirport.id, fromAirport, toAirport, flightType, relationship)
//        adjustedDemand += adjustment
//      }
//      toAirport.getFeatures().foreach { feature =>
//        val adjustment = feature.demandAdjustment(baseDemand, passengerType, toAirport.id, fromAirport, toAirport, flightType, relationship)
//        adjustedDemand += adjustment
//      }
//
//      //adjustments : diminished demand for short routes (290 so LGA-BOS works haha)
//      if (adjustedDemand >= 75 && distance < 290) {
//        adjustedDemand = 75 + Math.pow(adjustedDemand - 100, 0.6)
//      }
//      if (adjustedDemand >= 75 && distance < 150) {
//        adjustedDemand = 75 + Math.pow(adjustedDemand - 100, 0.3)
//      }
//
//      if ( adjustedDemand < 0) {
//        adjustedDemand = 0
//      }
//
//      //compute demand composition. depends on from airport income
//      val income = fromAirport.income
//
//      val firstClassPercentage : Double =
//        if (flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL || flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_INTERCONTINENTAL || flightType == LONG_HAUL_DOMESTIC || flightType == MEDIUM_HAUL_INTERCONTINENTAL || flightType == MEDIUM_HAUL_INTERNATIONAL) {
//          if (income <= FIRST_CLASS_INCOME_MIN) {
//            0
//          } else if (income >= FIRST_CLASS_INCOME_MAX) {
//            FIRST_CLASS_PERCENTAGE_MAX(passengerType)
//          } else {
//            FIRST_CLASS_PERCENTAGE_MAX(passengerType) * (income - FIRST_CLASS_INCOME_MIN) / (FIRST_CLASS_INCOME_MAX - FIRST_CLASS_INCOME_MIN)
//          }
//        } else {
//         0
//        }
//      val businessClassPercentage : Double =
//        if (income <= BUSINESS_CLASS_INCOME_MIN) {
//          0
//        } else if (income >= BUSINESS_CLASS_INCOME_MAX) {
//          BUSINESS_CLASS_PERCENTAGE_MAX(passengerType)
//        } else {
//          BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * (income - BUSINESS_CLASS_INCOME_MIN) / (BUSINESS_CLASS_INCOME_MAX - BUSINESS_CLASS_INCOME_MIN)
//        }
//      var firstClassDemand = (adjustedDemand * firstClassPercentage).toInt
//      var businessClassDemand = (adjustedDemand * businessClassPercentage).toInt
//      val economyClassDemand = (adjustedDemand - firstClassDemand - businessClassDemand).toInt
//
//      //add extra business and first class demand from lounge for major airports
//      if (fromAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT && toAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT) {
//        firstClassDemand = (firstClassDemand * 2.5).toInt
//        businessClassDemand = (businessClassDemand * 2.5).toInt
//      }
//
//      //add extra business and first class for all high population airports to international features
//      //adding later to get around income calculation
//      if (fromAirport.population >= 500000 && distance > 250) {
//        toAirport.getFeatures().foreach { feature =>
//          if ( feature.featureType == AirportFeatureType.INTERNATIONAL_HUB ) {
//            firstClassDemand += (fromAirport.population / 500000 * feature.strengthFactor).toInt
//            businessClassDemand += (fromAirport.population / 200000 * feature.strengthFactor).toInt
//          }
//        }
//      }
//
//      LinkClassValues.getInstance(economyClassDemand, businessClassDemand, firstClassDemand)
//    }
//  }

  def generateEventDemand(cycle : Int, airports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])] = {
    val eventDemand = ListBuffer[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
    EventSource.loadEvents().filter(_.isActive(cycle)).foreach { event =>
      event match {
        case olympics : Olympics => eventDemand.appendAll(generateOlympicsDemand(cycle, olympics, airports))
        case _ => //
      }

    }
    eventDemand.toList
  }


  val OLYMPICS_DEMAND_BASE = 50000
  def generateOlympicsDemand(cycle: Int, olympics : Olympics, airports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]  = {
    if (olympics.currentYear(cycle) == 4) { //only has special demand on 4th year
      val week = (cycle - olympics.startCycle) % Olympics.WEEKS_PER_YEAR //which week is this
      val demandMultiplier = Olympics.getDemandMultiplier(week)
      Olympics.getSelectedAirport(olympics.id) match {
        case Some(selectedAirport) => generateOlympicsDemand(cycle, demandMultiplier, Olympics.getAffectedAirport(olympics.id, selectedAirport), airports)
        case None => List.empty
      }
    } else {
      List.empty
    }

  }

  def generateOlympicsDemand(cycle: Int, demandMultiplier : Int, olympicsAirports : List[Airport], allAirports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]  = {
    val totalDemand = OLYMPICS_DEMAND_BASE * demandMultiplier

    val countryRelationships = CountrySource.getCountryMutualRelationships()
    //use existing logic, just scale the total back to totalDemand at the end
    val unscaledDemands = ListBuffer[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
    val otherAirports = allAirports.filter(airport => !olympicsAirports.map(_.id).contains(airport.id))

    otherAirports.foreach { airport =>
      val unscaledDemandsOfThisFromAirport = ListBuffer[(Airport, (PassengerType.Value, LinkClassValues))]()
      val fromAirport = airport
      olympicsAirports.foreach {  olympicsAirport =>
        val toAirport = olympicsAirport
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
        val affinity = Computation.calculateAffinityValue(fromAirport.zone, toAirport.zone, relationship)
        //        val computedDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.OLYMPICS)
        val demand = computeBaseDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
        val computedDemand = computeClassDemandBetweenAirports(fromAirport, toAirport, relationship, distance, PassengerType.OLYMPICS, (demand * 0.3).toInt)
          if (computedDemand.total > 0) {
          unscaledDemandsOfThisFromAirport.append((toAirport, (PassengerType.OLYMPICS, computedDemand)))
        }
      }
      unscaledDemands.append((fromAirport, unscaledDemandsOfThisFromAirport.toList))
    }

    //now scale all the demands based on the totalDemand
    val unscaledTotalDemands = unscaledDemands.map {
      case (toAirport, unscaledDemandsOfThisToAirport) => unscaledDemandsOfThisToAirport.map {
        case (fromAirport, (passengerType, demand)) => demand.total
      }.sum
    }.sum
    val multiplier = totalDemand.toDouble / unscaledTotalDemands
    println(s"olympics scale multiplier is $multiplier")
    val scaledDemands = unscaledDemands.map {
      case (toAirport, unscaledDemandsOfThisToAirport) =>
        (toAirport, unscaledDemandsOfThisToAirport.map {
          case (fromAirport, (passengerType, unscaledDemand)) =>
            (fromAirport, (passengerType, unscaledDemand * multiplier))
        })
    }.toList

    scaledDemands

  }

//    def getFlightPreferencePool(homeAirport : Airport, paxType : PassengerType) : FlightPreferencePool = {
//      val flightPreferences = ListBuffer[(FlightPreference, Int)]()
//
//
//    }


  def getFlightPreferencePoolOnAirport(homeAirport : Airport) : FlightPreferencePool = {
    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    
    val budgetTravelerMultiplier =
      if (homeAirport.income < Country.LOW_INCOME_THRESHOLD / 2) {
        3
      } else if (homeAirport.income < Country.LOW_INCOME_THRESHOLD) {
    	  2
  	  } else {
        1
      }

      /**
       * Pax breakdown
       * 
       * example poor country economy
       * 40 denominator
       * 24 budget 60%
       * 6 brand 15%
       * 2 simple 5%
       * 5 comprehensive 13%
       * 3 swift 8%
       * 
       * rich country economy
       * 20 denominator
       * 8 budget 40%
       * 2 brand 10%
       * 2 simple 10%
       * 5 comprehensive 25%
       * 3 swift 15%
       **/ 
    
    //ECONOMY prefs
    for (i <- 0 until budgetTravelerMultiplier) {
      //Brand
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.4), 1))
      //Budget
      flightPreferences.append((SimplePreference(homeAirport, 1.2, ECONOMY), 3)) //quite sensitive to price
      flightPreferences.append((SimplePreference(homeAirport, 1.4, ECONOMY), 2))
      flightPreferences.append((SimplePreference(homeAirport, 1.6, ECONOMY), 3)) //very sensitive to price
    }
    
    //Simple
    flightPreferences.append((SimplePreference(homeAirport, 0.7, ECONOMY), 2))
    //Swift
    flightPreferences.append((SpeedPreference(homeAirport, ECONOMY), 3))
    //Comprehensive
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 5))    
    
    /**
     * BUSINESS prefs
     * 10 demoninator
     * 40% swift
     * 20% comprehensive
     * 20% brand
     * 20% elite
     **/
    for (i <- 0 until 2) { //bit more randomness - set variation per group
      flightPreferences.append((SpeedPreference(homeAirport, BUSINESS), 4))
      //Comprehensive
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 0.8), 2))
      //Brand
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1.4), 2))
      //Elite
      flightPreferences.append((ElitePreference(homeAirport, BUSINESS, loungeLevelRequired = 1), 1))
      flightPreferences.append((ElitePreference(homeAirport, BUSINESS, loungeLevelRequired = 2), 1))
    }
    
    //FIRST prefs 
    flightPreferences.append((SpeedPreference(homeAirport, FIRST), 1))
    //Brand
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1.2), 2))
    //Elite
    flightPreferences.append((ElitePreference(homeAirport, FIRST, loungeLevelRequired = 1), 1))
    flightPreferences.append((ElitePreference(homeAirport, FIRST, loungeLevelRequired = 2), 1))
    flightPreferences.append((ElitePreference(homeAirport, FIRST, loungeLevelRequired = 3), 1))
    
    
    new FlightPreferencePool(flightPreferences.toList)
  }
  
  sealed case class Demand(businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}
