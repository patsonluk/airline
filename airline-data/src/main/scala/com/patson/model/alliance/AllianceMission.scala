package com.patson.model.alliance

import com.patson.data.{AirlineSource, AirportSource, AllianceMissionSource, CountrySource, CycleSource}
import com.patson.model._

import scala.collection.mutable

abstract class AllianceMission() extends IdObject {
  val missionType : AllianceMissionType.Value
  val startCycle : Int
  val duration : Int
  val allianceId : Int
  var status : AllianceMissionStatus.Value
  val endCycle = startCycle + duration
  val properties: Map[String, Long]
  val difficulty = properties("difficulty").toInt

  val isActive = (currentCycle : Int) => endCycle > currentCycle
  var id : Int

  def updateStats(currentCycle : Int, newStats : AllianceStats) : AllianceMissionPropertiesHistory
  def hasPendingRewards(currentCycle : Int) : Boolean = {
    if (isActive(currentCycle)) { //still ongoing
      false
    } else {
      pickedReward match {
        case Some(_) => //picked already
          false
        case None =>
          if (status == AllianceMissionStatus.CONCLUDED && currentCycle <= endCycle + AllianceMission.REWARD_REDEMPTION_DURATION) { //completed and still within redeem duration
            true
          } else {
            false
          }
      }
    }
  }

  lazy val rewardOptions =
    if (status == AllianceMissionStatus.CONCLUDED) {
      AllianceMissionSource.loadRewardOptions(id)
    } else {
      List.empty
    }
  lazy val pickedReward : Option[AllianceMissionReward] =
    if (status == AllianceMissionStatus.CONCLUDED) {
      properties.get("pickedReward").flatMap { pickedReward =>
        rewardOptions.find(_.id == pickedReward.toInt)
      }
    } else {
      None
    }

  def progress(currentCycle : Int) : Int //percentage

  def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) : AllianceMissionResult

  val currentYear = (currentCycle : Int) => {
    (currentCycle - startCycle) /  AllianceMission.WEEKS_PER_YEAR + 1
  }

  val isNewYear = (currentCycle : Int) => currentWeek(currentCycle) == 0
  val currentWeek = (currentCycle : Int) => (currentCycle - startCycle) % AllianceMission.WEEKS_PER_YEAR //start from 0 to WEEKS_PER_YEAR

//  import AllianceMissionStatus._
//  val status = (currentCycle : Int) =>
//    if (isActive(currentCycle)) {
//      currentYear(currentCycle) match {
//        case 1 => SELECTION
//        case _ => IN_PROGRESS
//      }
//    } else {
//      CONCLUDED
//    }
}

object AllianceMissionStatus extends Enumeration {
  type RewardCategory = Value
  val CANDIDATE, SELECTED, IN_PROGRESS, CONCLUDED = Value
}
case class AllianceMissionResult(completionFactor : Double) {
  val isSuccessful = completionFactor >= 1
}

abstract class AccumulativeAllianceMission() extends AllianceMission {
  def getValueFromStats(stats : AllianceStats) : Long //which field in stats matter for this mission?
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val newAccumulativeValue = AllianceMissionSource.loadPropertyHistory(id, currentCycle - 1).properties.getOrElse("accumulativeValue", 0L) + getValueFromStats(newStats)

    val newHistory = AllianceMissionPropertiesHistory(id, Map("accumulativeValue" -> newAccumulativeValue), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("accumulativeValue", 0L) * 100 / properties("goal")).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("accumulativeValue").toDouble / properties("goal"))
  }
}

abstract class DiscreteAllianceMission() extends AllianceMission {
  def getValueFromStats(stats : AllianceStats) : Long //which field in stats matter for this mission?
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val newHistory = AllianceMissionPropertiesHistory(id, Map("discreteValue" -> getValueFromStats(newStats)), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("discreteValue", 0L) * 100 / properties("goal")).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("discreteValue").toDouble / properties("goal"))
  }
}

abstract class DurationAllianceMission() extends AllianceMission {
  def getValueFromStats(stats : AllianceStats) : Long //which field in stats matter for this mission?
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val threshold = properties("threshold") //should be above this threshold to be count as a successful week
    var longestStreak = properties.getOrElse("longestStreak", 0L)
    var currentStreak = properties.getOrElse("currentStreak", 0L)
    val weeklyValue = getValueFromStats(newStats)
    if (weeklyValue >= threshold) { //then successful this week
      currentStreak = currentStreak + 1
      if (currentStreak > longestStreak) {
        longestStreak = currentStreak
      }
    } else { //reset current streak :(
      currentStreak = 0
    }
    val newHistory = AllianceMissionPropertiesHistory(id, Map("weeklyValue" -> weeklyValue, "currentStreak" -> currentStreak, "longestStreak" -> longestStreak), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("longestStreak", 0L) * 100 / properties("goal")).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("longestStreak").toDouble / properties("goal"))
  }
}



object AllianceMissionType extends Enumeration {
  type AllianceMissionType = Value
  val TOTAL_PAX, TOTAL_PREMIUM_PAX, TOTAL_LOUNGE_VISIT, AIRPORT_RANKING, COUNTRY_RANKING, CONTINENT_RANKING, TOTAL_REPUTATION, TOTAL_LOYALIST, SATISFACTION_FACTOR, TOTAL_REVENUE = Value
}

