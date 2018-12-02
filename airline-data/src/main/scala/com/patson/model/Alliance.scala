package com.patson.model

import scala.collection.mutable.ListBuffer
import com.patson.data.CountrySource
import com.patson.util.ChampionUtil

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
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE, BOOT_ALLIANCE = Value
}

object Alliance {
  val MAX_MEMBER_COUNT = 12
  val ESTABLISH_MIN_MEMBER_COUNT = 3

  val getReputationBonus: (Int => Double) = { (ranking: Int) =>
    if (ranking == 1) {
      20
    } else if (ranking == 2) {
      15
    } else if (ranking == 3) {
      12
    } else if (ranking == 4) {
      10
    } else if (ranking == 5) {
      8
    } else if (ranking == 6) {
      7
    } else if (ranking == 7) {
      6
    } else if (ranking == 8) {
      5
    } else if (ranking == 9) {
      4
    } else if (ranking == 10) {
      3      
    } else {
      2
    }
  }

  val getMaxFrequencyBonus: (Int => Int) = { (ranking: Int) =>
    if (ranking == 1) {
      15
    } else if (ranking == 2) {
      10
    } else if (ranking == 3) {
      5
    } else if (ranking <= 10) {
      2
    } else {
      0
    }
  }

  def getRankings(alliances: List[Alliance]): Map[Alliance, (Int, BigDecimal)] = {
    val countryChampions = ChampionUtil.getAllChampionInfo()
    val alliancesWithChampionPoints: List[(Alliance, BigDecimal)] = alliances.filter(_.status == AllianceStatus.ESTABLISHED).map {
      alliance =>
        var allianceChampionPoints: BigDecimal = 0.0
        alliance.members.foreach { allianceMember =>
          val memberChampiontPoints: BigDecimal =
            if (allianceMember.role == AllianceRole.APPLICANT) { //do not add champion points from applicant
              0
            } else {
              countryChampions.filter(_.airline.id == allianceMember.airline.id).map(_.reputationBoost).sum
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
}