package com.patson

import com.patson.data.{AirlineSource, AllianceMissionSource, AllianceSource}
import com.patson.model._
import com.patson.model.airplane.Airplane
import com.patson.model.alliance._
import com.patson.util.{AirlineCache, AirportChampionInfo, CountryChampionInfo}

import scala.collection.{MapView, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.Random


object AllianceSimulation {
  def simulate(cycle : Int, flightLinkResult : List[LinkConsumptionDetails], loungeResult : Map[Lounge, LoungeConsumptionDetails], airportChampionInfo : List[AirportChampionInfo], countryChampionInfo : List[CountryChampionInfo]) = {
    AllianceSource.deleteAllianceStatsBeforeCutoff(cycle - AllianceMissionSimulation.MAX_HISTORY_DURATION)

    val activeMissions = AllianceMissionSource.loadAllianceMissionsByCriteria(List(("status", AllianceMissionStatus.IN_PROGRESS.toString)))

    val eligibleAirlines = ListBuffer[Airline]() //only process airlines with active membership with established alliance

    val missionStartCycleByAllianceId = activeMissions.groupBy(_.allianceId).view.mapValues(_(0).startCycle)
    val missionAirlines = ListBuffer[Airline]() //only airlines that have active membership BEFORE the mission starts
    val alliances = AllianceSource.loadAllAlliances()
    val eligibleAlliances = alliances.filter(_.status == AllianceStatus.ESTABLISHED)
    val missionAllianceIds = activeMissions.map(_.allianceId).toSet
    val missionAlliances = alliances.filter(alliance => missionAllianceIds.contains(alliance.id))

    eligibleAlliances.foreach { alliance =>
      alliance.members.foreach { member =>
        if (member.role != AllianceRole.APPLICANT) {
          eligibleAirlines.append(member.airline)
          if (missionStartCycleByAllianceId.contains(alliance.id) && member.joinedCycle < missionStartCycleByAllianceId(alliance.id)) {
            missionAirlines.append(member.airline)
          }
        }
      }
    }

    val eligibleAirlineIds = eligibleAirlines.map(_.id)
    val eligibleFlightLinkResult = flightLinkResult.filter(linkResult => eligibleAirlineIds.contains(linkResult.link.airline.id))
    val eligibleLoungeVisit = loungeResult.filter {
      case (lounge, _) => eligibleAirlineIds.contains(lounge.airline.id)
    }.map(_._2).toList
    val eligibleAirportChampionInfo = airportChampionInfo.filter(entry => eligibleAirlineIds.contains(entry.loyalist.airline.id))
    val eligibleCountryChampionInfo = countryChampionInfo.filter(entry => eligibleAirlineIds.contains(entry.airline.id))

    val eligibleStats = buildAllianceStats(cycle, eligibleAlliances, eligibleFlightLinkResult, eligibleLoungeVisit, eligibleAirportChampionInfo, eligibleCountryChampionInfo)

    val missionAirlineIds = missionAirlines.map(_.id)
    val missionFlightLinkResult = flightLinkResult.filter(linkResult => missionAirlineIds.contains(linkResult.link.airline))
    val missionLoungeVisit = loungeResult.filter {
      case (lounge, _) => missionAirlineIds.contains(lounge.airline.id)
    }.map(_._2).toList
    val missionAirportChampionInfo = airportChampionInfo.filter(entry => missionAirlineIds.contains(entry.loyalist.airline.id))
    val missionCountryChampionInfo = countryChampionInfo.filter(entry => missionAirlineIds.contains(entry.airline.id))

    val missionStats = buildAllianceStats(cycle, missionAlliances, missionFlightLinkResult, missionLoungeVisit, missionAirportChampionInfo, missionCountryChampionInfo)

    AllianceSource.saveAllianceStats(eligibleStats)
    AllianceSource.saveAllianceMissionStats(missionStats)

    AllianceMissionSimulation.simulate(cycle, missionStats)
  }

  /**
    *
    * @param cycle
    * @param flightLinkResult
    * @param loungeVisits
    * @param airportChampionInfo
    * @param countryChampionInfo
    * @return
    */
  def buildAllianceStats(cycle : Int, alliances : List[Alliance], flightLinkResult : List[LinkConsumptionDetails], loungeVisits : List[LoungeConsumptionDetails],  airportChampionInfo : List[AirportChampionInfo], countryChampionInfo : List[CountryChampionInfo]) : List[AllianceStats] = {
    val linkResultByAllianceId : Map[Int, List[LinkConsumptionDetails]] = flightLinkResult.groupBy(_.link.airline.getAllianceId().get)
    val linkRidershipByAllianceId = mutable.HashMap[Int, LinkClassValues]()
    linkResultByAllianceId.foreach {
      case(allianceId, linkResult) =>
        val soldSeats = linkResult.map(_.link.soldSeats)
        val totalPaxByClass : Map[LinkClass, Int] = Map(ECONOMY -> soldSeats.map(_.economyVal).sum, BUSINESS -> soldSeats.map(_.businessVal).sum, FIRST -> soldSeats.map(_.firstVal).sum)
        linkRidershipByAllianceId.put(allianceId, LinkClassValues.getInstanceByMap(totalPaxByClass))
    }

    val loungeVisitsByAllianceId = loungeVisits.groupBy(_.lounge.allianceId.get).view.mapValues { consumptionEntries =>
      consumptionEntries.map(_.selfVisitors.toLong).sum + consumptionEntries.map(_.allianceVisitors.toLong).sum
    }

    val airportRankingByAllianceId : MapView[Int, List[AirportRankingCount]] = airportChampionInfo.groupBy(_.loyalist.airline.getAllianceId().get).view.mapValues { entriesByAlliance =>
      entriesByAlliance.groupBy(entry => (entry.loyalist.airport.size, entry.ranking)).map {
        case ((airportScale, ranking), championEntries) => AirportRankingCount(airportScale, ranking, championEntries.size)
      }.toList
    }

    val countryRankingByAllianceId : MapView[Int, List[CountryRankingCount]] = countryChampionInfo.groupBy(_.airline.getAllianceId().get).view.mapValues { entriesByAlliance =>
      entriesByAlliance.groupBy(entry => (getCountryPopulationThreshold(entry.country.airportPopulation), entry.ranking)).map {
        case ((populationThreshold, ranking), championEntries) => CountryRankingCount(populationThreshold, ranking, championEntries.size)
      }.toList
    }

    val loyalistByAllianceId : MapView[Int, Long] = airportChampionInfo.groupBy(_.loyalist.airline.getAllianceId().get).view.mapValues { entriesByAlliance =>
      entriesByAlliance.map(_.loyalist.amount.toLong).sum
    }


    alliances.map { alliance =>
      AllianceStats(alliance,
        linkRidershipByAllianceId.getOrElse(alliance.id, LinkClassValues.getInstance()),
        loungeVisitsByAllianceId.getOrElse(alliance.id, 0),
        loyalistByAllianceId.getOrElse(alliance.id, 0),
        airportRankingByAllianceId.getOrElse(alliance.id, List.empty),
        countryRankingByAllianceId.getOrElse(alliance.id, List.empty),
        cycle)
    }
  }

  val COUNTRY_POPULATION_THRESHOLD = List(1_000_000, 10_000_000, 100_000_000)

  def getCountryPopulationThreshold(population : Long) : Long = {
    var walker = 0
    COUNTRY_POPULATION_THRESHOLD.foreach { threshold =>
      if (population < threshold) {
        return walker
      }
      walker = threshold
    }

    walker
  }

}
