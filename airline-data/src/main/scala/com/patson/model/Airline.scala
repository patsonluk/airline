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

object TransactionType extends Enumeration {
  type TransactionType = Value
  val SELL_AIRPLANE, BUY_AIRPLANE, BUILD_BASE, CREATE_LINK = Value
}

object OtherBalanceItemType extends Enumeration {
  type OtherBalanceItemType = Value
  val INTEREST, BASE_UPKEEP, SERVICE_INVESTMENT, MAINTAINENCE_INVESTMENT, ADVERTISEMENT = Value
}

case class AirlineTransaction(airlineId : Int, transactionType : TransactionType.Value, amount : Long, var cycle : Int = 0)
case class AirlineBalanceData(airlineId : Int, profit : Long, revenue: Long, expense: Long, links : LinksBalanceData, transactions : TransactionsBalance, others : OthersBalance, var cycle : Int = 0)
case class LinksBalanceData(airlineId : Int, profit : Long, revenue : Long, expense : Long, ticketRevenue: Long, airportFee : Long, fuelCost : Long, crewCost : Long, depreciation : Long, inflightCost : Long, maintenanceCost: Long, var cycle : Int = 0)
case class TransactionsBalance(airlineId : Int, profit : Long, revenue: Long, expense: Long, transactionSummary : Map[TransactionType.Value, Long], var cycle : Int = 0)
case class OthersBalance(airlineId : Int, profit : Long, revenue: Long, expense: Long, othersSummary : Map[OtherBalanceItemType.Value, Long], var cycle : Int = 0)

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