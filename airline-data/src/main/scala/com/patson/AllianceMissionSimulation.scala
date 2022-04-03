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

//  val SELECTION_DURATION = 1 * AllianceMission.WEEKS_PER_YEAR //1 year
//  val MISSION_DURATION = 12 * AllianceMission.WEEKS_PER_YEAR// 11 years (first year is SELECTION)

  //TODO test
  val SELECTION_DURATION = 4 // 4 weeks
  val MISSION_DURATION = 4 + AllianceMission.WEEKS_PER_YEAR// 1

  //END TODO

  val MAX_MISSION_CANDIDATES = 3 //how many options

  def main(args : Array[String]) : Unit = {
    AllianceMissionSource.deleteAllianceMissionsByCutoff(0)

    simulate(1, Map.empty, Map.empty)

    for (i <- 1 + SELECTION_DURATION until (1 + MISSION_DURATION)) {
      simulate(i, Map.empty, Map.empty)
      println(i)
    }
    simulate(1 + MISSION_DURATION, Map.empty, Map.empty)


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
            generateMissionCandidates(eligibleAllianceStats(alliance.id), cycle, MISSION_DURATION)
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
                generateMissionCandidates(eligibleAllianceStats(alliance.id), cycle, MISSION_DURATION)
              }
            }
          }
      }
    }
  }

  def generateMissionCandidates(allianceStats : AllianceStats, startCycle : Int, duration : Int) = {
    var candidateMissions = AllianceMission.generateMissionCandidates(allianceStats).map { candidate =>
      AllianceMission.buildAllianceMission(candidate.missionType, startCycle, duration, allianceStats.alliance.id, AllianceMissionStatus.CANDIDATE, candidate.properties)
    }

    //randomly pick 3 mission types, and pick one for each type
    candidateMissions = Random.shuffle(candidateMissions.groupBy(_.missionType).toList.filter(_._2.length > 0)).take(MAX_MISSION_CANDIDATES).map {
      case(_, missionsByType) =>  missionsByType(Random.nextInt(missionsByType.length))
    }

    AllianceMissionSource.saveAllianceMissions(candidateMissions)
  }


  def updateMissionProgress(currentCycle : Int, mission : AllianceMission, newStats : AllianceStats) = {
    mission.updateStats(currentCycle, newStats)
  }

  def concludeMission(mission : AllianceMission, finalProgress : AllianceMissionPropertiesHistory) = {
    val result = mission.isSuccessful(finalProgress)
    if (result.isSuccessful) { //generate reward options
      val options = AllianceMissionReward.generateMissionRewardOptions(mission.id, result.completionFactor, mission.difficulty)
      AllianceMissionSource.saveRewardOptions(options)
    }
    mission.status = AllianceMissionStatus.CONCLUDED
    AllianceMissionSource.updateAllianceMission(mission)

  }
}
