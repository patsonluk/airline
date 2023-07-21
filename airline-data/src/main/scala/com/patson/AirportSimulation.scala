package com.patson

import java.util.Random
import com.patson.data._
import com.patson.model._
import com.patson.util.{AirlineCache, AirportCache, AirportChampionInfo, ChampionUtil}

import java.util.concurrent.ThreadLocalRandom
import scala.collection.{MapView, immutable, mutable}
import scala.collection.mutable.{ListBuffer, Map, Set}
import scala.math.BigDecimal.RoundingMode

object AirportSimulation {
  val LOYALTY_AUTO_INCREMENT_WITH_HQ = 0.05
  val LOYALTY_AUTO_INCREMENT_WITH_BASE = 0.02
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ = 30 //how much loyalty will increment to just because of being a HQ
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE = 15 //how much loyalty will increment to just because of being a HQ
  val LOYALTY_DECREMENT_BY_MINOR_DELAY = 0.5 //if all flights have minor delay
  val LOYALTY_DECREMENT_BY_MAJOR_DELAY = 2 //if all flights have major delay
  val LOYALTY_DECREMENT_BY_CANCELLATION = 4 //if all flights are cancelled
  
  private[patson] val LOYALTY_INCREMENT_BY_FLIGHTS = 1.0
  private[patson] val LOYALTY_DECREMENT_BY_FLIGHTS = 1.0


