package com.patson.model.airplane

import com.patson.model.{AbstractLinkClassValues, Airline}

case class AirplaneConfiguration(economyVal : Int, businessVal : Int, firstVal : Int, airline : Airline, model : Model, isDefault : Boolean, var id : Int = 0) extends AbstractLinkClassValues(economyVal, businessVal, firstVal) {
}

object AirplaneConfiguration {
  val empty = AirplaneConfiguration(0, 0, 0, Airline.fromId(0), Model.fromId(0), true)
  val default : ((Airline, Model) => AirplaneConfiguration) = (airline, model) => AirplaneConfiguration(economyVal = model.capacity, 0 ,0, airline, model, true)
  val MAX_CONFIGURATION_TEMPLATE_COUNT = 5 //per model and airline
}
