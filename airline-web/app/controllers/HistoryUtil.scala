package controllers

import com.patson.data.CycleSource
import com.patson.data.ConsumptionHistorySource
import com.patson.model.PassengerType
import com.patson.model.Route
import com.patson.model.Link
import com.patson.model.LinkHistory
import com.patson.model.LinkConsideration
import com.patson.model.RelatedLink
import com.patson.model.Airport

import scala.collection.mutable

object HistoryUtil {
  var loadedCycle = 0
  var consumptionCache : java.util.Map[Int, Map[Route, (PassengerType.Value, Int)]] = new java.util.concurrent.ConcurrentHashMap[Int, Map[Route, (PassengerType.Value, Int)]]() //key is Link
  
  def loadConsumptionByLink(link : Link, selfOnly : Boolean = false) : LinkHistory = {
    val relatedConsumptions = loadRelatedRoutesFromCache(link.id)
    val airlineId = link.airline.id

    println("Finished loading related consumption for " + link)

    val relatedFowardLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(route, _) => route.links.find { linkConsideration => !linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }.toList, airlineId, selfOnly
    ) 
    
    val relatedReverseLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(route, _) => route.links.find { linkConsideration => linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }.toList, airlineId, selfOnly
    )
       
    LinkHistory(0, relatedFowardLinks.toSet, relatedReverseLinks.toSet)
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
  
  
  private def loadRelatedRoutesFromCache(linkId : Int) : Map[Route, (PassengerType.Value, Int)] = {
    val currentCycle = CycleSource.loadCycle()
    synchronized {
      if (currentCycle != loadedCycle) {
        consumptionCache.clear();
      }
    }

    if (consumptionCache.containsKey(linkId)) {
      consumptionCache.get(linkId)
    } else {
      println("Updating link history cache on cycle " + currentCycle + " for link " + linkId )
      val result = ConsumptionHistorySource.loadRelatedConsumptionByLinkId(linkId)
      consumptionCache.put(linkId, result)
      result
    }
  }
}