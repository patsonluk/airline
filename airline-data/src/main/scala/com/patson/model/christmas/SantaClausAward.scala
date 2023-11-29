package com.patson.model.christmas

import com.patson.data.{AirlineSource, AirportSource, ChristmasSource, CycleSource}
import com.patson.model.christmas.SantaClausAward.{Difficulty, getDifficultyLevel}
import com.patson.model.{Airline, AirlineAppeal, AirlineBonus, BonusType, DelegateBoostAirlineModifier}
import com.patson.util.AirlineCache

abstract class SantaClausAward(santaClausInfo: SantaClausInfo) {
  val getType : SantaClausAwardType.Value
  val difficultyMultiplier : Int = getDifficultyLevel(santaClausInfo) match {
    case Some(Difficulty.NORMAL) => 1
    case Some(Difficulty.HARD) => 2
    case _ => 1
  }
  def apply: Unit = {
    applyAward()
    ChristmasSource.updateSantaClausInfo(santaClausInfo.copy(found = true, pickedAward = Some(getType)))
  }
  protected def applyAward() : Unit

  val description : String
  val integerFormatter = java.text.NumberFormat.getIntegerInstance
}



class CashAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.CASH
  val CASH_AMOUNT = 20000000 * difficultyMultiplier //rich santa claus wow, 20M!
  override def applyAward(): Unit = {
    AirlineSource.adjustAirlineBalance(santaClausInfo.airline.id, CASH_AMOUNT)
  }

  override val description: String = s"Santa Claus is feeling generous! He is giving you $$${integerFormatter.format(CASH_AMOUNT)} cash!"
}

class ServiceQualityAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.SERVICE_QUALITY
  val BONUS = 10 * difficultyMultiplier
  override def applyAward(): Unit = {
    val newQuality = Math.min(santaClausInfo.airline.getCurrentServiceQuality() + BONUS, Airline.MAX_SERVICE_QUALITY)

    santaClausInfo.airline.setCurrentServiceQuality(newQuality)
    AirlineSource.saveAirlineInfo(santaClausInfo.airline, updateBalance = false)
  }
  override val description: String = "Santa Claus offers you his pro tips on making your flights magical! Overall Airline Service Quality bonus +" + BONUS + " ! (not permanent, level will eventually return to normal level)"
}

class HqLoyaltyAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.HQ_LOYALTY
  val BONUS = 5 * difficultyMultiplier
  val DURATION = 52
  override def applyAward(): Unit = {
    AirlineCache.getAirline(santaClausInfo.airline.id, fullLoad = true).foreach { airline =>
      airline.getHeadQuarter().foreach { hq =>

        val bonus = AirlineBonus(BonusType.SANTA_CLAUS, AirlineAppeal(loyalty = BONUS), Some(CycleSource.loadCycle() + DURATION))
        AirportSource.saveAirlineAppealBonus(hq.airport.id, airline.id, bonus)
      }
    }
  }

  override val description: String = {
    val hq = AirlineCache.getAirline(santaClausInfo.airline.id, fullLoad = true).get.getHeadQuarter().get.airport

    s"Santa Claus agrees to travel to your HQ and ho-ho-ho for $DURATION weeks in ${hq.displayText} Loyalty bonus $BONUS in your HQ airport!"
  }
}

class AirportLoyaltyAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.AIRPORT_LOYALTY
  val BONUS = 20 * difficultyMultiplier
  val DURATION = 52
  override def applyAward(): Unit = {
    val airline = santaClausInfo.airline
    val bonus = AirlineBonus(BonusType.SANTA_CLAUS, AirlineAppeal(loyalty = BONUS), Some(CycleSource.loadCycle() + DURATION))

    AirportSource.saveAirlineAppealBonus(santaClausInfo.airport.id, airline.id, bonus)
  }
  override val description: String = s"Santa Claus is going to print your airline logo on all the gifts shipping for ${santaClausInfo.airport.displayText}! Loyalty bonus $BONUS in ${santaClausInfo.airport.displayText} for $DURATION weeks!"
}

class ReputationAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.REPUTATION
  val BONUS = 10 * difficultyMultiplier
  override def applyAward(): Unit = {
    val newReputation = santaClausInfo.airline.getReputation() + BONUS

    santaClausInfo.airline.setReputation(newReputation)
    AirlineSource.saveAirlineInfo(santaClausInfo.airline, updateBalance = false)
  }

  override val description: String = "Santa Claus will make an appearance on your TV commercial! Boosting your airline reputation by +" + BONUS + " ! (not permanent, reputation will eventually return to normal level)"
}


class DelegateAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.DELEGATE
  override def applyAward(): Unit = {
    AirlineSource.saveAirlineModifier(santaClausInfo.airline.id, DelegateBoostAirlineModifier(amount, duration, CycleSource.loadCycle()))
  }
  val amount = 3 * difficultyMultiplier
  val duration = 52

  override val description: String = s"Santa Claus dispatches his elves to help you! $amount extra delegates for $duration weeks"
}



object SantaClausAwardType extends Enumeration {
  val CASH, SERVICE_QUALITY, HQ_LOYALTY, AIRPORT_LOYALTY, REPUTATION, DELEGATE = Value
}

object SantaClausAward {
  def getAllRewards(info : SantaClausInfo) = {
    SantaClausAwardType.values.toList.map {
      getRewardByType(info, _)
    }
  }

  def getRewardByType(info : SantaClausInfo, rewardType : SantaClausAwardType.Value) = {
    rewardType match {
      case SantaClausAwardType.CASH => new CashAward(info)
      case SantaClausAwardType.SERVICE_QUALITY => new ServiceQualityAward(info)
      case SantaClausAwardType.HQ_LOYALTY => new HqLoyaltyAward(info)
      case SantaClausAwardType.AIRPORT_LOYALTY => new AirportLoyaltyAward(info)
      case SantaClausAwardType.REPUTATION => new ReputationAward(info)
      case SantaClausAwardType.DELEGATE => new DelegateAward(info)
    }
  }

  def getDifficultyLevel(santaClausInfo : SantaClausInfo) : Option[Difficulty.Value] = {
    if (santaClausInfo.guesses.isEmpty) {
      None
    } else {
      if (santaClausInfo.guesses.size + santaClausInfo.attemptsLeft == 5) {
        Some(Difficulty.HARD)
      } else {
        Some(Difficulty.NORMAL)
      }
    }
  }

  object Difficulty extends Enumeration {
    type Difficulty = Value
    val NORMAL, HARD = Value
  }
}
