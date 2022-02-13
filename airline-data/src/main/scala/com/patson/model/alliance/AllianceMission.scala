package com.patson.model.alliance

import com.patson.data.{AirlineSource, AirportSource, CountrySource, CycleSource, AllianceMissionSource}
import com.patson.model._

import scala.collection.mutable

abstract class AllianceMission() extends IdObject {
  val missionType : AllianceMissionType.Value
  val startCycle : Int
  val duration : Int

  val isActive = (currentCycle : Int) => startCycle + duration > currentCycle
}

object AllianceMissionType extends Enumeration {
  type AllianceMissionType = Value
  val TOTAL_PAX, TOTAL_PREMIUM_PAX, TOTAL_LOUNGE_VISIT, AIRPORT_RANKING, COUNTRY_RANKING, CONTINENT_RANKING, TOTAL_REPUTATION, TOTAL_LOYALIST, SATISFACTION_FACTOR, TOTAL_REVENUE = Value
}

import AllianceMissionType._
case class TotalPaxMission(override val startCycle : Int, override val duration : Int, var id : Int = 0) extends AllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_PAX
}

object AllianceMissionStatus extends Enumeration {
  type AllianceMissionStatus = Value
  val SELECTION, IN_PROGRESS, CONCLUDED = Value
}



object AllianceMission {
  def buildAllianceMission(startCycle : Int, duration : Int, id : Int, properties : Map[String, Long]) = {

  }
}


object RewardOption extends Enumeration {
  type RewardOption = Value
  val CASH, LOYALTY, REPUTATION, DELEGATE = Value
}

abstract class AllianceMissionReward(val eventType : AllianceMissionType.Value, val rewardCategory : RewardCategory.Value, val rewardOption : RewardOption.Value) {
  AllianceMissionReward.lookup.put((rewardCategory, rewardOption), this)

  def apply(event: AllianceMission, airline : Airline): Unit = {
    applyReward(event, airline)
    AllianceMissionSource.savePickedRewardOption(event.id, airline.id, this)
  }
  protected def applyReward(event: AllianceMission, airline : Airline)

  val description : String
  def redeemDescription(eventId: Int, airlineId: Int) = description
}

object AllianceMissionReward {
  private val lookup = mutable.HashMap[(RewardCategory.Value, RewardOption.Value), AllianceMissionReward]()
  def fromId(categoryId : Int, optionId : Int) = {
    lookup.get((RewardCategory(categoryId), RewardOption(optionId)))
  }
}

case class OlympicsVoteCashReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_VOTE, RewardOption.CASH) {
  val CASH_BONUS = 10000000 //10 millions
  override def applyReward(event: AllianceMission, airline : Airline) = {
    AirlineSource.adjustAirlineBalance(airline.id, CASH_BONUS)
  }

  override val description: String = "$10,000,000 subsidy in cash"
}

case class OlympicsVoteLoyaltyReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_VOTE, RewardOption.LOYALTY) {
  val LOYALTY_BONUS = 10
  override def applyReward(event: AllianceMission, airline : Airline) = {
    val bonus = AirlineBonus(BonusType.OLYMPICS_VOTE, AirlineAppeal(loyalty = LOYALTY_BONUS, awareness = 0), Some(event.startCycle + event.duration))
    Olympics.getAffectedAirport(event.id, Olympics.getSelectedAirport(event.id).get).foreach { affectedAirport =>
      AirportSource.saveAirlineAppealBonus(affectedAirport.id, airline.id, bonus)
    }
  }

  override val description: String = s"+$LOYALTY_BONUS loyalty bonus on airports around the host city until the end of Olympics"
}

case class OlympicsPassengerCashReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.CASH) {
  val MIN_CASH_REWARD = 20000000 //20 millions
  val SCORE_MULTIPLIER = 500


  def computeReward(eventId: Int, airlineId : Int) = {
    val stats: Map[Int, BigDecimal] = AllianceMissionSource.loadOlympicsAirlineStats (eventId, airlineId).toMap
    val totalScore = stats.view.values.sum
    Math.max((totalScore * 500).toLong, MIN_CASH_REWARD)
  }

  override def applyReward(event: AllianceMission, airline : Airline) = {
    val reward = computeReward(event.id, airline.id)
    AirlineSource.adjustAirlineBalance(airline.id, reward)
  }

  override val description: String = "$20,000,000 or $500 * score (whichever is higher) cash reward"
  override def redeemDescription(eventId: Int, airlineId : Int) = s"$$${java.text.NumberFormat.getIntegerInstance.format(computeReward(eventId, airlineId))} cash reward"

}

case class OlympicsPassengerLoyaltyReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.LOYALTY) {
  val LOYALTY_BONUS = 25
  override def applyReward(event: AllianceMission, airline : Airline) = {
    val bonus = AirlineBonus(BonusType.OLYMPICS_PASSENGER, AirlineAppeal(loyalty = LOYALTY_BONUS, awareness = 0), Some(event.startCycle + event.duration * 2))
    Olympics.getAffectedAirport(event.id, Olympics.getSelectedAirport(event.id).get).foreach { affectedAirport =>
      AirportSource.saveAirlineAppealBonus(affectedAirport.id, airline.id, bonus)
    }
  }

  override val description: String = s"+$LOYALTY_BONUS loyalty bonus on airports around the host city for 4 years after the Olympics Games ended"
}

case class OlympicsPassengerReputationReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.REPUTATION) {
  val REPUTATION_BONUS = 10
  override def applyReward(event: AllianceMission, airline : Airline) = {
    AirlineSource.adjustAirlineReputation(airline.id, REPUTATION_BONUS)
  }

  override val description: String = s"+$REPUTATION_BONUS reputation boost (one time only, reputation will eventually drop back to normal level)"
}

case class OlympicsPassengerDelegateReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_PASSENGER, RewardOption.DELEGATE) {
  val BASE_BONUS = 2
  val MAX_BONUS = 6
  val DURATION = 52 * 4
  override def applyReward(event: AllianceMission, airline : Airline) = {
    val cycle = CycleSource.loadCycle()
    val bonus = computeReward(event.id, airline.id)
    AirlineSource.saveAirlineModifier(airline.id, DelegateBoostAirlineModifier(bonus, DURATION, cycle))
  }

  def computeReward(eventId: Int, airlineId : Int) = {
    val stats: Map[Int, BigDecimal] = AllianceMissionSource.loadOlympicsAirlineStats (eventId, airlineId).toMap
    val totalScore = stats.view.values.sum
    AllianceMissionSource.loadOlympicsAirlineGoal(eventId, airlineId) match {
      case Some(goal) =>
        val overachieverRatio = Math.min(1.0, totalScore.toDouble / goal / 4) //at 400% then it claim 1.0 overachiever ratio
        val extraBonus = ((MAX_BONUS - BASE_BONUS) * overachieverRatio).toInt //could be 0
        BASE_BONUS + extraBonus
      case None => 0
    }

  }

  override val description: String = s"+$BASE_BONUS to $MAX_BONUS delegates for $DURATION weeks"
  override def redeemDescription(eventId: Int, airlineId : Int) = s"${computeReward(eventId, airlineId)} extra delegates for $DURATION weeks"
}

