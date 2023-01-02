package com.patson

import com.patson.data.{AirportSource, AllianceMissionSource, AllianceSource, CycleSource, EventSource, LinkStatisticsSource}
import com.patson.model.event.{EventType, Olympics, OlympicsAirlineVoteWithWeight, OlympicsVoteRound}
import com.patson.model._
import com.patson.model.alliance.{AllianceMission, AllianceMissionPropertiesHistory, AllianceMissionReward, AllianceMissionStatus, AllianceMissionType, AllianceStats, TotalLoungeVisitMission, TotalPaxMission}
import com.patson.util.AllianceCache

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable.ListBuffer
import scala.collection.{MapView, mutable}
import scala.util.Random


object AllianceMissionSimulation {
  val MAX_HISTORY_DURATION = 40 * AllianceMission.WEEKS_PER_YEAR //40 years

  val SELECTION_DURATION = 1 * AllianceMission.WEEKS_PER_YEAR //1 year
  val ACTIVE_DURATION = 10 * AllianceMission.WEEKS_PER_YEAR// 10 years

  val MISSION_DURATION = SELECTION_DURATION + ACTIVE_DURATION

  val MAX_MISSION_CANDIDATES = 3 //how many options

  def main(args : Array[String]) : Unit = {
    val startCycle = CycleSource.loadCycle() - MISSION_DURATION - 1
    AllianceMissionSource.deleteAllianceMissionsByCutoff(CycleSource.loadCycle())

    val initStats = AllianceSource.loadAllAlliances().filter(_.status == AllianceStatus.ESTABLISHED).map { alliance =>
      (alliance.id, AllianceStats.empty(alliance, 1))
    }.toMap
    simulate(startCycle, initStats, initStats)

    //select LOUNGE mission for test
    AllianceMissionSource.loadAllianceMissionsByCriteria(List.empty).filter(_.missionType == AllianceMissionType.TOTAL_LOUNGE_VISIT).foreach { mission =>
      mission.status = AllianceMissionStatus.SELECTED
      AllianceMissionSource.updateAllianceMission(mission)
      println(mission)
    }

    for (i <- startCycle + SELECTION_DURATION until (startCycle + MISSION_DURATION)) {
      val stats =
      AllianceMissionSource.loadAllianceMissionsByCriteria(List("status" -> "IN_PROGRESS")).map {
        case mission : TotalLoungeVisitMission =>
          println(mission)
          val stats = AllianceStats.empty(AllianceCache.getAlliance(mission.allianceId).get, i).copy(totalLoungeVisit = mission.threshold)
          (mission.allianceId, stats)
        case mission => (mission.allianceId, AllianceStats.empty(AllianceCache.getAlliance(mission.allianceId).get, i))
      }.toMap
      simulate(i, stats, stats)
      println(i)
    }

    val finishCycle = startCycle + MISSION_DURATION
    simulate(finishCycle, initStats, initStats)
  }

  def cycleToNextPhase(mission: AllianceMission, currentCycle : Int) = { //remaining duration until next phase
    mission.status match {
      case com.patson.model.alliance.AllianceMissionStatus.CANDIDATE | com.patson.model.alliance.AllianceMissionStatus.SELECTED =>
        mission.startCycle + AllianceMissionSimulation.SELECTION_DURATION - currentCycle
      case com.patson.model.alliance.AllianceMissionStatus.IN_PROGRESS =>
        mission.endCycle - currentCycle
      case com.patson.model.alliance.AllianceMissionStatus.CONCLUDED => 0
    }
  }

