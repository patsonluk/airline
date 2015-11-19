package com.patson.model

case class Airline(name: String, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo()
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
  def setServiceQuality(serviceQuality : Int) {
    airlineInfo.serviceQuality = serviceQuality
  }
}

case class AirlineInfo(var balance : Long = 0, var serviceQuality : Int = 0)

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("")
    airlineWithJustId.id = id
    airlineWithJustId
  }
  val MAX_SERVICE_QUALITY = 100
}