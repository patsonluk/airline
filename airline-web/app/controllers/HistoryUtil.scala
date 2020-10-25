package controllers

import java.util
import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.patson.data.{ConsumptionHistorySource, CycleSource}
import com.patson.model.{PassengerType, _}
import models.{LinkHistory, RelatedLink}

import scala.collection.mutable.ListBuffer

object HistoryUtil {
  var loadedCycle = 0
  //val simpleCache: LoadingCache[Int, Option[Airport]] = CacheBuilder.newBuilder.maximumSize(2000).expireAfterAccess(10, TimeUnit.MINUTES).build(new SimpleLoader())
  var consumptionCache : java.util.Map[Int, LoadingCache[Int, Map[Route, (PassengerType.Value, Int)]]] = new java.util.HashMap[Int, LoadingCache[Int, Map[Route, (PassengerType.Value, Int)]]]() //key is cycle

  /**
    * Group the related links base on traverse ordering
    *
    * For example the fromAirport is X, toAirport is Y, related links are:
    * [ U1 -> V, U2 -> V, V -> W, W -> X, X -> Y, Y -> Z1, Y -> Z2]
    *
    * Then it will return
    *
    * [[U1 -> V, U2 -> V], [V -> W] , [W -> X] , [X -> Y], [Y -> Z1, Y -> Z2]]
    *
    *
    * @param fromAirport
    * @param toAirport
    * @param relatedLinksOriginal
    * @return
    */
  def groupLinksByStep(fromAirport : Airport, toAirport : Airport, selectedAirline : Airline, relatedLinksOriginal: List[RelatedLink]) : List[List[RelatedLink]] = {
    val groupedLinks = ListBuffer[List[RelatedLink]]()

    var relatedLinks = ListBuffer[RelatedLink]()
    relatedLinks.appendAll(relatedLinksOriginal)

    //first find the links matching from/to
    relatedLinks.partition(link => link.fromAirport.id == fromAirport.id && link.toAirport.id == toAirport.id && link.airline.id == selectedAirline.id) match {
      case(originalLinks, remainingLinks) =>
        groupedLinks.append(originalLinks.toList)
        relatedLinks = remainingLinks
    }

    //traverse backwards
    var shouldContinue = true
    var backwardsAirportIds = List(fromAirport.id)
    while (shouldContinue) {
      relatedLinks.partition(link => backwardsAirportIds.contains(link.toAirport.id)) match {
        case (matchingLinks, remainingLinks) =>
          relatedLinks = remainingLinks
          if (matchingLinks.isEmpty) {
            shouldContinue = false
          } else {
            groupedLinks.prepend(matchingLinks.toList)
            backwardsAirportIds = matchingLinks.map(_.fromAirport.id).toList
          }
      }
    }

    //traverse forward
    shouldContinue = true
    var forwardsAirportIds = List(toAirport.id)
    while (shouldContinue) {
      relatedLinks.partition(link => forwardsAirportIds.contains(link.fromAirport.id)) match {
        case (matchingLinks, remainingLinks) =>
          relatedLinks = remainingLinks
          if (matchingLinks.isEmpty) {
            shouldContinue = false
          } else {
            groupedLinks.append(matchingLinks.toList)
            forwardsAirportIds = matchingLinks.map(_.toAirport.id).toList
          }
      }
    }
    groupedLinks.toList
  }

  def loadConsumptionByLink(link : Link, cycleDelta : Int = 0, selfOnly : Boolean = false) : LinkHistory = {
    val relatedConsumptions = loadRelatedRoutesFromCache(link.id, cycleDelta)
    val airlineId = link.airline.id

    println("Finished loading related consumption for " + link)

    val relatedForwardLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(route, _) => route.links.find { linkConsideration => !linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }.toList, airlineId, selfOnly
    )
    val groupedForwardLinks = groupLinksByStep(link.from, link.to, link.airline, relatedForwardLinks)
    
