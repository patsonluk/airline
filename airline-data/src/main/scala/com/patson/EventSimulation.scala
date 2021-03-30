package com.patson

import com.patson.data.{AirportSource, CycleSource, EventSource, LinkStatisticsSource}
import com.patson.model.event.{EventType, Olympics, OlympicsAirlineVote, OlympicsAirlineVoteWithWeight, OlympicsVoteRound}
import com.patson.model.{Airline, Airport, AirportFeatureType, Computation, OlympicsInProgressFeature, OlympicsPreparationsFeature}

import scala.collection.{MapView, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.Random


object EventSimulation {
  val MAX_HISTORY_DURATION = 40 * Olympics.WEEKS_PER_YEAR //40 years
  def simulate(cycle: Int): Unit = {
    //purge
    EventSource.deleteEventsBeforeCycle(CycleSource.loadCycle() - MAX_HISTORY_DURATION)
    val events = EventSource.loadEvents().sortBy(_.startCycle).reverse

    val currentOlympicsOption = events.find(_.eventType == EventType.OLYMPICS)map(_.asInstanceOf[Olympics])
    simulateOlympics(cycle, currentOlympicsOption)
  }

  def simulateOlympics(cycle: Int, currentOlympicsOption : Option[Olympics]): Unit = {
    currentOlympicsOption.foreach { currentOlympics =>
      if (!currentOlympics.isActive(cycle) && currentOlympics.isActive(cycle - 1)) { //just finished last turn
        simulateOlympicsEnding(currentOlympics)
      }
    }


    val createNewOlympics = currentOlympicsOption.isEmpty || !currentOlympicsOption.get.isActive(cycle)

    val olympics =
      if (createNewOlympics) {
        val newOlympics = Olympics(startCycle = cycle)
        EventSource.saveEvents(List(newOlympics))
        newOlympics
      } else {
        currentOlympicsOption.get
      }


    if (olympics.isNewYear(cycle)) { //then action!
      olympics.currentYear(cycle) match {
        case 1 =>
          val candidates = selectCandidates()
          EventSource.saveOlympicsCandidates(olympics.id, candidates)

          val affectedAirports = candidates.map { principalAirport =>
            (principalAirport, simulateOlympicsAffectedAirport(principalAirport))
          }.toMap

          println(s"Olympics airport(s) within radius: $affectedAirports")
          EventSource.saveOlympicsAffectedAirports(olympics.id, affectedAirports)

        case 2 =>
          //tally vote using exhaustive
          val voteRounds = simulateOlympicsVoteRounds(olympics)
          EventSource.saveOlympicsVoteRounds(olympics.id, voteRounds)

          if (voteRounds.size > 0) {
            val selectedAirport = voteRounds.last.votes.toList.sortBy(_._2).last._1
            println(s"Olympics airport: $selectedAirport")
          }
          //mark airports as in preparations
          Olympics.getSelectedAffectedAirports(olympics.id).foreach { airport =>
            AirportSource.saveAirportFeature(airport.id, OlympicsPreparationsFeature(1))
          }

          //set airline target
          val goals = simulateOlympicsPassengerGoals(olympics)
          EventSource.saveOlympicsAirlinePassengerGoals(olympics.id, goals)
        case 3 =>


        case 4 =>
          //mark airports as in progress
          Olympics.getSelectedAffectedAirports(olympics.id).foreach { airport =>
            AirportSource.deleteAirportFeature(airport.id, AirportFeatureType.OLYMPICS_PREPARATIONS)
            AirportSource.saveAirportFeature(airport.id, OlympicsInProgressFeature(1))
          }
      }
    }

  }

  def simulateOlympicsCandidates() = {
    selectCandidates()
  }

  val MAX_CANDIDATES_COUNT = 6
  val CANDIDATE_MIN_SIZE = 5
  val CANDIDATE_MIN_POPULATION = 1000000

  def selectCandidates() : List[Airport] = {
    selectCandidates(AirportSource.loadAllAirports())
  }

  val HOST_COUNTRY_COOLDOWN = 4 //should not be hosting it in the last 4 times
  val HOST_ZONE_COOLDOWN = 2 //should not be hosting it in last 2 times
  def selectCandidates(allAirports : List[Airport]) : List[Airport] = {
    val previousOlympics = EventSource.loadEvents().filter(_.eventType == EventType.OLYMPICS).sortBy(_.startCycle).dropRight(1) //drop the current one

    val cooldownCountries : List[String] = previousOlympics.takeRight(HOST_COUNTRY_COOLDOWN).map(_.asInstanceOf[Olympics]).map{ olympics =>
      Olympics.getSelectedAirport(olympics.id).map(_.countryCode)
    }.flatten

    val cooldownZones : List[String] = previousOlympics.takeRight(HOST_ZONE_COOLDOWN).map(_.asInstanceOf[Olympics]).map{ olympics =>
      Olympics.getSelectedAirport(olympics.id).map(_.zone)
    }.flatten

    val randomizedAirports = Random.shuffle(allAirports.filter { airport =>
      airport.size >= CANDIDATE_MIN_SIZE &&
      airport.population >= CANDIDATE_MIN_POPULATION &&
      !cooldownCountries.contains(airport.countryCode) &&
      !cooldownZones.contains(airport.zone)
    })
    val candidates = ListBuffer[Airport]()
    val candidateCountryCodes = mutable.HashSet[String]()
    randomizedAirports.foreach { airport =>
      if (!candidateCountryCodes.contains(airport.countryCode)) {
        candidates.append(airport)
      }
      candidateCountryCodes.add(airport.countryCode)
      if (candidates.length >= MAX_CANDIDATES_COUNT) {
        return candidates.sortBy(_.id).toList //sort by id for more predictable result
      }
    }
    return candidates.sortBy(_.id).toList //sort by id for more predictable result
  }

  val AFFECT_RADIUS = 80 //80km
  def simulateOlympicsAffectedAirport(principalAirport : Airport): List[Airport] = {
    Computation.getDomesticAirportWithinRange(principalAirport, AFFECT_RADIUS)
  }

  def simulateOlympicsVoteRounds(olympics: Olympics) : List[OlympicsVoteRound] = {
    val voteWeights = Olympics.getVoteWeights().view.map {
      case(airline, weight) => (airline.id, weight)
    }.toMap

    val airlineVotes: Map[Airline, OlympicsAirlineVoteWithWeight] = EventSource.loadOlympicsAirlineVotes(olympics.id).view.mapValues {
      airlineVote => airlineVote.withWeight(voteWeights.getOrElse(airlineVote.airline.id, 0))
    }.toMap
    val candidates: List[Airport] = EventSource.loadOlympicsCandidates(olympics.id)
    simulateOlympicsVoteRounds(candidates, airlineVotes)
  }

  def simulateOlympicsVoteRounds(candidates: List[Airport], airlineVotes: Map[Airline, OlympicsAirlineVoteWithWeight]) : List[OlympicsVoteRound]  = {
    val voteRoundResults = ListBuffer[OlympicsVoteRound]()
    var voteRound = 1
    val remainingCandidates = ListBuffer[Airport]()
    remainingCandidates.appendAll(candidates)

    while (remainingCandidates.size >= 1) {
      val votesByAirport = mutable.LinkedHashMap[Airport, Int]()
      remainingCandidates.foreach { candidate => //initialize the map
        votesByAirport.put(candidate, 0)
      }
      airlineVotes.foreach {
        case (airline, airlineVote) =>
          airlineVote.voteList.find(votedAirport => remainingCandidates.contains(votedAirport)) match { //go down by the list (highest precedence first), find the first one that is still in the candidates list
            case Some(legitVotedAirport) =>
              val currentVotesForThisAirport = votesByAirport(legitVotedAirport)
              votesByAirport.put(legitVotedAirport, currentVotesForThisAirport + airlineVote.weight)
            case None => //should not be this
          }
      }
      println(s"Round $voteRound : Olympics Votes: $votesByAirport")

      val resultOfThisRound = OlympicsVoteRound(round = voteRound, votes = votesByAirport.toMap)
      voteRoundResults.append(resultOfThisRound)

      //eliminate the candidate with least votes
      val evictingCandidate : Airport = votesByAirport.toList.sortBy(_._1.id).sortBy(_._2).apply(0)._1 //sort by airport Id first for more predictable result
      remainingCandidates.remove(remainingCandidates.indexOf(evictingCandidate))

      voteRound += 1
    }
    voteRoundResults.toList
  }

  val BASE_PASSENGER_GOAL = 2000
  val GOAL_BASE_FACTOR = 0.90 //90% of the max possible pax?
  def simulateOlympicsPassengerGoals(olympics: Olympics) = {
    val allLinkStats = LinkStatisticsSource.loadLinkStatisticsByCriteria(List.empty)
    val passengersByAirline: MapView[Airline, Int] = allLinkStats.groupBy(_.key.airline).view.mapValues(_.map(_.passengers).sum)
    val totalPassengers = passengersByAirline.values.sum

    var olympicsTotalPassengers = 0
    for (i <- 0 until Olympics.WEEKS_PER_YEAR) {
      olympicsTotalPassengers += Olympics.getDemandMultiplier(i) * DemandGenerator.OLYMPICS_DEMAND_BASE
    }

    val passengerGoalByAirline = passengersByAirline.mapValues { passengers =>
      val goal = (passengers.toDouble / totalPassengers * olympicsTotalPassengers * GOAL_BASE_FACTOR).toInt
      Math.max(BASE_PASSENGER_GOAL, goal)
    }.toMap

    passengerGoalByAirline
  }

  def simulateOlympicsEnding(olympics : Olympics) = {
    Olympics.getSelectedAffectedAirports(olympics.id).foreach { airport =>
      AirportSource.deleteAirportFeature(airport.id, AirportFeatureType.OLYMPICS_IN_PROGRESS)
    }
  }
  
}
