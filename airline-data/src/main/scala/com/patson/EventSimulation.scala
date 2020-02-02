package com.patson

import com.patson.data.{AirportSource, EventSource}
import com.patson.model.event.{EventType, Olympics, OlympicsAirlineVote, OlympicsVoteRound}
import com.patson.model.{Airline, Airport, Computation}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


object EventSimulation {




  def simulate(cycle: Int): Unit = {
    val events = EventSource.loadEvents().sortBy(_.startCycle).reverse

    val currentOlympicsOption = events.find(_.eventType == EventType.OLYMPICS)map(_.asInstanceOf[Olympics])
    simulateOlympics(cycle, currentOlympicsOption)
  }

  def simulateOlympics(cycle: Int, currentOlympicsOption : Option[Olympics]): Unit = {
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
        case 3 =>
          //nothing?
        case 4 =>
          //nothing?
      }

    }

  }

  def simulateOlympicsCandidates() = {
    selectCandidates()
  }

  val MAX_CANDIDATES_COUNT = 6
  val CANDIDATE_MIN_SIZE = 5

  def selectCandidates() : List[Airport] = {
    selectCandidates(AirportSource.loadAllAirports())
  }

  def selectCandidates(allAirports : List[Airport]) : List[Airport] = {
    val randomizedAirports = Random.shuffle(allAirports.filter(_.size >= CANDIDATE_MIN_SIZE))
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
    val affectedAirports = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(principalAirport, airport) <= AFFECT_RADIUS) {
        affectedAirports.append(airport)
      }
    }
    affectedAirports.toList
  }

  def simulateOlympicsVoteRounds(olympics: Olympics) : List[OlympicsVoteRound] = {
    val airlineVotes: Map[Airline, OlympicsAirlineVote] = EventSource.loadOlympicsAirlineVotes(olympics.id)
    val candidates: List[Airport] = EventSource.loadOlympicsCandidates(olympics.id)
    simulateOlympicsVoteRounds(candidates, airlineVotes)
  }

  def simulateOlympicsVoteRounds(candidates: List[Airport], airlineVotes: Map[Airline, OlympicsAirlineVote]) : List[OlympicsVoteRound]  = {
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
          airlineVote.voteList.find(votedAirport => remainingCandidates.contains(votedAirport)) match { //go down by the list (highest priority first), find the first one that is still in the candidates list
            case Some(legitVotedAirport) =>
              val currentVotesForThisAirport = votesByAirport(legitVotedAirport)
              votesByAirport.put(legitVotedAirport, currentVotesForThisAirport + airlineVote.voteWeight)
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
  
}
