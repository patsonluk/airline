package com.patson.model.oil

import com.patson.model.Airline

case class OilContract(airline : Airline, contractPrice : Double, volume : Int, contractCost : Long, startCycle : Int, contractDuration : Int) {
  val endCycle = startCycle + contractDuration
  def contractTerminationPenalty(currentCycle : Int) = ((endCycle - currentCycle) / contractDuration.toDouble).toLong * 5
  
}