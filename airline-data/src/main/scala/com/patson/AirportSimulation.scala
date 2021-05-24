package com.patson

import java.util.Random

import com.patson.data._
import com.patson.model._
import com.patson.util.{AirlineCache, AirportCache, ChampionUtil}

import scala.collection.{immutable, mutable}
import scala.collection.mutable.{ListBuffer, Map, Set}

object AirportSimulation {
  val AWARENESS_DECAY = 0.1
  val AWARENESS_INCREMENT_WITH_LINKS = 0.5
  val AWARENESS_INCREMENT_WITH_HQ = 1.0
  val AWARENESS_INCREMENT_WITH_BASE = 0.5
  val AWARENESS_INCREMENT_MAX_WITH_HQ = 50 //how much awareness will increment to just because of being a HQ
  val AWARENESS_INCREMENT_MAX_WITH_BASE = 30 //how much awareness will increment to just because of being a HQ
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

    println("Compute awareness")
    simulateAwareness(allAirports, linksByFromAirportId, linksByToAirportId)

    //update the loyalist on airports based on link consumption
    println("Adjust loyalist by link consumptions")
    simulateLoyalists(allAirports, linkRidershipDetails, cycle)


    println("Finished simulation of loyalty by link consumption")


    println("Finished loyalist and awareness simulation")
    airportProjectSimulation(allAirports)

