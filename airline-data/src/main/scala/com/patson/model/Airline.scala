package com.patson.model

case class Airline(name: String) extends IdObject {
  val airlineInfo = AirlineInfo()
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
}

case class AirlineInfo(var balance : Long = 0)

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("")
    airlineWithJustId.id = id
    airlineWithJustId
  }
}