import AllianceMissionType._
case class TotalPaxMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends AccumulativeAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_PAX

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalPax.total
}

case class AllianceMissionPropertiesHistory(missionId : Int, properties : Map[String, Long], cycle : Int)


object AllianceMission {
  def buildAllianceMission(missionType : AllianceMissionType, startCycle : Int, duration : Int, allianceId : Int, status : AllianceMissionStatus.Value, properties : Map[String, Long], id : Int = 0) : AllianceMission = {
    missionType match {
      case TOTAL_PAX => TotalPaxMission(startCycle, duration, allianceId, status, properties, id)
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
  val WEEKS_PER_YEAR = 52


  def generateMissionCandidates(allianceStats : AllianceStats) : List[AllianceMissionCandidate] = {
    AllianceMissionType.values.toList.flatMap { missionType =>
      missionType match {
        case TOTAL_PAX =>
          generateTotalPaxCandidates(allianceStats : AllianceStats)
        case TOTAL_PREMIUM_PAX => List.empty
        case TOTAL_LOUNGE_VISIT => List.empty
        case AIRPORT_RANKING => List.empty
        case COUNTRY_RANKING =>List.empty
        case CONTINENT_RANKING =>List.empty
        case TOTAL_REPUTATION =>List.empty
        case TOTAL_LOYALIST =>List.empty
        case SATISFACTION_FACTOR =>List.empty
        case TOTAL_REVENUE =>List.empty
      }
    }
  }

  def generateTotalPaxCandidates(allianceStats : AllianceStats) = {
    val totalPax = allianceStats.totalPax.total
    val (target : Double, difficulty) =
      if (totalPax < 10000) {
        (totalPax + 10000, 1)
      } else if (totalPax < 100000) {
        (totalPax * 1.3, 2)
      } else if (totalPax < 500000) {
        (totalPax * 1.2, 3)
      } else if (totalPax < 1000000) {
        (totalPax * 1.1, 4)
      } else {
        (totalPax * 1.07, 5)
      }
    List(AllianceMissionCandidate(AllianceMissionType.TOTAL_PAX, Map("goal" -> target.toLong, "difficulty" -> difficulty.toLong)))
  }


}

case class AllianceMissionCandidate(missionType : AllianceMissionType.Value, properties : Map[String, Long])



object RewardType extends Enumeration {
  type RewardType = Value
  val CASH, DELEGATE = Value
}

abstract class AllianceMissionReward() extends IdObject {
  def rewardType : RewardType.Value
  def missionId : Int
  def apply(mission: AllianceMission, airline : Airline): Unit = {
    applyReward(airline)
    AllianceMissionSource.savePickedRewardOption(mission.id, airline.id, this)
  }
  protected def applyReward(airline : Airline)

  val description : String
  var id : Int
  val properties : Map[String, Long]
  def redeemDescription(missionId: Int, airlineId: Int) = description
}

object AllianceMissionReward {
  def buildMissionReward(missionId : Int, rewardType: RewardType.Value, properties : Map[String, Long], id : Int) : AllianceMissionReward = {
    import RewardType._
    rewardType match {
      case CASH => CashReward(missionId, properties, id)
//      case LOYALTY => ???
//      case REPUTATION => ???
      case DELEGATE => DelegateReward(missionId, properties, id)
    }
  }

  def generateMissionRewardOptions(missionId : Int, completionFactor : Double, difficulty: Int) : List[AllianceMissionReward] = {
    import RewardType._
    val options = RewardType.values.toList.map {
      case CASH =>
        val amount = 10_000_000 * Math.min(3, completionFactor) * Math.pow(2, difficulty - 1)
        CashReward(missionId, Map("amount" -> amount.toLong), 0)
      case DELEGATE =>
        val extraBonus = Math.min(3, completionFactor.toInt - 1)
        val amount = difficulty + extraBonus
        DelegateReward(missionId, Map("amount" -> amount.toLong, "duration" -> 10 * AllianceMission.WEEKS_PER_YEAR), 0)
    }

    AllianceMissionSource.saveRewardOptions(options)

    options
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

case class CashReward(override val missionId: Int, override val properties : Map[String, Long], override var id : Int) extends AllianceMissionReward() {
  override val rewardType = RewardType.CASH

  override def applyReward(airline : Airline) = {
    val reward = properties("amount")
    AirlineSource.adjustAirlineBalance(airline.id, reward)
  }

  override val description: String = s"$$${java.text.NumberFormat.getIntegerInstance.format(properties("amount"))} cash reward"
}

case class DelegateReward(override val missionId: Int, override val properties : Map[String, Long], override var id : Int) extends AllianceMissionReward() {

  override val rewardType = RewardType.DELEGATE
  val delegateAmount = properties("amount").toInt
  val duration = properties("duration").toInt

  override def applyReward(airline : Airline) = {
    AirlineSource.saveAirlineModifier(airline.id, DelegateBoostAirlineModifier(delegateAmount, duration, CycleSource.loadCycle()))
  }

  override val description: String = s"$delegateAmount extra delegates for $duration weeks"
}

