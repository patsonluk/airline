package com.patson.model

case class BusyDelegate(airline : Airline, assignedTask : DelegateTask, availableCycle : Option[Int], var id : Int = 0) extends IdObject {
  val taskCompleted = availableCycle.isDefined
}

abstract class DelegateTask(startCycle : Int, taskType : DelegateTaskType.Value) {
  val description : String
  val getTaskType = taskType
  val getStartCycle = startCycle
  val coolDown : Int
}

object DelegateTask {
  val country = (startCycle : Int, country : Country) => CountryDelegateTask(startCycle, country)

  val linkNegotiation = (startCycle : Int, fromAirport : Airport, toAirport : Airport) => LinkNegotiationDelegateTask(startCycle, fromAirport, toAirport)
}

case class CountryDelegateTask(startCycle : Int, country: Country) extends DelegateTask(startCycle, DelegateTaskType.COUNTRY) {
  override val description: String = s"Develop relationship with ${country.name}"
  override val coolDown: Int = 0
}

case class LinkNegotiationDelegateTask(startCycle : Int, fromAirport : Airport, toAirport : Airport) extends DelegateTask(startCycle, DelegateTaskType.LINK_NEGOTIATION) {
  override val description: String = s"Flight from ${fromAirport.displayText} to ${toAirport.displayText}"
  override val coolDown: Int = 12
}

object DelegateTaskType extends Enumeration {
  type DelegateTaskType = Value
  val COUNTRY, LINK_NEGOTIATION = Value
}