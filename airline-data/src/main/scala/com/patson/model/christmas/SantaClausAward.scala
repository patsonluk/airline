package com.patson.model.christmas

import com.patson.data.{AirlineSource, AirportSource, ChristmasSource}
import com.patson.model.{Airline, AirlineAppeal, Airport}

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
  val CASH_AMOUNT = 10000000 //rich santa claus wow, 10M!
  override def applyAward(): Unit = {
    AirlineSource.adjustAirlineBalance(santaClausInfo.airline.id, CASH_AMOUNT)
  }

  override val description: String = "Santa Claus is feeling generous! He is giving you $" + CASH_AMOUNT + " cash!"
}

class ServiceQualityAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.SERVICE_QUALITY
  val BONUS = 5
  override def applyAward(): Unit = {
    val newQuality = Math.min(santaClausInfo.airline.getServiceQuality() + BONUS, Airline.MAX_SERVICE_QUALITY)

    santaClausInfo.airline.setServiceQuality(newQuality)
    AirlineSource.saveAirlineInfo(santaClausInfo.airline, updateBalance = false)
  }
  override val description: String = "Santa Claus offers you his pro tips on making your flights magical! Overall Airline Service Quality bonus +" + BONUS + " !"
}

class HqLoyaltyAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.HQ_LOYALTY
  val BONUS = 3
  override def applyAward(): Unit = {
    AirlineSource.loadAirlineById(santaClausInfo.airline.id, fullLoad = true).foreach { airline =>
      airline.getHeadQuarter().foreach { hq =>
        val airport = AirportSource.loadAirportById(hq.airport.id, fullLoad = true).get //need full load for appeal
        val existingAppeal = airport.getAirlineBaseAppeal(airline.id)
        val newLoyalty = Math.min(existingAppeal.loyalty + BONUS, AirlineAppeal.MAX_LOYALTY)
        AirportSource.updateAirlineAppeal(airport.id, airline.id, AirlineAppeal(newLoyalty, existingAppeal.awareness))
      }
    }
  }

  override val description: String = {
    val hq = AirlineSource.loadAirlineById(santaClausInfo.airline.id, fullLoad = true).get.getHeadQuarter().get.airport

    "Santa Claus agrees to travel to your HQ and ho-ho-ho for a week in " + hq.name + "! Loyalty bonus +" + BONUS + " in your HQ airport!"
  }
}

class AirportLoyaltyAward(santaClausInfo: SantaClausInfo) extends SantaClausAward(santaClausInfo) {
  override val getType: SantaClausAwardType.Value = SantaClausAwardType.AIRPORT_LOYALTY
  val BONUS = 10
  override def applyAward(): Unit = {
    val airline = santaClausInfo.airline
    val airport = AirportSource.loadAirportById(santaClausInfo.airport.id, fullLoad = true).get //need full load for appeal
    val existingAppeal = airport.getAirlineBaseAppeal(airline.id)
    val newLoyalty = Math.min(existingAppeal.loyalty + BONUS, AirlineAppeal.MAX_LOYALTY)
    AirportSource.updateAirlineAppeal(airport.id, airline.id, AirlineAppeal(newLoyalty, existingAppeal.awareness))


  }
  override val description: String = "Santa Claus is going to print your airline logo on all the gifts shipping for " + santaClausInfo.airport.name + "! Loyalty bonus +" + BONUS + " in " + santaClausInfo.airport.name + "!"
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
