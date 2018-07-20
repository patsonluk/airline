package com.patson.model

import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
import com.patson.model.Scheduling.TimeSlot
import java.util.concurrent.ConcurrentHashMap

/**
 * 
 * Frequency sum of all assigned plane
 */
case class Link(from : Airport, to : Airport, airline: Airline, price : LinkClassValues, distance : Int, capacity: LinkClassValues, rawQuality : Int, duration : Int, frequency : Int, flightType : FlightType.Value, var flightNumber : Int = 0, var id : Int = 0) extends IdObject{
  @volatile var availableSeats : LinkClassValues = capacity.copy()
  @volatile var soldSeats : LinkClassValues = LinkClassValues.getInstance()
  @volatile var cancelledSeats :  LinkClassValues = LinkClassValues.getInstance()
  @volatile var cancellationCount = 0
  @volatile var majorDelayCount = 0
  @volatile var minorDelayCount = 0
  @volatile private var assignedAirplanes : List[Airplane] = List.empty
  @volatile private var assignedModel : Option[Model] = None
  
  @volatile private var hasComputedQuality = false
  @volatile private var computedQualityStore : Int = 0
  @volatile private var computedQualityPriceAdjust : ConcurrentHashMap[LinkClass, Double] = new ConcurrentHashMap[LinkClass, Double]()
  
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
  
  /**
   * Find seats at or below the requestedLinkClass (can only downgrade 1 level)
   * 
   * Returns the tuple of the matching class and seats available for that class
   */
  def availableSeatsAtOrBelowClass(targetLinkClass : LinkClass) : Option[(LinkClass, Int)] = {
    if (targetLinkClass == ECONOMY) {
      if (availableSeats(ECONOMY) > 0) {
        return Some(ECONOMY, availableSeats(ECONOMY))
      }
    } else {
      if (availableSeats(targetLinkClass) > 0) {
        return Some(targetLinkClass, availableSeats(targetLinkClass))
      } else  {
        val lowerClass = LinkClass.fromLevel(targetLinkClass.level - 1)
        if (availableSeats(lowerClass) > 0) {
          return Some(lowerClass, availableSeats(lowerClass))
        } 
      }
    }
    
    return None
  }
  
  def computedQuality : Int= {
    if (!hasComputedQuality) {
      if (assignedAirplanes.isEmpty) {
        0
      } else {
        
        computedQualityStore = (rawQuality.toDouble / Link.MAX_QUALITY * 30 + airline.airlineInfo.serviceQuality.toDouble / Airline.MAX_SERVICE_QUALITY * 50 + (assignedAirplanes.foldLeft(0.0)( _ + _.condition.toDouble)) / assignedAirplanes.size / Airplane.MAX_CONDITION * 20).toInt
//        println("computed quality " + computedQualityStore)
        hasComputedQuality = true
        computedQualityStore
      }
    } else {
      computedQualityStore
    }
  }
  
  def setQuality(quality : Int) = {
    computedQualityStore = quality
    hasComputedQuality = true
  }
  
  def getTotalCapacity : Int = {
    capacity.total
  }
  
  def getTotalAvailableSeats : Int = {
    availableSeats.total
  }
  
  def getTotalSoldSeats : Int = {
    soldSeats.total 
  }
  
  
  def addSoldSeats(soldSeats : LinkClassValues) = {
    this.soldSeats = this.soldSeats + soldSeats;
    this.availableSeats = this.availableSeats - soldSeats;
  }
  
  def addSoldSeatsByClass(linkClass : LinkClass, soldSeats : Int) = {
    val soldSeatsClassValues = LinkClassValues(Map(linkClass -> soldSeats)) 
    addSoldSeats(soldSeatsClassValues) 
  }
  
  def addCancelledSeats(cancelledSeats : LinkClassValues) = {
    this.cancelledSeats = this.cancelledSeats + cancelledSeats;
    this.availableSeats = this.availableSeats - cancelledSeats;
  }
  
  def capacityPerFlight() = {
    if (frequency > 0) {
      capacity / frequency
    } else { //error 
      LinkClassValues.getInstance()
    }
  }
  
  
    
  //println("neutral quality : " + neutralQuality + " distance : " + distance)
  def computeQualityPriceAdjust(linkClass : LinkClass) : Double = {
    if (!computedQualityPriceAdjust.containsKey(linkClass)) {
      val neutralQuality = Link.neutralQualityOfClass(linkClass, from, to, flightType)
      val qualityDelta = computedQuality - neutralQuality.toDouble
      
      val multiplier =
        if (qualityDelta < 0) { //neutral at 50, quality 0 yields 2.0 (heavy penalty)
          2
        } else { //neutral at 50, quality 100 yields 0.75 (slight advantage)
          0.5
        }
      
        var priceAdjust = 1 - (qualityDelta / Link.MAX_QUALITY) * multiplier
        if (priceAdjust < 0.75) { //don't reduce too much
          priceAdjust = 0.75
        }
      
      computedQualityPriceAdjust.put(linkClass, priceAdjust)
    }
    computedQualityPriceAdjust.get(linkClass)
  }
  
  lazy val schedule : Seq[TimeSlot] = Scheduling.getLinkSchedule(this)
}

