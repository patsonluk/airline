package com.patson.util

import com.patson.data.{AirlineSource, AllianceSource, CycleSource, LinkSource, LoungeHistorySource}
import com.patson.model._
 

object AllianceRankingUtil {
  var loadedCycle = 0
  var cachedRankings: Map[Int, (Int, BigDecimal)] = Map.empty


  /**
    *
    * @return Map[allianceId, (ranking, champion points)] . Take note that ranking starts with 1 as the top alliance
    */
  def getRankings(): Map[Int, (Int, BigDecimal)] = {
    checkCache()
    cachedRankings
  }

  def getRanking(allianceId : Int) : Option[(Int, BigDecimal)] = {
    checkCache()
    cachedRankings.get(allianceId)
  }

  private def checkCache() = {
    val currentCycle = CycleSource.loadCycle()
    synchronized {
      if (currentCycle != loadedCycle) {
        println("Updating alliance ranking cache on cycle " + currentCycle)
        updateRankings() //get ranking on previous cycle
        println("Updated alliance ranking cache on cycle " + currentCycle)
      }
      loadedCycle = currentCycle
    }
  }

  private[this] def updateRankings() = {
    val alliances = AllianceSource.loadAllAlliances(false)
    cachedRankings = Alliance.getRankings(alliances).view.map {
      case(alliance, value) => (alliance.id, value)
    }.toMap
  }

}

//class AirlineRanking(rankingType : RankingType.Value, airline : Airline, ranking : Int, rankedValue : Number, var movement : Int = 0) extends Ranking(rankingType, airline, ranking, rankedValue, movement) {
//  
//}


