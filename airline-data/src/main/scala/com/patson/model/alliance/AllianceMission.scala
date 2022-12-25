package com.patson.model.alliance

import com.patson.{AllianceMissionSimulation, AllianceSimulation}
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
  val properties : Map[String, Long]
  val difficulty = properties("difficulty").toInt
  val description : String

  val isActive = (currentCycle : Int) => endCycle > currentCycle
  var id : Int

  def updateStats(currentCycle : Int, newStats : AllianceStats) : AllianceMissionPropertiesHistory

  def hasPendingRewards(currentCycle : Int, airlineId : Int) : Boolean = {
    if (isActive(currentCycle)) { //still ongoing
      false
    } else {
      pickedReward(airlineId) match {
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

  def getValueFromStats(stats : AllianceStats) : Long //which field in stats matter for this mission?

  def rewardOptions(airlineId : Int) =
    if (status == AllianceMissionStatus.CONCLUDED) {
      AllianceMissionSource.loadRewardOptions(id, airlineId)
    } else {
      List.empty
    }

  def pickedReward(airlineId : Int) : Option[AllianceMissionReward] =
    if (status == AllianceMissionStatus.CONCLUDED) {
      AllianceMissionSource.loadRewardOptions(id, airlineId).find(_.claimed)
    } else {
      None
    }

  def progress(cycle : Int) : Int //percentage

  def stats(cycle : Int) : AllianceMissionPropertiesHistory = {
    AllianceMissionSource.loadPropertyHistory(id, cycle)
  }

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
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val newAccumulativeValue = AllianceMissionSource.loadPropertyHistory(id, currentCycle - 1).properties.getOrElse("accumulativeValue", 0L) + getValueFromStats(newStats)

    val newHistory = AllianceMissionPropertiesHistory(id, Map("accumulativeValue" -> newAccumulativeValue), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("accumulativeValue", 0L) * 100.0 / properties("goal")).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("accumulativeValue").toDouble / properties("goal"))
  }
}

abstract class DiscreteAllianceMission() extends AllianceMission {
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val newHistory = AllianceMissionPropertiesHistory(id, Map("discreteValue" -> getValueFromStats(newStats)), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("discreteValue", 0L) * 100.0 / properties("goal")).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("discreteValue").toDouble / properties("goal"))
  }
}

//Fulfill the threshold continuously for a duration N
abstract class DurationAllianceMission() extends AllianceMission {
  val durationGoal = properties("goal")
  val threshold = properties("threshold")
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val history = AllianceMissionSource.loadPropertyHistory(id, currentCycle - 1).properties

    var longestStreak = history.getOrElse("longestStreak", 0L)
    val previousStreak = history.getOrElse("currentStreak", 0L)
    var currentStreak = 0L

    val weeklyValue = getValueFromStats(newStats)
    if (weeklyValue >= threshold) { //then successful this week
      currentStreak = previousStreak + 1
      if (currentStreak > longestStreak) {
        longestStreak = currentStreak
      }
    }
    val newHistory = AllianceMissionPropertiesHistory(id, Map("weeklyValue" -> weeklyValue, "currentStreak" -> currentStreak, "longestStreak" -> longestStreak), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("longestStreak", 0L) * 100.0 / durationGoal).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("longestStreak").toDouble / durationGoal)
  }
}

//Fulfill the thresholds by N months, does not have to be continuous
abstract class HitCountAllianceMission() extends AllianceMission {
  val hitCountGoal = properties("goal")
  val threshold = properties("threshold")
  override def updateStats(currentCycle : Int, newStats : AllianceStats): AllianceMissionPropertiesHistory = {
    val history = AllianceMissionSource.loadPropertyHistory(id, currentCycle - 1).properties

    var hitCount = history.getOrElse("hitCount", 0L)

    val weeklyValue = getValueFromStats(newStats)
    if (weeklyValue >= threshold) { //then successful this week
      hitCount = hitCount + 1
    }
    val newHistory = AllianceMissionPropertiesHistory(id, Map("weeklyValue" -> weeklyValue, "hitCount" -> hitCount), currentCycle)
    AllianceMissionSource.saveAllianceMissionPropertiesHistory(List(newHistory))
    newHistory
  }

  override def progress(cycle : Int) = {
    (AllianceMissionSource.loadPropertyHistory(id, cycle).properties.getOrElse("hitCount", 0L) * 100.0 / hitCountGoal).toInt
  }

  override def isSuccessful(finalProgress : AllianceMissionPropertiesHistory) = {
    AllianceMissionResult(finalProgress.properties("hitCount").toDouble / hitCountGoal)
  }
}

