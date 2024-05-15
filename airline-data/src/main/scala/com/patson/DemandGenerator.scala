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
  private[this] val FIRST_CLASS_PERCENTAGE_MAX: Map[PassengerType.Value, Double] = Map(PassengerType.TRAVELER -> 0, PassengerType.BUSINESS -> 0.09, PassengerType.TOURIST -> 0, PassengerType.ELITE -> 1, PassengerType.OLYMPICS -> 0)
  private[this] val BUSINESS_CLASS_INCOME_FLOOR = 8_000 //effectively boost low-income
  private[this] val BUSINESS_CLASS_INCOME_MAX = 135_000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX: Map[PassengerType.Value, Double] = Map(PassengerType.TRAVELER -> 0.15, PassengerType.BUSINESS -> 0.42, PassengerType.TOURIST -> 0.07, PassengerType.ELITE -> 0, PassengerType.OLYMPICS -> 0.15)
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

  def computeBaseDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, affinity : Int, distance : Int ) : Demand = {
    import FlightType._
    val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
    val hasFirstClass = (flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL || flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_DOMESTIC || flightType == MEDIUM_HAUL_INTERNATIONAL)
    val fromPopIncomeAdjusted = if (fromAirport.popMiddleIncome > 0) fromAirport.popMiddleIncome else 1

    val incomeRatio = math.min(1.5, (toAirport.income.toDouble / fromAirport.income.toDouble + 1) / 2.0)

    val distanceReducerExponent: Double =
      if (distance < 350) {
        distance.toDouble / 350
      } else if (distance > 5000) { //about long-distance or longer
        1.01 - distance.toDouble / (15000 * (affinity + 2)) //affinity affects perceived distance
      } else if (distance > 2000) { //bit less than medium-distance
        1.11 - distance.toDouble / 20000 //i.e 0.1, and then adding a .01 boost
      } else 1

    //domestic/foreign/affinity relation multiplier
    val airportAffinityMutliplier : Double =
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
      } else if (fromAirport.countryCode == "JP" && toAirport.countryCode == "JP" && distance < 500) {
        0.4 //also interconnected by HSR / intercity rail
      } else if (fromAirport.countryCode == "FR" && distance < 550 && toAirport.countryCode != "GB") {
        0.3
      } else if (fromAirport.countryCode == "IT" && distance < 500) {
        0.5
      } else if (fromAirport.zone.contains("EU") && distance < 260) {
        0.6
      } else 1.0

    val baseDemand : Double = specialCountryModifier * airportAffinityMutliplier * fromPopIncomeAdjusted * toAirport.population.toDouble / 225_000 / 225_000
    var demand = (Math.pow(baseDemand, distanceReducerExponent)).toInt

    //modeling provincial travel dynamics, but not for tourists
    val maxBonus = 2.0
    val populationRatio = if (distance < 2500 && toAirport.population > fromPopIncomeAdjusted) {
      math.min(maxBonus, math.pow(toAirport.population / fromPopIncomeAdjusted.toDouble, .125))
    } else {
      1.0
    }
    //lower demand to poor places, but not for tourists
    val toIncomeAdjust = Math.min(1.0, toAirport.income.toDouble / 20_000)
    //people migrate from poor to rich, affects travelers only
    //(commented out as isn't quite intuitive)