  def airportSimulation(cycle: Int, flightLinkResult : List[LinkConsumptionDetails], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int]) = {
    println("starting airport simulation")
    println("loading all airports")
    //do decay
    val allAirports = AirportSource.loadAllAirports(true)
    println("finished loading all airports")

    val flightLinks = LinkSource.loadAllLinks(LinkSource.ID_LOAD).filter(_.transportType == TransportType.FLIGHT).map(_.asInstanceOf[Link])
    val linksByFromAirportId = flightLinks.groupBy(_.from.id)
    val linksByToAirportId = flightLinks.groupBy(_.to.id)

    //update the loyalist on airports based on link consumption
    println("Adjust loyalist by link consumptions")
    val championInfo = simulateLoyalists(allAirports, linkRidershipDetails, cycle)

    //check whether lounge is still active
    updateLoungeStatus(allAirports, linkRidershipDetails)


    println("Finished simulation of loyalty by link consumption")


    println("Finished loyalist simulation")
    //airportProjectSimulation(allAirports)

    AirportSource.purgeAirlineAppealBonus(cycle)

    championInfo
  }

  val LOYALIST_HISTORY_SAVE_INTERVAL = 10 //every 10 cycles
  val LOYALIST_HISTORY_ENTRY_MAX = 50

  def getHistoryCycle(lastCompletedCycle : Int, delta : Int): Int = {
    val baseCycle = lastCompletedCycle - lastCompletedCycle % LOYALIST_HISTORY_SAVE_INTERVAL
    baseCycle + delta * LOYALIST_HISTORY_SAVE_INTERVAL
  }

  def processChampionInfoChanges(previousInfo : List[AirportChampionInfo], newInfo : List[AirportChampionInfo], currentCycle : Int) = {
    val previousInfoByAirlineId : Predef.Map[Int, List[AirportChampionInfo]] = previousInfo.groupBy(_.loyalist.airline.id)
    val newInfoByAirlineId : Predef.Map[Int, List[AirportChampionInfo]] = newInfo.groupBy(_.loyalist.airline.id)

    val airlineIds = previousInfoByAirlineId.keySet ++ newInfoByAirlineId.keySet
    val logs = ListBuffer[Log]()
    airlineIds.foreach { airlineId =>
      val changes = ListBuffer[ChampionInfoChange]()
      previousInfoByAirlineId.get(airlineId) match {
        case Some(previousRanks) =>
          newInfoByAirlineId.get(airlineId) match {
            case Some(newRanks) => //go from airport to airport
              val previousInfoByAirport = previousRanks.groupBy(_.loyalist.airport).view.mapValues(_(0)) //should be exactly one entry
              val newInfoByAirport = newRanks.groupBy(_.loyalist.airport).view.mapValues(_(0)) //should be exactly one entry
              val airportIds = previousInfoByAirport.keySet ++ newInfoByAirport.keySet
              airportIds.foreach { airportId =>
                if (previousInfoByAirport.get(airportId).map(_.ranking).getOrElse(0) != newInfoByAirport.get(airportId).map(_.ranking).getOrElse(0)) {
                  changes.append(ChampionInfoChange(previousInfoByAirport.get(airportId), newInfoByAirport.get(airportId)))
                }
              }
            case None => changes.appendAll(previousRanks.map(info => ChampionInfoChange(Some(info), None))) //lost all ranks
          }
        case None => changes.appendAll(newInfoByAirlineId(airlineId).map(info => ChampionInfoChange(None, Some(info)))) //all ranks are new
      }
      val airline = AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId))

      logs.appendAll(changes.map {
        case ChampionInfoChange(Some(previousRank), Some(newRank)) =>
          val reputationChange = BigDecimal.valueOf(newRank.reputationBoost - previousRank.reputationBoost).setScale(2, RoundingMode.HALF_UP)
          val displayChange =
            if (reputationChange >= 0) {
              "+" + reputationChange
            } else {
              reputationChange.toString
            }
          Log(airline, s"${newRank.loyalist.airport.displayText} ranking ${previousRank.ranking} -> ${newRank.ranking}. Reputation change $displayChange", LogCategory.AIRPORT_RANK_CHANGE, LogSeverity.INFO, currentCycle, immutable.Map("airportId" -> newRank.loyalist.airport.id.toString))
        case ChampionInfoChange(None, Some(newRank)) =>
          Log(airline, s"${newRank.loyalist.airport.displayText} new ranking ${newRank.ranking}. Reputation change +${BigDecimal.valueOf(newRank.reputationBoost).setScale(2, RoundingMode.HALF_UP)}", LogCategory.AIRPORT_RANK_CHANGE, LogSeverity.INFO, currentCycle, immutable.Map("airportId" -> newRank.loyalist.airport.id.toString))
        case ChampionInfoChange(Some(previousRank), None) =>
          Log(airline, s"${previousRank.loyalist.airport.displayText} lost ranking ${previousRank.ranking}. Reputation change -${BigDecimal.valueOf(previousRank.reputationBoost).setScale(2, RoundingMode.HALF_UP)}", LogCategory.AIRPORT_RANK_CHANGE, LogSeverity.INFO, currentCycle, immutable.Map("airportId" -> previousRank.loyalist.airport.id.toString))
        case _ => //should not happen
          Log(airline, s"Unknown rank change", LogCategory.AIRPORT_RANK_CHANGE, LogSeverity.INFO, currentCycle)
      })
    }
    println(s"Ranking changes count : ${logs.size}")
    LogSource.insertLogs(logs.toList)
 }
  case class ChampionInfoChange(previousRank : Option[AirportChampionInfo], newRank : Option[AirportChampionInfo])


  def simulateLoyalists(allAirports : List[Airport], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int], cycle : Int) = {
    var existingLoyalistByAirportId : immutable.Map[Int, List[Loyalist]] = LoyalistSource.loadLoyalistsByCriteria(List.empty).groupBy(_.airport.id)

    existingLoyalistByAirportId = decayLoyalists(allAirports, existingLoyalistByAirportId)

    val (updatingLoyalists,deletingLoyalists) = computeLoyalists(allAirports, linkRidershipDetails, existingLoyalistByAirportId)
    println(s"Updating ${updatingLoyalists.length} loyalists entries")
    LoyalistSource.updateLoyalists(updatingLoyalists)

    println(s"Deleting ${deletingLoyalists.length} loyalists entries")
    LoyalistSource.deleteLoyalists(deletingLoyalists)

    val allLoyalists = LoyalistSource.loadLoyalistsByCriteria(List.empty)
    println(s"Computing loyalist info with ${allLoyalists.length} entries")

    //compute champion info
    val previousInfo = ChampionUtil.loadAirportChampionInfo()
    val newInfo = ChampionUtil.computeAirportChampionInfo(allLoyalists)
    processChampionInfoChanges(previousInfo, newInfo, cycle)
    AirportSource.updateChampionInfo(newInfo)


    println("Done computing champions")

    if (cycle % LOYALIST_HISTORY_SAVE_INTERVAL == 0) {
      val cutoff = cycle - LOYALIST_HISTORY_ENTRY_MAX * LOYALIST_HISTORY_SAVE_INTERVAL
      println(s"Purging loyalist history before cycle $cutoff")
      LoyalistSource.deleteLoyalistHistoryBeforeCycle(cutoff)

      val historyEntries = allLoyalists.map(LoyalistHistory(_, cycle))
      println(s"Saving ${historyEntries.length} loyalist history entries")
      LoyalistSource.updateLoyalistHistory(historyEntries)
    }

    newInfo
  }

  val DECAY_RATE = 0.0005 //1 loyalist disappears per 2000 per week
  private[patson] def decayLoyalists(allAirports : List[Airport], existingLoyalistByAirportId : immutable.Map[Int, List[Loyalist]]) : immutable.Map[Int, List[Loyalist]] = {
    val updatingLoyalists, deletingLoyalists = ListBuffer[Loyalist]()
    val r = new Random()
    val result : immutable.Map[Int, List[Loyalist]] = existingLoyalistByAirportId.view.mapValues { loyalists =>
      loyalists.map {
        loyalist =>
          var decayAmount = (loyalist.amount * DECAY_RATE).toInt
          if (decayAmount == 0) { //less than 1. For populated airport, we should just use 1, this should aid with purging excessive loyalist records
            if (loyalist.airport.basePopulation >= 1_000_000) {
              decayAmount = 1
            } else { //for pop 0, one in 100 chance to lose 1
              if (r.nextInt(100) <= loyalist.airport.basePopulation / 10_000 ) {
                decayAmount = 1
              }
            }
          }

          if (decayAmount == 0) { //no change
            loyalist
          } else {
            val newLoyalist = Loyalist(loyalist.airport, loyalist.airline, loyalist.amount - decayAmount)
            if (newLoyalist.amount == 0) {
              deletingLoyalists.append(newLoyalist)
            } else {
              updatingLoyalists.append(newLoyalist)
            }
            newLoyalist
          }
      }.filter(_.amount > 0)
    }.toMap

    println(s"Decaying (update) ${updatingLoyalists.length} loyalists entries")
    LoyalistSource.updateLoyalists(updatingLoyalists.toList)

    println(s"Decaying (deletion) ${deletingLoyalists.length} loyalists entries")
    LoyalistSource.deleteLoyalists(deletingLoyalists.toList)

    result
  }

  val MAX_LOYALIST_FLIP_RATIO = 1
  val NEUTRAL_SATISFACTION = 0.6
  private[patson] def computeLoyalists(allAirports : List[Airport], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int], existingLoyalistByAirportId : immutable.Map[Int, List[Loyalist]]) = {
    val result = ListBuffer[Loyalist]() //airlineId, amount

    val highIncomeLevel = Computation.getIncomeLevel(Country.HIGH_INCOME_THRESHOLD)
    linkRidershipDetails.groupBy(_._1._1.fromAirport).foreach {
      case ((fromAirport, passengersFromThisAirport)) =>
        val loyalistIncrementOfAirlines = Map[Int, Int]() //airlineId, delta
        //passengersFromThisAirport.filter(_._1._1.preference.loyaltySensitivity > 0).toList.foreach { //only count pax that actually cares about loyalty now
        passengersFromThisAirport.toList.foreach {
          case ((passengerGroup, toAirport, route), paxCount) =>
            val totalDistance = route.links.map(_.link.distance).sum
            val flightLinks = route.links.filter(_.link.transportType == TransportType.FLIGHT) //only flights would generate loyalist
            flightLinks.foreach { linkConsideration =>
              val link = linkConsideration.link
              val preferredLinkClass = passengerGroup.preference.preferredLinkClass
              val standardPrice = Pricing.computeStandardPrice(link.distance, link.flightType, preferredLinkClass)


              val satisfaction = Computation.computePassengerSatisfaction(linkConsideration.cost, standardPrice)


              var conversionRatio =
                if (satisfaction < NEUTRAL_SATISFACTION) {
                  //(satisfaction - NEUTRAL_SATISFACTION) / NEUTRAL_SATISFACTION * MAX_LOYALIST_FLIP_RATIO
                  0
                } else {
                  val multiplier = Math.min(MAX_LOYALIST_FLIP_RATIO, passengerGroup.preference.loyaltySensitivity + 0.3)
                  (satisfaction - NEUTRAL_SATISFACTION) / (1 - NEUTRAL_SATISFACTION) * multiplier
                }
              //adjust with income level. Since lower income country has less pax to start with. Up to 3 times
              if (fromAirport.incomeLevel < highIncomeLevel) {
                val multiplier = 1 + 2 * (highIncomeLevel - fromAirport.incomeLevel) / highIncomeLevel
                conversionRatio *= multiplier
              }

              if (link.distance != totalDistance) {
                conversionRatio = conversionRatio * (0.5 / route.links.length  + 0.5 * link.distance / totalDistance) //half depends on # of leg, half proportional to the % of distance travel of the whole route
              }

              val loyalistDelta = (paxCount * conversionRatio).toInt
              val existingDelta = loyalistIncrementOfAirlines.getOrElse(link.airline.id, 0)
              loyalistIncrementOfAirlines.put(link.airline.id, existingDelta + loyalistDelta)
            }
        }

        //put a map of current royalist status to draw which loyalist to flip
        val loyalistDistribution = ListBuffer[(Int, Int)]() //airlineId, threshold
        var walker = 0
        val existingLoyalistOfThisAirport = existingLoyalistByAirportId.get(fromAirport.id).getOrElse(List.empty).map(entry => (entry.airline.id, entry.amount)).toMap
        existingLoyalistOfThisAirport.foreach {
          case(airlineId, loyalistAmount) =>
            walker = walker + loyalistAmount
            loyalistDistribution.append((airlineId, walker))
        }
//        if (loyalistDistribution.length == 4) {
//          println(s"distribution $loyalistDistribution")
//        }

//        val flippedLoyalists = mutable.Map[Int, Int]() //airlineId, flipped amount

        val CHUNK_SIZE = 5
        val updatingLoyalists = mutable.HashMap[Int, Int]() //airlineId, amount

        val totalLoyalist = existingLoyalistOfThisAirport.values.sum
        //now with delta, see what the flips are
        loyalistIncrementOfAirlines.foreach {
          case (gainAirlineId, increment) => //split into chunks for better randomness
            if (increment > 0) {
              var unclaimedLoyalist = (fromAirport.population - totalLoyalist).toInt
              var remainingIncrement = increment
              while (remainingIncrement > 0) {
                val chunk = if (remainingIncrement <= CHUNK_SIZE) remainingIncrement else CHUNK_SIZE

                //Has to compare pop vs total, as  in rare scenario fromAirport.population < existingLoyalistOfThisAirport.values.sum, for example demolished property that +pop
                val flipTrigger = ThreadLocalRandom.current().nextInt(Math.max(fromAirport.population, totalLoyalist).toInt)

                val flippedAirlineIdOption = loyalistDistribution.find {
                  case (airlineId : Int, threshold : Int) => flipTrigger < threshold
                }.map(_._1)

                flippedAirlineIdOption match {
                  case Some(flippedAirlineId) => //flip from existing airline - could be itself
                    if (flippedAirlineId == gainAirlineId) { //flipping from itself, ignore

                    } else {
                      val existingAmountOfFlippedAirline : Int = updatingLoyalists.get(flippedAirlineId) match {
                        case Some(existingAmount) => existingAmount
                        case None => existingLoyalistOfThisAirport.getOrElse(flippedAirlineId, 0)
                      }


                      if (existingAmountOfFlippedAirline <= 0) { //cannot flip, ignore...

                      } else {
                        val finalChunk =
                          if (existingAmountOfFlippedAirline < chunk) { //partial flip
                            existingAmountOfFlippedAirline
                          } else {
                            chunk
                          }
                        updatingLoyalists.put(flippedAirlineId, existingAmountOfFlippedAirline - finalChunk)
                        val existingAmountOfGainAirline = updatingLoyalists.getOrElse(gainAirlineId, existingLoyalistOfThisAirport.getOrElse(gainAirlineId, 0))
                        updatingLoyalists.put(gainAirlineId, existingAmountOfGainAirline + finalChunk)
                      }
                    }
                  case None => //flip from unclaimed
                    if (unclaimedLoyalist <= 0) { //ignore, can no longer draw loyalist

                    } else {
                      val finalChunk =
                        if (unclaimedLoyalist < chunk) { //partial flip
                          unclaimedLoyalist
                        } else {
                          chunk
                        }
                      unclaimedLoyalist = unclaimedLoyalist - finalChunk
                      val existingAmountOfGainAirline = updatingLoyalists.getOrElse(gainAirlineId, existingLoyalistOfThisAirport.getOrElse(gainAirlineId, 0))
                      updatingLoyalists.put(gainAirlineId, existingAmountOfGainAirline + finalChunk)
                    }
                }
                remainingIncrement = remainingIncrement - chunk
              }
            }
        }

        //write result of this airport to final result
        updatingLoyalists.foreach {
          case (airlineId, amount) => result.append(Loyalist(fromAirport, AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId)), amount))
        }
    }
    result.toList.partition(_.amount > 0)
  }

  def updateLoungeStatus(allAirports : List[Airport], linkRidershipDetails : Predef.Map[(PassengerGroup, Airport, Route), Int]) = {
    println("Checking lounge status")
    val passengersByAirport : MapView[Airport, MapView[Airline, Int]] = linkRidershipDetails.toList.flatMap {
      case ((passengerGroup, airport, route), count) =>
        route.links.filter(_.link.transportType == TransportType.FLIGHT).flatMap { linkConsideration =>
          List((linkConsideration.link.airline, linkConsideration.from, count), (linkConsideration.link.airline, linkConsideration.to, count))
        }

    }.groupBy {
      case (airline, airport, count) => airport
    }.view.mapValues { list =>
      list.map {
        case (airline, airport, count) => (airline, count)
      }.groupBy(_._1).view.mapValues(_.map(_._2).sum)
    }


    allAirports.foreach { airport =>
      if (!airport.getLounges().isEmpty) {
        val airlineIdsWithBase = airport.getAirlineBases().keys.toList
        //println(s"AIRPORT $airport : ${passengersByAirport.get(airport).map(_.toList)}")
        val airlinesByPassengers : List[(Airline, Int)] = passengersByAirport.get(airport).map(_.toList).getOrElse(List.empty).filter {
          case (airline, _) => airlineIdsWithBase.contains(airline.id) //only count airlines that has a base here
        }

        val eligibleAirlines = airlinesByPassengers.sortBy(_._2).takeRight(airport.getLounges()(0).getActiveRankingThreshold).map(_._1)
        airport.getLounges().foreach { lounge =>
          val newStatus =
            if (eligibleAirlines.contains(lounge.airline)) {
              LoungeStatus.ACTIVE
            } else {
              LoungeStatus.INACTIVE
            }

          if (lounge.status != newStatus) {
            println(s"Flipping status for lounge $lounge to $newStatus")
            AirlineSource.saveLounge(lounge.copy(status = newStatus))
          }
        }
      }
    }
  }
