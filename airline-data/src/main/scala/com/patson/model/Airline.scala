package com.patson.model

case class Airline(name: String, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo(0, 0, 0, 0)
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
  def setServiceQuality(serviceQuality : Double) {
    airlineInfo.serviceQuality = serviceQuality
  }
  def setReputation(reputation : Double) {
    airlineInfo.reputation = reputation
  }
  def setMaintainenceQuality(maintainenceQuality : Double) {
    airlineInfo.maintenanceQuality = maintainenceQuality
  }
  
  def getBalance() = airlineInfo.balance
  def getServiceQuality() = airlineInfo.serviceQuality
  def getReputation() = airlineInfo.reputation
  def getMaintenanceQuality() = airlineInfo.maintenanceQuality
}

case class AirlineInfo(var balance : Long, var serviceQuality : Double, var maintenanceQuality : Double, var reputation : Double)

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("<unknown>")
    airlineWithJustId.id = id
    airlineWithJustId
  }
  val MAX_SERVICE_QUALITY : Double = 100
  val MAX_MAINTENANCE_QUALITY : Double = 100
}