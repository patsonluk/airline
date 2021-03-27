package com.patson

import com.patson.model._
import com.patson.data._

import scala.collection.mutable._
import scala.collection.{immutable, mutable}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane.{Airplane, LinkAssignments}
import com.patson.model.event.Olympics

import scala.util.Random
import com.patson.model.oil.OilPrice

object LinkSimulation {


  private val FUEL_UNIT_COST = OilPrice.DEFAULT_UNIT_COST //for easier flight monitoring, let's make it the default unit price here
  private val CREW_UNIT_COST = 12 //for now...
  
  private[this] val VIP_COUNT = 5



  def linkSimulation(cycle: Int) : (List[LinkConsumptionDetails], scala.collection.immutable.Map[Lounge, LoungeConsumptionDetails], immutable.Map[(PassengerGroup, Airport, Route), Int]) = {
    println("Loading all links")
    val links = LinkSource.loadAllLinks(LinkSource.FULL_LOAD)
    val flightLinks = links.filter(_.transportType == TransportType.FLIGHT).map(_.asInstanceOf[Link])
    println("Finished loading all links")

    //val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)'
    val demand = DemandGenerator.computeDemand(cycle)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

    simulateLinkError(flightLinks)
    
    val (consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int], missedPassengerResult : immutable.Map[(PassengerGroup, Airport), Int])= PassengerSimulation.passengerConsume(demand, links)
    
    //generate statistic 
    println("Generating stats")
    val linkStatistics = generateLinkStatistics(consumptionResult, cycle)
    println("Saving generated stats to DB")
    LinkStatisticsSource.deleteLinkStatisticsBeforeCycle(cycle - 5)
    LinkStatisticsSource.saveLinkStatistics(linkStatistics)
    
    //generate country market share
    println("Generating country market share")
    val countryMarketShares = generateCountryMarketShares(consumptionResult)
    println("Saving country market share to DB")
    CountrySource.saveMarketShares(countryMarketShares)

    //generate Olympics stats
    EventSource.loadEvents().filter(_.isActive(cycle)).foreach { event =>
      event match {
        case olympics : Olympics =>
          println("Generating Olympics stats")
          val olympicsConsumptions = consumptionResult.filter {
            case ((passengerGroup, _, _), _) => passengerGroup.passengerType == PassengerType.OLYMPICS
          }
          val missedOlympicsPassengers = missedPassengerResult.filter {
            case ((passengerGroup, _), _) => passengerGroup.passengerType == PassengerType.OLYMPICS
          }
          val olympicsCountryStats = generateOlympicsCountryStats(cycle, olympicsConsumptions, missedOlympicsPassengers)
          EventSource.saveOlympicsCountryStats(olympics.id, olympicsCountryStats)
          val olympicsAirlineStats = generateOlympicsAirlineStats(cycle, olympicsConsumptions)
          EventSource.saveOlympicsAirlineStats(olympics.id, olympicsAirlineStats)
          println("Generated olympics country stats")
        case _ => //
      }

    }

    
    //save all consumptions
    val startTime = System.currentTimeMillis()
    println("Saving " + consumptionResult.size +  " consumptions")
    ConsumptionHistorySource.updateConsumptions(consumptionResult)
    val endTime = System.currentTimeMillis()
    println(s"Saved all consumptions. Took ${endTime - startTime} millisecs")


    //generate link history
//    println("Generating link history")
//    val linkHistory = generateLinkHistory(consumptionResult)
//    println("Saving " + linkHistory.size + " generated history to DB")
//    LinkHistorySource.updateLinkHistory(linkHistory)
    
//    println("Generating VIP")
//    val vipRoutes = generateVipRoutes(consumptionResult)
//    RouteHistorySource.deleteVipRouteBeforeCycle(cycle)
//    RouteHistorySource.saveVipRoutes(vipRoutes, cycle)
    