//
//  def airportProjectSimulation(allAirports : List[Airport]) = {
//    import ProjectStatus._
//    println("simulating airport projects")
//
//    val inProgressProjects = AirportSource.loadAllAirportProjects().filter { _.status != COMPLETED }
//  }
  
//  private def simulateAirlineAppeals(airport : Airport, soldLinksByAirline : Map[Int, Seq[LinkConsumptionDetails]]) = {
//    val newAppeals = mutable.HashMap[Int, AirlineAppeal]()
//    soldLinksByAirline.foreach {
//      case(airlineId, soldLinksByAirline) => {
//        val targetLoyalty = getTargetLoyalty(soldLinksByAirline, airport.population)
//        val appeal = airport.getAirlineBaseAppeal(airlineId)
//        val currentLoyalty = appeal.loyalty //get the unadjusted value
//        var newLoyalty = getNewLoyalty(currentLoyalty, targetLoyalty)
//        val penalty = getPenalty(soldLinksByAirline)
////        if (penalty > 0) {
////          println("penalty for " + airlineId + " at airport " + airport + " is " + penalty)
////        }
//        newLoyalty = newLoyalty - penalty
//        if (newLoyalty <= 0) {
//          newLoyalty = 0
//        }
//
//        //airport.setAirlineLoyalty(airlineId, newLoyalty)
//        //AirportSource.updateAirlineAppeal(airport.id, airlineId, AirlineAppeal(newLoyalty, appeal.awareness))
//        newAppeals.put(airlineId, AirlineAppeal(newLoyalty, appeal.awareness))
//       // println("airport " + airport.name + " airline " + airlineId + " loyalty updating from " + currentLoyalty + " to " + newLoyalty)
//      }
//    }
//    AirportSource.updateAirlineAppeals(airport.id, newAppeals.toMap)
//
//
//    if (!airport.getLounges().isEmpty) {
//      val airlinesByPassengers = soldLinksByAirline.mapValues( consumptionDetails => consumptionDetails.map {_.link.soldSeats.total}.sum).toList.sortBy(_._2) //Map[airlineId, totalPassengersForThisAirport]
//      val eligibleAirlines = airlinesByPassengers.takeRight(airport.getLounges()(0).getActiveRankingThreshold).map(_._1)
//      airport.getLounges().foreach { lounge =>
//        val newStatus =
//        if (eligibleAirlines.contains(lounge.airline.id)) {
//          LoungeStatus.ACTIVE
//        } else {
//          LoungeStatus.INACTIVE
//        }
//
//        if (lounge.status != newStatus) {
//          AirlineSource.saveLounge(lounge.copy(status = newStatus))
//        }
//      }
//
//    }
//
//
//
//  }
  
