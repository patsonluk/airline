package com.patson.model.alliance

import com.patson.data.{AirlineSource, AirportSource, CountrySource, CycleSource, AllianceMissionSource}
import com.patson.model._

import scala.collection.mutable

abstract class AllianceMission() extends IdObject {
  val missionType : AllianceMissionType.Value
  val startCycle : Int
  val duration : Int
  val target : Long
  val endCycle = startCycle + duration

  val isActive = (currentCycle : Int) => endCycle > currentCycle

  def updateStats(currentCycle : Int, currentCycleStats : Long) : Unit
  def hasPendingRewards(currentCycle : Int) : Boolean = {
    if (isActive(currentCycle)) { //still ongoing
      false
    } else {
      getPickedReward() match {
        case Some(_) => //picked already
          false
        case None =>
          if (currentCycle <= endCycle + AllianceMission.REWARD_REDEMPTION_DURATION && progress(currentCycle) >= 1) { //completed
            true
          } else {
            false
          }
      }
    }
  }

  def getPickedReward() : Option[AllianceMissionReward] = {
    ???
  }

  def getRewardOptions() = {
    ???
  }

  def generateRewardOptions() : List[AllianceMissionReward]



  def progress(currentCycle : Int) : Double = {
    val stats : Long =
      if (currentCycle < startCycle + duration) {
        AllianceMissionSource.loadStatsByCycle(currentCycle).getOrElse(0)
      } else { //it's over
        AllianceMissionSource.loadStatsByCycle(startCycle + duration).getOrElse(0)
      }
    stats.toDouble / target
  }
}

abstract class AccumulativeAllianceMission() extends AllianceMission {
  override def updateStats(currentCycle : Int, currentStats : Long): Unit = {
    val newStats = AllianceMissionSource.loadStatsByCycle(currentCycle).getOrElse(0) + currentStats
    AllianceMissionSource.saveStats(currentCycle, newStats)
  }
}

abstract class DiscreteAllianceMission() extends AllianceMission {
  override def updateStats(currentCycle : Int, currentStats : Long): Unit = {
    AllianceMissionSource.saveStats(currentCycle, currentStats)
  }
}

object AllianceMissionType extends Enumeration {
  type AllianceMissionType = Value
  val TOTAL_PAX, TOTAL_PREMIUM_PAX, TOTAL_LOUNGE_VISIT, AIRPORT_RANKING, COUNTRY_RANKING, CONTINENT_RANKING, TOTAL_REPUTATION, TOTAL_LOYALIST, SATISFACTION_FACTOR, TOTAL_REVENUE = Value
}

import AllianceMissionType._
case class TotalPaxMission(override val startCycle : Int, override val duration : Int, var id : Int = 0, target : Long) extends AccumulativeAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_PAX

  override def generateRewardOptions() : List[AllianceMissionReward] = {
    List(
      CashReward(missionId = id, id = 0, Map("base" -> target * 10, "multiplier" -> 5))
    )
  }
}

object AllianceMissionStatus extends Enumeration {
  type AllianceMissionStatus = Value
  val SELECTION, IN_PROGRESS, CONCLUDED = Value
}



object AllianceMission {
  def buildAllianceMission(missionType : AllianceMissionType, startCycle : Int, duration : Int, id : Int, properties : Map[String, Long]) : AllianceMission = {
    missionType match {
      case TOTAL_PAX => TotalPaxMission(startCycle, duration, id, properties("target"))
      case TOTAL_PREMIUM_PAX => ???
      case TOTAL_LOUNGE_VISIT => ???
      case AIRPORT_RANKING => ???
      case COUNTRY_RANKING => ???
      case CONTINENT_RANKING => ???
      case TOTAL_REPUTATION => ???
      case TOTAL_LOYALIST => ???
      case SATISFACTION_FACTOR => ???
      case TOTAL_REVENUE => ???
    }
  }

  val REWARD_REDEMPTION_DURATION = 52 * 3
}


object RewardType extends Enumeration {
  type RewardType = Value
  val CASH, LOYALTY, REPUTATION, DELEGATE = Value
}

abstract class AllianceMissionReward() extends IdObject {
  def rewardType : RewardType.Value
  def missionId : Int
  def apply(mission: AllianceMission, airline : Airline): Unit = {
    applyReward(mission, airline)
    AllianceMissionSource.savePickedRewardOption(mission.id, airline.id, this)
  }
  protected def applyReward(mission: AllianceMission, airline : Airline)

