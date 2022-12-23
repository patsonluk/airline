package com.patson.model

import scala.collection.mutable.ListBuffer
import com.patson.data.{AllianceSource, CountrySource, CycleSource}
import com.patson.model.AllianceEvent.{BOOT_ALLIANCE, LEAVE_ALLIANCE, PROMOTE_LEADER, REJECT_ALLIANCE}
import com.patson.util.{AirlineCache, ChampionUtil}
import play.api.libs.json.Json

import scala.math.BigDecimal.RoundingMode

case class Alliance(name: String, creationCycle: Int, members: List[AllianceMember], var id: Int = 0) {
  val status = {
    if (members.filter(_.role != AllianceRole.APPLICANT).length < Alliance.ESTABLISH_MIN_MEMBER_COUNT) {
      AllianceStatus.FORMING
    } else {
      AllianceStatus.ESTABLISHED
    }
  }

  def removeMember(member : AllianceMember, removeSelf : Boolean) : Unit = {
    import AllianceRole._
    val targetAirlineId = member.airline.id
    val currentCycle = CycleSource.loadCycle()
    if (members.find(_.airline.id == targetAirlineId).isDefined) { //just to play safe
      AllianceSource.deleteAllianceMember(targetAirlineId)
      if (removeSelf) {
        AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = name, airline = member.airline, event = LEAVE_ALLIANCE, cycle = currentCycle))
        if (member.role == LEADER) {
          members.filter(_.role == CO_LEADER).sortBy(_.joinedCycle).headOption match {
            case Some(coLeader) =>
              AllianceSource.saveAllianceMember(coLeader.copy(role = AllianceRole.LEADER))
              AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = name, airline = coLeader.airline, event = PROMOTE_LEADER, cycle = currentCycle))
            case None => AllianceSource.deleteAlliance(id) //no co-leader, remove the alliance
          }
        }
      } else {
        if (member.role == APPLICANT) {
          AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = name, airline = member.airline, event = REJECT_ALLIANCE, cycle = currentCycle))
        } else {
          AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = name, airline = member.airline, event = BOOT_ALLIANCE, cycle = currentCycle))
        }
      }
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
  val LEADER, CO_LEADER, MEMBER, APPLICANT = Value

  val isAdmin : AllianceRole => Boolean = (role : AllianceRole) => role match {
    case LEADER | CO_LEADER => true
    case _ => false
  }

  val isAccepted : AllianceRole => Boolean = (role : AllianceRole) => role match {
    case APPLICANT => false
    case _ => true
  }
}

case class AllianceHistory(allianceName: String, airline: Airline, event: AllianceEvent.Value, cycle: Int, var id: Int = 0)

object AllianceEvent extends Enumeration {
  type AllianceEvent = Value
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE, BOOT_ALLIANCE, PROMOTE_LEADER, PROMOTE_CO_LEADER, DEMOTE = Value
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
      35
    } else if (ranking == 4) {
      32
    } else if (ranking == 5) {
      30
    } else if (ranking == 6) {
      28
    } else if (ranking == 7) {
      26
    } else if (ranking == 8) {
      24
    } else if (ranking == 9) {
      22
    } else if (ranking == 10) {
      20
    } else {
      Math.max(30 - ranking, 5)
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