    println("Calculating profits by links")
    val linkConsumptionDetails = ListBuffer[LinkConsumptionDetails]()
    val loungeConsumptionDetails = ListBuffer[LoungeConsumptionDetails]()
    val allAirplaneAssignments: immutable.Map[Int, LinkAssignments] = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    //cost by link
    val costByLink = mutable.HashMap[Transport, ListBuffer[PassengerCost]]()
    consumptionResult.foreach {
      case((passengerGroup, airport, route), passengerCount) => route.links.foreach { linkConsideration =>
        costByLink.getOrElseUpdate(linkConsideration.link, ListBuffer[PassengerCost]()).append(PassengerCost(passengerGroup, passengerCount, linkConsideration.cost))
      }
    }

    links.foreach { link =>
      if (link.capacity.total > 0) {
        val (linkResult, loungeResult) = computeLinkAndLoungeConsumptionDetail(link, cycle, allAirplaneAssignments, costByLink.toMap.getOrElse(link, List.empty).toList)
        linkConsumptionDetails += linkResult
        loungeConsumptionDetails ++= loungeResult
      }
    }

    purgeAlerts()
    checkLoadFactor(flightLinks, cycle)

    LinkSource.deleteLinkConsumptionsByCycle(300)
    LinkSource.saveLinkConsumptions(linkConsumptionDetails.toList)

    println("Calculating Lounge usage")
    //condense the lounge result
    val loungeResult : scala.collection.immutable.Map[Lounge, LoungeConsumptionDetails] = loungeConsumptionDetails.groupBy(_.lounge).map{
      case (lounge, consumptionsForThisLounge) =>
        var totalSelfVisitors = 0
        var totalAllianceVistors = 0
        consumptionsForThisLounge.foreach {
          case LoungeConsumptionDetails(_, selfVisitors, allianceVisitors, _) =>
            totalSelfVisitors += selfVisitors
            totalAllianceVistors += allianceVisitors
        }
        (lounge, LoungeConsumptionDetails(lounge, totalSelfVisitors, totalAllianceVistors, cycle))
    }.toMap

    LoungeHistorySource.updateConsumptions(loungeResult.map(_._2).toList)
    //purge older result
    LoungeHistorySource.deleteConsumptionsBeforeCycle(cycle)


