package com.patson

import com.patson.data.{AirportSource, EventSource}
import com.patson.model.event.{EventType, Olympics, OlympicsVoteRound}
import com.patson.model.{Airport, Computation}

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
      olympics.currentYear match {
        case 1 =>
          simulateOlympicsCandidates(cycle, olympics)
        case 2 =>
          //tally vote using exhaustive
          val selectedAirport = simulateOlympicsVoteRounds(cycle, olympics)
          selectedAirport match {
            case Some(airport) =>
              simulateOlympicsAffectedAirport(olympics, airport)
            case None =>
          }

        case 3 =>
          //nothing?
        case 4 =>
          //nothing?
      }

    }

  }

  def simulateOlympicsCandidates(cycle: Int, olympics: Olympics) = {
    val candidates = selectCandidates()
    EventSource.saveOlympicsCandidates(olympics.id, candidates)
  }

  val MAX_CANDIDATES_COUNT = 6
  val CANDIDATE_MIN_SIZE = 5

  def selectCandidates() : List[Airport] = {
    val randomizedAirports = Random.shuffle(AirportSource.loadAllAirports().filter(_.size >= CANDIDATE_MIN_SIZE))
    val candidates = ListBuffer[Airport]()
    val candidateCountryCodes = mutable.HashSet[String]()
    randomizedAirports.foreach { airport =>
      if (!candidateCountryCodes.contains(airport.countryCode)) {
        candidates.append(airport)
      }
      if (candidates.length >= MAX_CANDIDATES_COUNT) {
        return candidates.toList
      }
    }
    return candidates.toList
  }

  val AFFECT_RADIUS = 150 //150km
  def simulateOlympicsAffectedAirport(olympics: Olympics, selectedAirport : Airport): Unit = {
    val affectedAirports = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(selectedAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(selectedAirport, airport) <= AFFECT_RADIUS) {
        affectedAirports.append(airport)
      }
    }

    EventSource.saveOlympicsAffectedAirports(olympics.id, affectedAirports.toList)
  }


  def simulateOlympicsVoteRounds(cycle: Int, olympics: Olympics) : Option[Airport] = {
    val airlineVotes = EventSource.loadOlympicsAirlineVotes(olympics.id)
    var candidates = EventSource.loadOlympicsCandidates(olympics.id)

    val voteRoundResults = ListBuffer[OlympicsVoteRound]()
    var voteRound = 1
    while (candidates.size > 2) {
      val votesByAirport = mutable.HashMap[Airport, Int]()
      airlineVotes.foreach {
        case (airline, airlineVote) =>
          airlineVote.voteList.find(votedAirport => candidates.contains(votedAirport)) match { //go down by the list (highest priority first), find the first one that is still in the candidates list
            case Some(legitVotedAirport) =>
              val currentVotesForThisAirport = votesByAirport.getOrElse(legitVotedAirport, 0)
              votesByAirport.put(legitVotedAirport, currentVotesForThisAirport + airlineVote.voteWeight)
            case None => //should not be this
          }
      }
      println(s"Round $voteRound : Olympics Votes: $votesByAirport")

      val resultOfThisRound = OlympicsVoteRound(round = voteRound, votes = votesByAirport.toMap)
      voteRoundResults.append(resultOfThisRound)

      //eliminate the candidate with least votes
      candidates = votesByAirport.toList.sortBy(_._2).drop(1).map(_._1)

      voteRound += 1
    }
    val selectedAirport = Olympics.getSelectedAirport(olympics.id)
    println(s"Olympic winning bid: $selectedAirport")
    selectedAirport
  }
  
}
