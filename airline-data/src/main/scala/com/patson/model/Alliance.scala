package com.patson.model

import scala.collection.mutable.ListBuffer
import com.patson.data.CountrySource
import com.patson.util.ChampionUtil

import scala.math.BigDecimal.RoundingMode

case class Alliance(name: String, creationCycle: Int, members: List[AllianceMember], var id: Int = 0) {
  val status = {
    if (members.filter(_.role != AllianceRole.APPLICANT).length < Alliance.ESTABLISH_MIN_MEMBER_COUNT) {
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
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE, BOOT_ALLIANCE, PROMOTE_LEADER = Value
}

object Alliance {
  val MAX_MEMBER_COUNT = 12
  val ESTABLISH_MIN_MEMBER_COUNT = 3

  val getReputationBonus: (Int => Double) = { (ranking: Int) =>
    if (ranking == 1) {
      50
    } else if (ranking == 2) {
      40
    } else if (ranking == 3) {
      30
    } else if (ranking == 4) {
      25
    } else if (ranking == 5) {
      20
    } else if (ranking == 6) {
      15
    } else if (ranking == 7) {
      12
    } else if (ranking == 8) {
      10
    } else if (ranking == 9) {
      9
    } else if (ranking == 10) {
      8
    } else {
      5
    }
  }

//  val getMaxFrequencyBonus: (Int => Int) = { (ranking: Int) =>
//    if (ranking == 1) {
//      15
//    } else if (ranking == 2) {
//      10
//    } else if (ranking == 3) {
//      5
//    } else if (ranking <= 10) {
//      2
//    } else {
//      0
//    }
//  }

  /**
    *
    * @param alliances
    * @return Map[Alliance, (ranking, champion points)] . Take note that ranking starts with 1 as the top alliance
    */
  def getRankings(alliances: List[Alliance]): Map[Alliance, (Int, BigDecimal)] = {
    //val countryChampions = ChampionUtil.getAllCountryChampionInfo()
    val airportChampionsByAirlineId = ChampionUtil.loadAirportChampionInfo().groupBy(_.loyalist.airline.id)
    val alliancesWithChampionPoints: List[(Alliance, BigDecimal)] = alliances.filter(_.status == AllianceStatus.ESTABLISHED).map {
      alliance =>
        var allianceChampionPoints: BigDecimal = 0.0
        alliance.members.foreach { allianceMember =>
          val memberChampionPoints: BigDecimal =
            if (allianceMember.role == AllianceRole.APPLICANT) { //do not add champion points from applicant
              0
            } else {
              BigDecimal(airportChampionsByAirlineId.get(allianceMember.airline.id).map(_.map(_.reputationBoost).sum).getOrElse(0.0))
            }
          //println(s"${allianceMember.airline.name} => " + memberChampionPoints)
          allianceChampionPoints = allianceChampionPoints + memberChampionPoints
        }

        (alliance, allianceChampionPoints.setScale(2, RoundingMode.HALF_UP))
    }

    val alliancesWithRanking = alliancesWithChampionPoints.sortBy(_._2)(Ordering.BigDecimal.reverse).zipWithIndex.map {
      case ((alliance, allianceChampionPoints), sortedIndex) => (alliance, (sortedIndex + 1, allianceChampionPoints))
    }

    alliancesWithRanking.toMap
  }

  def fromId(id : Int) = {
    Alliance("<unknown>", 0, List.empty, id)
  }
}