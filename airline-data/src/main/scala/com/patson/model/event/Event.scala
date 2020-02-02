package com.patson.model.event

import com.patson.data.EventSource
import com.patson.model._

abstract class Event(val eventType : EventType.Value, val startCycle : Int, val duration : Int, var id : Int = 0) {
  val isActive = (currentCycle : Int) => startCycle + duration > currentCycle
}

case class Olympics(override val startCycle : Int, override val duration : Int = Olympics.WEEKS_PER_YEAR * 4, var olympicsId : Int = 0) extends Event(EventType.OLYMPICS, startCycle, duration, olympicsId) {
  val currentYear = (currentCycle : Int) => (currentCycle - startCycle) /  Olympics.WEEKS_PER_YEAR + 1
  val isNewYear = (currentCycle : Int) => (currentCycle - startCycle) % Olympics.WEEKS_PER_YEAR == 0
}

object Olympics {
  val WEEKS_PER_YEAR = 52
  val GAMES_DURATION = 4
  def getCandidates(eventId : Int) : List[Airport] = {
    EventSource.loadOlympicsCandidates(eventId)
  }

  def getAirlineVotes(eventId : Int) : Map[Airline, OlympicsAirlineVote] = {
    EventSource.loadOlympicsAirlineVotes(eventId)
  }

  def getVoteRounds(eventId : Int) : List[OlympicsVoteRound] = {
    EventSource.loadOlympicsVoteRounds(eventId)
  }

  def getSelectedAirport(eventId : Int) : Option[Airport] = {
    val voteRounds = getVoteRounds(eventId : Int);
    if (voteRounds.isEmpty) {
      None
    } else {
      Some(voteRounds.last.votes.toList.sortBy(_._2).last._1)
    }
  }

  def getAffectedAirport(eventId : Int) : Map[Airport, List[Airport]] = {
    EventSource.loadOlympicsAffectedAirports(eventId)
  }

  def getAffectedAirport(eventId : Int, principalAirport : Airport) : List[Airport] = {
    EventSource.loadOlympicsAffectedAirports(eventId).apply(principalAirport)
  }
}

/**
  *
  * @param airline
  * @param voteWeight
  * @param voteList from the most favored to the least
  */
case class OlympicsAirlineVote(airline : Airline, voteWeight : Int, voteList : List[Airport])

/**
  * Starting from round 1
  * @param round
  */
case class OlympicsVoteRound(round : Int, votes : Map[Airport, Int])


object EventType extends Enumeration {
    type EventType = Value
    val OLYMPICS = Value
}


