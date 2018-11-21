package com.patson.model.oil

import com.patson.model.Airline

case class OilConsumptionHistory(airline : Airline, price : Double, volume : Int, consumptionType : OilConsumptionType.Value, cycle : Int)

object OilConsumptionType extends Enumeration {
  type OilConsumptionType = Value
  val CONTRACT, MARKET, INVENTORY, EXCESS = Value
  
  val description = (consumptionType : Value) =>
    consumptionType match {
      case CONTRACT => "Oil Contract"
      case MARKET => "Oil Market"
      case INVENTORY => "Oil Inventory"
      case EXCESS => "Excessive from Contract (sold)"
    }
    
}


