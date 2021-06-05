package controllers

import com.patson.data.{AirlineSource, CycleSource, LinkSource, LoungeHistorySource}
import com.patson.model._
 

object RankingUtil {
  var loadedCycle = 0
  var cachedRankings : Map[RankingType.Value, List[Ranking]] = Map.empty
  
//  val generatedLogo = ImageUtil.generateLogo("logo/star.bmp", Color.CYAN.getRGB, Color.RED.getRGB)
//  LogoUtil.saveLogo(1, generatedLogo)
  
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
    val linkConsumptions = LinkSource.loadLinkConsumptions()
    val linkConsumptionsByAirline = linkConsumptions.groupBy(_.link.airline.id)
    val links = LinkSource.loadAllFlightLinks().groupBy(_.airline.id)
    val airlinesById = AirlineSource.loadAllAirlines(fullLoad = true).map( airline => (airline.id, airline)).toMap
    
    val updatedRankings = scala.collection.mutable.Map[RankingType.Value, List[Ranking]]()
    updatedRankings.put(RankingType.PASSENGER, getPassengerRanking(linkConsumptionsByAirline, airlinesById))
    updatedRankings.put(RankingType.PASSENGER_MILE, getPassengerMileRanking(linkConsumptionsByAirline, airlinesById))
    updatedRankings.put(RankingType.REPUTATION, getReputationRanking(airlinesById))
    updatedRankings.put(RankingType.SERVICE_QUALITY, getServiceQualityRanking(airlinesById))
    updatedRankings.put(RankingType.LINK_COUNT, getLinkCountRanking(links, airlinesById))
    updatedRankings.put(RankingType.LINK_PROFIT, getLinkProfitRanking(linkConsumptions, airlinesById))
    updatedRankings.put(RankingType.LOUNGE, getLoungeRanking(LoungeHistorySource.loadAll, airlinesById))
    updatedRankings.put(RankingType.AIRPORT, getAirportRanking(linkConsumptions))
//    val linkConsumptionsByAirlineAndZone = getPassengersByZone(linkConsumptionsByAirline)
//    updatedRankings.put(RankingType.PASSENGER_AS, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_AS, "AS"))
//    updatedRankings.put(RankingType.PASSENGER_AF, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_AF, "AF"))
//    updatedRankings.put(RankingType.PASSENGER_OC, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_OC, "OC"))
//    updatedRankings.put(RankingType.PASSENGER_EU, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_EU, "EU"))
//    updatedRankings.put(RankingType.PASSENGER_NA, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_NA, "NA"))
//    updatedRankings.put(RankingType.PASSENGER_SA, getPassengerByZoneRanking(linkConsumptionsByAirlineAndZone, airlinesById, RankingType.PASSENGER_SA, "SA"))
    
    
    
    updateMovements(cachedRankings, updatedRankings.toMap)
    
