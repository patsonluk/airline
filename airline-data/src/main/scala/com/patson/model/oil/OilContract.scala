package com.patson.model.oil

import com.patson.model.Airline
import com.patson.model.IdObject

case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
  val endCycle = startCycle + contractDuration
  def contractTerminationPenalty(currentCycle : Int) = (((endCycle - currentCycle) / contractDuration.toDouble) * 5 * contractCost).toLong
  val contractCost : Long = volume.toLong * 5 * contractDuration //as if +10 per barrel
}

object OilContract {
  val MAX_CONTRACTS_ALLOWED = 5
  val MAX_VOLUME_FACTOR = 1.2
  val MAX_DURATION = 300
  val MIN_DURATION = 10
  val EXTRA_PER_BARREL_CHARGE = 5 //extra charge per barrel
}