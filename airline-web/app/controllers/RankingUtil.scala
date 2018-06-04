package controllers

import com.patson.data.CycleSource
import com.patson.data.ConsumptionHistorySource
import com.patson.model.PassengerType
import com.patson.model.Route
import com.patson.model.Link
import com.patson.model.LinkHistory
import com.patson.model.LinkConsideration
import com.patson.model.RelatedLink
import com.patson.data.LinkSource
import com.patson.model.Airline
import com.patson.model.LinkConsumptionDetails
import com.patson.data.AirlineSource
 

object RankingUtil {
  var loadedCycle = 0
  var cachedRankings : Map[RankingType.Value, List[Ranking]] = Map.empty
  
  def getRankings() : Map[RankingType.Value, List[Ranking]] = {
    checkCache()
    cachedRankings
  }
  
  private def checkCache() = {
    val currentCycle = CycleSource.loadCycle()
    synchronized {
      if (currentCycle != loadedCycle) {
        println("Updating ranking cache on cycle " + currentCycle)
        updateRankings() //get ranking on previous cycle
        println("Updated ranking cache on cycle " + currentCycle)
      }
      loadedCycle = currentCycle
    }
  }
  
  private[this] def updateRankings() = {
    val linkConsumptions = LinkSource.loadLinkConsumptions().groupBy(_.airlineId)
    val links = LinkSource.loadAllLinks().groupBy(_.airline.id)
    val airlinesById = AirlineSource.loadAllAirlines(fullLoad = true).map( airline => (airline.id, airline)).toMap
    
    val updatedRankings = scala.collection.mutable.Map[RankingType.Value, List[Ranking]]()
    updatedRankings.put(RankingType.PASSENGER, getPassengerRanking(linkConsumptions, airlinesById))
    updatedRankings.put(RankingType.PASSENGER_MILE, getPassengerMileRanking(linkConsumptions, airlinesById))
    updatedRankings.put(RankingType.REPUTATION, getReputationRanking(airlinesById))
    updatedRankings.put(RankingType.SERVICE_QUALITY, getServiceQualityRanking(airlinesById))
    updatedRankings.put(RankingType.LINK_COUNT, getLinkCountRanking(links, airlinesById))
    
    updateMovements(cachedRankings, updatedRankings.toMap)
    
    cachedRankings = updatedRankings.toMap
  }
  
  private[this] def getPassengerRanking(linkConsumptions : Map[Int, List[LinkConsumptionDetails]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val passengersByAirline : Map[Int, Long] = linkConsumptions.mapValues(_.map(_.soldSeats.total).sum)
    val sortedPassengersByAirline = passengersByAirline.toList.sortBy(_._2)(Ordering[Long].reverse)  //sort by total passengers of each airline
    
    sortedPassengersByAirline.zipWithIndex.map {
      case((airlineId, passengers), index) => Ranking(RankingType.PASSENGER,
                                                      airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = passengers)
    }.toList.sortBy(_.ranking)
  }
  private[this] def getPassengerMileRanking(linkConsumptions : Map[Int, List[LinkConsumptionDetails]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val passengerMileByAirline : Map[Int, Long] = linkConsumptions.mapValues(_.map { linkConsumption => 
        linkConsumption.soldSeats.total * linkConsumption.distance
      }.sum)
    
    val sortedPassengerMileByAirline= passengerMileByAirline.toList.sortBy(_._2)(Ordering[Long].reverse)  //sort by total passengers of each airline
    
    sortedPassengerMileByAirline.zipWithIndex.map {
      case((airlineId, passengerMile), index) => Ranking(RankingType.PASSENGER_MILE,
                                                      airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = passengerMile)
    }.toList.sortBy(_.ranking)
  }
  private[this] def getReputationRanking(airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val airlinesBySortedReputation = airlinesById.values.toList.sortBy(_.getReputation())(Ordering[Double].reverse)
    
    airlinesBySortedReputation.zipWithIndex.map {
      case(airline, index) =>  Ranking(RankingType.REPUTATION,
                                                      airline = airline,
                                                      ranking = index + 1,
                                                      rankedValue = airline.getReputation())
    }
  }
  private[this] def getServiceQualityRanking(airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val airlinesBySortedServiceQuality = airlinesById.values.toList.sortBy(_.getServiceQuality())(Ordering[Double].reverse)
    
    airlinesBySortedServiceQuality.zipWithIndex.map {
      case(airline, index) =>  Ranking(RankingType.SERVICE_QUALITY,
                                                      airline = airline,
                                                      ranking = index + 1,
                                                      rankedValue = airline.getServiceQuality())
    }
  }
  
  private[this] def getLinkCountRanking(linksByAirline : Map[Int, List[Link]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val linkCountByAirline : Map[Int, Int] = linksByAirline.mapValues(_.length)
    val sortedLinkCountByAirline = linkCountByAirline.toList.sortBy(_._2)(Ordering[Int].reverse)  //sort by total passengers of each airline
    
    sortedLinkCountByAirline.zipWithIndex.map {
      case((airlineId, linkCount), index) => Ranking(RankingType.LINK_COUNT,
                                                      airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = linkCount)
    }.toList
  }
  
  private[this] def updateMovements(previousRankings : Map[RankingType.Value, List[Ranking]], newRankings : Map[RankingType.Value, List[Ranking]]) = {
    val previousRankingsByAirlineId : Map[RankingType.Value, Map[Int, Int]] = previousRankings.mapValues { rankings =>
       rankings.map(ranking => (ranking.airline.id, ranking.ranking)).toMap
    }
    
    
    newRankings.foreach {
      case(rankingType, rankings) =>  {
        val previousRankingOfThisType = previousRankingsByAirlineId.get(rankingType)
        if (previousRankingOfThisType.isDefined) {
           rankings.foreach { newRankingEntry => 
             val previousRanking = previousRankingOfThisType.get.get(newRankingEntry.airline.id)
             if (previousRanking.isDefined) {
               newRankingEntry.movement = newRankingEntry.ranking - previousRanking.get 
             }
           }
        }
      }
    }
  }
}

object RankingType extends Enumeration {
  type RankingType = Value
  val PASSENGER, PASSENGER_MILE, REPUTATION, SERVICE_QUALITY, LINK_COUNT = Value
}

case class Ranking(rankingType : RankingType.Value, airline : Airline, ranking : Int, rankedValue : Number, var movement : Int = 0) 