  def simulate(cycle: Int, eligibleAllianceStats : Map[Int, AllianceStats], missionAllianceStats : Map[Int, AllianceStats]): Unit = {
    //purge
    AllianceMissionSource.deleteAllianceMissionsByCutoff(cycle - MAX_HISTORY_DURATION)



//    EventSource.deleteEventsBeforeCycle(CycleSource.loadCycle() - MAX_HISTORY_DURATION)
    //find missions (could be candidates) within mission duration
    val validMissionByAllianceId : Map[Int, List[AllianceMission]] = AllianceMissionSource.loadAllianceMissionsAfterCutoff(cycle - MISSION_DURATION).groupBy(_.allianceId)

    import com.patson.model.alliance.AllianceMissionStatus._
    //get alliance stats
    val establishedAlliances = AllianceSource.loadAllAlliances().filter(_.status == AllianceStatus.ESTABLISHED)
    //val establishedAllianceStats = allianceStats.map( stats => (stats.alliance.id, stats)).toMap
    establishedAlliances.foreach { alliance =>
      validMissionByAllianceId.get(alliance.id) match {
        case None =>
          if (eligibleAllianceStats.contains(alliance.id)) {
            generateMissionCandidates(eligibleAllianceStats(alliance.id), cycle)
          }
        case Some(validMissions) =>
          val inProgressMissions = validMissions.find(_.status == IN_PROGRESS) //should only be one for now
          if (inProgressMissions.isEmpty) {
            validMissions.groupBy(_.startCycle).foreach { //for now all of them should have same start cycle, but just in case...
              case (startCycle, missions) =>
                if (cycle >= startCycle + SELECTION_DURATION) { //find the selected mission
                  val selectedMission = missions.find(_.status == SELECTED).getOrElse(missions(0)) //leader too busy? just take the first one
                  selectedMission.status = IN_PROGRESS //the selected one should be in progress now
                  AllianceMissionSource.updateAllianceMission(selectedMission) //persist the status
                }
            }
          }

          inProgressMissions.foreach { activeMission => //should only be one for now
            val currentProgress = updateMissionProgress(cycle, activeMission, missionAllianceStats.getOrElse(alliance.id, AllianceStats.empty(alliance, cycle)))
            if (cycle >= activeMission.endCycle) { //the mission is over
              concludeMission(activeMission, currentProgress)
              if (eligibleAllianceStats.contains(alliance.id)) {
                generateMissionCandidates(eligibleAllianceStats(alliance.id), cycle)
              }
            }
          }
      }
    }
  }

  def generateMissionCandidates(allianceStats : AllianceStats, startCycle : Int) = {
    var candidateMissions = AllianceMission.generateMissionCandidates(ACTIVE_DURATION, allianceStats).map { candidate =>
      AllianceMission.buildAllianceMission(candidate.missionType, startCycle, MISSION_DURATION, allianceStats.alliance.id, AllianceMissionStatus.CANDIDATE, candidate.properties)
    }

    //randomly pick 3 mission types, and pick one for each type
    candidateMissions = Random.shuffle(candidateMissions.groupBy(_.missionType).toList.filter(_._2.length > 0)).take(MAX_MISSION_CANDIDATES).map {
      case(_, missionsByType) =>  missionsByType(ThreadLocalRandom.current().nextInt(missionsByType.length))
    }

    AllianceMissionSource.saveAllianceMissions(candidateMissions)
  }


  def updateMissionProgress(currentCycle : Int, mission : AllianceMission, newStats : AllianceStats) = {
    mission.updateStats(currentCycle, newStats)
  }

  def concludeMission(mission : AllianceMission, finalProgress : AllianceMissionPropertiesHistory) = {
    val result = mission.isSuccessful(finalProgress)
    if (result.isSuccessful) { //generate reward options
      AllianceCache.getAlliance(mission.allianceId).foreach { alliance =>
        alliance.members.filter(member => AllianceRole.isAccepted(member.role) && member.joinedCycle <= mission.startCycle).map(_.airline).foreach { airline =>
          val options = AllianceMissionReward.generateMissionRewardOptions(mission.id, airline.id, result.completionFactor, mission.difficulty)
          options.foreach(_.available = true)
          AllianceMissionSource.saveRewardOptions(options)
        }
      }

    }
    mission.status = AllianceMissionStatus.CONCLUDED
    AllianceMissionSource.updateAllianceMission(mission)

  }
}