object AllianceMissionType extends Enumeration {
  type AllianceMissionType = Value
  val TOTAL_PAX, TOTAL_PREMIUM_PAX, TOTAL_LOUNGE_VISIT, AIRPORT_RANKING, COUNTRY_RANKING, CONTINENT_RANKING, TOTAL_REPUTATION, TOTAL_LOYALIST, SATISFACTION_FACTOR, TOTAL_REVENUE = Value
}

import AllianceMissionType._
import AllianceMission._

case class TotalPaxMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_PAX
  override val description = s"Transport >= ${formatter.format(threshold)} weekly PAX for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalPax.total
}

case class TotalPremiumPaxMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_PREMIUM_PAX
  override val description = s"Transport >= ${formatter.format(threshold)} weekly Business and First class PAX for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalPax.firstVal + stats.totalPax.businessVal
}

case class TotalLoungeVisitMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_LOUNGE_VISIT
  override val description = s"Welcome >= ${formatter.format(threshold)} weekly Lounge Visitors for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalLoungeVisit
}

case class TotalLoyalistMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_LOYALIST
  override val description = s"Maintain >= ${formatter.format(threshold)} Loyalists in all airports for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalLoyalist
}

case class TotalRevenueMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = TOTAL_REVENUE
  override val description = s"Achieve >= ${formatter.format(threshold)} total weekly Alliance Revenue for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.totalRevenue
}


case class AirportRankingMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = AIRPORT_RANKING
  val rankingRequirement = properties.get("rankingRequirement") //for example if it's = 2, then only ranking 2 or above will be counted
  val scaleRequirement = properties.get("scaleRequirement") //airport size/scale requirement

  val scaleText = scaleRequirement match {
    case Some(scale) => s"Scale $scale or above"
    case None => "Any scale"
  }
  val rankText = rankingRequirement match  {
    case Some(rank) => s"Rank $rank or above"
    case None => "Any reputation giving rank"
  }
  override val description = s"Hold ${threshold} airports of ${scaleText} with $rankText for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.airportRankingCount(rankingRequirement, scaleRequirement)
}

case class CountryRankingMission(override val startCycle : Int, override val duration : Int, override val allianceId : Int, override var status : AllianceMissionStatus.Value,  override val properties : Map[String, Long], var id : Int = 0) extends HitCountAllianceMission {
  override val missionType : AllianceMissionType.Value = COUNTRY_RANKING
  val rankingRequirement = properties.get("rankingRequirement")
  val populationRequirement = properties.get("populationRequirement")

  val populationText = populationRequirement match {
    case Some(population) => s"Population ${formatter.format(population)} or above"
    case None => "Any population"
  }
  val rankText = rankingRequirement match  {
    case Some(rank) => s"Rank $rank or above"
    case None => "Any reputation giving rank"
  }
  override val description = s"Hold ${threshold} countries of ${populationText} with $rankText for at least $hitCountGoal weeks"

  override def getValueFromStats(stats : AllianceStats) : Long = stats.countryRankingCount(rankingRequirement, populationRequirement)
}

case class AllianceMissionPropertiesHistory(missionId : Int, properties : Map[String, Long], cycle : Int)


object AllianceMission {
  val formatter = java.text.NumberFormat.getIntegerInstance
  def buildAllianceMission(missionType : AllianceMissionType, startCycle : Int, duration : Int, allianceId : Int, status : AllianceMissionStatus.Value, properties : Map[String, Long], id : Int = 0) : AllianceMission = {
    val missionFunction = missionType match {
      case TOTAL_PAX => TotalPaxMission
      case TOTAL_PREMIUM_PAX => TotalPremiumPaxMission
      case TOTAL_LOUNGE_VISIT => TotalLoungeVisitMission
      case AIRPORT_RANKING => AirportRankingMission
      case COUNTRY_RANKING => CountryRankingMission
      case CONTINENT_RANKING => ???
      case TOTAL_REPUTATION => ???
      case TOTAL_LOYALIST => TotalLoyalistMission
      case SATISFACTION_FACTOR => ???
      case TOTAL_REVENUE => TotalRevenueMission
    }
    missionFunction(startCycle, duration, allianceId, status, properties, id)
  }

  val REWARD_REDEMPTION_DURATION = 52 * 3
  val WEEKS_PER_YEAR = 52


  def generateMissionCandidates(activeDuration : Int, allianceStats : AllianceStats) : List[AllianceMissionCandidate] = {
    AllianceMissionType.values.toList.flatMap { missionType =>
      val generateFunction : (AllianceMissionType.Value, Int, AllianceStats) => List[AllianceMissionCandidate] = missionType match {
        case TOTAL_PAX => generateTotalPaxCandidates
        case TOTAL_PREMIUM_PAX =>generateTotalPremiumPaxCandidates
        case TOTAL_LOUNGE_VISIT => generateTotalLoungeVisitCandidates
        case AIRPORT_RANKING => generateAirportRankingCandidates
        case COUNTRY_RANKING => generateCountryRankingCandidates
        case CONTINENT_RANKING => emptyGenerateMissionCandidates
        case TOTAL_REPUTATION => emptyGenerateMissionCandidates
        case TOTAL_LOYALIST => generateTotalLoyalistCandidates
        case SATISFACTION_FACTOR => emptyGenerateMissionCandidates
        case TOTAL_REVENUE => generateTotalRevenueCandidates
      }
      generateFunction(missionType, activeDuration, allianceStats)
    }
  }
  def emptyGenerateMissionCandidates(missionType : AllianceMissionType.Value, duration : Int, allianceStats : AllianceStats) = List.empty

