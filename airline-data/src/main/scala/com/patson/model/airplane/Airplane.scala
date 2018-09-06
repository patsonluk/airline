package com.patson.model.airplane

import com.patson.model.Airline
import com.patson.model.IdObject

case class Airplane(model : Model, owner : Airline, constructedCycle : Int, condition : Double, depreciationRate : Int, value : Int, var isSold : Boolean = false, var dealerRatio : Double = Airplane.DEFAULT_DEALER_RATIO, var id : Int = 0) extends IdObject {
  val isReady = (currentCycle : Int) => currentCycle >= constructedCycle && !isSold
}

object Airplane {
  val MAX_CONDITION = 100
  val BAD_CONDITION = 40
  val CRITICAL_CONDITION = 20
  val DEFAULT_DEALER_RATIO = 1.2
}