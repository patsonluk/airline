package com.patson

import java.util.{ArrayList, Collections}
import com.patson.data.{AirportSource, CountrySource, DestinationSource, EventSource}
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

  private[this] val FIRST_CLASS_INCOME_MAX = 135_000
  private[this] val FIRST_CLASS_PERCENTAGE_MAX: Map[PassengerType.Value, Double] = Map(PassengerType.TRAVELER -> 0, PassengerType.BUSINESS -> 0.14, PassengerType.TOURIST -> 0, PassengerType.ELITE -> 1, PassengerType.OLYMPICS -> 0)
  private[this] val BUSINESS_CLASS_INCOME_FLOOR = 8_000 //effectively boost low-income
  private[this] val BUSINESS_CLASS_INCOME_MAX = 135_000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX: Map[PassengerType.Value, Double] = Map(PassengerType.TRAVELER -> 0.16, PassengerType.BUSINESS -> 0.49, PassengerType.TOURIST -> 0.09, PassengerType.ELITE -> 0, PassengerType.OLYMPICS -> 0.15)
  private[this] val DISCOUNT_CLASS_PERCENTAGE_MAX: Map[PassengerType.Value, Double] = Map(PassengerType.TRAVELER -> 0.34, PassengerType.BUSINESS -> 0.0, PassengerType.TOURIST -> 0.76, PassengerType.ELITE -> 0, PassengerType.OLYMPICS -> 0.15)
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
	    val demandList = Collections.synchronizedList(new ArrayList[(Airport, (PassengerType.Value, LinkClassValues))]())

      airports.par.foreach { toAirport =>
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        if (fromAirport != toAirport && fromAirport.population != 0 && toAirport.population != 0 && distance >= MIN_DISTANCE) {
          val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
          val affinity = Computation.calculateAffinityValue(fromAirport.zone, toAirport.zone, relationship)

          val demand = computeBaseDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
          if (demand.travelerDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.TRAVELER, demand.travelerDemand)))
          }
          if (demand.businessDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.BUSINESS, demand.businessDemand)))
          }
          if (demand.touristDemand.total > 0) {
            demandList.add((toAirport, (PassengerType.TOURIST, demand.touristDemand)))
          }
        }
      }
	    allDemands.add((fromAirport, demandList.asScala.toList))
  }

    val allDemandsAsScala = allDemands.asScala

    allDemandsAsScala.appendAll(generateEventDemand(cycle, airports))

    println("generating elite demand...")
    val eliteDemand = generateEliteDemand(airports)
    allDemandsAsScala.appendAll(eliteDemand)
    println(s"generated ${eliteDemand.length} elite demand groups")

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
                  allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(passengerType, linkClass, fromAirport, toAirport), passengerType), toAirport, demandChunkSize))
                  remainingDemand -= demandChunkSize
                  demandChunkSize = baseDemandChunkSize + ThreadLocalRandom.current().nextInt(baseDemandChunkSize)
                }
                allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(passengerType, linkClass, fromAirport, toAirport), passengerType), toAirport, remainingDemand)) // don't forget the last chunk
              }
            }
        }

	  }

    allDemandChunks.toList
  }

  def computeBaseDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, affinity : Int, distance : Int) : Demand = {
    import FlightType._
    val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
    val hasFirstClass = (flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL || flightType == ULTRA_LONG_HAUL_DOMESTIC || flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_DOMESTIC || flightType == MEDIUM_HAUL_INTERNATIONAL)
    val fromPopIncomeAdjusted = if (fromAirport.popMiddleIncome > 0) fromAirport.popMiddleIncome else 1
    val demand = computeRawDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, affinity : Int, distance : Int)

    //modeling provincial travel dynamics where folks go from small city to big city, but not for tourists
    val maxBonus = 2.0
    val populationRatio = if (distance < 2500 && toAirport.population > fromPopIncomeAdjusted) {
      math.min(maxBonus, math.pow(toAirport.population / fromPopIncomeAdjusted.toDouble, .125))
    } else {
      1.0
    }
    //lower demand to poor places, but not for tourists
    val toIncomeAdjust = Math.min(1.0, toAirport.income.toDouble / 20_000)
    //however squash tourism if extremely poor
    val toTourismIncomeAdjust = Math.min(1.0, toAirport.income.toDouble / 6000)

    val percentTraveler = Math.min(0.7, fromAirport.income.toDouble / 40_000)

    val demands = Map((PassengerType.TRAVELER -> demand * percentTraveler * populationRatio * toIncomeAdjust), (PassengerType.BUSINESS -> demand * (1 - percentTraveler - 0.1) * toIncomeAdjust), (PassengerType.TOURIST -> demand * 0.1 * toTourismIncomeAdjust))

    val featureAdjustedDemands = demands.map { case (passengerType, demand) =>
      val fromAdjustments = fromAirport.getFeatures().map(feature => feature.demandAdjustment(demand, passengerType, fromAirport.id, fromAirport, toAirport, flightType, affinity, distance))
      val toAdjustments = toAirport.getFeatures().map(feature => feature.demandAdjustment(demand, passengerType, toAirport.id, fromAirport, toAirport, flightType, affinity, distance))
      (passengerType, (fromAdjustments.sum + toAdjustments.sum + demand).toDouble)
    }.toMap

    Demand(
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.TRAVELER).getOrElse(0.0), fromAirport.income, PassengerType.TRAVELER, hasFirstClass),
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.BUSINESS).getOrElse(0.0), fromAirport.income, PassengerType.BUSINESS, hasFirstClass),
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.TOURIST).getOrElse(0.0), fromAirport.income, PassengerType.TOURIST, hasFirstClass)
    )
  }

  def computeRawDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, affinity : Int, distance : Int) : Int = {
    val fromPopIncomeAdjusted = if (fromAirport.popMiddleIncome > 0) fromAirport.popMiddleIncome else 1
    val incomeRatio = math.min(1.5, (toAirport.income.toDouble / fromAirport.income.toDouble + 1) / 2.0)

    val distanceReducerExponent: Double =
      if (distance < 350 && !List("BS", "TC", "VI", "VG", "GR", "CY", "CO").contains(fromAirport.countryCode)) {
        distance.toDouble / 350
      } else if (distance > 5000) {
        1.0 - distance.toDouble / 35000 * (1 - affinity.toDouble / 10.0) //affinity affects perceived distance
      } else if (distance > 2000) { //bit less than medium-distance, with a 0.01 boost
        1.11 - distance.toDouble / 20000 * (1 - affinity.toDouble / 20.0) //affinity affects perceived distance
      } else 1

    //domestic/foreign/affinity relation multiplier
    val airportAffinityMutliplier: Double =
      if (affinity >= 5) (affinity - 5) * 0.1 + 1 //domestic+
      else if (affinity < 0) 0.025
      else affinity * 0.1 + 0.05

    val specialCountryModifier =
      if (fromAirport.countryCode == "AU" || fromAirport.countryCode == "NZ") {
        9.0 //they travel a lot; difficult to model
      } else if (fromAirport.countryCode == "NO" && toAirport.countryCode == "NO") {
        4.0 //very busy domestic routes
      } else if (List("NO", "IS", "FO", "GL", "GR", "CY", "FJ", "KR").contains(fromAirport.countryCode)) {
        2.0 //very high per captia flights https://ourworldindata.org/grapher/air-trips-per-capita
      } else if (List("SE", "GB", "CL", "BS").contains(fromAirport.countryCode)) {
        1.5 // high per captia flights
      } else if (List("CD", "CG", "CV", "CI", "GN", "GW", "LR", "ML", "MR", "NE", "SD", "SO", "SS", "TD", "TG").contains(fromAirport.countryCode)) {
        4.0 //very poor roads but unstable governance
      } else if (List("AO", "BI", "BJ", "BW", "CM", "CV", "DJ", "ET", "GA", "GH", "GM", "GQ", "KE", "KM", "LS", "MG", "MU", "MW", "MZ", "NA", "NG", "RW", "SC", "SL", "SN", "ST", "SZ", "TZ", "UG", "ZA", "ZM", "ZW").contains(fromAirport.countryCode)) {
        6.0 //very poor roads
      } else if (fromAirport.countryCode == "IN" && toAirport.countryCode == "IN") {
        0.8 //pops are just very large
      } else if (fromAirport.countryCode == "CN" && toAirport.countryCode == "CN") {
        if(distance < 900) {
          0.7 //China has a very extensive highspeed rail network, pops are just very large
        } else {
          0.85
        }
      } else if (fromAirport.countryCode == "JP" && toAirport.countryCode == "JP" && distance < 500) {
        0.4 //also interconnected by HSR / intercity rail
      } else if (fromAirport.countryCode == "FR" && distance < 550 && toAirport.countryCode != "GB") {
        0.2
      } else if (fromAirport.countryCode == "IT" && distance < 500) {
        0.5
      } else if (distance < 260 && fromAirport.zone.contains("EU")) {
        0.6
      } else 1.0

    val baseDemand : Double = specialCountryModifier * airportAffinityMutliplier * fromPopIncomeAdjusted * toAirport.population.toDouble / 250_000 / 250_000
    (Math.pow(baseDemand, distanceReducerExponent)).toInt
  }

  def computeClassCompositionFromIncome(demand: Double, income: Int, passengerType: PassengerType.Value, hasFirstClass: Boolean) : LinkClassValues = {
    val firstClassDemand = if (hasFirstClass) {
        if (income > FIRST_CLASS_INCOME_MAX) {
          demand * FIRST_CLASS_PERCENTAGE_MAX(passengerType)
        } else {
          demand * FIRST_CLASS_PERCENTAGE_MAX(passengerType) * income / FIRST_CLASS_INCOME_MAX
        }
      } else {
        0
      }
    val businessClassDemand = if (income > BUSINESS_CLASS_INCOME_MAX) {
        demand * BUSINESS_CLASS_PERCENTAGE_MAX(passengerType)
      } else {
        demand * BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * income / BUSINESS_CLASS_INCOME_MAX
      }
    val discountClassDemand = demand * DISCOUNT_CLASS_PERCENTAGE_MAX(passengerType) * (1 - Math.min(income.toDouble / 30_000, 0.5))
    //adding cutoffs to reduce the tail and have fewer passenger groups to calculate
    val businessClassCutoff = if (businessClassDemand > 1) businessClassDemand else 0
    val discountClassCutoff = if (discountClassDemand > 6) discountClassDemand else 0

    val economyClassDemand = demand - firstClassDemand - businessClassCutoff - discountClassCutoff
    LinkClassValues.getInstance(economyClassDemand.toInt, businessClassCutoff.toInt, firstClassDemand.toInt, discountClassCutoff.toInt)
  }

  val ELITE_MIN_GROUP_SIZE = 6
  val ELITE_MAX_GROUP_SIZE = 10

  def generateEliteDemand(airports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])] = {
    val eliteDemands = new ArrayList[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
    val destinationList = DestinationSource.loadAllEliteDestinations()
    val eliteAirports = airports.filter(_.popElite > 0)

    eliteAirports.par.foreach { fromAirport =>
      val demandList = Collections.synchronizedList(new ArrayList[(Airport, (PassengerType.Value, LinkClassValues))]())
      val groupSize = ThreadLocalRandom.current().nextInt(ELITE_MIN_GROUP_SIZE, ELITE_MAX_GROUP_SIZE)
      val closeDestinations = destinationList.filter { destination =>
        val distance = Computation.calculateDistance(fromAirport, destination.airport)
        distance >= 100 && distance <= 1200
      }
      val farAwayDestinations = destinationList.filter { destination =>
        val distance = Computation.calculateDistance(fromAirport, destination.airport)
        distance > 1200
      }

      var numberDestinations = Math.ceil(fromAirport.popElite / groupSize.toDouble).toInt

      while (numberDestinations >= 0) {
        val destination = if (numberDestinations % 2 == 1 && closeDestinations.length > 5) {
          closeDestinations(ThreadLocalRandom.current().nextInt(closeDestinations.length))
        } else {
          farAwayDestinations(ThreadLocalRandom.current().nextInt(farAwayDestinations.length))
        }
        numberDestinations -= 1
        demandList.add((destination.airport, (PassengerType.ELITE, LinkClassValues(0, 0, groupSize))))
      }
      eliteDemands.add((fromAirport, demandList.asScala.toList))
    }
    eliteDemands.asScala.toList
  }

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
        val baseDemand = computeRawDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
        val computedDemand = computeClassCompositionFromIncome(baseDemand, fromAirport.income, PassengerType.OLYMPICS, true)
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

  def getFlightPreferencePoolOnAirport(homeAirport : Airport) : FlightPreferencePool = {
    //price mods are also used on frontend for demand estimation
    //modding price by pax type
    val touristMod = PassengerType.priceAdjust(PassengerType.TOURIST)
    val travelerMod = PassengerType.priceAdjust(PassengerType.TRAVELER)
    val defaultMod = PassengerType.priceAdjust(PassengerType.BUSINESS)
    //modding price (sometimes) by class
    val discountPlus = 0.15
    val economyPlus = 0.05
    val businessPlus = 0.1
    val firstPlus = 0.1
    val flightPreferences = Map(
      PassengerType.BUSINESS -> List( //is default, i.e. also elite & olympic
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, defaultMod), 3),
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, defaultMod + discountPlus), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, defaultMod, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, defaultMod, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, defaultMod, loungeLevelRequired = 0, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, ECONOMY, defaultMod + economyPlus + 0.05, loungeLevelRequired = 0), 1),
        (LastMinutePreference(homeAirport, ECONOMY, defaultMod + economyPlus + 0.15, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, defaultMod, loungeLevelRequired = 1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, defaultMod, loungeLevelRequired = 2, loyaltyRatio = 1.15), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, defaultMod, loungeLevelRequired = 2, loyaltyRatio = 1.25), 1),
        (LastMinutePreference(homeAirport, BUSINESS, defaultMod + businessPlus, loungeLevelRequired = 0), 1),
        (LastMinutePreference(homeAirport, BUSINESS, defaultMod + businessPlus + 0.16, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, defaultMod, loungeLevelRequired = 2), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, defaultMod, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, defaultMod + firstPlus, loungeLevelRequired = 3, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, FIRST, defaultMod + firstPlus, loungeLevelRequired = 1), 1),
        (LastMinutePreference(homeAirport, FIRST, defaultMod + firstPlus + 0.2, loungeLevelRequired = 0), 1),
      ),
      PassengerType.TOURIST -> List(
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, touristMod), 3),
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, touristMod + discountPlus), 2),
        (DealPreference(homeAirport, ECONOMY, touristMod), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, touristMod + economyPlus, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, touristMod + economyPlus, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (LastMinutePreference(homeAirport, ECONOMY, touristMod - 0.01, loungeLevelRequired = 0), 2),
        (DealPreference(homeAirport, BUSINESS, touristMod), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, touristMod + businessPlus, loungeLevelRequired = 1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, touristMod + businessPlus + 0.16, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1),
        (LastMinutePreference(homeAirport, BUSINESS, touristMod - 0.01, loungeLevelRequired = 1), 1),
        (DealPreference(homeAirport, FIRST, touristMod), 1),
      ),
      PassengerType.TRAVELER -> List(
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, travelerMod), 3),
        (DealPreference(homeAirport, DISCOUNT_ECONOMY, travelerMod + discountPlus), 2),
        (DealPreference(homeAirport, ECONOMY, travelerMod), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, travelerMod, loungeLevelRequired = 0), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, travelerMod + economyPlus, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (LastMinutePreference(homeAirport, ECONOMY, travelerMod + economyPlus + 0.06, loungeLevelRequired = 0), 1),
        (DealPreference(homeAirport, BUSINESS, travelerMod), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, travelerMod, loungeLevelRequired = 1), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, travelerMod + businessPlus, loungeLevelRequired = 1, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, BUSINESS, travelerMod + businessPlus + 0.16, loungeLevelRequired = 0), 1),
      )
    )

    new FlightPreferencePool(flightPreferences)
  }

  sealed case class Demand(travelerDemand: LinkClassValues, businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}
