package com.patson.model

case class Airline(name: String, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo(0, 0, 0, 0, 0)
  var bases : List[AirlineBase] = List.empty
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
  def setServiceQuality(serviceQuality : Double) {
    airlineInfo.serviceQuality = serviceQuality
  }
  def setServiceFunding(serviceFunding : Int) {
    airlineInfo.serviceFunding = serviceFunding
  }
  def setReputation(reputation : Double) {
    airlineInfo.reputation = reputation
  }
  def setMaintainenceQuality(maintainenceQuality : Double) {
    airlineInfo.maintenanceQuality = maintainenceQuality
  }
  def setBases(bases : List[AirlineBase]) {
    this.bases = bases
  }
  
  def getBases() = bases
  def getHeadQuarter() = bases.find( _.headquarter ).get
  def getCountryCode() = getHeadQuarter().countryCode
  def getBalance() = airlineInfo.balance
  def getServiceQuality() = airlineInfo.serviceQuality
  def getServiceFunding() = airlineInfo.serviceFunding
  def getReputation() = airlineInfo.reputation
  def getMaintenanceQuality() = airlineInfo.maintenanceQuality
  def getAirlineCode() = {
    var code = name.split("\\s+").foldLeft("")( (foldString, nameToken) => foldString + nameToken.charAt(0).toUpper)
    if (code.length() > 2) {
      code.substring(0, 2)
    } else {
      code
    }
  }
}

case class AirlineInfo(var balance : Long, var serviceQuality : Double, var maintenanceQuality : Double, var serviceFunding : Int, var reputation : Double)

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("<unknown>")
    airlineWithJustId.id = id
    airlineWithJustId
  }
  val MAX_SERVICE_QUALITY : Double = 100
  val MAX_MAINTENANCE_QUALITY : Double = 100
  val MAX_REPUTATION : Double = 100
}