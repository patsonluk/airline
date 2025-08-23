package com.patson.util

import com.patson.model._
import com.patson.data.{AirlineSource, AirportSource, Constants, CountrySource, LoyalistSource}
import com.patson.util.ChampionUtil.ReputationBonus

import scala.collection.mutable.ListBuffer

case class CountryChampionInfo(airline : Airline, country : Country, passengerCount : Long, ranking : Int)
case class AirportChampionInfo(loyalist : Loyalist, ranking : Int, reputationBoost : Double, bonuses : List[ReputationBonus])

object ChampionUtil {
  def getAllCountryChampionInfo() : List[CountryChampionInfo] =  {
     getCountryChampionInfoByFilter(_ => true, List.empty)
  }

  def getCountryChampionInfoByAirlineId(airlineId : Int) = {
    getCountryChampionInfoByFilter(checkAirlineId => airlineId == checkAirlineId, List.empty)
  }

  def getCountryChampionInfoByCountryCode(countryCode : String) = {
    getCountryChampionInfoByFilter(_ => true, List(("country", countryCode)))
  }

  def getCountryChampionInfoByFilter(airlineIdFilter : Int => Boolean, marketShareCriteria : List[(String, Any)]) = {
    val result = ListBuffer[CountryChampionInfo]()

    val allMarketShares = CountrySource.loadMarketSharesByCriteria(marketShareCriteria)

    val airlineIds = allMarketShares.flatMap(_.airlineShares.keys).toList.filter(airlineIdFilter)

    val airlines = AirlineSource.loadAirlinesByIds(airlineIds, false).map(airline => (airline.id, airline)).toMap

    allMarketShares.map {
      case CountryMarketShare(countryCode, airlineShares) => {
        val country = CountryCache.getCountry(countryCode).get
        val topAirlineSharesWithSortedIndex : List[((Int, Long), Int)] = airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(10).zipWithIndex

        val championInfoForThisCountry = topAirlineSharesWithSortedIndex.map {
          case((airlineId, passengerCount), index) => {
            if (airlineIdFilter(airlineId)) {
              val ranking = index + 1
              Some(CountryChampionInfo(airlines.getOrElse(airlineId, Airline.fromId(airlineId)), country, passengerCount, ranking))
            } else {
              None
            }
          }
        }
        result ++= championInfoForThisCountry.flatten
      }
    }
    
    result.toList
  }

  val BASE_BOOST = 0.5
  val MAX_ECONOMIC_BOOST = 20.0
  val reputationBoostBrackets : Map[Int, Double] = Map(
    1 -> 1,
    2 -> 0.5,
    3 -> 0.3,
    4 -> 0.2,
    5 -> 0.1,
    6 -> 0.08,
    7 -> 0.06,
    8 -> 0.04,
    9 -> 0.03,
    10 -> 0.02,
    11 -> 0.015,
    12 -> 0.01
  )

  case class ReputationBonus(description : String)
  /**
    * Reputation boost if airport is at full loyalist ie loyalist = population
    * @return
    */
  def computeFullReputationBoost(airport : Airport, airline : Airline, ranking : Int) : (Double, List[ReputationBonus]) = {
    val bonuses = ListBuffer[ReputationBonus]()
    val ratioToModelAirportPower = airport.power.toDouble / Computation.MODEL_AIRPORT_POWER
    var boost = BASE_BOOST
    //val economicPowerRating = Math.max(0, math.log10(ratioToModelAirportPower * 100) / 2) //0 to 1
    val economicPowerRating = Math.max(0, math.log(ratioToModelAirportPower * 16) / math.log(2) / 4) //0 to 1
    boost += MAX_ECONOMIC_BOOST * economicPowerRating

    boost += (airport.size match {
      case x if (x >= 3) => airport.size - 2
      case _ => 0
    })

    //multiplier for HQ
    AirlineCache.getAirline(airline.id, fullLoad = true).foreach { airline =>
      airline.getHeadQuarter().foreach { hq =>
        if (airport.id == hq.airport.id) {
          val boostMultiplier = airport.size match {
            case x if x <= 7 => 1 + (8 - airport.size) * 0.5
            case _ => 0
          }

          if (boostMultiplier > 1) {
            boost *= boostMultiplier
            bonuses.append(ReputationBonus(s"Base reputation multiplied by $boostMultiplier as HQ in smaller airport"))
          }
        }
      }
    }
    
    import AirportFeatureType._
    airport.getFeatures().foreach { feature =>
      val featureBoost = feature.featureType match {
        case GATEWAY_AIRPORT => 3
        case INTERNATIONAL_HUB => feature.strengthFactor * 25
        case FINANCIAL_HUB => feature.strengthFactor * 15
        case VACATION_HUB => feature.strengthFactor * 10
        case AVIATION_HUB => feature.strengthFactor * 25
        case _ => 0
      }

      boost += featureBoost
    }


    (boost * reputationBoostBrackets(ranking), bonuses.toList)
  }

//  def updateAirportChampionInfo(loyalists: List[Loyalist]) = {
//    val result = computeAirportChampionInfo(loyalists)
//    AirportSource.updateChampionInfo(result)
//    result
//  }


  def computeAirportChampionInfo(loyalists: List[Loyalist]) = {
    val result = ListBuffer[AirportChampionInfo]()

//    val loyalists = airportIdFilter match {
//      case Some(airportId) => LoyalistSource.loadLoyalistsByAirportId(airportId)
//      case None => LoyalistSource.loadLoyalistsByCriteria(List.empty)
//    }

    loyalists.groupBy(_.airport.id).foreach {
      case (airportId, loyalists) =>
        val airport = AirportCache.getAirport(airportId, true).get //need to load detailed airport here to get features
        val championCount = getAirportChampionCount(airport)
        val loyalistToPopRatio = Math.min(1, loyalists.map(_.amount).sum.toDouble / airport.population) //just in case the loyalist is out of wack, ie > pop
        val topAirlineWithSortedIndex : List[(Loyalist, Int)] = loyalists.sortBy(_.amount)(Ordering.Int.reverse).take(championCount).zipWithIndex

        val championInfoForThisAirport = topAirlineWithSortedIndex.map {
          case(loyalist, index) => {
              val ranking = index + 1
              val fullBoostInfo = computeFullReputationBoost(airport, loyalist.airline, ranking)
              val reputationBoost = fullBoostInfo._1 * loyalistToPopRatio
              Some(AirportChampionInfo(loyalist, ranking, reputationBoost, fullBoostInfo._2))
          }
        }
        result ++= championInfoForThisAirport.flatten
    }
    result.toList
  }

  def loadAirportChampionInfo() = {
    AirportSource.loadChampionInfoByCriteria(List.empty)
  }

  def loadAirportChampionInfoByAirline(airlineId : Int) = {
    AirportSource.loadChampionInfoByCriteria(List(("airline", airlineId)))
  }

  def loadAirportChampionInfoByAirport(airportId : Int) = {
    AirportSource.loadChampionInfoByCriteria(List(("airport", airportId)))
  }

  def getAirportChampionCount(airport: Airport) = {
    airport.size + 2
  }
}