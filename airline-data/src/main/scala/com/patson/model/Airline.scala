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
  val CAPITAL_GAIN, CREATE_LINK = Value
}

object OtherIncomeItemType extends Enumeration {
  type OtherBalanceItemType = Value
  val LOAN_INTEREST, BASE_UPKEEP, SERVICE_INVESTMENT, MAINTENANCE_INVESTMENT, ADVERTISEMENT, DEPRECIATION = Value
}

object Period extends Enumeration {
  type Period = Value
  val WEEKLY, MONTHLY, YEARLY = Value
}


case class AirlineTransaction(airlineId : Int, transactionType : TransactionType.Value, amount : Long, var cycle : Int = 0)
case class AirlineIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, links : LinksIncome, transactions : TransactionsIncome, others : OthersIncome, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  /**
   * Current income is expected to be MONTHLY/YEARLY. Adds parameter (WEEKLY income) to this current income object and return a new Airline income with period same as this object but cycle as the parameter
   */
  def update(income2 : AirlineIncome) : AirlineIncome = {
    AirlineIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        links = links.update(income2.links),
        transactions = transactions.update(income2.transactions),
        others = others.update(income2.others),
        period = period,
        cycle = income2.cycle)
  }
}
case class LinksIncome(airlineId : Int, profit : Long = 0, revenue : Long = 0, expense : Long = 0, ticketRevenue: Long = 0, airportFee : Long = 0, fuelCost : Long = 0, crewCost : Long = 0, inflightCost : Long = 0, maintenanceCost: Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : LinksIncome) : LinksIncome = {
    LinksIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        ticketRevenue = ticketRevenue + income2.ticketRevenue,
        airportFee = airportFee + income2.airportFee,
        fuelCost = fuelCost + income2.fuelCost,
        crewCost = crewCost + income2.crewCost,
        inflightCost = inflightCost + income2.inflightCost,
        maintenanceCost = maintenanceCost + income2.maintenanceCost,
        period = period,
        cycle = income2.cycle)
  }
}
case class TransactionsIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, capitalGain : Long = 0, createLink : Long = 0,  period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : TransactionsIncome) : TransactionsIncome = {
    TransactionsIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        capitalGain = capitalGain + income2.capitalGain,
        createLink = createLink + income2.createLink,
        period = period,
        cycle = income2.cycle)
  }  
}
case class OthersIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, loanInterest : Long = 0, baseUpkeep : Long = 0, serviceInvestment : Long = 0, maintenanceInvestment : Long = 0, advertisement : Long = 0, depreciation : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : OthersIncome) : OthersIncome = {
    OthersIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        loanInterest = loanInterest + income2.loanInterest,
        baseUpkeep = baseUpkeep + income2.baseUpkeep,
        serviceInvestment = serviceInvestment + income2.serviceInvestment,
        maintenanceInvestment = maintenanceInvestment + income2.maintenanceInvestment,
        advertisement = advertisement + income2.advertisement,
        depreciation = depreciation + income2.depreciation,
        period = period,
        cycle = income2.cycle)
  }    
}

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