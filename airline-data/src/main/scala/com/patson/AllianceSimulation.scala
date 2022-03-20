package com.patson

import com.patson.data.{AirlineSource, AllianceMissionSource, AllianceSource}
import com.patson.model._
import com.patson.model.airplane.Airplane
import com.patson.model.alliance._
import com.patson.util.{AirlineCache, AirportChampionInfo}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


object AllianceSimulation {
  def simulate(cycle : Int, flightLinkResult : List[LinkConsumptionDetails], loungeResult : Map[Lounge, LoungeConsumptionDetails], airportChampionInfo : List[AirportChampionInfo], countryChampionInfo : List[CountryAirlineTitle]) = {
    AllianceSource.deleteAllianceStatsBeforeCutoff(cycle - AllianceMissionSimulation.MAX_HISTORY_DURATION)

    val activeMissions = AllianceMissionSource.loadAllianceMissionsByCriteria(List(("status", AllianceMissionStatus.IN_PROGRESS.toString)))

    val eligibleAirlines = ListBuffer[Airline]() //only process airlines with active membership with established alliance

    val missionStartCycleByAllianceId = activeMissions.groupBy(_.allianceId).view.mapValues(_(0).startCycle)
    val missionAirlines = ListBuffer[Airline]()
    val alliances = AllianceSource.loadAllAlliances()
    alliances.foreach { alliance =>
      if (alliance.status == AllianceStatus.ESTABLISHED) {
        alliance.members.foreach { member =>
          if (member.role != AllianceRole.APPLICANT) {
            eligibleAirlines.append(member.airline)
            if (missionStartCycleByAllianceId.contains(alliance.id) && member.joinedCycle >= missionStartCycleByAllianceId(alliance.id)) {
              missionAirlines.append(member.airline)
            }
          }
        }
      }


    val eligibleAirlineIds = eligibleAirlines.map(_.id)
    val eligibleFlightLinkResult = flightLinkResult.filter(linkResult => eligibleAirlineIds.contains(linkResult.link.airline))
    val eligibleAirportChampionInfo = airportChampionInfo.filter(entry => eligibleAirlineIds.contains(entry.loyalist.airline.id))
    val eligibleCountryChampionInfo = countryChampionInfo.filter(entry => eligibleAirlineIds.contains(entry.airline.id))

    val eligibleStats = buildAllianceStats(cycle, eligibleFlightLinkResult, eligibleAirportChampionInfo, eligibleCountryChampionInfo)

    val missionAirlineIds = missionAirlines.map(_.id)
    val missionFlightLinkResult = flightLinkResult.filter(linkResult => missionAirlineIds.contains(linkResult.link.airline))
    val missionAirportChampionInfo = airportChampionInfo.filter(entry => missionAirlineIds.contains(entry.loyalist.airline.id))
    val missionCountryChampionInfo = countryChampionInfo.filter(entry => missionAirlineIds.contains(entry.airline.id))

    val missionStats = buildAllianceStats(cycle, missionFlightLinkResult, missionAirportChampionInfo, missionCountryChampionInfo)

    AllianceSource.saveAllianceStats(eligibleStats)
    AllianceSource.saveAllianceMissionStats(missionStats)

    AllianceMissionSimulation.simulate(cycle, missionStats)
  }

  /**
    *
    * @param cycle
    * @param linkRidershipDetails
    * @param loungeResult
    * @param airportChampionInfo
    * @param countryChampionInfo
    * @return
    */
  def buildAllianceStats(cycle : Int, alliances : List[Alliance], flightLinkResult : List[LinkConsumptionDetails], airportChampionInfo : List[AirportChampionInfo], countryChampionInfo : List[CountryAirlineTitle]) : List[AllianceStats] = {
    val linkResultByAllianceId : Map[Int, List[LinkConsumptionDetails]] = flightLinkResult.groupBy(_.link.airline.getAllianceId().get)
    val linkRidershipByAllianceId = mutable.HashMap[Int, LinkClassValues]()
    linkResultByAllianceId.foreach {
      case(allianceId, linkResult) =>
        val soldSeats = linkResult.map(_.link.soldSeats)
        val totalPaxByClass : Map[LinkClass, Int] = Map(ECONOMY -> soldSeats.map(_.economyVal).sum, BUSINESS -> soldSeats.map(_.businessVal).sum, FIRST -> soldSeats.map(_.firstVal).sum)
        linkRidershipByAllianceId.put(allianceId, LinkClassValues.getInstanceByMap(totalPaxByClass))
    }

    alliances.map { alliance =>
      AllianceStats(alliance,
        linkRidershipByAllianceId.getOrElse(alliance.id, LinkClassValues.getInstance()),
        cycle)
    }
  }

  }


}
