package com.patson.model

import com.patson.data.{AirlineSource, AirplaneSource, AllianceSource, BankSource, CycleSource, DelegateSource, LinkSource, OilSource}

import scala.collection.mutable.ListBuffer

case class Airline(name: String, isGenerated : Boolean = false, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo(0, 0, 0, 0, 0)
  var allianceId : Option[Int] = None
  var bases : List[AirlineBase] = List.empty
  
  def setBalance(balance : Long) = { 
    airlineInfo.balance = balance 
  }
  def setCurrentServiceQuality(serviceQuality : Double) {
    airlineInfo.currentServiceQuality = serviceQuality
  }
  def setTargetServiceQuality(targetServiceQuality : Int) {
    airlineInfo.targetServiceQuality = targetServiceQuality
  }
  def setReputation(reputation : Double) {
    airlineInfo.reputation = reputation
  }
  def setMaintenanceQuality(maintenanceQuality : Double) {
    airlineInfo.maintenanceQuality = maintenanceQuality
  }

  def removeCountryCode() = {
    airlineInfo.countryCode = None
  }

  def setCountryCode(countryCode : String) = {
    airlineInfo.countryCode = Some(countryCode)
  }
  def getCountryCode() = {
    airlineInfo.countryCode
  }

  def setAirlineCode(airlineCode : String) = {
    airlineInfo.airlineCode = airlineCode
  }
  def getAirlineCode() = {
    airlineInfo.airlineCode
  }

  def setAllianceId(allianceId : Int) = {
    this.allianceId = Some(allianceId)
  }

  def getAllianceId() : Option[Int] = {
    allianceId
  }



  def setBases(bases : List[AirlineBase]) {
    this.bases = bases
  }

//  import FlightCategory._
//  val getLinkLimit = (flightCategory :FlightCategory.Value) => flightCategory match {
//      case DOMESTIC => None
//      case REGIONAL => None
//      case INTERCONTINENTAL =>
//        if (airlineGrade.value <= 4) {
//         Some(0)
//        } else {
//          Some((airlineGrade.value - 4) * 3)
//        }
//  }


  def airlineGrade : AirlineGrade = {
    val reputation = airlineInfo.reputation
    if (reputation < 10) {
  		AirlineGrade.NEW
  	} else if (reputation < 20) {
  	  AirlineGrade.LOCAL
  	} else if (reputation < 30) {
  		AirlineGrade.MUNICIPAL
  	} else if (reputation < 40) {
  		AirlineGrade.REGIONAL
  	} else if (reputation < 50) {
  		AirlineGrade.CONTINENTAL
  	} else if (reputation < 60) {
  		AirlineGrade.LESSER_INTERNATIONAL
  	} else if (reputation < 70) {
  		AirlineGrade.THIRD_INTERNATIONAL
  	} else if (reputation < 80) {
  		AirlineGrade.SECOND_INTERNATIONAL
  	} else if (reputation < 90) {
  		AirlineGrade.MAJOR_INTERNATIONAL
  	} else if (reputation < 100) {
  		AirlineGrade.TOP_INTERNATIONAL
  	} else if (reputation < 125) {
  	  AirlineGrade.TOP_INTERNATIONAL_2
  	} else if (reputation < 150) {
  	  AirlineGrade.TOP_INTERNATIONAL_3
  	} else if (reputation < 175) {
  	  AirlineGrade.TOP_INTERNATIONAL_4
  	} else if (reputation < 200) {
  	  AirlineGrade.TOP_INTERNATIONAL_5
  	} else if (reputation < 225) {
  	  AirlineGrade.LEGENDARY
  	} else if (reputation < 250) {
      AirlineGrade.ULTIMATE
    } else {
      AirlineGrade.CELESTIAL
    }
  }

  case class AirlineGrade(value : Int, description: String) {
    val getBaseLimit = {
      if (value <= 2) {
        1
      } else {
        value - 1
      }

    }

    val getModelFamilyLimit =  {
      if (value >= 10) 10 else value
    }
  }

  object AirlineGrade {
    val NEW = AirlineGrade(1, "New Airline")
    val LOCAL = AirlineGrade(2, "Local Airline")
    val MUNICIPAL = AirlineGrade(3, "Municipal Airline")
    val REGIONAL = AirlineGrade(4, "Regional Airline")
    val CONTINENTAL = AirlineGrade(5, "Continental Airline")
    val LESSER_INTERNATIONAL = AirlineGrade(6, "Lesser International Airline")
    val THIRD_INTERNATIONAL = AirlineGrade(7, "Third-class International Airline")
    val SECOND_INTERNATIONAL = AirlineGrade(8, "Second-class International Airline")
    val MAJOR_INTERNATIONAL = AirlineGrade(9, "Major International Airline")
    val TOP_INTERNATIONAL = AirlineGrade(10, "Top International Airline")
    val TOP_INTERNATIONAL_2 = AirlineGrade(11, "Top International Airline II")
    val TOP_INTERNATIONAL_3 = AirlineGrade(12, "Top International Airline III")
    val TOP_INTERNATIONAL_4 = AirlineGrade(13, "Top International Airline IV")
    val TOP_INTERNATIONAL_5 = AirlineGrade(14, "Top International Airline V")
    val LEGENDARY = AirlineGrade(15, "Legendary Airline")
    val ULTIMATE = AirlineGrade(16, "Ultimate Airline")
    val CELESTIAL = AirlineGrade(17, "Celestial Spaceline")
  }

  def getBases() = bases
  def getHeadQuarter() = bases.find( _.headquarter )

  def getBalance() = airlineInfo.balance
  def getCurrentServiceQuality() = airlineInfo.currentServiceQuality
  def getTargetServiceQuality() : Int = airlineInfo.targetServiceQuality

  def getReputation() = airlineInfo.reputation
  def getMaintenanceQuality() = airlineInfo.maintenanceQuality

  def getDefaultAirlineCode() : String = {
    var code = name.split("\\s+").foldLeft("")( (foldString, nameToken) => {
      val firstCharacter = nameToken.charAt(0)
      if (Character.isLetter(firstCharacter)) {
        foldString + firstCharacter.toUpper
      } else {
        foldString
      }
    })

    if (code.length() > 2) {
      code = code.substring(0, 2)
    } else if (code.length() < 2) {
      code = name.substring(0, 2).toUpperCase()
    }
    code
  }

  def getDelegateInfo() : DelegateInfo = {
    val busyDelegates = DelegateSource.loadBusyDelegatesByAirline(id)
    val availableCount = delegateCount - busyDelegates.size

    DelegateInfo(availableCount, busyDelegates)
  }

  lazy val delegateCount = getBases().map(_.delegateCapacity).sum
}