//    val travelerIncomeRatio = math.min(1.5, (toAirport.income.toDouble / fromAirport.income.toDouble + 1) / 2.0)

    val demands = Map((PassengerType.TRAVELER -> demand * 0.7 * populationRatio * toIncomeAdjust), (PassengerType.BUSINESS -> demand * 0.2 * populationRatio * toIncomeAdjust), (PassengerType.TOURIST -> demand * 0.1))

    val featureAdjustedDemands = demands.map { case (passengerType, demand) =>
      val fromAdjustments = fromAirport.getFeatures().map(feature => feature.demandAdjustment(demand, passengerType, fromAirport.id, fromAirport, toAirport, flightType, affinity, distance))
      val toAdjustments = toAirport.getFeatures().map(feature => feature.demandAdjustment(demand, passengerType, toAirport.id, fromAirport, toAirport, flightType, affinity, distance))
      (passengerType, (fromAdjustments.sum + toAdjustments.sum + demand).toDouble)
//      (passengerType, (fromAdjustments.sum + demand).toDouble)
    }.toMap

    Demand(
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.TRAVELER).getOrElse(0.0), fromAirport.income, PassengerType.TRAVELER, hasFirstClass),
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.BUSINESS).getOrElse(0.0), fromAirport.income, PassengerType.BUSINESS, hasFirstClass),
      computeClassCompositionFromIncome(featureAdjustedDemands.get(PassengerType.TOURIST).getOrElse(0.0), fromAirport.income, PassengerType.TOURIST, hasFirstClass)
    )
  }

  def computeClassCompositionFromIncome(demand: Double, income: Int, passengerType: PassengerType.Value, hasFirstClass: Boolean) : LinkClassValues = {

    val firstClassDemand = if (hasFirstClass) demand * FIRST_CLASS_PERCENTAGE_MAX(passengerType) * income  / FIRST_CLASS_INCOME_MAX else 0
    val businessClassDemand = demand * BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * income  / (income.toDouble + BUSINESS_CLASS_INCOME_FLOOR)
    //adding cutoffs to reduce the tail and have fewer passenger groups to calculate
    val businessClassCutoff = if (businessClassDemand > 1) businessClassDemand.toInt else 0
    val economyClassDemand = demand - firstClassDemand - businessClassCutoff

    LinkClassValues.getInstance(economyClassDemand.toInt, businessClassCutoff.toInt, firstClassDemand.toInt)
  }

  val ELITE_MIN_GROUP_SIZE = 7
  val ELITE_MAX_GROUP_SIZE = 11

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
        //        val computedDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.OLYMPICS)
        val demand = computeBaseDemandBetweenAirports(fromAirport, toAirport, affinity, distance)
//        val computedDemand = computeClassDemandBetweenAirports(fromAirport, toAirport, relationship, distance, PassengerType.OLYMPICS, (demand * 0.3).toInt)
//          if (computedDemand.total > 0) {
//          unscaledDemandsOfThisFromAirport.append((toAirport, (PassengerType.OLYMPICS, computedDemand)))
//        }
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
    val flightPreferences = Map(
      PassengerType.BUSINESS -> List( //is default, i.e. also elite & olympic
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, 0.9, ECONOMY, loungeLevelRequired = 0), 1),
        (LastMinutePreference(homeAirport, 0.75, ECONOMY, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 2, loyaltyRatio = 1.15), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 3, loyaltyRatio = 1.25), 1),
        (LastMinutePreference(homeAirport, 0.85, BUSINESS, loungeLevelRequired = 0), 1),
        (LastMinutePreference(homeAirport, 0.7, BUSINESS, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 2), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 3, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, 0.8, FIRST, loungeLevelRequired = 1), 1),
        (LastMinutePreference(homeAirport, 0.65, FIRST, loungeLevelRequired = 0), 1),
      ),
      PassengerType.TOURIST -> List(
        //20% want a 30% discount
        (DealPreference(homeAirport, 1.1, ECONOMY), 1),
        (DealPreference(homeAirport, 1.3, ECONOMY), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (LastMinutePreference(homeAirport, 1.25, ECONOMY, loungeLevelRequired = 0), 1),
        (DealPreference(homeAirport, 1.1, BUSINESS), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1), 1),
        (LastMinutePreference(homeAirport, 1.2, BUSINESS, loungeLevelRequired = 1), 1),
//        (DealPreference(homeAirport, 1.2, FIRST), 1),
      ),
      PassengerType.TRAVELER -> List(
        //20% want a 30% discount
        (DealPreference(homeAirport, 1.1, ECONOMY), 1),
        (DealPreference(homeAirport, 1.3, ECONOMY), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1),
        (LastMinutePreference(homeAirport, 0.9, ECONOMY, loungeLevelRequired = 0), 1),
        (DealPreference(homeAirport, 1.1, BUSINESS), 1),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1), 2),
        (AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1, loyaltyRatio = 1.2), 1),
        (LastMinutePreference(homeAirport, 0.85, BUSINESS, loungeLevelRequired = 0), 1),
      )
    )

//    } else if (paxType == PassengerType.OLYMPICS) {
////      flightPreferences.append((DealPreference(homeAirport, 1.05, ECONOMY), 2))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 1))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 1))
////      flightPreferences.append((LastMinutePreference(homeAirport, 1.05, ECONOMY, loungeLevelRequired = 0), 1))
////      flightPreferences.append((DealPreference(homeAirport, 1.0, BUSINESS), 1))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 0), 1))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1, loyaltyRatio = 1.1), 1))
////      flightPreferences.append((LastMinutePreference(homeAirport, 1.05, BUSINESS, loungeLevelRequired = 0), 1))
////      flightPreferences.append((DealPreference(homeAirport, 1.0, FIRST), 1))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 0), 1))
////      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1))
//    } else {

    new FlightPreferencePool(flightPreferences)
  }

  sealed case class Demand(travelerDemand: LinkClassValues, businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}
