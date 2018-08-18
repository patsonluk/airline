package com.patson.model

import scala.collection.mutable.ListBuffer
import com.patson.data.CountrySource

case class Alliance(name: String, creationCycle: Int, members: List[AllianceMember], var id: Int = 0) {
  val status = {
    if (members.length < Alliance.ESTABLISH_MIN_MEMBER_COUNT) {
      AllianceStatus.FORMING
    } else {
      AllianceStatus.ESTABLISHED
    }
  }
}

object AllianceStatus extends Enumeration {
  type AllianceStatus = Value
  val ESTABLISHED, FORMING = Value
}

case class AllianceMember(allianceId: Int, airline: Airline, role: AllianceRole.Value, joinedCycle: Int)

object AllianceRole extends Enumeration {
  type AllianceRole = Value
  val LEADER, FOUNDING_MEMBER, MEMBER, APPLICANT = Value
}

case class AllianceHistory(allianceName: String, airline: Airline, event: AllianceEvent.Value, cycle: Int, var id: Int = 0)

object AllianceEvent extends Enumeration {
  type AllianceEvent = Value
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE, BOOT_ALLIANCE = Value
}

object Alliance {
  val MAX_MEMBER_COUNT = 10
  val ESTABLISH_MIN_MEMBER_COUNT = 3

  val getReputationBonus: (Int => Double) = { (ranking: Int) =>
    if (ranking == 1) {
      15
    } else if (ranking == 2) {
      10
    } else if (ranking == 3) {
      5
    } else {
      2
    }
  }

  val getMaxFrequencyBonus: (Int => Double) = { (ranking: Int) =>
    if (ranking == 1) {
      15
    } else if (ranking == 2) {
      10
    } else if (ranking == 3) {
      5
    } else {
      0
    }
  }

  def getRankings(alliances: List[Alliance]): Map[Alliance, (Int, BigDecimal)] = {
    val countryChampionsByAirline: Map[Int, List[(Country, Double)]] = getCountryChampions()
    val alliancesWithChampionPoints: List[(Alliance, BigDecimal)] = alliances.map {
      alliance =>
        var allianceChampionPoints: BigDecimal = 0.0
        alliance.members.foreach { allianceMember =>
          val memberChampiontPoints: BigDecimal =
            if (allianceMember.role == AllianceRole.APPLICANT) { //do not add champion points from applicant
              0
            } else {
              countryChampionsByAirline.get(allianceMember.airline.id) match {
                case Some(championedCountries) => {
                  championedCountries.map(boostEntry => BigDecimal.valueOf(boostEntry._2)).sum
                }
                case None => 0
              }
            }
          allianceChampionPoints = allianceChampionPoints + memberChampiontPoints
        }
        (alliance, allianceChampionPoints)
    }

    val alliancesWithRanking = alliancesWithChampionPoints.sortBy(_._2)(Ordering.BigDecimal.reverse).zipWithIndex.map {
      case ((alliance, allianceChampionPoints), sortedIndex) => (alliance, (sortedIndex + 1, allianceChampionPoints))
    }

    alliancesWithRanking.toMap
  }
  
    
  /**
   * returns Map[AirlineId, List[CountryCode, ReputationBoost]]
   */
  def getCountryChampions() : Map[Int, List[(Country, Double)]] = {
    val topChampionsByCountryCode : List[(String, List[((Int, Long), Int)])]= CountrySource.loadMarketSharesByCriteria(List()).map {
      case CountryMarketShare(countryCode, airlineShares) => (countryCode, airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(3).zipWithIndex)
    }
    
    val championedCountryByAirline: scala.collection.mutable.Map[Int, ListBuffer[(Country, Double)]] = scala.collection.mutable.Map[Int, ListBuffer[(Country, Double)]]()  
      
    val countriesByCode = CountrySource.loadAllCountries().map(country => (country.countryCode, country)).toMap
    topChampionsByCountryCode.foreach { //(country, reputation boost)
      case (countryCode, champions) => champions.foreach {
        case ((championAirlineId, passengerCount), rankingIndex) =>
          val country = countriesByCode(countryCode)
          val ranking = rankingIndex + 1
          val reputationBoost = Computation.computeReputationBoost(country, ranking)
          val existingBoosts : ListBuffer[(Country, Double)] = championedCountryByAirline.getOrElseUpdate(championAirlineId, ListBuffer[(Country, Double)]())
          existingBoosts.append((country, reputationBoost))
      }
    }
    
    championedCountryByAirline.mapValues( _.toList).toMap
  }
 
  


}