case class DelegateInfo(availableCount : Int, busyDelegates: List[BusyDelegate])

case class AirlineInfo(var balance : Long, var currentServiceQuality : Double, var maintenanceQuality : Double, var targetServiceQuality : Int, var reputation : Double, var countryCode : Option[String] = None, var airlineCode : String = "")

object TransactionType extends Enumeration {
  type TransactionType = Value
  val CAPITAL_GAIN, CREATE_LINK = Value
}

object OtherIncomeItemType extends Enumeration {
  type OtherBalanceItemType = Value
  val LOAN_INTEREST, BASE_UPKEEP, OVERTIME_COMPENSATION, SERVICE_INVESTMENT, MAINTENANCE_INVESTMENT, LOUNGE_UPKEEP, LOUNGE_COST, LOUNGE_INCOME, ADVERTISEMENT, DEPRECIATION, FUEL_PROFIT = Value
}

object CashFlowType extends Enumeration {
  type CashFlowType = Value
  val BASE_CONSTRUCTION, BUY_AIRPLANE, SELL_AIRPLANE, CREATE_LINK, FACILITY_CONSTRUCTION, OIL_CONTRACT = Value
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
case class LinksIncome(airlineId : Int, profit : Long = 0, revenue : Long = 0, expense : Long = 0, ticketRevenue: Long = 0, airportFee : Long = 0, fuelCost : Long = 0, crewCost : Long = 0, inflightCost : Long = 0, delayCompensation : Long = 0, maintenanceCost: Long = 0, loungeCost : Long = 0, depreciation : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
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
        delayCompensation = delayCompensation + income2.delayCompensation,
        maintenanceCost = maintenanceCost + income2.maintenanceCost,
        loungeCost = loungeCost + income2.loungeCost,
        depreciation = depreciation + income2.depreciation,
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
case class OthersIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, loanInterest : Long = 0, baseUpkeep : Long = 0, overtimeCompensation : Long = 0, serviceInvestment : Long = 0, maintenanceInvestment : Long = 0, advertisement : Long = 0, loungeUpkeep : Long = 0, loungeCost : Long = 0, loungeIncome : Long = 0, fuelProfit : Long = 0, depreciation : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : OthersIncome) : OthersIncome = {
    OthersIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        loanInterest = loanInterest + income2.loanInterest,
        baseUpkeep = baseUpkeep + income2.baseUpkeep,
        overtimeCompensation = overtimeCompensation + income2.overtimeCompensation,
        serviceInvestment = serviceInvestment + income2.serviceInvestment,
        maintenanceInvestment = maintenanceInvestment + income2.maintenanceInvestment,
        advertisement = advertisement + income2.advertisement,
        loungeUpkeep = loungeUpkeep + income2.loungeUpkeep,
        loungeCost = loungeCost + income2.loungeCost,
        loungeIncome = loungeIncome + income2.loungeIncome,
        fuelProfit = fuelProfit + income2.fuelProfit,
        depreciation = depreciation + income2.depreciation,
        period = period,
        cycle = income2.cycle)
  }    
}