object Link {
  val MAX_QUALITY = 100
  def fromId(id : Int) : Link = {
    Link(from = Airport.fromId(0), to = Airport.fromId(0), Airline.fromId(0), price = LinkClassValues.getInstance(), distance = 0, capacity = LinkClassValues.getInstance(), rawQuality = 0, duration = 0, frequency = 0, flightType = FlightType.SHORT_HAUL_DOMESTIC, id = id)
  }
  
   //adjust by quality
  import FlightType._
  val neutralQualityOfClass = (linkClass : LinkClass, from : Airport, to : Airport, flightType : FlightType.Value) => {
    val linkClassMultiplier = linkClass.level - 1
    flightType match {
      case SHORT_HAUL_DOMESTIC => 30 + linkClassMultiplier * 15
      case SHORT_HAUL_INTERNATIONAL => 35 + linkClassMultiplier * 15
      case SHORT_HAUL_INTERCONTINENTAL => 40 + linkClassMultiplier * 15
      case LONG_HAUL_DOMESTIC => 45 + linkClassMultiplier * 15
      case LONG_HAUL_INTERNATIONAL => 50 + linkClassMultiplier * 15
      case LONG_HAUL_INTERCONTINENTAL => 55 + linkClassMultiplier * 15
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 60 + linkClassMultiplier * 15
    }
  }
}

/**
 * Cost is the adjusted price
 */
case class LinkConsideration(link : Link, cost : Double, linkClass : LinkClass, inverted : Boolean, var id : Int = 0) extends IdObject {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " " + linkClass + " (inverted?) " + inverted + ")"
    }
}

sealed abstract class LinkClass(val code : String, val spaceMultiplier : Double, val resourceMultiplier : Double, val priceMultiplier : Double, val level : Int) //level for sorting/comparison purpose
case object FIRST extends LinkClass("F", spaceMultiplier = 6, resourceMultiplier = 3, priceMultiplier = 8, 3)
case object BUSINESS extends LinkClass("J", spaceMultiplier = 2.5, resourceMultiplier = 2, priceMultiplier = 2.5, 2)
case object ECONOMY extends LinkClass("Y", spaceMultiplier = 1, resourceMultiplier = 1, priceMultiplier = 1, 1)
object LinkClass {
  val values = List(FIRST, BUSINESS, ECONOMY)
  
  val fromCode : String => LinkClass = (code : String) => {
    values.find { _.code == code }.get
  }
  
  val fromLevel : Int => LinkClass = (level : Int) => {
    values.find { _.level  == level}.get 
  }
}