  def generateTotalPaxCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val totalPax = allianceStats.totalPax.total
    val (target : Long, difficulty : Int) =
      if (totalPax < 10000) {
        (totalPax + 5000L, 2)
      } else if (totalPax < 100000) {
        ((totalPax * 1.2).toLong, 3)
      } else if (totalPax < 500000) {
        ((totalPax * 1.15).toLong, 4)
      } else if (totalPax < 1000000) {
        ((totalPax * 1.10).toLong, 5)
      } else {
        ((totalPax * 1.08).toLong, 6)
      }
    List(AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "goal" -> activeDuration / 10, "difficulty" -> difficulty.toLong)),
         AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> (target * 1.1).toLong, "goal" -> activeDuration / 10, "difficulty" -> (difficulty.toLong + 1)))) //12 weeks
  }

  def generateTotalPremiumPaxCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val totalPax = allianceStats.totalPax.businessVal + allianceStats.totalPax.firstVal
    val (target : Long, difficulty) =
      if (totalPax < 5000) {
        (totalPax + 5000L, 3)
      } else if (totalPax < 10000) {
        ((totalPax * 1.2).toLong, 4)
      } else if (totalPax < 50000) {
        ((totalPax * 1.15).toLong, 5)
      } else if (totalPax < 200000) {
        ((totalPax * 1.10).toLong, 6)
      } else {
        ((totalPax * 1.08).toLong, 7)
      }
    List(AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "goal" -> activeDuration / 10, "difficulty" -> difficulty.toLong)))
  }

  def generateTotalLoungeVisitCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val totalLoungeVisit = allianceStats.totalLoungeVisit
    val (target : Long, difficulty) =
      if (totalLoungeVisit < 2000) {
        (totalLoungeVisit + 1000L, 3)
      } else if (totalLoungeVisit < 5000) {
        ((totalLoungeVisit * 1.2).toLong, 4)
      } else if (totalLoungeVisit < 10000) {
        ((totalLoungeVisit * 1.15).toLong, 5)
      } else if (totalLoungeVisit < 50000) {
        ((totalLoungeVisit * 1.10).toLong, 6)
      } else {
        ((totalLoungeVisit * 1.08).toLong, 7)
      }
    List(AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "goal" -> activeDuration / 10, "difficulty" -> difficulty.toLong)))
  }

  def generateTotalRevenueCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val totalRevenue = allianceStats.totalRevenue
    val (target : Long, difficulty) =
      if (totalRevenue < 10_000_000) {
        (totalRevenue + 5_000_000L, 1)
      } else if (totalRevenue < 100_000_000) {
        ((totalRevenue * 1.2).toLong, 2)
      } else if (totalRevenue < 500_000_000) {
        ((totalRevenue * 1.15).toLong, 3)
      } else if (totalRevenue < 1_000_000_000) {
        ((totalRevenue * 1.10).toLong, 4)
      } else {
        ((totalRevenue * 1.06).toLong, 5)
      }
    List(AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "goal" -> activeDuration / 10, "difficulty" -> difficulty.toLong))) //12 weeks
  }

  def generateTotalLoyalistCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val totalLoyalist = allianceStats.totalLoyalist
    val (target : Long, difficulty) =
      if (totalLoyalist < 100_000) {
        (totalLoyalist + 10_000L, 4)
      } else if (totalLoyalist < 500_000) {
        ((totalLoyalist * 1.1).toLong, 5)
      } else if (totalLoyalist < 5_000_000) {
        ((totalLoyalist * 1.08).toLong, 6)
      } else if (totalLoyalist < 50_000_000) {
        ((totalLoyalist * 1.06).toLong, 7)
      } else {
        ((totalLoyalist * 1.04).toLong, 8)
      }
    List(AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "goal" -> activeDuration / 5, "difficulty" -> difficulty.toLong))) //a year
  }

  def generateCountryRankingCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val rankingRequirements = List(1, 2, 3)
    val candidates = rankingRequirements.flatMap { rankingRequirement =>
      var populationDifficulty = 1
      AllianceSimulation.COUNTRY_POPULATION_THRESHOLD.map { populationRequirement =>
        val (target : Long, baseDifficulty : Int) = {
          val currentCount = allianceStats.countryRankingCount(Some(rankingRequirement), Some(populationRequirement.toLong))
          if (currentCount < 5) {
            (currentCount + 2L, 0)
          } else if (currentCount < 15) {
            (currentCount + 2L, 1)
          } else {
            (currentCount + 3L, 2)
          }
        }
        populationDifficulty += 1
        val difficulty = baseDifficulty + populationDifficulty + (3 - rankingRequirement)
        AllianceMissionCandidate(missionType, Map[String, Long]("threshold" -> target.toLong, "populationRequirement" -> populationRequirement, "rankingRequirement" -> rankingRequirement, "goal" -> activeDuration / 5, "difficulty" -> difficulty.toLong)) //a year
      }
    }

    candidates
  }

  def generateAirportRankingCandidates(missionType : AllianceMissionType.Value, activeDuration : Int, allianceStats : AllianceStats) = {
    val rankingRequirements = List(1, 2, 3)
    val scaleRequirements = List(None, Some(3), Some(4), Some(5))
    val candidates = rankingRequirements.flatMap { rankingRequirement =>
      scaleRequirements.map { scaleRequirement =>
        val (target : Long, baseDifficulty : Int) = {
          val currentCount = allianceStats.airportRankingCount(Some(rankingRequirement), scaleRequirement.map(_.toLong))
          if (currentCount < 10) {
            (currentCount + 2L, 0)
          } else if (currentCount < 30) {
            (currentCount + 3L, 1)
          } else {
            (currentCount + 4L, 2)
          }
        }
        val difficulty = baseDifficulty + scaleRequirement.getOrElse(0) + (3 - rankingRequirement)
        var properties = Map[String, Long]("threshold" -> target.toLong, "rankingRequirement" -> rankingRequirement, "goal" -> activeDuration / 5, "difficulty" -> difficulty.toLong)  //a year
        if (scaleRequirement.isDefined) {
          properties = properties + ("scaleRequirement" -> scaleRequirement.get)
        }
        AllianceMissionCandidate(missionType, properties)
      }
    }

    candidates
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
  def airlineId : Int

  def apply(mission : AllianceMission, airline : Airline) : Unit = {
    if (!claimed && available) {
      applyReward(airline)
      this.claimed = true
      AllianceMissionSource.updateRewardOption(this)
    }
  }

  protected def applyReward(airline : Airline)

  val description : String
  var id : Int
  val properties : Map[String, Long]
  var available : Boolean = false
  var claimed : Boolean = false
}

