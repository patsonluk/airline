package com.patson.util

import com.patson.model._
import com.patson.data.{AirlineSource, AirportSource, CountrySource, LoyalistSource}

import scala.collection.mutable.ListBuffer

case class CountryChampionInfo(airline : Airline, country : Country, passengerCount : Long, ranking : Int)
case class AirportChampionInfo(loyalist : Loyalist, ranking : Int, reputationBoost : Double)

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

  lazy val modelAirportPower : Long = AirportSource.loadAllAirports().map(_.power).sorted.last
  val BASE_BOOST = 0.5
  val MAX_ECONOMIC_BOOST = 20.0
  val reputationBoostTop10 : Map[Int, Double] = Map(
    1 -> 1,
    2 -> 0.5,
    3 -> 0.3,
    4 -> 0.2,
    5 -> 0.1,
    6 -> 0.08,
    7 -> 0.06,
    8 -> 0.04,
    9 -> 0.03,
    10 -> 0.02
  )

  /**
    * Reputation boost if airport is at full loyalist ie loyalist = population
    * @param airport
    * @param ranking
    * @return
    */
  def computeFullReputationBoost(airport : Airport, ranking : Int) : Double = {
    val ratioToModelAirportPower = airport.power.toDouble / modelAirportPower
    var boost = BASE_BOOST
    //val economicPowerRating = Math.max(0, math.log10(ratioToModelAirportPower * 100) / 2) //0 to 1
    val economicPowerRating = Math.max(0, math.log(ratioToModelAirportPower * 16) / math.log(2) / 4) //0 to 1
    boost += MAX_ECONOMIC_BOOST * economicPowerRating

    import AirportFeatureType._
    airport.getFeatures().foreach { feature =>
      val featureBoost = feature.featureType match {
        case GATEWAY_AIRPORT => 3
        case INTERNATIONAL_HUB => feature.strengthFactor * 25
        case FINANCIAL_HUB => feature.strengthFactor * 15
        case VACATION_HUB => feature.strengthFactor * 10
        case _ => 0
      }

      boost += featureBoost
    }

    boost += (airport.size match {
      case x if (x >= 3) => airport.size - 2
      case _ => 0
    })

    boost * reputationBoostTop10(ranking)
  }

  def updateAirportChampionInfo(loyalists: List[Loyalist]) = {
    val result = computeAirportChampionInfo(loyalists)
    AirportSource.updateChampionInfo(result)
    result
  }


  private[this] def computeAirportChampionInfo(loyalists: List[Loyalist]) = {
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
              val reputationBoost = computeFullReputationBoost(airport, ranking) * loyalistToPopRatio
              Some(AirportChampionInfo(loyalist, ranking, reputationBoost))
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
    airport.size
  }
}