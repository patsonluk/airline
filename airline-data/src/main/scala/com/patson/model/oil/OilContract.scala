package com.patson.model.oil

import com.patson.model.Airline
import com.patson.model.IdObject

case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
  val endCycle = startCycle + contractDuration
  def contractTerminationPenalty(currentCycle : Int) = ((endCycle - currentCycle) / contractDuration.toDouble).toLong * 5
  val contractCost : Long = volume.toLong * 5 * contractDuration
}

object OilContract {
  val MAX_CONTRACTS_ALLOWED = 5
  val MAX_VOLUME_FACTOR = 1.2
}