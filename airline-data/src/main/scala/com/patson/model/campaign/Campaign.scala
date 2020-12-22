package com.patson.model.campaign

import com.patson.model._

case class Campaign(airline:  Airline, baseAirport : Airport, radius : Int, populationCoverage : Long, area : List[Airport], var id: Int = 0) extends IdObject {
  def getAirlineBonus(targetAirport: Airport, campaignDelegateTasks : List[CampaignDelegateTask], currentCycle : Int) : AirlineBonus = {
    if (area.map(_.id).contains(targetAirport.id)) { //safety check
      var totalLoyaltyBonus = 0.0
      var totalAwarenessBonus = 0.0
      campaignDelegateTasks.map(task => getAirlineBonus(targetAirport, task, currentCycle)).foreach {
        case AirlineAppeal(loyalty, awareness) =>
          totalLoyaltyBonus += loyalty
          totalAwarenessBonus += awareness
      }
      AirlineBonus(BonusType.CAMPAIGN, AirlineAppeal(totalLoyaltyBonus, totalAwarenessBonus), None)
    } else {
      AirlineBonus(BonusType.CAMPAIGN, AirlineAppeal(0, 0), None)
    }
  }

  val SAMPLE_POP_COVERAGE = 1000000
  val AWARENESS_BASE_BONUS = 40 //awareness boost for 1M pop coverage for each level of delegate
  val LOYALTY_BASE_BONUS = 8 //loyalty boost for 1M pop coverage for each level of delegate
  val LOYALTY_MAX_BONUS_PER_DELEGATE = 20

  private def getAirlineBonus(targetAirport : Airport, campaignDelegateTask : CampaignDelegateTask, currentCycle : Int) = {
    val popCoverageRatio = SAMPLE_POP_COVERAGE.toDouble / populationCoverage
    val awarenessBonus = AWARENESS_BASE_BONUS * popCoverageRatio * campaignDelegateTask.level(currentCycle)
    val loyaltyBonus = Math.min(LOYALTY_MAX_BONUS_PER_DELEGATE, LOYALTY_BASE_BONUS * popCoverageRatio * campaignDelegateTask.level(currentCycle))
    AirlineAppeal(loyalty = loyaltyBonus, awareness = awarenessBonus)
  }
}

