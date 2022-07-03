package controllers

import com.patson.AllianceMissionSimulation
import com.patson.data.{AllianceMissionSource, AllianceSource, CycleSource}
import com.patson.model._
import com.patson.model.alliance.{AllianceMission, AllianceMissionReward, AllianceMissionStatus}
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.mvc._
import play.api.libs.json._

import javax.inject.Inject


class AllianceMissionApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  
  def selectAllianceMission(airlineId : Int, missionId : Int) = AuthenticatedAirline(airlineId) { request =>

    AllianceSource.loadAllianceMemberByAirline(request.user) match {
      case Some(allianceMember) =>
        if (AllianceRole.isAdmin(allianceMember.role)) {
          AllianceMissionSource.loadAllianceMissionsById(missionId) match {
            case None => NotFound(s"Mission with id $missionId not found")
            case Some(mission) =>
              val cycle = CycleSource.loadCycle()
              if (!mission.isActive(cycle) || !(mission.status == AllianceMissionStatus.CANDIDATE || mission.status == AllianceMissionStatus.SELECTED)) {
                BadRequest(s"mission $mission is not in correct state to be selected")
              } else if (mission.allianceId != allianceMember.allianceId) {
                BadRequest(s"airline ${request.user} cannot select mission of alliance id ${mission.allianceId}")
              } else {
                  AllianceMissionSource.loadAllianceMissionsByCriteria(List(("alliance", allianceMember.allianceId), ("status", AllianceMissionStatus.SELECTED.toString))).foreach { previouslySelectedMission =>
                    previouslySelectedMission.status = AllianceMissionStatus.CANDIDATE
                    AllianceMissionSource.updateAllianceMission(previouslySelectedMission)
                  }
                  mission.status = AllianceMissionStatus.SELECTED
                  AllianceMissionSource.updateAllianceMission(mission)

                  Ok(Json.obj(
                    "current" -> AllianceMissionUtil.buildCurrentMissionJson(allianceMember),
                    "previous" -> AllianceMissionUtil.buildPreviousMissionJson(allianceMember)))
                }
              }
        } else {
          Forbidden("Not an admin to select mission")
        }
      case None =>
        NotFound(s"Airline ${request.user} is not in an alliance, cannot select mission $missionId")
    }
  }

  def getAllianceMissionStats(airlineId : Int, missionId : Int) = AuthenticatedAirline(airlineId) { request =>
    AllianceMissionSource.loadAllianceMissionsById(missionId) match {
      case None => NotFound("mission not found")
      case Some(mission) =>
        if (request.user.getAllianceId().isEmpty || request.user.getAllianceId().get != mission.allianceId) {
          BadRequest(s"User ${request.user} should not attempt to load mission ${mission}")
        } else {
          val weeklyValues = AllianceSource.loadAllianceMissionStatsByCriteria(
            List(("alliance", "=", mission.allianceId),
              ("cycle", ">=", mission.startCycle + AllianceMissionSimulation.SELECTION_DURATION),
              ("cycle", "<=", mission.endCycle))).sortBy(_.cycle).map { stats =>
            val weeklyValue = mission.getValueFromStats(stats)
            weeklyValue
          }
          var result = Json.obj("stats" -> weeklyValues)
          mission.properties.get("threshold").foreach { threshold =>
            result = result + ("threshold" -> JsNumber(threshold))
          }
          Ok(result)
        }
    }
  }

  def selectAllianceMissionReward(airlineId : Int, missionId : Int, rewardId : Int) = AuthenticatedAirline(airlineId) { request =>
    AllianceSource.loadAllianceMemberByAirline(request.user) match {
      case Some(allianceMember) =>
        if (!AllianceRole.isAccepted(allianceMember.role)) {
          BadRequest(s"${request.user} is not an accepted member ${allianceMember}")
        } else {
          //verify it's the correct mission
          AllianceMissionSource.loadAllianceMissionsById(missionId) match {
            case None => NotFound(s"Mission with id $missionId not found")
            case Some(mission) =>
              if (mission.allianceId != allianceMember.allianceId) {
                BadRequest(s"${request.user} tried to pick reward from another alliance mission ${mission}")
              } else if (allianceMember.joinedCycle >= mission.startCycle) {
                BadRequest(s"${request.user} tried to pick reward from alliance mission ${mission} but joined after start time ${allianceMember}")
              } else {
                val rewardOptions = AllianceMissionSource.loadRewardOptions(missionId, airlineId)
                rewardOptions.find(_.id == rewardId) match {
                  case None => NotFound(s"$rewardId not found in mission ${mission}")
                  case Some(reward) =>
                    if (reward.claimed || !reward.available) {
                      BadRequest(s"Bad reward status ${reward}")
                    } else { //claim it!
                      reward.apply(mission, request.user)
                      //update available status
                      rewardOptions.foreach { option =>
                        option.available = false
                        AllianceMissionSource.updateRewardOption(option)
                      }
                    }
                    Ok(Json.toJson(rewardOptions))
                }
              }
          }
        }
      case None =>
        NotFound(s"Airline ${request.user} is not in an alliance, cannot select mission $missionId")
    }
  }

}