object AllianceMissionReward {
  def buildMissionReward(missionId : Int, airlineId : Int, rewardType: RewardType.Value, available : Boolean, claimed : Boolean, properties : Map[String, Long], id : Int) : AllianceMissionReward = {
    import RewardType._
    val reward = rewardType match {
      case CASH => CashReward(missionId, airlineId, properties, id)
//      case LOYALTY => ???
//      case REPUTATION => ???
      case DELEGATE => DelegateReward(missionId, airlineId, properties, id)
    }
    reward.available = available
    reward.claimed = claimed
    reward
  }

  def generateMissionRewardOptions(missionId : Int, airlineId : Int, completionFactor : Double, difficulty: Int) : List[AllianceMissionReward] = {
    import RewardType._
    val options = RewardType.values.toList.map {
      case CASH =>
        val amount = 10_000_000 * Math.min(3, completionFactor) * Math.pow(2, difficulty - 1)
        CashReward(missionId, airlineId, Map("amount" -> amount.toLong), 0)
      case DELEGATE =>
        val extraBonus = Math.min(3, completionFactor.toInt - 1)
        val amount = difficulty + extraBonus
        DelegateReward(missionId, airlineId, Map("amount" -> amount.toLong, "duration" -> 10 * AllianceMission.WEEKS_PER_YEAR), 0)
    }
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

case class CashReward(override val missionId: Int, override val airlineId : Int, override val properties : Map[String, Long], override var id : Int) extends AllianceMissionReward() {
  override val rewardType = RewardType.CASH

  override def applyReward(airline : Airline) = {
    val reward = properties("amount")
    AirlineSource.adjustAirlineBalance(airline.id, reward)
  }

  override val description: String = s"$$${java.text.NumberFormat.getIntegerInstance.format(properties("amount"))} cash reward"
}

case class DelegateReward(override val missionId: Int, override val airlineId : Int, override val properties : Map[String, Long], override var id : Int) extends AllianceMissionReward() {

  override val rewardType = RewardType.DELEGATE
  val delegateAmount = properties("amount").toInt
  val duration = properties("duration").toInt

  override def applyReward(airline : Airline) = {
    AirlineSource.saveAirlineModifier(airline.id, DelegateBoostAirlineModifier(delegateAmount, duration, CycleSource.loadCycle()))
  }

  override val description: String = s"$delegateAmount extra delegates for $duration weeks"
}

