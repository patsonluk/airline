package com.patson.model.airplane

import com.patson.model.{AbstractLinkClassValues, Airline, BUSINESS, FIRST, LinkClassValues}

case class AirplaneConfiguration(economyVal : Int, businessVal : Int, firstVal : Int, airline : Airline, model : Model, isDefault : Boolean, var id : Int = 0) extends AbstractLinkClassValues(economyVal, businessVal, firstVal) {
  lazy val minimized : AirplaneConfiguration = { //config that has least capacity
    val minimizedFirst = (model.capacity / FIRST.spaceMultiplier).toInt
    val minimizedBusiness = ((model.capacity - minimizedFirst * FIRST.spaceMultiplier) / BUSINESS.spaceMultiplier).toInt
    //no eco as user can lock econ to zero
    AirplaneConfiguration(0, minimizedBusiness, minimizedFirst, airline, model, isDefault)
  }
}

object AirplaneConfiguration {
  val empty = AirplaneConfiguration(0, 0, 0, Airline.fromId(0), Model.fromId(0), true)
  val default : ((Airline, Model) => AirplaneConfiguration) = (airline, model) => AirplaneConfiguration(economyVal = model.capacity, 0 ,0, airline, model, true)
  val MAX_CONFIGURATION_TEMPLATE_COUNT = 5 //per model and airline
}
