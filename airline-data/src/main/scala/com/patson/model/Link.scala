package com.patson.model

import com.patson.model.airplane.Airplane

case class Link(from : Airport, to : Airport, airline: Airline, price : Int, distance : Int, capacity: Int, rawQuality : Int, duration : Int, frequency : Int, var id : Int = 0) extends IdObject{
  var availableSeats : Int = capacity
  var assignedAirplanes : List[Airplane] = List.empty
  val MAX_RAW_QUALITY = 100
  private var hasComputedQuality = false
  private var hasComputedQualityPriceAdjust = false
  private var computedQualityStore : Int = 0
  private var computedQualityPriceAdjustStore : Double = 1.0
  
  def computedQuality : Int= {
    if (!hasComputedQuality) {
      if (assignedAirplanes.isEmpty) {
        0
      } else {
        hasComputedQuality = true
        computedQualityStore = (rawQuality.toDouble / MAX_RAW_QUALITY * 30 + airline.airlineInfo.serviceQuality.toDouble / Airline.MAX_SERVICE_QUALITY * 50 + (assignedAirplanes.foldLeft(0.0)( _ + _.condition.toDouble)) / assignedAirplanes.size / Airplane.MAX_CONDITION * 20).toInt
//        println("computed quality " + computedQualityStore)
        computedQualityStore
      }
    } else {
      computedQualityStore
    }
  }
  
   //adjust by quality
  
  //shortest haul at 30 is ok. long haul 7000+ requires 70 quality to be neutral
  private val LONG_HAUL_CUT_OFF = 7000
  private val SHORT_HAUL_CUT_OFF = 500
  private val LONG_HAUL_NEUTRAL_QUALITY = 70
  private val SHORT_HAUL_NEUTRAL_QUALITY = 30
  private val neutralQuality = 
    if (distance > LONG_HAUL_CUT_OFF) {
      LONG_HAUL_NEUTRAL_QUALITY 
    } else if (distance < SHORT_HAUL_CUT_OFF) {
      SHORT_HAUL_NEUTRAL_QUALITY
    } else {
      (SHORT_HAUL_NEUTRAL_QUALITY + (distance - SHORT_HAUL_CUT_OFF).toDouble / (LONG_HAUL_CUT_OFF - SHORT_HAUL_CUT_OFF) * (LONG_HAUL_NEUTRAL_QUALITY - SHORT_HAUL_NEUTRAL_QUALITY)).toInt
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

case class LinkWithCost(link : Link, cost : Double, inverted : Boolean) {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " (inverted?) " + inverted + ")"
    }
}