    cachedRankings = updatedRankings.toMap
  }
  
  private[this] def getPassengerRanking(linkConsumptions : Map[Int, List[LinkConsumptionDetails]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val passengersByAirline : Map[Int, Long] = linkConsumptions.view.mapValues(_.map(_.link.soldSeats.total.toLong).sum).toMap
    val sortedPassengersByAirline = passengersByAirline.toList.sortBy(_._2)(Ordering[Long].reverse)  //sort by total passengers of each airline
    
    sortedPassengersByAirline.zipWithIndex.map {
      case((airlineId, passengers), index) => Ranking(RankingType.PASSENGER,
                                                      key = airlineId,
                                                      entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = passengers)
    }.toList.sortBy(_.ranking)
  }
  
  
  private[this] def getPassengerByZoneRanking(passengersByAirlineAndZone : Map[Int, Map[String, Long]], airlinesById : Map[Int, Airline], rankingType : RankingType.Value, zone : String) : List[Ranking] = {
    val passengersByAirline : Map[Int, Long] = passengersByAirlineAndZone.view.mapValues { passengersByZone =>
      passengersByZone.getOrElse(zone, 0L)
    }.toMap

    val sortedPassengersByAirline = passengersByAirline.toList.sortBy(_._2)(Ordering[Long].reverse)  //sort by total passengers of each airline
    
    sortedPassengersByAirline.zipWithIndex.map {
      case((airlineId, passengers), index) => Ranking(rankingType,
                                                      key = airlineId,
                                                      entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = passengers)
    }.toList.sortBy(_.ranking)
  }
  
  private[this] def getPassengersByZone(linkConsumptions : Map[Int, List[LinkConsumptionDetails]]) : Map[Int, Map[String, Long]] = {
    val passengersByAirlineAndZone : Map[Int, Map[String, Long]] = linkConsumptions.view.mapValues(_.groupBy(_.link.from.zone).view.mapValues { linkConsumptionsByZone =>
      linkConsumptionsByZone.map(_.link.soldSeats.total.toLong).sum
    }.toMap).toMap
    
    passengersByAirlineAndZone    
  }
  
  
  
  private[this] def getPassengerMileRanking(linkConsumptions : Map[Int, List[LinkConsumptionDetails]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val passengerMileByAirline : Map[Int, Long] = linkConsumptions.view.mapValues(_.map { linkConsumption =>
        linkConsumption.link.soldSeats.total.toLong * linkConsumption.link.distance
      }.sum).toMap
    
    val sortedPassengerMileByAirline= passengerMileByAirline.toList.sortBy(_._2)(Ordering[Long].reverse)  //sort by total passengers of each airline
    
    sortedPassengerMileByAirline.zipWithIndex.map {
      case((airlineId, passengerMile), index) => Ranking(RankingType.PASSENGER_MILE,
                                                      key = airlineId,
                                                      entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = passengerMile)
    }.toList.sortBy(_.ranking)
  }
  
  private[this] def getLinkProfitRanking(linkConsumptions : List[LinkConsumptionDetails], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val mostProfitableLinks : List[LinkConsumptionDetails] = linkConsumptions.sortBy(_.profit)(Ordering[Int].reverse)
    
    mostProfitableLinks.zipWithIndex.map {
      case(linkConsumption, index) => {
        val airlineId = linkConsumption.link.airline.id
        val ranking = Ranking(RankingType.PASSENGER_MILE,
                key = linkConsumption.link.id,
                entry = linkConsumption.link.asInstanceOf[Link].copy(airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId))),
                ranking = index + 1,
                rankedValue = linkConsumption.profit)
        ranking
      }
                                           
    }.toList.sortBy(_.ranking).take(500)
  }
  
  private[this] def getLoungeRanking(loungeConsumptions : List[LoungeConsumptionDetails], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val mostVisitedLounges : List[LoungeConsumptionDetails] = loungeConsumptions.sortBy(entry => entry.selfVisitors + entry.allianceVisitors)(Ordering[Int].reverse)
    
    mostVisitedLounges.zipWithIndex.map {
      case(details, index) => {
        val lounge = details.lounge
        val ranking = Ranking(RankingType.LOUNGE,
                key = s"$lounge.airline.id|$lounge.airport.id",
                entry = lounge,
                ranking = index + 1,
                rankedValue = details.selfVisitors + details.allianceVisitors)
        ranking
      }
                                           
    }.toList.sortBy(_.ranking)
  }
  
  
  
  private[this] def getReputationRanking(airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val airlinesBySortedReputation = airlinesById.values.toList.sortBy(_.getReputation())(Ordering[Double].reverse)
    
    airlinesBySortedReputation.zipWithIndex.map {
      case(airline, index) =>  Ranking(RankingType.REPUTATION,
                                                      key = airline.id,
                                                      entry = airline,
                                                      ranking = index + 1,
                                                      rankedValue = airline.getReputation())
    }
  }
  private[this] def getServiceQualityRanking(airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val airlinesBySortedServiceQuality = airlinesById.values.toList.sortBy(_.getCurrentServiceQuality())(Ordering[Double].reverse)
    
    airlinesBySortedServiceQuality.zipWithIndex.map {
      case(airline, index) =>  Ranking(RankingType.SERVICE_QUALITY,
                                                      key = airline.id,
                                                      entry = airline,
                                                      ranking = index + 1,
                                                      rankedValue = airline.getCurrentServiceQuality())
    }
  }
  
  private[this] def getLinkCountRanking(linksByAirline : Map[Int, List[Link]], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val linkCountByAirline : Map[Int, Int] = linksByAirline.view.mapValues(_.length).toMap
    val sortedLinkCountByAirline = linkCountByAirline.toList.sortBy(_._2)(Ordering[Int].reverse)  //sort by total passengers of each airline
    
    sortedLinkCountByAirline.zipWithIndex.map {
      case((airlineId, linkCount), index) => Ranking(RankingType.LINK_COUNT,
                                                      key = airlineId,
                                                      entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
                                                      ranking = index + 1,
                                                      rankedValue = linkCount)
    }.toList
  }
  
  private[this] def getAirportRanking(linkConsumptions : List[LinkConsumptionDetails]) : List[Ranking] = {
    val passengersByAirport = scala.collection.mutable.Map[Airport, Long]()
    
    linkConsumptions.foreach {  consumption =>
      val fromAirport = consumption.link.from
      val toAirport = consumption.link.to
      val passengers = consumption.link.getTotalSoldSeats.toLong
      passengersByAirport.put(fromAirport, passengersByAirport.getOrElse(fromAirport, 0L) + passengers)
      passengersByAirport.put(toAirport, passengersByAirport.getOrElse(toAirport, 0L) + passengers)
    }
    
    passengersByAirport.toList.sortBy(_._2)(Ordering[Long].reverse).zipWithIndex.map {
      case((airport, passengers), index) => Ranking(RankingType.AIRPORT,
                                                      key = airport.id,
                                                      entry = airport,
                                                      ranking = index + 1,
                                                      rankedValue = passengers)
    }.toList.take(40) //40 max for now
  }
  
  private[this] def updateMovements(previousRankings : Map[RankingType.Value, List[Ranking]], newRankings : Map[RankingType.Value, List[Ranking]]) = {
    val previousRankingsByKey : Map[RankingType.Value, Map[Any, Int]] = previousRankings.view.mapValues { rankings =>
       rankings.map(ranking => (ranking.key, ranking.ranking)).toMap
    }.toMap
    
    
    newRankings.foreach {
      case(rankingType, rankings) =>  {
        val previousRankingOfThisType = previousRankingsByKey.get(rankingType)
        if (previousRankingOfThisType.isDefined) {
           rankings.foreach { newRankingEntry => 
             val previousRanking = previousRankingOfThisType.get.get(newRankingEntry.key)
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
  val PASSENGER, PASSENGER_MILE, REPUTATION, SERVICE_QUALITY, LINK_COUNT, LINK_PROFIT, LOUNGE, AIRPORT, PASSENGER_AS, PASSENGER_AF, PASSENGER_OC, PASSENGER_EU, PASSENGER_NA, PASSENGER_SA = Value
}


case class Ranking(rankingType : RankingType.Value, key : Any, entry : Any, ranking : Int, rankedValue : Number, var movement : Int = 0)


//class AirlineRanking(rankingType : RankingType.Value, airline : Airline, ranking : Int, rankedValue : Number, var movement : Int = 0) extends Ranking(rankingType, airline, ranking, rankedValue, movement) {
//  
//}