case class AirlineCashFlowItem(airlineId : Int, cashFlowType : CashFlowType.Value, amount : Long, var cycle : Int = 0)
case class AirlineCashFlow(airlineId : Int, cashFlow : Long = 0, operation : Long = 0, loanInterest : Long = 0, loanPrincipal : Long = 0, baseConstruction : Long = 0, buyAirplane : Long = 0, sellAirplane : Long = 0,  createLink : Long = 0, facilityConstruction : Long = 0, oilContract : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
/**
   * Current income is expected to be MONTHLY/YEARLY. Adds parameter (WEEKLY income) to this current income object and return a new Airline income with period same as this object but cycle as the parameter
   */
  def update(cashFlow2 : AirlineCashFlow) : AirlineCashFlow = {
    AirlineCashFlow(airlineId, 
        cashFlow = cashFlow + cashFlow2.cashFlow,
        operation = operation + cashFlow2.operation,
        loanInterest = loanInterest + cashFlow2.loanInterest,
        loanPrincipal = loanPrincipal + cashFlow2.loanPrincipal,
        baseConstruction = baseConstruction + cashFlow2.baseConstruction,
        buyAirplane = buyAirplane + cashFlow2.buyAirplane,
        sellAirplane = sellAirplane + cashFlow2.sellAirplane,
        createLink = createLink + cashFlow2.createLink,
        facilityConstruction = facilityConstruction + cashFlow2.facilityConstruction,
        oilContract = oilContract + cashFlow2.oilContract,
        period = period,
        cycle = cashFlow2.cycle)
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
  val MAX_REPUTATION_BY_PASSENGERS : Double = 50
  val MAX_REPUTATION : Double = 100


  def resetAirline(airlineId : Int, newBalance : Long) : Option[Airline] = {
    AirlineSource.loadAirlineById(airlineId, true) match {
      case Some(airline) =>
        LinkSource.deleteLinksByAirlineId(airlineId)//remove all links

        //remove all airplanes
        AirplaneSource.deleteAirplanesByCriteria(List(("owner", airlineId)));
        //remove all bases
        AirlineSource.deleteAirlineBaseByCriteria(List(("airline", airlineId)))
        //remove all loans
        BankSource.loadLoansByAirline(airlineId).foreach { loan =>
          BankSource.deleteLoan(loan.id)
        }
        //remove all facilities
        AirlineSource.deleteLoungeByCriteria(List(("airline", airlineId)))

        //remove all oil contract
        OilSource.deleteOilContractByCriteria(List(("airline", airlineId)))

        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          AllianceSource.deleteAllianceMember(airlineId)
          if (allianceMember.role == AllianceRole.LEADER) { //remove the alliance
            AllianceSource.deleteAlliance(allianceMember.allianceId)
          }
        }

        airline.setBalance(newBalance)

        //unset country code
        airline.removeCountryCode()
        //unset service investment
        airline.setTargetServiceQuality(0)
        airline.setCurrentServiceQuality(0)

        //reset all busy delegates
        DelegateSource.deleteBusyDelegateByCriteria(List(("airline", "=", airlineId)))

        AirlineSource.saveAirlineInfo(airline)
        println(s"Reset airline - $airline")
        Some(airline)
      case None =>
        None
    }
  }
}