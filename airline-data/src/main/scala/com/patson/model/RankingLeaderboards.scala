package com.patson.model

import com.patson.data.{AirlineSource, CycleSource, LinkSource, LoungeHistorySource}


object RankingLeaderboards {
  var loadedCycle = 0
  var cachedRankings : Map[RankingType.Value, List[Ranking]] = Map.empty

  //  val generatedLogo = ImageUtil.generateLogo("logo/star.bmp", Color.CYAN.getRGB, Color.RED.getRGB)
  //  LogoUtil.saveLogo(1, generatedLogo)

  def getRankings() : Map[RankingType.Value, List[Ranking]] = {
    checkCache()
    cachedRankings
  }

  def reputationBonus(maxBonus : Int, rank : Int) = {
    val bonus = ((10 - rank) / 10.0) * maxBonus
    val computed = if( rank >= 20 ) 0 else if( bonus < 1 ) 1 else bonus
    Some(computed)
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
    val flightConsumptions = LinkSource.loadLinkConsumptions().filter(_.link.transportType == TransportType.FLIGHT)
    val flightConsumptionsByAirline = flightConsumptions.groupBy(_.link.airline.id)
    val links = LinkSource.loadAllFlightLinks().groupBy(_.airline.id)
    val airlinesById = AirlineSource.loadAllAirlines(fullLoad = true).map( airline => (airline.id, airline)).toMap

    val updatedRankings = scala.collection.mutable.Map[RankingType.Value, List[Ranking]]()
    //    updatedRankings.put(RankingType.PASSENGER, getPassengerRanking(flightConsumptionsByAirline, airlinesById))
    updatedRankings.put(RankingType.PASSENGER_MILE, getPassengerMileRanking(flightConsumptionsByAirline, airlinesById))
    updatedRankings.put(RankingType.PASSENGER_QUALITY, getPassengerQualityRanking(flightConsumptionsByAirline, airlinesById))
    //    updatedRankings.put(RankingType.REPUTATION, getReputationRanking(airlinesById))
    updatedRankings.put(RankingType.SERVICE_QUALITY, getServiceQualityRanking(airlinesById))
    updatedRankings.put(RankingType.PASSENGER_SATISFACTION, getPassengerSFRanking(flightConsumptionsByAirline,airlinesById))
    updatedRankings.put(RankingType.LINK_COUNT, getLinkCountRanking(links, airlinesById))
    updatedRankings.put(RankingType.UNIQUE_COUNTRIES, getCountriesRanking(links, airlinesById))
    updatedRankings.put(RankingType.UNIQUE_IATA, getIataRanking(links, airlinesById))
    updatedRankings.put(RankingType.LINK_PROFIT, getLinkProfitRanking(flightConsumptions, airlinesById))
    updatedRankings.put(RankingType.LINK_FREQUENCY, getLinkFrequent(flightConsumptions, airlinesById))
    updatedRankings.put(RankingType.LINK_DISTANCE, getLinkLongest(flightConsumptions, airlinesById))
    updatedRankings.put(RankingType.LOUNGE, getLoungeRanking(LoungeHistorySource.loadAll, airlinesById))
    updatedRankings.put(RankingType.AIRPORT, getAirportRanking(flightConsumptions))
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
      case((airlineId, passengerMile), index) => Ranking(
        RankingType.PASSENGER_MILE,
        key = airlineId,
        entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
        ranking = index + 1,
        rankedValue = (passengerMile * 0.6213711922).toInt,
        reputationPrize = reputationBonus(20, index)
      )
    }.toList.sortBy(_.ranking).take(200)
  }
  private[this] def getPassengerQualityRanking(linkConsumptions: Map[Int, List[LinkConsumptionDetails]], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val passengerQualityByAirline: Map[Int, Double] = linkConsumptions.view.mapValues { linkConsumption =>
      val paxMiles = linkConsumption.map { linkConsumption =>
        linkConsumption.link.soldSeats.total * linkConsumption.link.distance
      }.sum
      val qualityPaxMiles = linkConsumption.map { linkConsumption =>
        linkConsumption.link.soldSeats.total.toLong * linkConsumption.link.distance * linkConsumption.link.computedQuality
      }.sum
      qualityPaxMiles / paxMiles.toDouble
    }.toMap

    val sortedPassengerByAirline = passengerQualityByAirline.toList.sortBy(_._2)(Ordering[Double].reverse) //sort by total passengers of each airline

    sortedPassengerByAirline.zipWithIndex.map {
      case ((airlineId, quality), index) => Ranking(
        RankingType.PASSENGER_QUALITY,
        key = airlineId,
        entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
        ranking = index + 1,
        rankedValue = BigDecimal(quality).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        reputationPrize = reputationBonus(20, index)
      )
    }.toList.sortBy(_.ranking).take(200)
  }

  private[this] def getPassengerSFRanking(linkConsumptions: Map[Int, List[LinkConsumptionDetails]], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val passengerQualityByAirline: Map[Int, Double] = linkConsumptions.view.mapValues { linkConsumption =>
      val paxMiles = linkConsumption.map { linkConsumption =>
        linkConsumption.link.soldSeats.total.toLong * linkConsumption.link.distance
      }.sum
      val qualityPaxMiles = linkConsumption.map { linkConsumption =>
        linkConsumption.link.soldSeats.total.toLong * linkConsumption.link.distance * linkConsumption.satisfaction
      }.sum
      qualityPaxMiles / paxMiles
    }.toMap

    val sortedPassengerByAirline = passengerQualityByAirline.toList.sortBy(_._2)(Ordering[Double].reverse) //sort by total passengers of each airline

    sortedPassengerByAirline.zipWithIndex.map {
      case ((airlineId, satisfaction), index) => Ranking(
        RankingType.PASSENGER_SATISFACTION,
        key = airlineId,
        entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
        ranking = index + 1,
        rankedValue = BigDecimal(satisfaction * 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        reputationPrize = reputationBonus(20, index)
      )
    }.toList.sortBy(_.ranking).take(200)
  }

  private[this] def getLinkProfitRanking(linkConsumptions : List[LinkConsumptionDetails], airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val mostProfitableLinks : List[LinkConsumptionDetails] = linkConsumptions
      .sortBy(_.profit)(Ordering[Int].reverse)
      .foldLeft(List.empty[LinkConsumptionDetails]) { (acc, linkDetail) =>
        if (acc.exists(_.link.airline.id == linkDetail.link.airline.id)) {
          acc
        } else {
          linkDetail :: acc // Add to the front of the accumulator
        }
      }
      .reverse

    mostProfitableLinks.zipWithIndex.map {
      case(linkConsumption, index) => {
        val airlineId = linkConsumption.link.airline.id
        val ranking = Ranking(
          RankingType.PASSENGER_MILE,
          key = linkConsumption.link.id,
          entry = linkConsumption.link.asInstanceOf[Link].copy(airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId))),
          ranking = index + 1,
          rankedValue = linkConsumption.profit,
          reputationPrize = reputationBonus(10, index)
        )
        ranking
      }

    }.toList.sortBy(_.ranking).take(200)
  }

  private[this] def getLinkLongest(linkConsumptions: List[LinkConsumptionDetails], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val longestLinkPerAirline = linkConsumptions
      .filter(_.link.soldSeats.total > 200)
      .sortBy(_.link.distance)(Ordering[Int].reverse)
      .foldLeft(List.empty[LinkConsumptionDetails]) { (acc, linkDetail) =>
        if (acc.exists(_.link.airline.id == linkDetail.link.airline.id)) {
          acc
        } else {
          linkDetail :: acc // Add to the front of the accumulator
        }
      }
      .reverse

    longestLinkPerAirline.zipWithIndex.map {
      case (linkConsumption, index) => {
        val airlineId = linkConsumption.link.airline.id
        val ranking = Ranking(
          RankingType.LINK_DISTANCE,
          key = linkConsumption.link.id,
          entry = linkConsumption.link.asInstanceOf[Link].copy(airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId))),
          ranking = index + 1,
          rankedValue = linkConsumption.link.distance,
          reputationPrize = reputationBonus(10, index)
        )
        ranking
      }

    }.toList.sortBy(_.ranking).take(200)
  }

  private[this] def getLinkFrequent(linkConsumptions: List[LinkConsumptionDetails], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val mostFrequentLinks: List[LinkConsumptionDetails] = linkConsumptions
      .filter(_.link.soldSeats.total > 2400)
      .sortBy(_.link.frequency)(Ordering[Int].reverse)
      .foldLeft(List.empty[LinkConsumptionDetails]) { (acc, linkDetail) =>
        if (acc.exists(_.link.airline.id == linkDetail.link.airline.id)) {
          acc
        } else {
          linkDetail :: acc // Add to the front of the accumulator
        }
      }.reverse

    var prevValue: Int = 0
    var prevRanking: Int = 0
    mostFrequentLinks.zipWithIndex.map {
      case (linkConsumption, index) => {
        prevRanking = if (prevValue == linkConsumption.link.frequency) prevRanking else index
        prevValue = linkConsumption.link.frequency
        val airlineId = linkConsumption.link.airline.id
        val ranking = Ranking(
          RankingType.LINK_FREQUENCY,
          key = linkConsumption.link.id,
          entry = linkConsumption.link.asInstanceOf[Link].copy(airline = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId))),
          ranking = prevRanking + 1,
          rankedValue = linkConsumption.link.frequency,
          reputationPrize = reputationBonus(10, prevRanking)
        )
        ranking
      }

    }.toList.sortBy(_.ranking).take(200)
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
          rankedValue = details.selfVisitors + details.allianceVisitors,
          reputationPrize = reputationBonus(10, index)
        )
        ranking
      }

    }.toList.sortBy(_.ranking)
  }
  private[this] def getServiceQualityRanking(airlinesById : Map[Int, Airline]) : List[Ranking] = {
    val airlinesBySortedServiceQuality = airlinesById.values.toList.sortBy(_.getCurrentServiceQuality())(Ordering[Double].reverse)

    airlinesBySortedServiceQuality.zipWithIndex.map {
      case(airline, index) =>  Ranking(RankingType.SERVICE_QUALITY,
        key = airline.id,
        entry = airline,
        ranking = index + 1,
        rankedValue = airline.getCurrentServiceQuality())
    }.take(20)
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
    }.toList.take(20)
  }

  //  import scala.collection.mutable.LinkedHashMap
  private[this] def getCountriesRanking(linksByAirline: Map[Int, List[Link]], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val uniqueAirportCounts : Map[Int, Int] = linksByAirline.mapValues { links =>
      links.map(_.to.countryCode).toSet.size
    }.toMap
    val sortedLinkCountByAirline = uniqueAirportCounts.toList.sortBy(_._2)(Ordering[Int].reverse)
    var prevValue : Int = 0
    var prevRanking : Int = 0
    sortedLinkCountByAirline.zipWithIndex.map {
      case ((airlineId, linkCount), index) => {
        prevRanking = if (prevValue == linkCount) prevRanking else index
        prevValue = linkCount
        Ranking(RankingType.UNIQUE_COUNTRIES,
          key = airlineId,
          entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
          ranking = prevRanking + 1,
          rankedValue = prevValue,
          reputationPrize = reputationBonus(10, prevRanking)
        )
      }
    }.toList.take(200)
  }

  private[this] def getIataRanking(linksByAirline: Map[Int, List[Link]], airlinesById: Map[Int, Airline]): List[Ranking] = {
    val uniqueAirportCounts : Map[Int, Int] = linksByAirline.mapValues { links =>
      links.map(_.to.iata).toSet.size
    }.toMap
    val sortedLinkCountByAirline = uniqueAirportCounts.toList.sortBy(_._2)(Ordering[Int].reverse)
    var prevValue: Int = 0
    var prevRanking: Int = 0
    sortedLinkCountByAirline.zipWithIndex.map {
      case ((airlineId, linkCount), index) => {
        prevRanking = if (prevValue == linkCount) prevRanking else index
        prevValue = linkCount
        Ranking(RankingType.UNIQUE_COUNTRIES,
          key = airlineId,
          entry = airlinesById.getOrElse(airlineId, Airline.fromId(airlineId)),
          ranking = prevRanking + 1,
          rankedValue = prevValue,
          reputationPrize = reputationBonus(10, prevRanking)
        )
      }
    }.toList.take(200)
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
    }.toList.take(20)
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
  val PASSENGER_MILE, PASSENGER_QUALITY, PASSENGER_SATISFACTION, UNIQUE_COUNTRIES, UNIQUE_IATA, SERVICE_QUALITY, LINK_COUNT, LINK_PROFIT, LINK_DISTANCE, LINK_FREQUENCY, LOUNGE, AIRPORT, AIRPORT_TRANSFERS = Value
}


case class Ranking(rankingType : RankingType.Value, key : Any, entry : Any, ranking : Int, rankedValue : Number, var movement : Int = 0, reputationPrize : Option[Double] = None )
