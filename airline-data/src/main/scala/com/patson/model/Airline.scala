package com.patson.model

case class Airline(name: String, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo()
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
  def setServiceQuality(serviceQuality : Double) {
    airlineInfo.serviceQuality = serviceQuality
  }
  def setReputation(reputation : Double) {
    airlineInfo.reputation = reputation
  }
  
  def getBalance() = airlineInfo.balance
  def getServiceQuality() = airlineInfo.serviceQuality
  def getReputation() = airlineInfo.reputation
}

case class AirlineInfo(var balance : Long = 0, var serviceQuality : Double = 0, var reputation : Double = 0)

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("<unknown>")
    airlineWithJustId.id = id
    airlineWithJustId
  }
  val MAX_SERVICE_QUALITY = 100
}