package com.patson.model

import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model

/**
 * 
 * Frequency sum of all assigned plane
 */
case class Link(from : Airport, to : Airport, airline: Airline, price : LinkPrice, distance : Int, capacity: LinkCapacity, rawQuality : Int, duration : Int, frequency : Int, var id : Int = 0) extends IdObject{
  var availableSeats : LinkCapacity = capacity.copy()
  private var assignedAirplanes : List[Airplane] = List.empty
  private var assignedModel : Option[Model] = None
  
  private var hasComputedQuality = false
  private var computedQualityStore : Int = 0
  private var computedQualityPriceAdjust : collection.mutable.Map[LinkClass, Double] = collection.mutable.Map[LinkClass, Double]()
  
  def setAssignedAirplanes(assignedAirplanes : List[Airplane]) = {
    this.assignedAirplanes = assignedAirplanes
    if (!assignedAirplanes.isEmpty) {
      assignedModel = Some(assignedAirplanes(0).model)
    }
  }
  
  def getAssignedAirplanes() = {
    assignedAirplanes
  }
  
  def getAssignedModel() : Option[Model] = {
    assignedModel
  }
  
  def computedQuality : Int= {
    if (!hasComputedQuality) {
      if (assignedAirplanes.isEmpty) {
        0
      } else {
        hasComputedQuality = true
        computedQualityStore = (rawQuality.toDouble / Link.MAX_RAW_QUALITY * 30 + airline.airlineInfo.serviceQuality.toDouble / Airline.MAX_SERVICE_QUALITY * 50 + (assignedAirplanes.foldLeft(0.0)( _ + _.condition.toDouble)) / assignedAirplanes.size / Airplane.MAX_CONDITION * 20).toInt
//        println("computed quality " + computedQualityStore)
        computedQualityStore
      }
    } else {
      computedQualityStore
    }
  }
  
  def getTotalCapacity : Int = {
    capacity.capacityMap.map(_._2).foldLeft(0)(_ + _)
  }
  
  def getTotalAvailableSeats : Int = {
    availableSeats.capacityMap.map(_._2).foldLeft(0)(_ + _)
  }
  
  def getTotalSoldSeats : Int = {
    getTotalCapacity - getTotalAvailableSeats 
  }
  
  
  
  def soldSeats : LinkCapacity = {
    LinkCapacity(
      capacity.capacityMap.map { 
        case (linkClass, capacity) =>
        (linkClass, capacity - availableSeats(linkClass))
      }
    )
  }
  
   //adjust by quality
  import FlightType._
  val neutralQualityOfClass = (linkClass : LinkClass) => {
    val linkClassMultiplier = linkClass.level - 1
    Computation.getFlightType(from, to) match {
      case SHORT_HAUL_DOMESTIC => 30 + linkClassMultiplier * 15
      case SHORT_HAUL_INTERNATIONAL => 35 + linkClassMultiplier * 15
      case LONG_HAUL_DOMESTIC => 45 + linkClassMultiplier * 15
      case LONG_HAUL_INTERNATIONAL => 50 + linkClassMultiplier * 15
      case ULTRA_LONG_HAUL_INTERNATIONAL => 55 + linkClassMultiplier * 15
    }
  }
    
  private val MAX_QUALITY = 100
    
  //println("neutral quality : " + neutralQuality + " distance : " + distance)
  def computeQualityPriceAdjust(linkClass : LinkClass) : Double = {
    if (!computedQualityPriceAdjust.isDefinedAt(linkClass)) {
      val neutralQuality = neutralQualityOfClass(linkClass)
      computedQualityPriceAdjust.put(linkClass, 1 + ((neutralQuality - computedQuality).toDouble / MAX_QUALITY) * 0.5) //if neutral is at 50, 0 quality yields 1.25, max quality yields 0.75
    }
    computedQualityPriceAdjust(linkClass)
  }
}

object Link {
  val MAX_RAW_QUALITY = 100
}

/**
 * Take note that cost is in terms of flight distance (km)
 */
case class LinkConsideration(link : Link, cost : Double, linkClass : LinkClass, inverted : Boolean) {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " (inverted?) " + inverted + ")"
    }
}

sealed abstract class LinkClass(val spaceMultiplier : Double, val resourceMultiplier : Double, val priceMultiplier : Double, val level : Int) //level for sorting/comparison purpose
case object FIRST extends LinkClass(spaceMultiplier = 8, resourceMultiplier = 6, priceMultiplier = 10, 3)
case object BUSINESS extends LinkClass(spaceMultiplier = 4, resourceMultiplier = 3, priceMultiplier = 4, 2)
case object ECONOMY extends LinkClass(spaceMultiplier = 1, resourceMultiplier = 1, priceMultiplier = 1, 1)
object LinkClass {
  def values() : List[LinkClass] =  {
    List(FIRST, BUSINESS, ECONOMY)
  }
}



case class LinkCapacity(capacityMap : Map[LinkClass, Int]) {
  def apply(linkClass : LinkClass) = { capacityMap.getOrElse(linkClass, 0) }
}
object LinkCapacity {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkCapacity = {
    LinkCapacity(Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first))
  }
}

case class LinkPrice(priceMap : Map[LinkClass, Int]) {
  def apply(linkClass : LinkClass) = { priceMap.getOrElse(linkClass, 0) } 
}
object LinkPrice {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkPrice = {
    LinkPrice(Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first))
  }
}


