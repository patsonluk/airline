package com.patson

import com.patson.data.{AirportSource, AllianceMissionSource, AllianceSource, CycleSource, EventSource, LinkStatisticsSource}
import com.patson.model.event.{EventType, Olympics, OlympicsAirlineVoteWithWeight, OlympicsVoteRound}
import com.patson.model._
import com.patson.model.alliance.{AllianceMission, AllianceMissionPropertiesHistory, AllianceMissionReward, AllianceMissionStatus, AllianceStats}

import scala.collection.mutable.ListBuffer
import scala.collection.{MapView, mutable}
import scala.util.Random


object AllianceMissionSimulation {
  val MAX_HISTORY_DURATION = 40 * AllianceMission.WEEKS_PER_YEAR //40 years
  val SELECTION_DURATION = 1 * AllianceMission.WEEKS_PER_YEAR //1 year
  val MISSION_DURATION = 12 * AllianceMission.WEEKS_PER_YEAR// 11 years (first year is SELECTION)
  val MAX_MISSION_CANDIDATES = 3 //how many options


  def simulate(cycle: Int, allianceStats : List[AllianceStats]): Unit = {
    //purge
    AllianceMissionSource.deleteAllianceMissionsByCutoff(cycle - MAX_HISTORY_DURATION)



//    EventSource.deleteEventsBeforeCycle(CycleSource.loadCycle() - MAX_HISTORY_DURATION)
    //find missions (could be candidates) within mission duration
    val validMissionByAllianceId : Map[Int, List[AllianceMission]] = AllianceMissionSource.loadAllianceMissionsAfterCutoff(cycle - MISSION_DURATION).groupBy(_.allianceId)

    import com.patson.model.alliance.AllianceMissionStatus._
    //get alliance stats
    val establishedAlliances = AllianceSource.loadAllAlliances().filter(_.status == AllianceStatus.ESTABLISHED)
    val establishedAllianceStats = allianceStats.map( stats => (stats.alliance.id, stats)).toMap
    establishedAlliances.foreach { alliance =>
      validMissionByAllianceId.get(alliance.id) match {
        case None => generateMissionCandidates(establishedAllianceStats(alliance.id), cycle, MISSION_DURATION)
        case Some(validMissions) =>
          validMissions.groupBy(_.startCycle).foreach {   //for now all of them should have same start cycle, but just in case...
            case (startCycle, missions) =>
              if (cycle == startCycle + SELECTION_DURATION) { //find the selected mission
                val selectedMission = missions.find(_.status == SELECTED).getOrElse(missions(0)) //leader too busy? just take the first one
                selectedMission.status = IN_PROGRESS //the selected one should be in progress now
                AllianceMissionSource.updateAllianceMission(selectedMission) //persist the status
              }
          }

          validMissions.find(_.status == IN_PROGRESS).foreach { activeMission => //should only be one for now
            val currentProgress = updateMissionProgress(cycle, activeMission, establishedAllianceStats(alliance.id))
            if (activeMission.startCycle + MISSION_DURATION >= cycle) { //the mission is over
              concludeMission(activeMission, currentProgress)
              generateMissionCandidates(establishedAllianceStats(alliance.id), cycle, MISSION_DURATION)
            }
          }
      }
    }
  }

  def generateMissionCandidates(allianceStats : AllianceStats, startCycle : Int, duration : Int) = {
    var candidateMissions = AllianceMission.generateMissionCandidates(allianceStats).map { candidate =>
      AllianceMission.buildAllianceMission(candidate.missionType, startCycle, duration, allianceStats.alliance.id, AllianceMissionStatus.CANDIDATE, candidate.properties)
    }
    if (candidateMissions.length > MAX_MISSION_CANDIDATES) {
      candidateMissions = Random.shuffle(candidateMissions).take(MAX_MISSION_CANDIDATES)
    }
    AllianceMissionSource.saveAllianceMissions(candidateMissions)
  }


  def updateMissionProgress(currentCycle : Int, mission : AllianceMission, newStats : AllianceStats) = {
    mission.updateStats(currentCycle, newStats)
  }

  def concludeMission(mission : AllianceMission, finalProgress : AllianceMissionPropertiesHistory) = {
    val result = mission.isSuccessful(finalProgress)
    if (result.isSuccessful) { //generate reward options
      AllianceMissionReward.generateMissionRewardOptions(mission.id, result.completionFactor, mission.difficulty)
    }
    mission.status = AllianceMissionStatus.CONCLUDED
    AllianceMissionSource.updateAllianceMission(mission)

  }
}
