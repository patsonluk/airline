package controllers

import com.patson.data.CycleSource
import com.patson.data.ConsumptionHistorySource
import com.patson.model.PassengerType
import com.patson.model.Route
import com.patson.model.Link
import com.patson.model.LinkHistory
import com.patson.model.LinkConsideration
import com.patson.model.RelatedLink

object HistoryUtil {
  var loadedCycle = 0
  var allConsumptions : List[(PassengerType.Value, Int, Route)] = List() 
  
  def loadConsumptionByLink(link : Link, selfOnly : Boolean = false) : LinkHistory = {
    checkCache()
    
    val airlineId = link.airline.id
    val relatedConsumptions = allConsumptions.filter {
      case(_, _, route) => {
        route.links.exists { linkConsideration => 
          linkConsideration.link.id == link.id
        }
      }
    }
    
    println("Finished loading related consumption for " + link)
    
    val relatedFowardLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(_, _, route) => route.links.find { linkConsideration => !linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }, airlineId, selfOnly    
    ) 
    
    val relatedReverseLinks : List[RelatedLink] = computeRelatedLinks(relatedConsumptions.filter {
        case(_, _, route) => route.links.find { linkConsideration => linkConsideration.inverted && linkConsideration.link.id == link.id}.isDefined
      }, airlineId, selfOnly
    )
       
    LinkHistory(0, relatedFowardLinks.toSet, relatedReverseLinks.toSet)
  }
  
  private def computeRelatedLinks(relatedConsumption : List[(PassengerType.Value, Int, Route)], airlineId : Int, selfOnly : Boolean) : List[RelatedLink] = {
    val relatedLinkConsumptions : List[(PassengerType.Value, Int, LinkConsideration)] = relatedConsumption.flatMap {
      case(passengerType, passengerCount, route) => route.links.map { (passengerType, passengerCount, _) }.filter {
        case(_, _, link) => !selfOnly || link.link.airline.id == airlineId  
      }
    } //flat map by expanding the route to the links of the route
    
       
    //now group the link by the passenger type and the link itself
    val groupedLinkConsumptions = relatedLinkConsumptions.groupBy { case(passengerType, _, linkConsideration) => (linkConsideration.link, linkConsideration.inverted, passengerType) }
    
    //fold the value of the grouped map, we only care about passenger count now
    val computedConsumedLinks = groupedLinkConsumptions.mapValues{
      _.foldLeft(0)( (totalPassengerCount, entry) => totalPassengerCount + entry._2)
    }
    
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
  
  
  private def checkCache() = {
    val currentCycle = CycleSource.loadCycle()
    synchronized {
      if (currentCycle != loadedCycle) {
        println("Updating link history cache on cycle " + currentCycle)
        allConsumptions = ConsumptionHistorySource.loadAllConsumptions
        println("Updated link history cache on cycle " + currentCycle)
      }
      loadedCycle = currentCycle
    }
  }
}