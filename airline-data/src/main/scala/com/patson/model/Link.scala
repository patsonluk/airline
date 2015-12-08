package com.patson.model

import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model

/**
 * 
 * Frequency sum of all assigned plane
 */
case class Link(from : Airport, to : Airport, airline: Airline, price : Int, distance : Int, capacity: Int, rawQuality : Int, duration : Int, frequency : Int, var id : Int = 0) extends IdObject{
  var availableSeats : Int = capacity
  private var assignedAirplanes : List[Airplane] = List.empty
  private var assignedModel : Option[Model] = None
  
  private var hasComputedQuality = false
  private var hasComputedQualityPriceAdjust = false
  private var computedQualityStore : Int = 0
  private var computedQualityPriceAdjustStore : Double = 1.0
  
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
  
   //adjust by quality
  import FlightType._
  val neutralQuality = 
    Computation.getFlightType(from, to) match {
      case SHORT_HAUL_DOMESTIC => 30
      case SHORT_HAUL_INTERNATIONAL => 40
      case LONG_HAUL_DOMESTIC => 50
      case LONG_HAUL_INTERNATIONAL => 60
      case ULTRA_LONG_HAUL_INTERNATIONAL => 70
    }
    
  private val MAX_QUALITY = 100
  private val MAX_QUALITY_PRICE_MULTIPLIER = 0.5 // At Max quality, perceived price can cut by half
  private val MIN_QUALITY_PRICE_MULITPLIER = 0.5 // At Min quality, perceived price can increase by 0.5
    
  //println("neutral quality : " + neutralQuality + " distance : " + distance)
  def computeQualityPriceAdjust : Double = {
    if (!hasComputedQualityPriceAdjust) { 
      if (computedQuality > neutralQuality) {
        computedQualityPriceAdjustStore = 1 - MAX_QUALITY_PRICE_MULTIPLIER * (computedQuality - neutralQuality).toDouble / (MAX_QUALITY - neutralQuality)
      } else if (computedQuality < neutralQuality) {
        computedQualityPriceAdjustStore = 1 + MIN_QUALITY_PRICE_MULITPLIER  * (neutralQuality - computedQuality).toDouble / (neutralQuality)
      } 
      
      hasComputedQualityPriceAdjust = true
//      println("computed adjust " + computedQualityPriceAdjustStore)
      computedQualityPriceAdjustStore
    } else {
      computedQualityPriceAdjustStore
    }
  }
  
}

object Link {
  val MAX_RAW_QUALITY = 100
}

/**
 * Take note that cost is in terms of flight distance (km)
 */
case class LinkWithCost(link : Link, cost : Double, inverted : Boolean) {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " (inverted?) " + inverted + ")"
    }
}