    val relatedReverseLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(route, _) => route.links.find { linkConsideration => linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }.toList, airlineId, selfOnly
    )

    val groupedReverseLinks = groupLinksByStep(link.to, link.from, link.airline, relatedReverseLinks)
       
    LinkHistory(0, groupedForwardLinks, groupedReverseLinks)
  }
  
  def loadConsumptionByAirport(airportId : Int) : Map[Airport, Int] = {
    val linksWithPassengers = ConsumptionHistorySource.loadConsumptionsByAirport(airportId)
    //find all the "other" airport and sum up passenger count
    val passengersByOtherAirport = scala.collection.mutable.Map[Airport, Int]()
    linksWithPassengers.foreach {
      case (link, passengers) => {
        val otherAirport = if (link.from.id == airportId) {
          link.to
        } else {
          link.from
        }
        
        val sum = passengersByOtherAirport.getOrElse(otherAirport, 0)
        passengersByOtherAirport.put(otherAirport, sum + passengers)
      }
    }
    passengersByOtherAirport.toMap
  }
  
  private def computeRelatedLinks(relatedConsumption : List[(Route, (PassengerType.Value, Int))], airlineId : Int, selfOnly : Boolean) : List[RelatedLink] = {
    val relatedLinkConsumptions : List[(PassengerType.Value, Int, LinkConsideration)] = relatedConsumption.flatMap {
      case(route, (passengerType, passengerCount)) => route.links.map { (passengerType, passengerCount, _) }.filter {
        case(_, _, link) => !selfOnly || link.link.airline.id == airlineId  
      }
    } //flat map by expanding the route to the links of the route
    
       
    //now group the link by the passenger type and the link itself
    val groupedLinkConsumptions = relatedLinkConsumptions.groupBy { case(passengerType, _, linkConsideration) => (linkConsideration.link, linkConsideration.inverted, passengerType) }
    
    //fold the value of the grouped map, we only care about passenger count now
    val computedConsumedLinks = groupedLinkConsumptions.view.mapValues{
      _.foldLeft(0)( (totalPassengerCount, entry) => totalPassengerCount + entry._2)
    }.toMap
    
    //now it should have a nice map of 
    //key: Link, inverted, passengerType
    //value: number of passengers
    
    computedConsumedLinks.map {
      case (key, value) => {
        val link = key._1
        val inverted = key._2
        val passengerType = key._3
        val passengerCount = value
        if (!inverted) {
          new RelatedLink(link.id, link.from, link.to, link.airline, passengerCount)  
        } else {
          new RelatedLink(link.id, link.to, link.from, link.airline, passengerCount)
        }
      }
    }.toList
  }

  private def loadRelatedRoutesFromCache(linkId : Int, cycleDelta : Int) : Map[Route, (PassengerType.Value, Int)] = {
    val currentCycle = CycleSource.loadCycle()
    val targetCycle = currentCycle + cycleDelta
    if (targetCycle > currentCycle || targetCycle < currentCycle - ConsumptionHistorySource.MAX_CONSUMPTION_HISTORY_WEEK) {
      return Map.empty
    }
    var consumptionCacheOfCycle: LoadingCache[Int, Map[Route, (PassengerType.Value, Int)]] = null

    synchronized {
      if (currentCycle != loadedCycle) {
        purgeExpiredCache(currentCycle - ConsumptionHistorySource.MAX_CONSUMPTION_HISTORY_WEEK)
        loadedCycle = currentCycle
      }

      consumptionCacheOfCycle = consumptionCache.get(targetCycle)
      if (consumptionCacheOfCycle == null) {
        val cache: LoadingCache[Int, Map[Route, (PassengerType.Value, Int)]] = CacheBuilder.newBuilder.maximumSize(500).expireAfterAccess(10, TimeUnit.MINUTES).build(new SimpleLoader(targetCycle))
        consumptionCache.put(targetCycle, cache)
        consumptionCacheOfCycle = cache
      }
    }

    consumptionCacheOfCycle.get(linkId)
  }

  class SimpleLoader(cycle : Int) extends CacheLoader[Int, Map[Route, (PassengerType.Value, Int)]] {
    override def load(linkId: Int) = {
      println(s"Updating link history cache on cycle $cycle for link " + linkId )
      ConsumptionHistorySource.loadRelatedConsumptionByLinkId(linkId, cycle)
    }
  }

  private[this] val purgeExpiredCache = (cutoff : Int) => {
    val cycleIterator = consumptionCache.keySet().iterator()
    val removingCycles = new util.HashSet[Int]()
    while (cycleIterator.hasNext()) {
      val cycle = cycleIterator.next()
      if (cycle < cutoff) {
        removingCycles.add(cycle)
        consumptionCache.get(cycle).invalidateAll()
      }
    }
    consumptionCache.keySet().removeAll(removingCycles)
  }
}