package com.patson

import com.patson.model.{Airline, Airport, BusyDelegate, DelegateTask}
import com.patson.model.campaign._
import org.scalatest.{Matchers, WordSpecLike}

class CampaignSpec extends WordSpecLike with Matchers {
  val airport1 = Airport("", "", "Test Airport 1", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 1000000L, 0, id = 1)
  val airport2 = Airport("", "", "Test Airport 2", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 2000000L, 0, id = 2)
  val airport3 = Airport("", "", "Test Airport 3", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 3000000L, 0, id = 3)
  val airline1 = Airline.fromId(1)
  val airline2 = Airline.fromId(2)

  "campaign".must {
    "Generate no loyalty for the area if delegate is level 0".in {
      val area = List(airport1, airport2)
      val campaign1 = Campaign(airline1, airport1, 100, area.map(_.population).sum, area, 1)
      val campaign1DelegateTask = DelegateTask.campaign(0, campaign1)
      val currentCycle = 0
      val result = campaign1.getAirlineBonus(airport1, List(campaign1DelegateTask), currentCycle)
      assert(result.bonus.loyalty == 0)
    }
    "Generate some loyalty for the area if delegate is level > 0".in {
      val area = List(airport1, airport2)
      val campaign1 = Campaign(airline1, airport1, 100, area.map(_.population).sum, area, 1)
      val campaign1DelegateTask = DelegateTask.campaign(0, campaign1)
      val currentCycle = 10
      val airport1Bonus = campaign1.getAirlineBonus(airport1, List(campaign1DelegateTask), currentCycle)
      val airport2Bonus = campaign1.getAirlineBonus(airport2, List(campaign1DelegateTask), currentCycle)
      val airport3Bonus = campaign1.getAirlineBonus(airport3, List(campaign1DelegateTask), currentCycle)
      assert(airport1Bonus.bonus.loyalty > 0)
      assert(airport2Bonus.bonus.loyalty > 0)
      assert(airport3Bonus.bonus.loyalty == 0) //no coverage
    }

    "Generate more loyalty for the area if delegate is level is higher".in {
      val area = List(airport1, airport2)
      val campaign1 = Campaign(airline1, airport1, 100, area.map(_.population).sum, area, 1)
      val campaign1DelegateTask = DelegateTask.campaign(0, campaign1) //higher level delegate
      val campaign2 = Campaign(airline1, airport1, 100, area.map(_.population).sum, area, 1)
      val campaign2DelegateTask = DelegateTask.campaign(90, campaign2) //lower level delegate

      val currentCycle = 100
      val campaign1Bonus = campaign1.getAirlineBonus(airport1, List(campaign1DelegateTask), currentCycle)
      val campaign2Bonus = campaign2.getAirlineBonus(airport1, List(campaign2DelegateTask), currentCycle)
      assert(campaign1Bonus.bonus.loyalty > 0)
      assert(campaign2Bonus.bonus.loyalty > 0)
      assert(campaign1Bonus.bonus.loyalty > campaign2Bonus.bonus.loyalty)
      println(campaign1Bonus.bonus)
    }
    "Generate loyalty according to area pop ratio".in {
      val area1 = List(airport1, airport2)
      val area2 = List(airport1, airport2, airport3)
      val campaign1 = Campaign(airline1, airport1, 100, area1.map(_.population).sum, area1, 1)
      val campaign1DelegateTask = DelegateTask.campaign(0, campaign1) //higher level delegate
      val campaign2 = Campaign(airline1, airport1, 100, area2.map(_.population).sum, area2, 1) //coverage more pop
      val campaign2DelegateTask = DelegateTask.campaign(0, campaign2) //lower level delegate

      val currentCycle = 10
      val campaign1Bonus = campaign1.getAirlineBonus(airport1, List(campaign1DelegateTask), currentCycle)
      val campaign2Bonus = campaign2.getAirlineBonus(airport1, List(campaign2DelegateTask), currentCycle)

      assert(campaign1Bonus.bonus.loyalty > 0)
      assert(campaign2Bonus.bonus.loyalty > 0)
      assert(campaign1Bonus.bonus.loyalty > campaign2Bonus.bonus.loyalty) //campaign1 more effective as it covers less area
    }
  }
  

}
