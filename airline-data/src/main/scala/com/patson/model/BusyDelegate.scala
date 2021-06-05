package com.patson.model

import com.patson.model.campaign.Campaign

case class BusyDelegate(airline : Airline, assignedTask : DelegateTask, availableCycle : Option[Int], var id : Int = 0) extends IdObject {
  val taskCompleted = availableCycle.isDefined
}

abstract class DelegateTask(startCycle : Int, taskType : DelegateTaskType.Value) {
  val description : String
  val getTaskType = taskType
  val getStartCycle = startCycle
  val coolDown : Int
}

object DelegateTask {
  val country = (startCycle : Int, country : Country) => CountryDelegateTask(startCycle, country)
  val campaign = (startCycle : Int, campaign : Campaign) => CampaignDelegateTask(startCycle, campaign)
  val linkNegotiation = (startCycle : Int, fromAirport : Airport, toAirport : Airport) => LinkNegotiationDelegateTask(startCycle, fromAirport, toAirport)

}

case class CountryDelegateTask(startCycle : Int, country: Country) extends LevelingDelegateTask(startCycle, DelegateTaskType.COUNTRY) {
  override val description: String = s"Develop relationship with ${country.name}"
}

case class CampaignDelegateTask(startCycle : Int, campaign : Campaign) extends LevelingDelegateTask(startCycle, DelegateTaskType.CAMPAIGN) {
  override val description: String = s"Campaign in the area around ${campaign.principalAirport.displayText}"

  override val LEVEL_CYCLE_THRESHOLDS = List(1, 4, 12, 52)



  val cost = CampaignDelegateTask.cost(campaign.principalAirport.income)
}

object CampaignDelegateTask {
  private[this] val BASE_COST = 10000
  def cost = (principalAirportIncome : Int) => BASE_COST + principalAirportIncome
}

abstract class LevelingDelegateTask(startCycle : Int, delegateTaskType: DelegateTaskType.Value) extends DelegateTask(startCycle, delegateTaskType) {
  override val coolDown: Int = 0

  val LEVEL_CYCLE_THRESHOLDS = List(4, 1 * 52, 3 * 52, 10 * 52)
  val level = (currentCycle: Int) => {
    var levelWalker = 0
    val taskDuration = currentCycle - startCycle
    LEVEL_CYCLE_THRESHOLDS.find(threshold => {
      val higherThanThisLevel = taskDuration >= threshold
      if (higherThanThisLevel) {
        levelWalker = levelWalker + 1
      }
      !higherThanThisLevel
    })
    levelWalker
  }
  val nextLevelCycleCount = (currentCycle: Int) => {
    val currentLevel : Int = level(currentCycle)
    val taskDuration = currentCycle - startCycle
    if (currentLevel >= LEVEL_CYCLE_THRESHOLDS.length) //max level already
      None
    else
      Some(LEVEL_CYCLE_THRESHOLDS(currentLevel) - taskDuration)
  }

  val levelDescription = (currentCycle: Int) => {
    level(currentCycle) match {
      case 0 => "Trainee"
      case 1 => "Novice"
      case 2 => "Established"
      case 3 => "Experienced"
      case _ => "Veteran"
    }

  }
}

case class LinkNegotiationDelegateTask(startCycle : Int, fromAirport : Airport, toAirport : Airport) extends DelegateTask(startCycle, DelegateTaskType.LINK_NEGOTIATION) {
  override val description: String = s"Flight from ${fromAirport.displayText} to ${toAirport.displayText}"
  override val coolDown: Int = 12
}

object DelegateTaskType extends Enumeration {
  type DelegateTaskType = Value
  val COUNTRY, LINK_NEGOTIATION, CAMPAIGN = Value
}