//  private[patson] val getTargetLoyalty : (Seq[LinkConsumptionDetails], Long) => Double = (consumptionDetails, population) => {
//    val totalTransportedPassengers = consumptionDetails.map { _.link.soldSeats.total }.sum
//    val totalQualityProduct = consumptionDetails.map { consumptionDetailsEntry => consumptionDetailsEntry.link.soldSeats.total * consumptionDetailsEntry.link.computedQuality }.sum
//    val averageQuality = if (totalTransportedPassengers == 0) 0 else totalQualityProduct / totalTransportedPassengers
//    val targetLoyaltyByQuality = averageQuality
////    //to attain MAX loyalty requires transporting everyone (1 X pop) once per year, the increment in on power to MAX loyalty = weekly passenger
////    //ie base ^ 100 = pop / 52
////    //   base = (pop / 52) ^ 0.01
////    //and base ^ targetLoyaltyBypassengerVolume  = passenger
////    //    targetLoyaltyBypassengerVolume * log(base) = log(passenger)
////    //    targetLoyaltyBypassengerVolume = log(passenger) / (0.01 * log(pop/52))
////    //    targetLoyaltyBypassengerVolume = log(passenger) * 100 / log(pop/52)
////
////    // now pop needs to be bigger than 52, otherwise we have problem, in fact lets make min pop 10000
////
////
////    val targetLoyaltyBypassengerVolume = Math.log(totalTransportedPassengers) * 100 / Math.log(Math.max(10000, population) / 52)
//
//
//    //2nd formula:
//    //  1. loyalty range [1, 100] are split into steps of 1
//    //  2. From current step n, to reach next step n + 1 would require passengers of current step * a multiplier, denote the passenger of current step as p(n). we let p(1) = 1
//    //  3. The multiplier itself is also a function to the step n, denote as m(n)
//    //  4. From 2. and 3., we can write formula of p(n + 1) = p(n) * m(n)
//    //  5. Multiplier decrease proportionally to n, ie m(x) = m(1) - (m(1) - m(99)) * (x - 1) / 98
//    //  6. Now we create conditions, we let:
//    //    b. To reach last step of loyalty 100, m(99) = 1.0001
//    //    a. at loyalty 1, m(1) > m(100), which we need to figure out in order to work on 5.
//    //    c. Thinking from a city with 1M pop:
//    //        it should reach loyalty 100 if it transport every single pop within that year, therefore weekly passenger is 1000000 / 52, and we want p(100) = 1000000 / 52
//    //        it should start with loyalty 1 if it transport 1 pop per week, therefore we want p(1) = 1
//    //  7. Probably can be solved in some other fancy math, but i decided to let computer to estimate it for me ;) ...using binary search :\ (see method estimateM0), found m0 = 1.159339475631714
//    //  8. Build an array LOYALTY_TO_PASSENGER_VOLUME, that lists required passenger to reach certain loyalty, the index is the Loyalty (based on 1mil pop)
//    //  9. Now scale the required passengers with the city size relative to 1 million and search for matching loyalty
//
//    if (totalTransportedPassengers == 0) {
//      0
//    } else {
//      var upper = AirlineAppeal.MAX_LOYALTY
//      var lower = 1
//      var found = false
//      var estLoyalty = lower
//
//      val normalizePassengers = totalTransportedPassengers.toDouble * 1000000 / population
//
//      while (!found) {
//        estLoyalty = (upper + lower) / 2
//        if (LOYALTY_TO_PASSENGER_VOLUME(estLoyalty) > normalizePassengers) {
//          upper = estLoyalty
//        } else if (LOYALTY_TO_PASSENGER_VOLUME(estLoyalty) < normalizePassengers) {
//          lower = estLoyalty
//        } else {
//          found = true
//        }
//
//        if (upper - lower == 1) {
//          found = true
//          estLoyalty += 1 //bump up one to the upper bound
//        }
//      }
//
//      val targetLoyaltyBypassengerVolume = estLoyalty
//      var targetLoyalty = Math.min(targetLoyaltyByQuality, targetLoyaltyBypassengerVolume).doubleValue()
//
//      targetLoyalty
//    }
//  }
  
  private[patson] val getPenalty : Seq[LinkConsumptionDetails] => Double = consumptionDetails => {
      //add penalty for delays and cancellation
      val totalCapacity = consumptionDetails.map { _.link.capacity.total }.sum
      if (totalCapacity > 0) {
        val totalMinorDelayCapacity = consumptionDetails.filter(_.link.frequency > 0).map { linkConsumption => linkConsumption.link.capacity.total * linkConsumption.link.minorDelayCount / linkConsumption.link.frequency }.sum
        val totalMajorDelayCapacity = consumptionDetails.filter(_.link.frequency > 0).map { linkConsumption => linkConsumption.link.capacity.total * linkConsumption.link.majorDelayCount / linkConsumption.link.frequency}.sum
        val totalCancellationCapacity = consumptionDetails.filter(_.link.frequency > 0).map { linkConsumption => linkConsumption.link.capacity.total * linkConsumption.link.cancellationCount / linkConsumption.link.frequency}.sum
      
        val minorDelayPercentage = totalMinorDelayCapacity.toDouble / totalCapacity
        val majorDelayPercentage = totalMajorDelayCapacity.toDouble / totalCapacity
        val cancellationPercentage = totalCancellationCapacity.toDouble / totalCapacity
        
        minorDelayPercentage * LOYALTY_DECREMENT_BY_MINOR_DELAY + majorDelayPercentage * LOYALTY_DECREMENT_BY_MAJOR_DELAY + cancellationPercentage * LOYALTY_DECREMENT_BY_CANCELLATION
      } else {
        0
      }
  }
  
  private[patson] val LOYALTY_TO_PASSENGER_VOLUME : Array[Int] = { //array that lists required passenger to reach certain loyalty, the index is the Loyalty
    val m1 = estimateM1
    val m99 = 1.0001
    val result = Array.fill(AirlineAppeal.MAX_LOYALTY + 1)(0.0)
    
    result(1) = 1 //init
    for (x <- 1 until AirlineAppeal.MAX_LOYALTY) {
      val mx = m1 - (m1 - m99) * (x.toDouble - 1) / (AirlineAppeal.MAX_LOYALTY - 2) //m(x) = m(1) - (m(1) - m(99)) * (x - 1) / 98
      result(x + 1) = result(x) * mx 
    }
    
    result.map(_.toInt)
  }
  
