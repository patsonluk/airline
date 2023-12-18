package controllers

import com.patson.AirportSimulation
import com.patson.data.{AirportSource, CountrySource, LinkStatisticsSource, LoyalistSource}
import com.patson.model.AirportFeatureType.{FINANCIAL_HUB, INTERNATIONAL_HUB, VACATION_HUB}
import com.patson.model._
import com.patson.util.{AirlineCache, AirportChampionInfo, ChampionUtil, CountryCache}
import models.AirportWithChampion
import websocket.MyWebSocketActor

import scala.collection.MapView

object AirportUtil {
  var cachedAirportChampions : List[AirportWithChampion] = getAirportChampions()

  def getAirportChampions() : List[AirportWithChampion] = {
    val latestHistoryCycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, 0)
    val compareToCycle = AirportSimulation.getHistoryCycle(MyWebSocketActor.lastSimulatedCycle, -2)
    val cycleDelta = latestHistoryCycle - compareToCycle
    val allAirportsLatestLoyalists : MapView[Int, Map[Int, Int]] = LoyalistSource.loadLoyalistHistoryByCycle(latestHistoryCycle).groupBy(_.entry.airport.id).view.mapValues(_.map(history => (history.entry.airline.id, history.entry.amount)).toMap)
    val allAirportsPreviousLoyalists : MapView[Int, Map[Int, Int]] = LoyalistSource.loadLoyalistHistoryByCycle(compareToCycle).groupBy(_.entry.airport.id).view.mapValues(_.map(history => (history.entry.airline.id, history.entry.amount)).toMap)
    val loyalistByAirportId : Map[Int, List[AirportChampionInfo]] = ChampionUtil.loadAirportChampionInfo().groupBy(_.loyalist.airport.id)

    cachedAirportsByPower.map { airport =>
      loyalistByAirportId.get(airport.id) match {
        case Some(loyalists) =>
          val airlinesSortByRank = loyalists.sortBy(_.ranking).map(_.loyalist.airline)
          val championAirline = airlinesSortByRank.headOption
          val contestingAirline = {
            if (airlinesSortByRank.size < 2) {
              None
            } else {
              val championCurrentLoyalistCount = getLoyalistCount(allAirportsLatestLoyalists, airport, airlinesSortByRank(0))
              val contenderCurrentLoyalistCount = getLoyalistCount(allAirportsLatestLoyalists, airport, airlinesSortByRank(1))
              val championLoyalistDeltaPerCycle = (championCurrentLoyalistCount - getLoyalistCount(allAirportsPreviousLoyalists, airport, airlinesSortByRank(0))).toDouble / cycleDelta
              val contenderLoyalistDeltaPerCycle = (contenderCurrentLoyalistCount - getLoyalistCount(allAirportsPreviousLoyalists, airport, airlinesSortByRank(1))).toDouble / cycleDelta

              val predictionDuration = 200 //what about 200 cycles from now?
              val championPredictedLoyalistCount = championCurrentLoyalistCount + predictionDuration * championLoyalistDeltaPerCycle
              val contenderPredictedLoyalistCount = contenderCurrentLoyalistCount + predictionDuration * contenderLoyalistDeltaPerCycle
              if (contenderPredictedLoyalistCount > championPredictedLoyalistCount) {
                Some(airlinesSortByRank(1))
              } else {
                None
              }
            }
          }
          AirportWithChampion(airport, championAirline, contestingAirline)
        case None => AirportWithChampion(airport, None, None)
      }
    }
  }

  def getLoyalistCount(allAirportsLoyalists : MapView[Int, Map[Int, Int]], airport : Airport, airline : Airline) : Int = {
    allAirportsLoyalists.get(airport.id) match {
      case Some(loyalistsByAirline) => loyalistsByAirline.getOrElse(airline.id, 0)
      case None => 0
    }
  }

  def refreshAirports() = {
    cachedAirportChampions = getAirportChampions()
    visibleAirports = getVisibleAirports(airportByPowerCount)
  }

  private val airportByPowerCount = 4000
  var visibleAirports = getVisibleAirports(airportByPowerCount)

  private[this] def getVisibleAirports(airportByPowerCount : Int) : List[AirportWithChampion] = {
    val cachedAirportChampions = AirportUtil.cachedAirportChampions
    val powerfulAirports : Map[Int, AirportWithChampion] = cachedAirportChampions.takeRight(airportByPowerCount).map(entry => (entry.airport.id, entry)).toMap
    val mostPowerfulAirportsPerCountry : List[AirportWithChampion] = cachedAirportChampions.groupBy(_.airport.countryCode).values.flatMap { airportsOfACountry =>
      if (airportsOfACountry.length > 0) {
        List(airportsOfACountry.reverse.apply(0))
      } else {
        List()
      }
    }.toList
    val result = (powerfulAirports.values ++ mostPowerfulAirportsPerCountry.filter { mostPowerfulAirportOfACountry =>
      val alreadyInList = powerfulAirports.contains(mostPowerfulAirportOfACountry.airport.id)
      //println(s"$alreadyInList ? $mostPowerfulAirportOfACountry")
      !alreadyInList
    }).toList
    result
  }
}


