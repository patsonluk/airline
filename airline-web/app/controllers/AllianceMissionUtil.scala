package controllers

import com.patson.{AllianceMissionSimulation, AllianceSimulation}
import com.patson.data.{AllianceMissionSource, AllianceSource, CycleSource}
import com.patson.model.alliance.{AirportRankingCount, AllianceMission, AllianceMissionReward, AllianceMissionStatus, AllianceStats}
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json, Writes}

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

      smallAirportScaleTopStats.foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> "other", "count" -> smallAirportScaleOtherCount))

      //For airport scale 4-7, key is ranking (1 - 3) value is count for that ranking
      for (scale <- smallAirportThreshold + 1 to largeAirportThreshold - 1) {
        val (topRankings, otherRankings) = stats.airportRankingStats.filter(_.airportScale == scale).partition(_.ranking <= 3)
        topRankings.foreach {
          case AirportRankingCount(_, ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> ranking, "count" -> count))
        }
        airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> "other", "count" -> otherRankings.map(_.count).sum))
      }

      //For airport scale 8+, key is ranking (1 - 3) value is count for that ranking
      val (largeAirportTopRankings, largeAirportOtherRankings) = stats.airportRankingStats.filter(_.airportScale >= largeAirportThreshold).groupBy(_.ranking).partition(_._1 <= 3)
      val largeAirportScaleTopStats : List[(Int, Int)] = largeAirportTopRankings.view.mapValues(_.map(_.count).sum).toList.sortBy(_._1)
      val largeAirportScaleOtherCount : Int = largeAirportOtherRankings.view.mapValues(_.map(_.count).sum).map(_._2).sum

      largeAirportScaleTopStats.foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> "other", "count" -> largeAirportScaleOtherCount))

      result = result + ("airportStats" -> airportStatsJson)

      result = result + ("championedAirports" -> JsNumber(stats.airportRankingStats.filter(_.ranking == 1).map(_.count).sum))

      val thresholds = AllianceSimulation.COUNTRY_POPULATION_THRESHOLD
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

  def buildMissionJson(allianceId : Int): JsObject = {
    val cycle = CycleSource.loadCycle()
    val stats = AllianceSource.loadAllianceStatsByCycle(allianceId, cycle - 1)
    var result = Json.obj()
    result = result + ("stats" -> Json.toJson(stats))

    val missions = AllianceMissionSource.loadAllianceMissionsAfterCutoff(allianceId, cycle - AllianceMissionSimulation.MISSION_DURATION)

    var potentialMissions : List[AllianceMission] = missions.filter(_.status == AllianceMissionStatus.CANDIDATE)
    missions.filter(mission => mission.status == AllianceMissionStatus.IN_PROGRESS || mission.status == AllianceMissionStatus.SELECTED).foreach { selectedMission =>
      var selectedMissionJson = Json.toJson(selectedMission).asInstanceOf[JsObject]
      val progress = Math.max(selectedMission.progress(cycle).toDouble / 100, 1)
      selectedMissionJson = selectedMissionJson + ("potentialRewards" -> Json.toJson(AllianceMissionReward.generateMissionRewardOptions(selectedMission.id, progress, selectedMission.difficulty)))

      result = result + ("selectedMission" -> selectedMissionJson)
      potentialMissions = potentialMissions :+ selectedMission
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