//  private[patson] val getNewLoyalty : (Double, Double) => Double = (currentLoyalty, targetLoyalty) =>  {
//    if (currentLoyalty < targetLoyalty) {
//      if (currentLoyalty + LOYALTY_INCREMENT_BY_FLIGHTS >= targetLoyalty) {
//        targetLoyalty
//      } else {
//        currentLoyalty + LOYALTY_INCREMENT_BY_FLIGHTS
//      }
//    } else {
//      if (currentLoyalty - LOYALTY_DECREMENT_BY_FLIGHTS <= targetLoyalty) {
//        targetLoyalty
//      } else {
//        currentLoyalty - LOYALTY_DECREMENT_BY_FLIGHTS
//      }
//    }
//  }
  
  
  def estimateM1 : Double = {
    val m99 = 1.0001
    var m1Min = m99
    var m1Max = 2.0 //super crazy here
    val p = 1000000 / 52  //a city with 1 mil pop
    var pEst = 1.0 //estimate when n = 1
    var result = 0.0
    while (p.toInt != pEst.toInt) {
      pEst = 1.0 
      val m1Est = (m1Min + m1Max) / 2
      
      for (x <- 1 to AirlineAppeal.MAX_LOYALTY - 1) { //to reach the next loyalty step
        val mx = m1Est - (m1Est - m99) * (x.toDouble - 1) / (AirlineAppeal.MAX_LOYALTY - 2) //m(x) = m(1) - (m(1) - m(99)) * (x - 1) / 98
        pEst *= mx        
      }
      
      if (pEst > p) { //pEst to large
        m1Max = m1Est
      } else {
        m1Min = m1Est
      }
      
      result = m1Est
    }
    
    result
  }
  
 
}