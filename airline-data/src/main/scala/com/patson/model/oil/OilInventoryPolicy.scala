package com.patson.model.oil
import com.patson.model.Airline

object OilInventoryPolicyOption extends Enumeration {
  type OilInventoryPolicyOption = Value
  val CONSERVATIVE, BALANCED, AGGRESSIVE, NONE = Value
}

import OilInventoryPolicyOption._

case class OilInventoryPolicy(airline : Airline, factor : Double, startCycle : Int) {
  val inventoryPrice = (marketPrice : Double) => {
    val deltaFromDefault = marketPrice - OilPrice.DEFAULT_PRICE
    BigDecimal(OilPrice.DEFAULT_PRICE + deltaFromDefault * (1 - factor)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  val option = factor match {
    case 0.9 => CONSERVATIVE
    case 0.5 => BALANCED
    case 0.2 => AGGRESSIVE
    case 0 => NONE
  }
}


object OilInventoryPolicy {
  val MIN_CHANGE_DURATION = 50 //how many weeks before one can change the policy again
  def byOption(option : OilInventoryPolicyOption.Value, airline : Airline, startCycle : Int) = {
    val factor : Double = 
      option match {
        case CONSERVATIVE => 0.9
        case BALANCED => 0.5
        case AGGRESSIVE => 0.2
        case NONE => 0
      }
    
    OilInventoryPolicy(airline, factor, startCycle)
  }
  
  def getDefaultPolicy(airline : Airline) = {
    byOption(CONSERVATIVE, airline, 0) 
  }
  
  val description = (value : Value) => {
    value match {
        case CONSERVATIVE => "Conservative - shields from 90% of price fluctuation"
        case BALANCED => "Balanced - shields from 50% of price fluctuation"
        case AGGRESSIVE => "Aggressive - shields from 20% of price fluctuation"
        case NONE => "No Inventory - buys all required fuel at market price"
      }
  }
    
}

