package controllers

import com.patson.{AllianceMissionSimulation, AllianceSimulation}
import com.patson.data.{AllianceMissionSource, AllianceSource, CycleSource}
import com.patson.model.{AllianceMember, AllianceRole}
import com.patson.model.alliance.{AirportRankingCount, AllianceMission, AllianceMissionReward, AllianceMissionStatus, AllianceStats}
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsValue, Json, Writes}

object AllianceMissionUtil {
  implicit object AllianceStatsWrites extends Writes[AllianceStats] {
    def writes(stats: AllianceStats): JsValue = {
      var result = Json.obj(
        "pax" -> stats.totalPax,
        "loyalist" -> stats.totalLoyalist,
        "loungeVisit" -> stats.totalLoungeVisit,
        "revenue" -> stats.totalRevenue,
      )
      //fold airport by scales
      var airportStatsJson = Json.arr()

      val smallAirportThreshold = 3
      val largeAirportThreshold = 8

      //For airport scale 3-, key is ranking (1 - 3) value is count for that ranking
      val (smallAirportTopRankings, smallAirportOtherRankings) = stats.airportRankingStats.filter(_.airportScale <= smallAirportThreshold).groupBy(_.ranking).partition(_._1 <= 3)
      val smallAirportScaleTopStats : List[(Int, Int)] = smallAirportTopRankings.view.mapValues(_.map(_.count).sum).toList.sortBy(_._1)
      val smallAirportScaleOtherCount : Int = smallAirportOtherRankings.view.mapValues(_.map(_.count).sum).map(_._2).sum

      smallAirportScaleTopStats.sortBy(_._1).foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> "other", "count" -> smallAirportScaleOtherCount))

