package controllers

import com.patson.{AllianceMissionSimulation, AllianceSimulation}
import com.patson.data.{AllianceMissionSource, AllianceSource, CycleSource}
import com.patson.model.{AllianceMember, AllianceRole}
import com.patson.model.alliance.{AirportRankingCount, AllianceMission, AllianceMissionReward, AllianceMissionStatus, AllianceStats}
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsValue, Json, Writes}

object AllianceMissionUtil {


  def buildPreviousMissionJson(allianceMember : AllianceMember): JsObject = {
    val allianceId = allianceMember.allianceId
    val cycle = CycleSource.loadCycle()
    val missionsByStartCycle : List[(Int, List[AllianceMission])] = AllianceMissionSource.loadAllianceMissionsAfterCutoff(allianceId, cycle - 2 * AllianceMissionSimulation.MISSION_DURATION).groupBy(_.startCycle).toList.sortBy(_._1)

    if (missionsByStartCycle.length >= 2) {
      var result = Json.obj()
      val previousMissions = missionsByStartCycle(0)._2
      var potentialMissions : List[AllianceMission] = previousMissions
      previousMissions.filter(mission => mission.status != AllianceMissionStatus.CANDIDATE).foreach { selectedMission =>
        var selectedMissionJson = Json.toJson(selectedMission).asInstanceOf[JsObject]

        val includeRewards = AllianceRole.isAccepted(allianceMember.role) && allianceMember.joinedCycle < selectedMission.startCycle

        if (includeRewards) {
          var rewards = AllianceMissionSource.loadRewardOptions(selectedMission.id, allianceMember.airline.id)
          if (rewards.isEmpty) { //not successful, just use "generated" reward for display
            rewards = AllianceMissionReward.generateMissionRewardOptions(selectedMission.id, allianceMember.airline.id,  1, selectedMission.difficulty)
          }

          selectedMissionJson = selectedMissionJson + ("potentialRewards" -> Json.toJson(rewards))
        }

        result = result + ("selectedMission" -> selectedMissionJson)
      }
      result = result + ("missionCandidates" -> Json.toJson(potentialMissions))
      result
    } else {
      Json.obj()
    }
  }



  def buildCurrentMissionJson(allianceMember : AllianceMember): JsObject = {
    val allianceId = allianceMember.allianceId
    val cycle = CycleSource.loadCycle()

    val stats = AllianceSource.loadAllianceMissionStatsByCriteria(List(("alliance", "=", allianceId), ("cycle", "=", cycle - 1)))
    var result = Json.obj()

    val missions = AllianceMissionSource.loadAllianceMissionsAfterCutoff(allianceId, cycle - AllianceMissionSimulation.MISSION_DURATION)

    var potentialMissions : List[AllianceMission] = missions
    missions.filter(mission => mission.status != AllianceMissionStatus.CANDIDATE).foreach { selectedMission =>
      var selectedMissionJson = Json.toJson(selectedMission).asInstanceOf[JsObject]

      val includeRewards = AllianceRole.isAccepted(allianceMember.role) && allianceMember.joinedCycle < selectedMission.startCycle

      if (includeRewards) {
        var rewards = AllianceMissionSource.loadRewardOptions(selectedMission.id, allianceMember.airline.id)
        if (rewards.isEmpty) { //nothing saved yet. Just use "generated" reward for display
          rewards = AllianceMissionReward.generateMissionRewardOptions(selectedMission.id, allianceMember.airline.id, 1, selectedMission.difficulty)
        }

        selectedMissionJson = selectedMissionJson + ("potentialRewards" -> Json.toJson(rewards))
      }

      result = result + ("selectedMission" -> selectedMissionJson)
    }

    var missionCandidatesJson = Json.arr()
    potentialMissions.sortBy(_.id).foreach { potentialMission => //find the corresponding last week stat
      var missionCandidateJson = Json.toJson(potentialMission).asInstanceOf[JsObject]
      if (cycle <= potentialMission.startCycle + AllianceMissionSimulation.SELECTION_DURATION) { //add last week value to make selecting mission easier
        stats.foreach { stats =>
          missionCandidateJson = missionCandidateJson + ("lastWeekValue" -> JsNumber(potentialMission.getValueFromStats(stats)))
        }
      }

      missionCandidatesJson = missionCandidatesJson.append(missionCandidateJson)
    }

    result = result + ("missionCandidates" -> missionCandidatesJson)
    result
  }
}
