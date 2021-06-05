package com.patson.model.event

import com.patson.data.{AirlineSource, AirportSource, CountrySource, EventSource}
import com.patson.model._

import scala.collection.mutable

abstract class Event(val eventType : EventType.Value, val startCycle : Int, val duration : Int, var id : Int = 0) {
  val isActive = (currentCycle : Int) => startCycle + duration > currentCycle
}

case class Olympics(override val startCycle : Int, override val duration : Int = Olympics.WEEKS_PER_YEAR * 4, var olympicsId : Int = 0) extends Event(EventType.OLYMPICS, startCycle, duration, olympicsId) {
  val currentYear = (currentCycle : Int) => {
    (currentCycle - startCycle) /  Olympics.WEEKS_PER_YEAR + 1
  } //start from 1 to 4
  val isNewYear = (currentCycle : Int) => currentWeek(currentCycle) == 0
  val currentWeek = (currentCycle : Int) => (currentCycle - startCycle) % Olympics.WEEKS_PER_YEAR //start from 0 to WEEKS_PER_YEAR

  import OlympicsStatus._
  val status = (currentCycle : Int) =>
    if (isActive(currentCycle)) {
      currentYear(currentCycle) match {
        case 1 => VOTING
        case 2 => HOST_CITY_SELECTED
        case 3 => PREPARATION
        case 4 =>
          val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - currentWeek(currentCycle)
          if (weeksBeforeGames > 0) {
            OLYMPICS_YEAR
          } else {
            IN_PROGRESS
          }
        case _ => UNKNOWN
      }
    } else {
      CONCLUDED
    }
}

object OlympicsStatus extends Enumeration {
  type RewardCategory = Value
  val VOTING, HOST_CITY_SELECTED, PREPARATION, OLYMPICS_YEAR, IN_PROGRESS, CONCLUDED, UNKNOWN = Value
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

  def getSelectedAffectedAirports(eventId : Int) : List[Airport] = {
    Olympics.getSelectedAirport(eventId) match {
      case Some(principalAirport) => Olympics.getAffectedAirport(eventId, principalAirport)
      case None => List.empty
    }
  }

  def getAffectedAirport(eventId : Int) : Map[Airport, List[Airport]] = {
    EventSource.loadOlympicsAffectedAirports(eventId)
  }

  val VOTE_REPUTATION_THRESHOLD = 30

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

  def getGoalByCycle(eventId : Int, airlineId : Int, cycle : Int): Option[Int] = {
    EventSource.loadEventById(eventId) match {
      case Some(olympics: Olympics) =>
        EventSource.loadOlympicsAirlineGoal(eventId, airlineId) match {
          case Some(goal) =>
            Some(goal * getDemandMultiplier(olympics.currentWeek(cycle)) / demandMultiplierSum)
          case None => None
        }
      case _ =>
        None
    }
  }

  val voteRewardOptions : List[EventReward] = List(OlympicsVoteCashReward(), OlympicsVoteLoyaltyReward())
  val passengerRewardOptions : List[EventReward] = List(OlympicsPassengerCashReward(), OlympicsPassengerLoyaltyReward(), OlympicsPassengerReputationReward())

  val getDemandMultiplier = (weekOfYear: Int) => {
      if (weekOfYear < Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION * 12) {
        1
      } else if (weekOfYear < Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION * 3) { //3 months before the game
        2
      } else if (weekOfYear < Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION) { //1 momnth beofre the game
        4
      } else if (weekOfYear < Olympics.WEEKS_PER_YEAR) { //game is on
        10
      } else {
        0
      }
  }
  val demandMultiplierSum = (0 until WEEKS_PER_YEAR).map(getDemandMultiplier(_)).sum
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
  val OLYMPICS_VOTE, OLYMPICS_PASSENGER = Value
}

object RewardOption extends Enumeration {
  type RewardOption = Value
  val CASH, LOYALTY, REPUTATION = Value
}

abstract class EventReward(val eventType : EventType.Value, val rewardCategory : RewardCategory.Value, val rewardOption : RewardOption.Value) {
  EventReward.lookup.put((rewardCategory, rewardOption), this)

  def apply(event: Event, airline : Airline): Unit = {
    applyReward(event, airline)
    EventSource.savePickedRewardOption(event.id, airline.id, this)
  }
  protected def applyReward(event: Event, airline : Airline)

  val description : String
}

object EventReward {
  private val lookup = mutable.HashMap[(RewardCategory.Value, RewardOption.Value), EventReward]()
  def fromId(categoryId : Int, optionId : Int) = {
    lookup.get((RewardCategory(categoryId), RewardOption(optionId)))
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

case class OlympicsPassengerCashReward() extends EventReward(EventType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.CASH) {
  val MIN_CASH_REWARD = 20000000 //20 millions
  val SCORE_MULTIPLIER = 500



  override def applyReward(event: Event, airline : Airline) = {
    val stats: Map[Int, BigDecimal] = EventSource.loadOlympicsAirlineStats (event.id, airline.id).toMap
    val totalScore = stats.view.values.sum
    val reward = Math.max((totalScore * 500).toLong, MIN_CASH_REWARD)
    AirlineSource.adjustAirlineBalance(airline.id, reward)
  }

  override val description: String = "$20,000,000 or $500 * score (whichever is higher) cash reward"
}

case class OlympicsPassengerLoyaltyReward() extends EventReward(EventType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.LOYALTY) {
  val LOYALTY_BONUS = 3
  override def applyReward(event: Event, airline : Airline) = {
    val bonus = AirlineBonus(BonusType.OLYMPICS_PASSENGER, AirlineAppeal(loyalty = LOYALTY_BONUS, awareness = 0), Some(event.startCycle + event.duration * 2))
    Olympics.getAffectedAirport(event.id, Olympics.getSelectedAirport(event.id).get).foreach { affectedAirport =>
      AirportSource.saveAirlineAppealBonus(affectedAirport.id, airline.id, bonus)
    }
  }

  override val description: String = s"+$LOYALTY_BONUS loyalty bonus on airports around the host city for 4 years after the Olympics Games ended"
}

case class OlympicsPassengerReputationReward() extends EventReward(EventType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.REPUTATION) {
  val REPUTATION_BONUS = 5
  override def applyReward(event: Event, airline : Airline) = {
    AirlineSource.adjustAirlineReputation(airline.id, REPUTATION_BONUS)
  }

  override val description: String = s"+$REPUTATION_BONUS reputation boost (one time only, reputation will eventually drop back to normal level)"
}

