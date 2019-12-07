package com.patson.model.airplane

import com.patson.model.Airline
import com.patson.model.IdObject

case class Airplane(model : Model, var owner : Airline, constructedCycle : Int, var purchasedCycle : Int, condition : Double, depreciationRate : Int, value : Int, var isSold : Boolean = false, var dealerRatio : Double = Airplane.DEFAULT_DEALER_RATIO, var id : Int = 0) extends IdObject {
  val isReady = (currentCycle : Int) => currentCycle >= constructedCycle && !isSold
  val dealerValue = {
    (value * dealerRatio).toInt
  }
  
  def sellToDealer() = {
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = true
  }
  
  def buyFromDealer(airline : Airline, currentCycle : Int) = {
    owner = airline
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = false
    purchasedCycle = currentCycle
  }
}

object Airplane {
  val MAX_CONDITION = 100
  val BAD_CONDITION = 40
  val CRITICAL_CONDITION = 20
  val DEFAULT_DEALER_RATIO = 1.2
}