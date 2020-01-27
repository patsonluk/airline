package com.patson.model.oil

import com.patson.model.Airline
import com.patson.model.IdObject

case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
  val endCycle = startCycle + contractDuration
  def contractTerminationPenalty(currentCycle : Int) = ((endCycle - currentCycle) * volume.toLong * 5) //$5 penalty per barrel left
  val contractCost : Long = volume.toLong * 3 * contractDuration 
}

object OilContract {
  val MAX_CONTRACTS_ALLOWED = 5
  val MAX_VOLUME_FACTOR = 1.2
  val MAX_DURATION = 200
  val MIN_DURATION = 10
  
  
  def getOilContract(airline : Airline, marketPrice : Double, volume : Int, currentCycle : Int, contractDuration : Int) : OilContract = {
    val deltaFromDefault = marketPrice - OilPrice.DEFAULT_PRICE
    val durationRatio =
      if (contractDuration > MAX_DURATION / 2) {
        0.5
      } else {
        1 - contractDuration.toDouble / (MAX_DURATION / 2) / 2
      }
    
    val contractPrice = BigDecimal(OilPrice.DEFAULT_PRICE + deltaFromDefault * durationRatio).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    
    OilContract(airline, contractPrice, volume, currentCycle, contractDuration)
  }
}