    AirportSource.purgeAirlineAppealBonus(cycle)
  }

  def simulateAwareness(allAirports : List[Airport], linksByFromAirportId : immutable.Map[Int, List[Link]], linksByToAirportId : immutable.Map[Int, List[Link]]) = {
    allAirports.foreach { airport =>
      val updatingAwareness = mutable.HashMap[Int, Double]()
      airport.getAirlineBaseAppeals().foreach {
        case(airlineId, AirlineAppeal(loyalty, awareness)) =>
          //decay
          val newAwareness = if (awareness - AWARENESS_DECAY <= 0) 0 else awareness - AWARENESS_DECAY
          updatingAwareness.put(airlineId, newAwareness)
      }
      //add base on bases
      airport.getAirlineBases().values.foreach { base =>
        val airline = base.airline
        var awareness : Double = updatingAwareness.getOrElse(airline.id, 0)

        if (base.headquarter) {
          if (awareness < AWARENESS_INCREMENT_MAX_WITH_HQ) {
            awareness += AWARENESS_INCREMENT_WITH_HQ
          }
        } else {
          if (awareness < AWARENESS_INCREMENT_MAX_WITH_BASE) {
            awareness += AWARENESS_INCREMENT_WITH_BASE
          }
        }
        if (awareness > AirlineAppeal.MAX_AWARENESS) {
          awareness = AirlineAppeal.MAX_AWARENESS
        }
        updatingAwareness.put(airline.id, awareness)
      }
      //adjust awareness by links
      val linksOfThisAirport : List[Link] = linksByFromAirportId.get(airport.id).getOrElse(List.empty[Link]) ++ linksByToAirportId.get(airport.id).getOrElse(List.empty[Link])
      linksOfThisAirport.map(_.airline.id).toSet.foreach { airlineIdConnectedToThisAirport : Int =>
        val existingAwareness : Double = updatingAwareness.getOrElse(airlineIdConnectedToThisAirport, 0)
        val newAwareness =
          if ((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) >= AirlineAppeal.MAX_AWARENESS) {
            AirlineAppeal.MAX_AWARENESS
          } else {
            existingAwareness + AWARENESS_INCREMENT_WITH_LINKS
          }
        //airport.setAirlineAwareness(airlineId, newAwareness)
        updatingAwareness.put(airlineIdConnectedToThisAirport, newAwareness)
      }


      if (!updatingAwareness.isEmpty || !airport.getAirlineBaseAppeals().isEmpty) { //2nd or is for removal of existing appeals
        AirportSource.replaceAirlineAppeals(airport.id, updatingAwareness.toMap)
      }

    }
  }


  val random = new Random()
  val LOYALIST_HISTORY_SAVE_INTERVAL = 10 //every 10 cycles
  val LOYALIST_HISTORY_ENTRY_MAX = 50

  def getHistoryCycle(lastCompletedCycle : Int, delta : Int): Int = {
    val baseCycle = lastCompletedCycle - lastCompletedCycle % LOYALIST_HISTORY_SAVE_INTERVAL
    baseCycle + delta * LOYALIST_HISTORY_SAVE_INTERVAL
  }

  def simulateLoyalists(allAirports : List[Airport], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int], cycle : Int) = {
    val existingLoyalistByAirportId : immutable.Map[Int, List[Loyalist]] = LoyalistSource.loadLoyalistsByCriteria(List.empty).groupBy(_.airport.id)
    val (updatingLoyalists,deletingLoyalists) = computeLoyalists(allAirports, linkRidershipDetails, existingLoyalistByAirportId)
    println(s"Updating ${updatingLoyalists.length} loyalists entries")
    LoyalistSource.updateLoyalists(updatingLoyalists)

    println(s"Deleting ${deletingLoyalists.length} loyalists entries")
    LoyalistSource.deleteLoyalists(deletingLoyalists)

    val allLoyalists = LoyalistSource.loadLoyalistsByCriteria(List.empty)
    println(s"Computing loyalist info with ${allLoyalists.length} entries")

    //compute champion info
    ChampionUtil.updateAirportChampionInfo(allLoyalists)
    println("Done computing champions")

    if (cycle % LOYALIST_HISTORY_SAVE_INTERVAL == 0) {
      val cutoff = cycle - LOYALIST_HISTORY_ENTRY_MAX * LOYALIST_HISTORY_SAVE_INTERVAL
      println(s"Purging loyalist history before cycle $cutoff")
      LoyalistSource.deleteLoyalistHistoryBeforeCycle(cutoff)

      val historyEntries = allLoyalists.map(LoyalistHistory(_, cycle))
      println(s"Saving ${historyEntries.length} loyalist history entries")
      LoyalistSource.updateLoyalistHistory(historyEntries)
    }
  }

  val MAX_LOYALIST_FLIP_RATIO = 1
  val NEUTRAL_SATISFACTION = 0.6
  private[patson] def computeLoyalists(allAirports : List[Airport], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int], existingLoyalistByAirportId : immutable.Map[Int, List[Loyalist]]) = {
    val result = ListBuffer[Loyalist]() //airlineId, amount

    linkRidershipDetails.groupBy(_._1._1.fromAirport).foreach {
      case ((fromAirport, passengersFromThisAirport)) =>
        val loyalistIncrementOfAirlines = Map[Int, Int]() //airlineId, delta
        //passengersFromThisAirport.filter(_._1._1.preference.loyaltySensitivity > 0).toList.foreach { //only count pax that actually cares about loyalty now
        passengersFromThisAirport.toList.foreach {
          case ((passengerGroup, toAirport, route), paxCount) =>
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
            //println(s"${linkConsideration.cost} vs standard price $standardPrice. Conversion Ratio : ${conversionRatio}")


            conversionRatio = conversionRatio / flightLinks.length // for example if the route has 3 legs, it will convert at most 1/3 of the ratio

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

        //now with delta, see what the flips are
        loyalistIncrementOfAirlines.foreach {
          case (gainAirlineId, increment) => //split into chunks for better randomness
            if (increment > 0) {
              var unclaimedLoyalist = (fromAirport.population - existingLoyalistOfThisAirport.values.sum).toInt
              var remainingIncrement = increment
              while (remainingIncrement > 0) {
                val chunk = if (remainingIncrement <= CHUNK_SIZE) remainingIncrement else CHUNK_SIZE
                val flipTrigger = random.nextInt(fromAirport.population.toInt)

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
                    if (unclaimedLoyalist == 0) { //ignore, can no longer draw loyalist

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

  def airportProjectSimulation(allAirports : List[Airport]) = {
    import ProjectStatus._
    println("simulating airport projects")
    
    val inProgressProjects = AirportSource.loadAllAirportProjects().filter { _.status != COMPLETED }
  }
  
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