package com.patson.util

import com.patson.model.Country
import com.patson.model.Computation
import com.patson.model.CountryMarketShare
import com.patson.data.CountrySource
import com.patson.model.Airline
import scala.collection.mutable.ListBuffer
import com.patson.data.AirlineSource

case class ChampionInfo(airline : Airline, country : Country, passengerCount : Long, ranking : Int, reputationBoost : Double)

object ChampionUtil {
  def getAllChampionInfo() : List[ChampionInfo] =  { 
     getChampionInfoByFilter(_ => true, List.empty)
  }
  
  def getChampionInfoByAirlineId(airlineId : Int) = {
    getChampionInfoByFilter(checkAirlineId => airlineId == checkAirlineId, List.empty)
  }
  
  def getChampionInfoByCountryCode(countryCode : String) = {
    getChampionInfoByFilter(_ => true, List(("country", countryCode)))
  }
  
  def getChampionInfoByFilter(airlineIdFilter : Int => Boolean, marketShareCriteria : List[(String, Any)]) = {
    val result = ListBuffer[ChampionInfo]() 
    
    val allMarketShares = CountrySource.loadMarketSharesByCriteria(marketShareCriteria)
    
    val airlineIds = allMarketShares.flatMap(_.airlineShares.keys).toList.filter(airlineIdFilter)
    
    val airlines = AirlineSource.loadAirlinesByIds(airlineIds, false).map(airline => (airline.id, airline)).toMap
    
    allMarketShares.map {
      case CountryMarketShare(countryCode, airlineShares) => {
        val country = CountrySource.loadCountryByCode(countryCode).get
        val topAirlineSharesWithSortedIndex : List[((Int, Long), Int)] = airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(country.championBonusRankingCount).zipWithIndex
        
        val championInfoForThisCountry = topAirlineSharesWithSortedIndex.map {
          case((airlineId, passengerCount), index) => {
            if (airlineIdFilter(airlineId)) {
              val ranking = index + 1
              Some(ChampionInfo(airlines.getOrElse(airlineId, Airline.fromId(airlineId)), country, passengerCount, ranking, Computation.computeReputationBoost(country, ranking)))
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
}