    (linkConsumptionDetails.toList, loungeResult, consumptionResult)
  }

  case class PassengerCost(group : PassengerGroup, passengerCount : Int, cost : Double)

  val minorDelayNormalThreshold = 0.4  // so it's around 24% at 40% condition (multiplier at 0.6) to run into minor delay OR worse
  val majorDelayNormalThreshold = 0.1 // so it's around 6% at 40% condition (multiplier at 0.6) to run into major delay OR worse
  val cancellationNormalThreshold = 0.03 // so it's around 1.8% at 40% condition (multiplier at 0.6) to run into cancellation
  val minorDelayBadThreshold = 0.5  // so it's around 40% at 20% condition (multiplier at 0.8) to run into minor delay OR worse
  val majorDelayBadThreshold = 0.2 // so it's around 16% at 20% condition (multiplier at 0.8) to run into major delay OR worse
  val cancellationBadThreshold = 0.1 // so it's around 8% at 20% condition (multiplier at 0.8) to run into cancellation
  val minorDelayCriticalThreshold = 1  // so it's around 100% at 0% condition (multiplier at 1) to run into minor delay OR worse
  val majorDelayCriticalThreshold = 0.5 // so it's around 50% at 0% condition (multiplier at 1) to run into major delay OR worse
  val cancellationCriticalThreshold = 0.3 // so it's around 30% at 0% condition (multiplier at 1) to run into cancellation

  def simulateLinkError(links : List[Link]) = {
    links.foreach {
      link => {
        var i = 0
        val assignedInServiceAirplanes = link.getAssignedAirplanes().filter(_._1.isReady)
        for ( i <- 0 until link.frequency) {
          var airplaneCount : Int = assignedInServiceAirplanes.size
          if (airplaneCount > 0) {
            val airplane = assignedInServiceAirplanes.toList.map(_._1)(i % airplaneCount)           //round robin
            val errorValue = Random.nextDouble()
            val conditionMultipler = (Airplane.MAX_CONDITION - airplane.condition).toDouble / Airplane.MAX_CONDITION
            var minorDelayThreshold : Double = 0
            var majorDelayThreshold : Double = 0
            var cancellationThreshold : Double = 0
            if (airplane.condition > Airplane.BAD_CONDITION) { //small chance of delay and cancellation
              if (errorValue < cancellationNormalThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayNormalThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayNormalThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }
            } else if (airplane.condition > Airplane.CRITICAL_CONDITION) {
              if (errorValue < cancellationBadThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayBadThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayBadThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }
            } else {
              if (errorValue < cancellationCriticalThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayCriticalThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayCriticalThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }
            }
          }
        }
      }
      if (link.cancellationCount > 0) {
        link.addCancelledSeats(link.capacityPerFlight() * link.cancellationCount)
      }
    }
  }

  /**
    * Only called by test cases
    * @param link
    * @param cycle
    * @return
    */
  def computeFlightLinkConsumptionDetail(link : Link, cycle : Int) : LinkConsumptionDetails = {
    //for testing, assuming all airplanes are only assigned to this link
    val assignmentsToThis = link.getAssignedAirplanes().filter(_._1.isReady).toList.map {
      case(airplane, assignment) => (airplane.id, LinkAssignments(immutable.Map(link.id -> assignment)))
    }.toMap
    computeLinkAndLoungeConsumptionDetail(link, cycle, assignmentsToThis, List.empty)._1
  }

  def computeLinkAndLoungeConsumptionDetail(link : Transport, cycle : Int, allAirplaneAssignments : immutable.Map[Int, LinkAssignments], passengerCostEntries : List[PassengerCost]) : (LinkConsumptionDetails, List[LoungeConsumptionDetails]) = {
    link.transportType match {
      case TransportType.FLIGHT =>
        val flightLink = link.asInstanceOf[Link]
        val loadFactor = flightLink.getTotalSoldSeats.toDouble / flightLink.getTotalCapacity

        //val totalFuelBurn = link //fuel burn actually similar to crew cost
        val fuelCost = flightLink.getAssignedModel() match {
          case Some(model) =>
            (if (flightLink.duration <= 90) {
              val ascendTime, descendTime = (flightLink.duration / 2)
              (model.fuelBurn * 10 * ascendTime + model.fuelBurn * descendTime) * FUEL_UNIT_COST * (flightLink.frequency - flightLink.cancellationCount)
            } else {
              (model.fuelBurn * 10 * 45 + model.fuelBurn * (flightLink.duration - 30)) * FUEL_UNIT_COST * (flightLink.frequency - flightLink.cancellationCount) //first 60 minutes huge burn, then cruising at 1/4 the cost
            } * (0.7 + 0.3 * loadFactor)).toInt //at 0 LF, 70% fuel cost
          case None => 0
        }


        val inServiceAssignedAirplanes = flightLink.getAssignedAirplanes().filter(_._1.isReady)
        //the % of time spent on this link for each airplane
        val assignmentWeights : immutable.Map[Airplane, Double] = { //0 to 1
          inServiceAssignedAirplanes.view.map {
            case(airplane, assignment) =>
              allAirplaneAssignments.get(airplane.id) match {
                case Some(linkAssignmentsToThisAirplane) =>
                  val weight : Double = assignment.flightMinutes.toDouble / linkAssignmentsToThisAirplane.assignments.values.map(_.flightMinutes).sum
                  (airplane, weight)
                case None => (airplane, 1.0) //100%
              } //it shouldn't be else...but just to play safe, if it's not found in "all" table, assume this is the only link assigned
          }.toMap
        }
        var maintenanceCost = 0
        inServiceAssignedAirplanes.foreach {
          case(airplane, _) =>
            //val maintenanceCost = (link.getAssignedAirplanes.toList.map(_._1).foldLeft(0)(_ + _.model.maintenanceCost) * link.airline.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY).toInt
            maintenanceCost += (airplane.model.maintenanceCost * assignmentWeights(airplane) * flightLink.airline.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY).toInt
        }



        val airportFees = flightLink.getAssignedModel() match {
          case Some(model) =>
            val airline = flightLink.airline
            (flightLink.from.slotFee(model, airline) + flightLink.to.slotFee(model, airline) + flightLink.from.landingFee(model) + flightLink.to.landingFee(model)) * flightLink.frequency
          case None => 0
        }

        var depreciation = 0
        inServiceAssignedAirplanes.foreach {
          case(airplane, _) =>
            //link.getAssignedAirplanes().toList.map(_._1).foldLeft(0)(_ + _.depreciationRate)
            depreciation += (airplane.depreciationRate * assignmentWeights(airplane)).toInt
        }

        var inflightCost, crewCost, revenue = 0
        LinkClass.values.foreach { linkClass =>
          val capacity = flightLink.capacity(linkClass)
          val soldSeats = flightLink.soldSeats(linkClass)

          inflightCost += computeInflightCost(linkClass, flightLink, soldSeats)
          crewCost += (linkClass.resourceMultiplier * capacity * flightLink.duration / 60 * CREW_UNIT_COST).toInt
          revenue += soldSeats * flightLink.price(linkClass)
        }

        // delays incur extra cost
        var delayCompensation = Computation.computeCompensation(flightLink)

        // lounge cost
        val fromLounge = flightLink.from.getLounge(flightLink.airline.id, flightLink.airline.getAllianceId(), activeOnly = true)
        val toLounge = flightLink.to.getLounge(flightLink.airline.id, flightLink.airline.getAllianceId(), activeOnly = true)
        var loungeCost = 0
        val loungeConsumptionDetails = ListBuffer[LoungeConsumptionDetails]()
        if (fromLounge.isDefined || toLounge.isDefined) {
          val visitorCount = flightLink.soldSeats(BUSINESS) + flightLink.soldSeats(FIRST)
          if (fromLounge.isDefined) {
            loungeCost += visitorCount * Lounge.PER_VISITOR_CHARGE
            loungeConsumptionDetails += (
              if (fromLounge.get.airline.id == flightLink.airline.id) {
                LoungeConsumptionDetails(fromLounge.get, selfVisitors = visitorCount, allianceVisitors = 0, cycle)
              } else {
                LoungeConsumptionDetails(fromLounge.get, selfVisitors = 0, allianceVisitors = visitorCount, cycle)
              })
          }
          if (toLounge.isDefined) {
            loungeCost += visitorCount * Lounge.PER_VISITOR_CHARGE
            loungeConsumptionDetails += (
              if (toLounge.get.airline.id == flightLink.airline.id) {
                LoungeConsumptionDetails(toLounge.get, selfVisitors = visitorCount, allianceVisitors = 0, cycle)
              } else {
                LoungeConsumptionDetails(toLounge.get, selfVisitors = 0, allianceVisitors = visitorCount, cycle)
              })
          }

        }

        val profit = revenue - fuelCost - maintenanceCost - crewCost - airportFees - inflightCost - delayCompensation - depreciation - loungeCost

        //calculation overall satisifaction
        var satisfactionTotalValue : Double = 0
        var totalPassengerCount = 0
        passengerCostEntries.foreach {
          case PassengerCost(passengerGroup, passengerCount, cost) =>
            val preferredLinkClass = passengerGroup.preference.preferredLinkClass
            val standardPrice = Pricing.computeStandardPrice(flightLink.distance, flightLink.flightType, preferredLinkClass)
            val satisfaction = Computation.computePassengerSatisfaction(cost, standardPrice)
            satisfactionTotalValue += satisfaction * passengerCount
            totalPassengerCount += passengerCount
        }
        val overallSatisfaction = if (totalPassengerCount == 0) 0 else satisfactionTotalValue / totalPassengerCount

        //val result = LinkConsumptionDetails(link.id, link.price, link.capacity, link.soldSeats, link.computedQuality, fuelCost, crewCost, airportFees, inflightCost, delayCompensation = delayCompensation, maintenanceCost, depreciation = depreciation, revenue, profit, link.cancellationCount, linklink.from.id, link.to.id, link.airline.id, link.distance, cycle)
        val result = LinkConsumptionDetails(flightLink, fuelCost, crewCost, airportFees, inflightCost, delayCompensation = delayCompensation, maintenanceCost, depreciation = depreciation, loungeCost = loungeCost, revenue, profit, overallSatisfaction, cycle)
        //println("model : " + link.getAssignedModel().get + " profit : " + result.profit + " result: " + result)
        (result, loungeConsumptionDetails.toList)
      case TransportType.SHUTTLE =>
        (LinkConsumptionDetails(link, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, cycle), List.empty)
    }

  }

  val BASE_INFLIGHT_COST = 20

  val computeInflightCost = (linkClass : LinkClass, link : Link, soldSeats : Int) => {
    val star = link.rawQuality / 20
    val durationCostPerHour =
      if (star <= 1) {
        1
      } else if (star == 2) {
        4
      } else if (star == 3) {
        8
      } else if (star == 4) {
        13
      } else {
        20
      }

    val costPerPassenger = BASE_INFLIGHT_COST + durationCostPerHour * link.duration.toDouble / 60
    (costPerPassenger * soldSeats * 2).toInt //Roundtrip X 2
  }
  
  val LOAD_FACTOR_ALERT_LINK_COUNT_THRESHOLD = 3 //how many airlines before load factor is checked
  val LOAD_FACTOR_ALERT_THRESHOLD = 0.5 //LF threshold
  val LOAD_FACTOR_ALERT_DURATION = 52

  /**
    * Purge alerts that are no longer valid
    */
  def purgeAlerts() = {
    //only purge link cancellation alerts for now
    val existingAlerts = AlertSource.loadAlertsByCategory(AlertCategory.LINK_CANCELLATION)

    //try to purge the alerts, as some alerts might get inserted while the link is deleted during the simulation time
    val liveLinkIds : List[Int] = LinkSource.loadAllLinks(LinkSource.ID_LOAD).map(_.id)
    val deadAlerts = existingAlerts.filter(alert => alert.targetId.isDefined && !liveLinkIds.contains(alert.targetId.get))
    AlertSource.deleteAlerts(deadAlerts)
    println("Purged alerts with no corresponding links... " + deadAlerts.size)
  }

  def checkLoadFactor(links : List[Link], cycle : Int) = {
    val existingAlerts = AlertSource.loadAlertsByCategory(AlertCategory.LINK_CANCELLATION)

    //group links by from and to airport ID Tuple(id1, id2), smaller ID goes first in the tuple
    val linksByAirportIds = links.filter(_.capacity.total > 0).groupBy( link =>
      if (link.from.id < link.to.id) (link.from.id, link.to.id) else (link.to.id, link.from.id)
    )

    val existingAlertsByLinkId : scala.collection.immutable.Map[Int, Alert] = existingAlerts.map(alert => (alert.targetId.get, alert)).toMap

    val updatingAlerts = ListBuffer[Alert]()
    val newAlerts = ListBuffer[Alert]()
    val deletingAlerts = ListBuffer[Alert]()
    val deletingLinks = ListBuffer[Link]()
    val newLogs = ListBuffer[Log]()

    linksByAirportIds.foreach {
      case((airportId1, airportId2), links) =>
        if (links.size >= LOAD_FACTOR_ALERT_LINK_COUNT_THRESHOLD) {
          links.foreach { link =>
            val loadFactor = link.getTotalSoldSeats.toDouble / link.getTotalCapacity
            if (loadFactor < LOAD_FACTOR_ALERT_THRESHOLD) {
              existingAlertsByLinkId.get(link.id) match {
                case Some(existingAlert) => //continue to have problem
                  if (existingAlert.duration <= 1) { //kaboom! deleting
                    deletingAlerts.append(existingAlert)
                    deletingLinks.append(link)
                    val message = "Airport authorities have revoked license of " + link.airline.name + " to operate route between " +  link.from.displayText + " and " + link.to.displayText + " due to prolonged low load factor"
                    newLogs += Log(airline = link.airline, message = message, category = LogCategory.LINK, severity = LogSeverity.WARN, cycle = cycle)
                    //notify competitors too with lower severity
                    links.filter(_.id != link.id).foreach { competitorLink =>
                      newLogs += Log(airline = competitorLink.airline, message = message, category = LogCategory.LINK, severity = LogSeverity.INFO, cycle = cycle)
                    }
                  } else { //clock is ticking!
                     updatingAlerts.append(existingAlert.copy(duration = existingAlert.duration -1))
                  }
                case None => //new warning
                  val message = "Airport authorities have issued warning to " + link.airline.name + " on low load factor of route between " +  link.from.displayText + " and " + link.to.displayText + ". If the load factor remains lower than " + LOAD_FACTOR_ALERT_THRESHOLD * 100 + "% for the remaining duration, the license to operate this route will be revoked!"
                  val alert = Alert(airline = link.airline, message = message, category = AlertCategory.LINK_CANCELLATION, targetId = Some(link.id), cycle = cycle, duration = LOAD_FACTOR_ALERT_DURATION)
                  newAlerts.append(alert)
              }
            } else { //LF good, delete existing alert if any
              existingAlertsByLinkId.get(link.id).foreach { existingAlert =>
                deletingAlerts.append(existingAlert)
              }
            }
          }
        } else { //not enough competitor, check if alert should be removed
          links.foreach { link =>
            existingAlertsByLinkId.get(link.id).foreach { existingAlert =>
              deletingAlerts.append(existingAlert)
            }
          }
        }
    }

    
    deletingLinks.foreach { link =>
       println("Revoked link: " + link)
       LinkSource.deleteLink(link.id)
    }
    AlertSource.updateAlerts(updatingAlerts.toList)
    AlertSource.insertAlerts(newAlerts.toList)
    AlertSource.deleteAlerts(deletingAlerts.toList)
    
    LogSource.insertLogs(newLogs.toList)
  }
  
  def generateLinkStatistics(consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int], cycle : Int) : List[LinkStatistics] = {
    val statistics = Map[LinkStatisticsKey, Int]()
    consumptionResult.foreach {
      case ((_, _, route), passengerCount) =>
        for (i <- 0 until route.links.size) {
          val link = route.links(i) 
          val airline = link.link.airline
          val key = 
            if (i == 0) {
              if (route.links.size == 1) {
                LinkStatisticsKey(link.from, link.to, true, true, airline)  
              } else {
                LinkStatisticsKey(link.from, link.to, true, false, airline)
              }
            } else if (i == route.links.size -1) { //last one in list
              LinkStatisticsKey(link.from, link.to, false, true, airline)
            } else { //in the middle
              LinkStatisticsKey(link.from, link.to, false, false, airline)
            }
          val newPassengerCount = statistics.getOrElse(key, 0) + passengerCount
          statistics.put(key, newPassengerCount)
        }
    }
    
    statistics.map { 
      case (linkStatisticsKey, passenger) =>
        LinkStatistics(linkStatisticsKey, passenger, cycle)
    }.toList
    
  }
  
  def generateCountryMarketShares(consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int]) : List[CountryMarketShare] = {
    val countryAirlinePassengers = Map[String, Map[Int, Long]]()
    consumptionResult.foreach {
      case ((_, _, route), passengerCount) =>
        for (i <- 0 until route.links.size) {
          val link = route.links(i)
          if (link.link.transportType == TransportType.FLIGHT) {
            val airline = link.link.airline
            val country = link.from.countryCode
            val airlinePassengers = countryAirlinePassengers.getOrElseUpdate(country, Map[Int, Long]())
            val currentSum : Long = airlinePassengers.getOrElse(airline.id, 0L)
            airlinePassengers.put(airline.id, currentSum + passengerCount)
          }
        }
    }

    countryAirlinePassengers.map {
      case ((countryCode, airlinePassengers)) => { 
        CountryMarketShare(countryCode, airlinePassengers.toMap)
      }
    }.toList

  }

  case class PassengerTransportStats(cycle : Int, transported : Int, total : Int)
  /**
    * Stats on how much pax from a country was carried/missed
    * @param olympicsConsumptions
    * @param missedOlympicsPassengers
    * @return Map[countryCode, transportRate]
    */
  def generateOlympicsCountryStats(cycle : Int, olympicsConsumptions: immutable.Map[(PassengerGroup, Airport, Route), Int], missedOlympicsPassengers: immutable.Map[(PassengerGroup, Airport), Int]) : immutable.Map[String, PassengerTransportStats] = {
    val passengersByCountry = mutable.HashMap[String, Int]()
    val missedPassengersByCountry = mutable.HashMap[String, Int]()

    val allCountries = mutable.HashSet[String]()
    olympicsConsumptions.foreach {
      case ((passengerGroup, _, _), passengerCount) =>
        val countryCode = passengerGroup.fromAirport.countryCode
        val currentCount = passengersByCountry.getOrElse(countryCode, 0)
        passengersByCountry.put(countryCode, currentCount + passengerCount)
        allCountries.add(countryCode)
    }

    missedOlympicsPassengers.foreach {
      case ((passengerGroup, _), passengerCount) =>
        val countryCode = passengerGroup.fromAirport.countryCode
        val currentCount = missedPassengersByCountry.getOrElse(countryCode, 0)
        missedPassengersByCountry.put(countryCode, currentCount + passengerCount)
        allCountries.add(countryCode)
    }


    allCountries.map { countryCode =>
      val transportStats =
        passengersByCountry.get(countryCode) match {
          case Some(passengers) => missedPassengersByCountry.get(countryCode) match {
            case Some(missedPassengers) => PassengerTransportStats(cycle, passengers, (passengers + missedPassengers))
            case None => PassengerTransportStats(cycle, passengers, passengers)
          }
          case None => PassengerTransportStats(cycle, 0, missedPassengersByCountry.getOrElse(countryCode, 0))
        }
      (countryCode, transportStats)
    }.toMap
  }


  /**
    *
    * @param olympicsConsumptions
    * @return Map[airline, scope] score if 1 if Airline A has direct flight that takes the pax to olympics city, otherwise each airline in the route get 1 / n, which n is the number of hops
    */
  def generateOlympicsAirlineStats(cycle : Int, olympicsConsumptions: immutable.Map[(PassengerGroup, Airport, Route), Int]) : immutable.Map[Airline, (Int, BigDecimal)] = {
    val scoresByAirline = mutable.HashMap[Airline, BigDecimal]()

    olympicsConsumptions.foreach {
      case ((_, _, Route(links, _, _)), passengerCount) =>
        links.foreach { link =>
          val existingScore : BigDecimal = scoresByAirline.getOrElse(link.link.airline, 0)
          scoresByAirline.put(link.link.airline, existingScore + passengerCount.toDouble / links.size)
        }
    }

    scoresByAirline.view.mapValues( score => (cycle, score)).toMap
  }

  /**
    * Refresh link capacity and frequency if necessary
    */
  def refreshLinksPostCycle() = {
    println("Refreshing link capacity and frequency to find discrepancies")
    val simpleLinks = LinkSource.loadAllLinks(LinkSource.ID_LOAD)
    val fullLinks = LinkSource.loadAllLinks(LinkSource.FULL_LOAD).map(link => (link.id, link)).toMap
    println("Finished loading both the simple and full links")
    //not too ideal, but even if someone update the link assignment when this is in progress, it should be okay, as that assignment
    //is suppose to update the link capacity and frequency anyway
    simpleLinks.foreach { simpleLink =>
      fullLinks.get(simpleLink.id).foreach { fullLink =>
        if (simpleLink.frequency != fullLink.frequency || simpleLink.capacity != fullLink.capacity) {
          println(s"Adjusting capacity/frequency of  $simpleLink to $fullLink")
          LinkSource.updateLink(fullLink)
        }
      }
    }
  }

  def simulatePostCycle(cycle : Int) = {
    //now update the link capacity if necessary
    LinkSimulation.refreshLinksPostCycle()
    purgeNegotiationCoolDowns(cycle)
  }

  def purgeNegotiationCoolDowns(cycle: Int): Unit = {
    LinkSource.purgeNegotiationCoolDowns(cycle)
    NegotiationSource.deleteLinkDiscountBeforeExpiry(cycle)
  }
}