      //For airport scale 4-7, key is ranking (1 - 3) value is count for that ranking
      for (scale <- smallAirportThreshold + 1 to largeAirportThreshold - 1) {
        val (topRankings, otherRankings) = stats.airportRankingStats.filter(_.airportScale == scale).partition(_.ranking <= 3)
        topRankings.sortBy(_.ranking).foreach {
          case AirportRankingCount(_, ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> ranking, "count" -> count))
        }
        airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> "other", "count" -> otherRankings.map(_.count).sum))
      }

      //For airport scale 8+, key is ranking (1 - 3) value is count for that ranking
      val (largeAirportTopRankings, largeAirportOtherRankings) = stats.airportRankingStats.filter(_.airportScale >= largeAirportThreshold).groupBy(_.ranking).partition(_._1 <= 3)
      val largeAirportScaleTopStats : List[(Int, Int)] = largeAirportTopRankings.view.mapValues(_.map(_.count).sum).toList.sortBy(_._1)
      val largeAirportScaleOtherCount : Int = largeAirportOtherRankings.view.mapValues(_.map(_.count).sum).map(_._2).sum

      largeAirportScaleTopStats.sortBy(_._1).foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> "other", "count" -> largeAirportScaleOtherCount))

      result = result + ("airportStats" -> airportStatsJson)

      result = result + ("championedAirports" -> JsNumber(stats.airportRankingStats.filter(_.ranking == 1).map(_.count).sum))

      val thresholds = 0 :: AllianceSimulation.COUNTRY_POPULATION_THRESHOLD
      var countryStatsJson = Json.arr()
      var index = 0
      val countryRankingStats = stats.countryRankingStats.sortBy(_.populationThreshold)
      val formatter = java.text.NumberFormat.getIntegerInstance
      thresholds.foreach { threshold =>
        val populationDescription =
          if (thresholds.last != threshold) {
            s"${formatter.format(threshold)} - ${formatter.format(thresholds(index + 1))}"
          } else {
            s"${formatter.format(threshold)}+"
          }
        for (ranking <- 1 to 3) {
          val count = countryRankingStats.find(entry => entry.populationThreshold == threshold && entry.ranking == ranking).map(_.count).getOrElse(0)
          countryStatsJson = countryStatsJson.append(Json.obj("population" -> populationDescription, "ranking" -> ranking, "count" -> count))
        }
        val lowRankingCount = countryRankingStats.filter(entry => entry.populationThreshold == threshold && entry.ranking > 3).map(_.count).sum
        countryStatsJson = countryStatsJson.append(Json.obj("population" -> populationDescription, "ranking" -> "other", "count" -> lowRankingCount))
        index += 1
      }

      result = result + ("countryStats" -> countryStatsJson)
      result = result + ("championedCountries" -> JsNumber(stats.countryRankingStats.filter(_.ranking == 1).map(_.count).sum))

      result
    }
  }

  def buildPreviousMissionJson(allianceMember : AllianceMember): JsObject = {
    val allianceId = allianceMember.allianceId
    val cycle = CycleSource.loadCycle()
    val missionsByStartCycle : List[(Int, List[AllianceMission])] = AllianceMissionSource.loadAllianceMissionsAfterCutoff(allianceId, cycle - 2 * AllianceMissionSimulation.MISSION_DURATION).groupBy(_.startCycle).toList.sortBy(_._1)

    if (missionsByStartCycle.length >= 2) {
      var result = Json.obj()
      val previousMissions = missionsByStartCycle(1)._2
      var potentialMissions : List[AllianceMission] = previousMissions
      previousMissions.filter(mission => mission.status != AllianceMissionStatus.CANDIDATE).foreach { selectedMission =>
        var selectedMissionJson = Json.toJson(selectedMission).asInstanceOf[JsObject]
        val progress = Math.max(selectedMission.progress(cycle).toDouble / 100, 1)

        val includeRewards = AllianceRole.isAccepted(allianceMember.role) && allianceMember.joinedCycle < selectedMission.startCycle

        if (includeRewards) {
          var rewards = AllianceMissionSource.loadRewardOptions(selectedMission.id)
          if (rewards.isEmpty) { //not successful, just use "generated" reward for display
            rewards = AllianceMissionReward.generateMissionRewardOptions(selectedMission.id, progress, selectedMission.difficulty)
          }

          selectedMissionJson = selectedMissionJson + ("potentialRewards" -> Json.toJson(rewards))
        }

        result = result + ("selectedMission" -> selectedMissionJson)
        potentialMissions = potentialMissions :+ selectedMission
        result = result + ("missionCandidates" -> Json.toJson(potentialMissions))
      }
      result
    } else {
      Json.obj()
    }
  }



  def buildCurrentMissionJson(allianceMember : AllianceMember): JsObject = {
    val allianceId = allianceMember.allianceId
    val cycle = CycleSource.loadCycle()
    val stats = AllianceSource.loadAllianceStatsByCycle(allianceId, cycle - 1)
    var result = Json.obj()
    result = result + ("stats" -> Json.toJson(stats))

    val missions = AllianceMissionSource.loadAllianceMissionsAfterCutoff(allianceId, cycle - AllianceMissionSimulation.MISSION_DURATION)

    var potentialMissions : List[AllianceMission] = missions
    missions.filter(mission => mission.status != AllianceMissionStatus.CANDIDATE).foreach { selectedMission =>
      var selectedMissionJson = Json.toJson(selectedMission).asInstanceOf[JsObject]
      val progress = Math.max(selectedMission.progress(cycle).toDouble / 100, 1)

      val includeRewards = AllianceRole.isAccepted(allianceMember.role) && allianceMember.joinedCycle < selectedMission.startCycle

      if (includeRewards) {
        var rewards = AllianceMissionSource.loadRewardOptions(selectedMission.id)
        if (rewards.isEmpty) { //nothing saved yet. Just use "generated" reward for display
          rewards = AllianceMissionReward.generateMissionRewardOptions(selectedMission.id, progress, selectedMission.difficulty)
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
    result = result + ("isAdmin" -> JsBoolean(AllianceRole.isAdmin(allianceMember.role)))
    result
  }
}