  val description : String
  def redeemDescription(missionId: Int, airlineId: Int) = description
}

object AllianceMissionReward {
  def buildMissionReward(missionId : Int, rewardType: RewardType.Value, id : Int, properties : Map[String, Long]) : AllianceMissionReward = {
    rewardType match {
      case com.patson.model.alliance.RewardType.CASH => CashReward(missionId, id, properties)
      case com.patson.model.alliance.RewardType.LOYALTY => ???
      case com.patson.model.alliance.RewardType.REPUTATION => ???
      case com.patson.model.alliance.RewardType.DELEGATE => DelegateReward(missionId, id, properties)
    }
  }
}


//case class LoyaltyReward() extends AllianceMissionReward(AllianceMissionType.OLYMPICS, RewardCategory.OLYMPICS_VOTE, RewardType.LOYALTY) {
//  val LOYALTY_BONUS = 10
//  override def applyReward(mission: AllianceMission, airline : Airline) = {
//    val bonus = AirlineBonus(BonusType.OLYMPICS_VOTE, AirlineAppeal(loyalty = LOYALTY_BONUS, awareness = 0), Some(mission.startCycle + mission.duration))
//    Olympics.getAffectedAirport(mission.id, Olympics.getSelectedAirport(mission.id).get).foreach { affectedAirport =>
//      AirportSource.saveAirlineAppealBonus(affectedAirport.id, airline.id, bonus)
//    }
//  }
//
//  override val description: String = s"+$LOYALTY_BONUS loyalty bonus on airports around the host city until the end of Olympics"
//}

case class CashReward(override val missionId: Int, override var id : Int, properties : Map[String, Long]) extends AllianceMissionReward() {
  val BASE_CASH_REWARD = properties("base")
  val SCORE_MULTIPLIER = properties("multiplier")

  override val rewardType = RewardType.CASH

  def computeReward(missionId : Int, airlineId : Int) = {
    val stats: Map[Int, BigDecimal] = AllianceMissionSource.loadOlympicsAirlineStats(missionId, airlineId).toMap
    val totalScore = stats.view.values.sum
    Math.max((totalScore * 500).toLong, BASE_CASH_REWARD)
  }

  override def applyReward(mission: AllianceMission, airline : Airline) = {
    val reward = computeReward(mission.id, airline.id)
    AirlineSource.adjustAirlineBalance(airline.id, reward)
  }

  override val description: String = "$20,000,000 or $500 * score (whichever is higher) cash reward"
  override def redeemDescription(missionId: Int, airlineId : Int) = s"$$${java.text.NumberFormat.getIntegerInstance.format(computeReward(missionId, airlineId))} cash reward"

}

case class DelegateReward(override val missionId: Int, override var id : Int, properties : Map[String, Long]) extends AllianceMissionReward() {
  val BASE_BONUS = properties("base")
  val MAX_BONUS = properties("max")
  val DURATION = 52 * 4

  override val rewardType = RewardType.DELEGATE

  override def applyReward(mission: AllianceMission, airline : Airline) = {
    val cycle = CycleSource.loadCycle()
    val bonus = computeReward(mission.id, airline.id)
    AirlineSource.saveAirlineModifier(airline.id, DelegateBoostAirlineModifier(bonus, DURATION, cycle))
  }

  def computeReward(missionId: Int, airlineId : Int) = {
    val stats: Map[Int, BigDecimal] = AllianceMissionSource.loadOlympicsAirlineStats (missionId, airlineId).toMap
    val totalScore = stats.view.values.sum
    AllianceMissionSource.loadOlympicsAirlineGoal(missionId, airlineId) match {
      case Some(goal) =>
        val overachieverRatio = Math.min(1.0, totalScore.toDouble / goal / 4) //at 400% then it claim 1.0 overachiever ratio
        val extraBonus = ((MAX_BONUS - BASE_BONUS) * overachieverRatio).toInt //could be 0
        BASE_BONUS + extraBonus
      case None => 0
    }

  }

  override val description: String = s"+$BASE_BONUS to $MAX_BONUS delegates for $DURATION weeks"
  override def redeemDescription(missionId: Int, airlineId : Int) = s"${computeReward(missionId, airlineId)} extra delegates for $DURATION weeks"
}

