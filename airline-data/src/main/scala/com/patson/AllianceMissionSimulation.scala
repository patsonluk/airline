package com.patson

import com.patson.data.{AirportSource, AllianceMissionSource, CycleSource, EventSource, LinkStatisticsSource, AllianceSource}
import com.patson.model.event.{EventType, Olympics, OlympicsAirlineVoteWithWeight, OlympicsVoteRound}
import com.patson.model._
import com.patson.model.alliance.AllianceMission

import scala.collection.mutable.ListBuffer
import scala.collection.{MapView, mutable}
import scala.util.Random


object AllianceMissionSimulation {
  val MAX_HISTORY_DURATION = 40 * AllianceMission.WEEKS_PER_YEAR //40 years
  val MISSION_DURATION = 12 * AllianceMission.WEEKS_PER_YEAR// 12 years


  def simulate(cycle: Int): Unit = {
    //purge
    AllianceMissionSource.deleteAllianceMissionsByCutoff(cycle - MAX_HISTORY_DURATION)


//    EventSource.deleteEventsBeforeCycle(CycleSource.loadCycle() - MAX_HISTORY_DURATION)
    val latestMissionByAllianceId : MapView[Int, AllianceMission] = AllianceMissionSource.loadAllianceMissionsAfterCutoff(cycle - MISSION_DURATION).groupBy(_.allianceId).view.mapValues {
      missionsByAlliance => missionsByAlliance.sortBy(_.startCycle).last
    }

    AllianceSource.loadAllAlliances().foreach { alliance =>
      if (alliance.status == AllianceStatus.ESTABLISHED) {
        latestMissionByAllianceId.get(alliance.id) match {
          case None => generateMissionOptions(alliance)
          case Some(mission) =>
            import com.patson.model.alliance.AllianceMissionStatus._
            mission.status(cycle) match {
              case SELECTION => //do nothing
              case IN_PROGRESS =>
                if (mission.currentWeek(cycle) == 0) { //check selection
                  saveMissionSelection(alliance)
                }
                updateMissionProgress(alliance)
              case CONCLUDED =>
                generateMissionOptions(alliance)
            }
        }
      }

    }

  }

  def generateMissionOptions(alliance : Alliance) = {
    ???
  }
  def saveMissionSelection(alliance : Alliance) = ???

  def updateMissionProgress(alliance : Alliance) = ???


  val BASE_PASSENGER_GOAL = 2000
  val GOAL_BASE_FACTOR = 0.90 //90% of the max possible pax?
  def simulateOlympicsPassengerGoals(olympics: Olympics) = {
    val allLinkStats = LinkStatisticsSource.loadLinkStatisticsByCriteria(List.empty)
    val passengersByAirline: MapView[Airline, Int] = allLinkStats.groupBy(_.key.airline).view.mapValues(_.map(_.passengers).sum)
    val totalPassengers = passengersByAirline.values.sum

    var olympicsTotalPassengers = 0
    for (i <- 0 until Olympics.WEEKS_PER_YEAR) {
      olympicsTotalPassengers += Olympics.getDemandMultiplier(i) * DemandGenerator.OLYMPICS_DEMAND_BASE
    }

    val passengerGoalByAirline = passengersByAirline.mapValues { passengers =>
      val goal = (passengers.toDouble / totalPassengers * olympicsTotalPassengers * GOAL_BASE_FACTOR).toInt
      Math.max(BASE_PASSENGER_GOAL, goal)
    }.toMap

    passengerGoalByAirline
  }

  def simulateOlympicsEnding(olympics : Olympics) = {
    Olympics.getSelectedAffectedAirports(olympics.id).foreach { airport =>
      AirportSource.deleteAirportFeature(airport.id, AirportFeatureType.OLYMPICS_IN_PROGRESS)
    }
  }
  
}
