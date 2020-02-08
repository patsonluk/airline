package com.patson.model.event

import com.patson.data.{AirlineSource, AirportSource, CountrySource, EventSource}
import com.patson.model._

import scala.collection.mutable

abstract class Event(val eventType : EventType.Value, val startCycle : Int, val duration : Int, var id : Int = 0) {
  val isActive = (currentCycle : Int) => startCycle + duration > currentCycle
}

case class Olympics(override val startCycle : Int, override val duration : Int = Olympics.WEEKS_PER_YEAR * 4, var olympicsId : Int = 0) extends Event(EventType.OLYMPICS, startCycle, duration, olympicsId) {
  val currentYear = (currentCycle : Int) => (currentCycle - startCycle) /  Olympics.WEEKS_PER_YEAR + 1
  val isNewYear = (currentCycle : Int) => (currentCycle - startCycle) % Olympics.WEEKS_PER_YEAR == 0
}

object Olympics {
  val WEEKS_PER_YEAR = 4
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

  def getSelectedAffectedAirports(eventId : Int) : List[Airport] = {
    Olympics.getSelectedAirport(eventId) match {
      case Some(principalAirport) => Olympics.getAffectedAirport(eventId, principalAirport)
      case None => List.empty
    }
  }

  def getAffectedAirport(eventId : Int) : Map[Airport, List[Airport]] = {
    EventSource.loadOlympicsAffectedAirports(eventId)
  }

  val VOTE_REPUTATION_THRESHOLD = 40

  def getAffectedAirport(eventId : Int, principalAirport : Airport) : List[Airport] = {
    EventSource.loadOlympicsAffectedAirports(eventId).apply(principalAirport)
  }

  def getVoteWeight(airline : Airline) : Int = {
    val nationalAirlineTitles = CountrySource.loadCountryAirlineTitlesByCriteria(List(("airline", airline.id), ("title", Title.NATIONAL_AIRLINE)))
    computeVoteWeight(airline, !nationalAirlineTitles.isEmpty)
  }
  private def computeVoteWeight(airline : Airline, isNationalAirline : Boolean): Int = {
    var voteWeight =
      if (airline.getReputation() >= VOTE_REPUTATION_THRESHOLD)
        1
      else
        0

    if (isNationalAirline) {
      voteWeight += 1
    }
    voteWeight
  }

  def getVoteWeights() : Map[Airline, Int] = {
    val nationalAirlineIds = CountrySource.loadCountryAirlineTitlesByCriteria(List(("title", Title.NATIONAL_AIRLINE))).map(_.airline.id)
    AirlineSource.loadAllAirlines().map { airline =>
      (airline, computeVoteWeight(airline, nationalAirlineIds.contains(airline.id)))
    }.toMap
  }

  val voteRewardOptions : List[EventReward] = List(OlympicsVoteCashReward(), OlympicsVoteLoyaltyReward())

  val getDemandMultiplier = (weekOfYear: Int) => {
      if (weekOfYear < Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION * 2) {
        1
      } else if (weekOfYear < Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION) {
        4
      } else if (weekOfYear < Olympics.WEEKS_PER_YEAR) { //game is on
        10
      } else {
        0
      }
  }
}

/**
  *
  * @param airline
  * @param voteList from the most favored to the least
  */
case class OlympicsAirlineVote(airline : Airline, voteList : List[Airport]) {
  def withWeight(weight: Int) = {
    OlympicsAirlineVoteWithWeight(airline, weight, voteList)
  }
}

case class OlympicsAirlineVoteWithWeight(airline: Airline, weight: Int, voteList : List[Airport])

/**
  * Starting from round 1
  * @param round
  */
case class OlympicsVoteRound(round : Int, votes : Map[Airport, Int])


object EventType extends Enumeration {
    type EventType = Value
    val OLYMPICS = Value
}

object RewardCategory extends Enumeration {
  type RewardCategory = Value
  val OLYMPICS_VOTE = Value
}

object RewardOption extends Enumeration {
  type RewardOption = Value
  val CASH, LOYALTY = Value
}

abstract class EventReward(val eventType : EventType.Value, val rewardCategory : RewardCategory.Value, val rewardOption : RewardOption.Value) {
  EventReward.lookup.put(rewardOption, this)

  def apply(event: Event, airline : Airline): Unit = {
    applyReward(event, airline)
    EventSource.savePickedRewardOption(event.id, airline.id, this)
  }
  protected def applyReward(event: Event, airline : Airline)

  val description : String
}

object EventReward {
  private val lookup = mutable.HashMap[RewardOption.Value, EventReward]()
  def fromOptionId(optionId : Int) = {
    lookup.get(RewardOption(optionId))
  }
}

case class OlympicsVoteCashReward() extends EventReward(EventType.OLYMPICS, RewardCategory.OLYMPICS_VOTE, RewardOption.CASH) {
  val CASH_BONUS = 10000000 //10 millions
  override def applyReward(event: Event, airline : Airline) = {
    AirlineSource.adjustAirlineBalance(airline.id, CASH_BONUS)
  }

  override val description: String = "$10,000,000 subsidy in cash"
}

case class OlympicsVoteLoyaltyReward() extends EventReward(EventType.OLYMPICS, RewardCategory.OLYMPICS_VOTE, RewardOption.LOYALTY) {
  val LOYALTY_BONUS = 2
  override def applyReward(event: Event, airline : Airline) = {
    val bonus = AirlineBonus(BonusType.OLYMPICS_VOTE, AirlineAppeal(loyalty = LOYALTY_BONUS, awareness = 0), Some(event.startCycle + event.duration))
    Olympics.getAffectedAirport(event.id, Olympics.getSelectedAirport(event.id).get).foreach { affectedAirport =>
      AirportSource.saveAirlineAppealBonus(affectedAirport.id, airline.id, bonus)
    }
  }

  override val description: String = "+2 loyalty bonus on airports around the host city until the end of Olympics"
}


