package com.patson.model.christmas

import com.patson.data.{AirlineSource, AirportSource, ChristmasSource, CycleSource}
import com.patson.model.{Airline, AirlineAppeal, AirlineBonus, BonusType}
import com.patson.util.AirlineCache

abstract class SantaClausAward(santaClausInfo: SantaClausInfo) {
  val getType : SantaClausAwardType.Value
  def apply: Unit = {
    applyAward()
    ChristmasSource.updateSantaClausInfo(santaClausInfo.copy(found = true, pickedAward = Some(getType)))
  }
  protected def applyAward() : Unit

  val description : String
}


class CashAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.CASH
  val CASH_AMOUNT = 20000000 //rich santa claus wow, 20M!
  override def applyAward(): Unit = {
    AirlineSource.adjustAirlineBalance(santaClausInfo.airline.id, CASH_AMOUNT)
  }

  override val description: String = "Santa Claus is feeling generous! He is giving you $" + CASH_AMOUNT + " cash!"
}

class ServiceQualityAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.SERVICE_QUALITY
  val BONUS = 5
  val DURATION = 8
  override def applyAward(): Unit = {
    val newQuality = Math.min(santaClausInfo.airline.getCurrentServiceQuality() + BONUS, Airline.MAX_SERVICE_QUALITY)

    santaClausInfo.airline.setCurrentServiceQuality(newQuality)
    AirlineSource.saveAirlineInfo(santaClausInfo.airline, updateBalance = false)
  }
  override val description: String = "Santa Claus offers you his pro tips on making your flights magical! Overall Airline Service Quality bonus +" + BONUS + " !"
}

class HqLoyaltyAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.HQ_LOYALTY
  val BONUS = 3
  val DURATION = 8
  override def applyAward(): Unit = {
    AirlineCache.getAirline(santaClausInfo.airline.id, fullLoad = true).foreach { airline =>
      airline.getHeadQuarter().foreach { hq =>

        val bonus = AirlineBonus(BonusType.SANTA_CLAUS, AirlineAppeal(loyalty = BONUS, awareness = 0), Some(CycleSource.loadCycle() + DURATION))
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
  val BONUS = 10
  val DURATION = 8
  override def applyAward(): Unit = {
    val airline = santaClausInfo.airline
    val bonus = AirlineBonus(BonusType.SANTA_CLAUS, AirlineAppeal(loyalty = BONUS, awareness = 0), Some(CycleSource.loadCycle() + DURATION))

    AirportSource.saveAirlineAppealBonus(santaClausInfo.airport.id, airline.id, bonus)
  }
  override val description: String = s"Santa Claus is going to print your airline logo on all the gifts shipping for ${santaClausInfo.airport.displayText}! Loyalty bonus $BONUS in ${santaClausInfo.airport.displayText} for $DURATION weeks!"
}

class ReputationAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.REPUTATION
  val BONUS = 5
  override def applyAward(): Unit = {
    val newReputation = santaClausInfo.airline.getReputation() + BONUS

    santaClausInfo.airline.setReputation(newReputation)
    AirlineSource.saveAirlineInfo(santaClausInfo.airline, updateBalance = false)
  }

  override val description: String = "Santa Claus will make an appearance on your TV commercial! Boosting your airline reputation by +" + BONUS + " !"
}


object SantaClausAwardType extends Enumeration {
  val CASH, SERVICE_QUALITY, HQ_LOYALTY, AIRPORT_LOYALTY, REPUTATION = Value
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
    }
  }
}
