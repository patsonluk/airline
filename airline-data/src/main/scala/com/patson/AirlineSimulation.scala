package com.patson

import com.patson.model._
import com.patson.data._

import scala.collection.mutable._
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane.Airplane
import com.patson.model.oil.OilPrice
import com.patson.model.oil.OilInventoryPolicy
import com.patson.model.oil.OilConsumptionHistory
import com.patson.model.oil.OilConsumptionType
import com.patson.util.{AirportChampionInfo, ChampionUtil, CountryChampionInfo}

object AirlineSimulation {
  private val AIRLINE_FIXED_COST = 0 //for now...
  val MAX_SERVICE_QUALITY_INCREMENT : Double = 0.5
  val MAX_SERVICE_QUALITY_DECREMENT : Double = 10
  val MAX_REPUTATION_DELTA = 1
  val BANKRUPTCY_CASH_THRESHOLD = -10000000 //-10M

  def airlineSimulation(cycle: Int, flightLinkResult : List[LinkConsumptionDetails], loungeResult : scala.collection.immutable.Map[Lounge, LoungeConsumptionDetails], airplanes : List[Airplane]) = {
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    val allLinks = LinkSource.loadAllLinks(LinkSource.FULL_LOAD)
    val allFlightLinksByAirlineId = allLinks.filter(_.transportType == TransportType.FLIGHT).map(_.asInstanceOf[Link]).groupBy(_.airline.id)
    val allShuttleLinksByAirlineId = allLinks.filter(_.transportType == TransportType.SHUTTLE).map(_.asInstanceOf[Shuttle]).groupBy(_.airline.id)
    val allTransactions = AirlineSource.loadTransactions(cycle).groupBy { _.airlineId }
    val allTransactionalCashFlowItems: scala.collection.immutable.Map[Int, List[AirlineCashFlowItem]] = AirlineSource.loadCashFlowItems(cycle).groupBy { _.airlineId }
    //purge the older transactions
    AirlineSource.deleteTransactions(cycle - 1)
    AirlineSource.deleteCashFlowItems(cycle - 1)
    val flightLinkResultByAirline = flightLinkResult.groupBy(_.link.airline.id)
    //val otherTransportResultByAirline = linkResult.filter(_.link.transportType != TransportType.FLIGHT).groupBy(_.link.airline.id)


    val airplanesByAirline = airplanes.groupBy(_.owner.id)
    val allCountries = CountrySource.loadAllCountries().map( country => (country.countryCode, country)).toMap

    val loungesByAirlineId = scala.collection.mutable.Map[Int, ListBuffer[Lounge]]()
    AirlineSource.loadAllLounges.foreach(lounge =>
      loungesByAirlineId.getOrElseUpdate(lounge.airline.id, ListBuffer[Lounge]()) += lounge
    )

    val shuttleServicesByAirlineId = AirlineSource.loadShuttleServiceByCriteria(List.empty).groupBy(_.airline.id)

    val allIncomes = ListBuffer[AirlineIncome]()
    val allCashFlows = ListBuffer[AirlineCashFlow]() //cash flow for accounting purpose

    val currentCycle = MainSimulation.currentWeek
    //val champions : scala.collection.immutable.Map[Airline, List[ChampionInfo]] = ChampionUtil.getAllCountryChampionInfo().groupBy(_.airline)
    val airportChampionsByAirlineId : immutable.Map[Int, List[AirportChampionInfo]] = ChampionUtil.loadAirportChampionInfo().groupBy(_.loyalist.airline.id)
    val titlesByCountryCodeAndAirlineId : immutable.Map[(String, Int), List[CountryAirlineTitle]]= CountrySource.loadCountryAirlineTitlesByCriteria(List.empty).groupBy(entry => (entry.country.countryCode, entry.airline.id)) //key is (CountryCode, AirlineId)
    val cashFlows = Map[Airline, Long]() //cash flow for actual deduction

    val alliances = AllianceSource.loadAllAlliances()
    val allianceByAirlineId :scala.collection.immutable.Map[Int, Alliance] = alliances.flatMap { alliance => (alliance.members.filter(_.role != AllianceRole.APPLICANT).map(member => (member.airline.id, alliance))) }.toMap
    val allianceRankings = Alliance.getRankings(alliances)

    val fuelContractsByAirlineId = OilSource.loadAllOilContracts().groupBy(contract => contract.airline.id)
    val fuelInventoryPolicyByAirlineId = OilSource.loadAllOilInventoryPolicies.map(policy => (policy.airline.id, policy)).toMap
    val currentFuelPrice = OilSource.loadOilPriceByCycle(cycle).get.price
    val oilConsumptionEntries = ListBuffer[OilConsumptionHistory]()

    allAirlines.foreach { airline =>
        var totalCashRevenue = 0L
        var totalCashExpense = 0L
        var linksDepreciation = 0L
        val linksIncome = flightLinkResultByAirline.get(airline.id) match {
          case Some(linkConsumptions) => {
            val linksProfit = linkConsumptions.foldLeft(0L)(_ + _.profit)
            val linksAirportFee = linkConsumptions.foldLeft(0L)(_ + _.airportFees)
            val linksCrewCost = linkConsumptions.foldLeft(0L)(_ + _.crewCost)
            val linksFuelCost = linkConsumptions.foldLeft(0L)(_ + _.fuelCost)
            val linksInflightCost = linkConsumptions.foldLeft(0L)(_ + _.inflightCost)
            val linksDelayCompensation = linkConsumptions.foldLeft(0L)(_ + _.delayCompensation)
            val linksMaintenanceCost = linkConsumptions.foldLeft(0L)(_ + _.maintenanceCost)
            linksDepreciation = linkConsumptions.foldLeft(0L)(_ + _.depreciation)
            val linksLoungeCost = linkConsumptions.foldLeft(0L)(_ + _.loungeCost)
            val linksRevenue = linkConsumptions.foldLeft(0L)(_ + _.revenue)

            val linksExpense = linksAirportFee + linksCrewCost + linksFuelCost + linksInflightCost + linksDelayCompensation + linksMaintenanceCost + linksDepreciation + linksLoungeCost

            totalCashRevenue += linksRevenue
            totalCashExpense += linksExpense - linksDepreciation //airplane depreciation is already deducted on the plane, not a cash expense
            LinksIncome(airline.id, profit = linksProfit, revenue = linksRevenue, expense = linksExpense, ticketRevenue = linksRevenue, airportFee = -1 * linksAirportFee, fuelCost = -1 * linksFuelCost, crewCost = -1 * linksCrewCost, inflightCost = -1 * linksInflightCost, delayCompensation = -1 * linksDelayCompensation, maintenanceCost= -1 * linksMaintenanceCost, loungeCost= -1 * linksLoungeCost,  depreciation = -1 * linksDepreciation, cycle = currentCycle)
          }
          case None => LinksIncome(airline.id, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, cycle = currentCycle)
        }

        val transactionsIncome = allTransactions.get(airline.id) match {
          case Some(transactions) => {
            var expense = 0L
            var revenue = 0L
            val summary = Map[TransactionType.Value, Long]()
            transactions.foreach { transaction =>
              if (transaction.amount >= 0) {
                revenue += transaction.amount
              } else {
                expense -= transaction.amount
              }

              val existingAmount = summary.getOrElse(transaction.transactionType, 0L)
              summary.put(transaction.transactionType, existingAmount + transaction.amount)
            }
            TransactionsIncome(airline.id, revenue - expense, revenue, expense, capitalGain = summary.getOrElse(TransactionType.CAPITAL_GAIN, 0), createLink = summary.getOrElse(TransactionType.CREATE_LINK, 0), cycle = currentCycle)
          }
          case None => TransactionsIncome(airline.id, 0, 0, 0, capitalGain = 0, createLink = 0, cycle = currentCycle)
        }



        val othersSummary = Map[OtherIncomeItemType.Value, Long]()
        //calculate service funding required
        val linksOfThisAirline = allFlightLinksByAirlineId.getOrElse(airline.id, List.empty)
        var serviceFunding = getServiceFunding(airline.getTargetServiceQuality(), linksOfThisAirline)
        val targetServiceQuality =
          if (airline.getBalance() < 0) { //cease all funding, target will be 0
            serviceFunding = 0
            0
          } else {
            airline.getTargetServiceQuality()
          }
        val currentServiceQuality = airline.getCurrentServiceQuality()
        airline.setCurrentServiceQuality(getNewQuality(currentServiceQuality, targetServiceQuality))

        othersSummary.put(OtherIncomeItemType.SERVICE_INVESTMENT, serviceFunding * -1)
        totalCashExpense += serviceFunding

        val baseUpkeep = airline.bases.foldLeft(0L)((upkeep, base) => {
          upkeep + base.getUpkeep
        })

        othersSummary.put(OtherIncomeItemType.BASE_UPKEEP, -1 * baseUpkeep) //negative number
        totalCashExpense += baseUpkeep

      //overtime compensation
        val linksByFromAirportId = allFlightLinksByAirlineId.get(airline.id).getOrElse(List.empty).groupBy(_.from.id)

        var overtimeCompensation = 0
        airline.bases.foreach { base =>
          val staffRequired = linksByFromAirportId.get(base.airport.id) match {
            case Some(links) => links.map(_.getCurrentOfficeStaffRequired).sum
            case None => 0
          }
          val titleOption : Option[Title.Value] = titlesByCountryCodeAndAirlineId.get((base.airport.countryCode, airline.id)) match {
            case Some(titles) =>
              if (titles.length > 0) Some(titles(0).title) else None //now only 1 title per airline per country
            case None => None
          }
          val staffCapacity = base.getOfficeStaffCapacity
          val compensationOfThisBase = base.getOvertimeCompensation(staffRequired)
          if (compensationOfThisBase > 0) {
            println(s"${airline.name} Overtime compensation $compensationOfThisBase : capacity $staffCapacity ; required $staffRequired")
          }
          overtimeCompensation += compensationOfThisBase
        }

        othersSummary.put(OtherIncomeItemType.OVERTIME_COMPENSATION, -1 * overtimeCompensation) //negative number
        totalCashExpense += overtimeCompensation


        val allAirplanesDepreciation = airplanesByAirline.getOrElse(airline.id, List.empty).foldLeft(0L) {
          case(depreciation, airplane) => (depreciation + airplane.depreciationRate)
        }

        val unassignedAirplanesDepreciation = allAirplanesDepreciation - linksDepreciation //account depreciation on planes that are without assigned links
        othersSummary.put(OtherIncomeItemType.DEPRECIATION, -1 * unassignedAirplanesDepreciation) //not a cash expense

        val (loanPayment, interestPayment) = updateLoans(airline, currentCycle + 1) //have to plus one here, as this is supposed to be done postcycle, but for accounting purpose we have to put it here
        othersSummary.put(OtherIncomeItemType.LOAN_INTEREST, -1 * interestPayment)
        totalCashExpense += loanPayment //paying both principle + interest

        val loungeUpkeep = loungesByAirlineId.get(airline.id) match {
          case Some(lounges) => lounges.map(_.getUpkeep).sum
          case None => 0
        }
        var loungeCost = 0L
        var loungeIncome = 0L;

        loungeResult.filter(_._1.airline.id == airline.id).map {
          case (_, LoungeConsumptionDetails(_, selfVisitors, allianceVisitors, _)) => {
            loungeCost += (selfVisitors + allianceVisitors) * Lounge.PER_VISITOR_COST
            loungeIncome += (selfVisitors + allianceVisitors) * Lounge.PER_VISITOR_CHARGE
          }
        }
        othersSummary.put(OtherIncomeItemType.LOUNGE_UPKEEP, -1 * loungeUpkeep)
        othersSummary.put(OtherIncomeItemType.LOUNGE_COST, -1 * loungeCost)
        othersSummary.put(OtherIncomeItemType.LOUNGE_INCOME, loungeIncome)


        var shuttleCost = 0L
        allShuttleLinksByAirlineId.get(airline.id).foreach { links =>
          shuttleCost = links.map(_.upkeep).sum
        }
        shuttleCost += shuttleServicesByAirlineId.getOrElse(airline.id, List.empty).map(_.basicUpkeep).sum
        othersSummary.put(OtherIncomeItemType.SHUTTLE_COST, -1 * shuttleCost)

        totalCashExpense += loungeUpkeep + loungeCost + shuttleCost
        totalCashRevenue += loungeIncome

        //calculate extra cash flow due to difference in fuel cost
        val accountingFuelCost = linksIncome.fuelCost * -1
        val barrelsUsed = (accountingFuelCost / OilPrice.DEFAULT_PRICE).toInt
        val actualFuelCost = fuelContractsByAirlineId.get(airline.id) match {
          case Some(contracts) =>
            val totalPaymentFromContract = contracts.map{ contract =>
                oilConsumptionEntries += OilConsumptionHistory(airline, contract.contractPrice, contract.volume, OilConsumptionType.CONTRACT, currentCycle)
                contract.contractPrice * contract.volume
              }.sum

            val totalVolumeFromContract = contracts.map(_.volume).sum
            if (totalVolumeFromContract <= barrelsUsed) {
              //has to use inventory
              val inventoryPolicy = fuelInventoryPolicyByAirlineId.getOrElse(airline.id, OilInventoryPolicy.getDefaultPolicy(airline))
              val inventoryPrice = inventoryPolicy.inventoryPrice(currentFuelPrice)
              val volumeFromInventory = barrelsUsed - totalVolumeFromContract
              val consumptionType = if (inventoryPolicy.factor == 0) OilConsumptionType.MARKET else OilConsumptionType.INVENTORY
              if (volumeFromInventory > 0) {
                oilConsumptionEntries += OilConsumptionHistory(airline, inventoryPrice, volumeFromInventory, consumptionType, currentCycle)
              }
              totalPaymentFromContract + volumeFromInventory * inventoryPrice
            } else { //excessive
              //sell to someone else
              val sellPrice = currentFuelPrice / 2
              val excessBarrles = totalVolumeFromContract - barrelsUsed
              oilConsumptionEntries += OilConsumptionHistory(airline, sellPrice, excessBarrles * -1, OilConsumptionType.EXCESS, currentCycle)
              totalPaymentFromContract - excessBarrles * sellPrice //and sell the rest half market price
            }
          case None =>
            val inventoryPolicy = fuelInventoryPolicyByAirlineId.getOrElse(airline.id, OilInventoryPolicy.getDefaultPolicy(airline))
            val inventoryPrice = inventoryPolicy.inventoryPrice(currentFuelPrice)
            val consumptionType = if (inventoryPolicy.factor == 0) OilConsumptionType.MARKET else OilConsumptionType.INVENTORY
            if (barrelsUsed > 0) {
              oilConsumptionEntries += OilConsumptionHistory(airline, inventoryPrice, barrelsUsed, consumptionType, currentCycle)
            }
            barrelsUsed * inventoryPrice
        }

        val fuelProfit = accountingFuelCost - actualFuelCost.toLong
        if (fuelProfit > 0) {
          totalCashRevenue += fuelProfit
        } else {
          totalCashExpense += fuelProfit * -1
        }
//        println("airline " + airline)
//        println("barrels used: " + barrelsUsed + " acc. fuel cost " + accountingFuelCost + " actual fuel cost " + actualFuelCost.toLong + " profit " + fuelProfit)

        othersSummary.put(OtherIncomeItemType.FUEL_PROFIT, fuelProfit)

      //campaign/ads
        val advertisementCost = DelegateSource.loadCampaignTasksByAirlineId(airline.id).map(_.cost).sum
        othersSummary.put(OtherIncomeItemType.ADVERTISEMENT, advertisementCost * -1)


        var othersRevenue = 0L
        var othersExpense = 0L
        othersSummary.foreach {
          case (_, amount) => {
            if (amount >= 0) {
              othersRevenue += amount
            } else {
              othersExpense -= amount
            }
          }
        }

        val othersIncome = OthersIncome(airline.id, othersRevenue - othersExpense, othersRevenue, othersExpense
            , loanInterest = othersSummary.getOrElse(OtherIncomeItemType.LOAN_INTEREST, 0)
            , baseUpkeep = othersSummary.getOrElse(OtherIncomeItemType.BASE_UPKEEP, 0)
            , overtimeCompensation = othersSummary.getOrElse(OtherIncomeItemType.OVERTIME_COMPENSATION, 0)
            , serviceInvestment = othersSummary.getOrElse(OtherIncomeItemType.SERVICE_INVESTMENT, 0)
            , maintenanceInvestment = othersSummary.getOrElse(OtherIncomeItemType.MAINTENANCE_INVESTMENT, 0)
            , advertisement = othersSummary.getOrElse(OtherIncomeItemType.ADVERTISEMENT, 0)
            , loungeUpkeep = othersSummary.getOrElse(OtherIncomeItemType.LOUNGE_UPKEEP, 0)
            , loungeCost = othersSummary.getOrElse(OtherIncomeItemType.LOUNGE_COST, 0)
            , loungeIncome = othersSummary.getOrElse(OtherIncomeItemType.LOUNGE_INCOME, 0)
            , shuttleCost = othersSummary.getOrElse(OtherIncomeItemType.SHUTTLE_COST, 0)
            , fuelProfit = othersSummary.getOrElse(OtherIncomeItemType.FUEL_PROFIT, 0)
            , depreciation = othersSummary.getOrElse(OtherIncomeItemType.DEPRECIATION, 0)
            , cycle = currentCycle
        )


        val airlineRevenue = linksIncome.revenue + transactionsIncome.revenue + othersIncome.revenue
        val airlineExpense = linksIncome.expense + transactionsIncome.expense + othersIncome.expense
        val airlineProfit = airlineRevenue - airlineExpense
        val airlineWeeklyIncome = AirlineIncome(airline.id, airlineProfit, airlineRevenue, airlineExpense, linksIncome, transactionsIncome, othersIncome, cycle = currentCycle)
        allIncomes += airlineWeeklyIncome
        allIncomes ++= computeAccumulateIncome(airlineWeeklyIncome)

        //cash flow computation
        val totalCashFlow = totalCashRevenue - totalCashExpense

        val operationCashFlow = totalCashFlow + loanPayment //exclude both interest and principle here, which WAS included in the total cash flow
        cashFlows.put(airline, totalCashFlow) //this is week end flow, used for actual adjustment

        //below is for accounting purpose
        //cash flow item that is already applied during this week, still need to load them for accounting purpose
        val transactionalCashFlowItems : scala.collection.immutable.Map[CashFlowType.Value, Long] = allTransactionalCashFlowItems.get(airline.id) match {
          case Some(items) => items.groupBy(_.cashFlowType).view.mapValues( itemsByType => itemsByType.map(_.amount).sum).toMap
          case None => scala.collection.immutable.Map.empty
        }

        //include cash flow during the week, only use for accounting purpose here
        val baseConstruction = transactionalCashFlowItems.getOrElse(CashFlowType.BASE_CONSTRUCTION, 0L)
        val buyAirplane = transactionalCashFlowItems.getOrElse(CashFlowType.BUY_AIRPLANE, 0L)
        val sellAirplane = transactionalCashFlowItems.getOrElse(CashFlowType.SELL_AIRPLANE, 0L)
        val createLink = transactionalCashFlowItems.getOrElse(CashFlowType.CREATE_LINK, 0L)
        val facilityConstruction = transactionalCashFlowItems.getOrElse(CashFlowType.FACILITY_CONSTRUCTION, 0L)
        val oilContract = transactionalCashFlowItems.getOrElse(CashFlowType.OIL_CONTRACT, 0L)

        val accountingCashFlow = totalCashFlow + baseConstruction + buyAirplane + sellAirplane + createLink + facilityConstruction + oilContract

        val loanPrincipal = loanPayment - interestPayment
        val airlineWeeklyCashFlow = AirlineCashFlow(airline.id, cashFlow = accountingCashFlow, operation = operationCashFlow, loanInterest = interestPayment * -1, loanPrincipal = loanPrincipal * -1, baseConstruction = baseConstruction, buyAirplane = buyAirplane, sellAirplane = sellAirplane, createLink = createLink, facilityConstruction = facilityConstruction, oilContract = oilContract, cycle = currentCycle)
        allCashFlows += airlineWeeklyCashFlow
        allCashFlows ++= computeAccumulateCashFlow(airlineWeeklyCashFlow)


        //update reputation
        val reputationBreakdowns = ListBuffer[ReputationBreakdown]()
        val reputationByPassengers = flightLinkResultByAirline.get(airline.id) match {
          case Some(linkConsumptions) =>
            val totalPassengerKilometers = linkConsumptions.foldLeft(0L) { (foldLong, linkConsumption) =>
              foldLong + linkConsumption.link.soldSeats.total * linkConsumption.link.distance
            }

            //https://en.wikipedia.org/wiki/World%27s_largest_airlines
            val reputation = Math.log(totalPassengerKilometers / 50000) * 30
            if (reputation > Airline.MAX_REPUTATION_BY_PASSENGERS) {
              Airline.MAX_REPUTATION_BY_PASSENGERS
            } else if (reputation < 10) { //any pax
              10
            } else {
              reputation
            }
          case None =>
            0
        }
        reputationBreakdowns.append(ReputationBreakdown(ReputationType.FLIGHT_PASSENGERS, reputationByPassengers))

//        champions.get(airline).foreach { //if this airline championed anything
//          _.foreach { championInfo =>
//              targetReputation = targetReputation + championInfo.reputationBoost
//          }
//        }

        val reputationByAirportChampions = airportChampionsByAirlineId.get(airline.id) match {
          case Some(airportChampions) => airportChampions.map(_.reputationBoost).sum
          case None => 0
        }
        reputationBreakdowns.append(ReputationBreakdown(ReputationType.AIRPORT_LOYALIST_RANKING, reputationByAirportChampions))

        val reputationBonusFromAlliance : Double = allianceByAirlineId.get(airline.id) match {
          case Some(alliance) => allianceRankings.get(alliance) match {
            case Some((ranking, _)) => Alliance.getReputationBonus(ranking)
            case None => 0.0
          }
          case None => 0.0
        }

        reputationBreakdowns.append(ReputationBreakdown(ReputationType.ALLIANCE_BONUS, reputationBonusFromAlliance))

        val finalBreakdowns = ReputationBreakdowns(reputationBreakdowns.toList)
        AirlineSource.updateReputationBreakdowns(airline.id, finalBreakdowns)

        val currentReputation = airline.getReputation()
        var targetReputation = finalBreakdowns.total
        //make sure it increases/decreases gradually based on passenger volume
        if (targetReputation >  currentReputation && targetReputation - currentReputation > MAX_REPUTATION_DELTA) {
          targetReputation = currentReputation + MAX_REPUTATION_DELTA
        } else if (targetReputation <  currentReputation && currentReputation - targetReputation > MAX_REPUTATION_DELTA) {
          targetReputation = currentReputation - MAX_REPUTATION_DELTA
        }


        airline.setReputation(targetReputation)



        //check bankruptcy
        if (airline.getBalance() < BANKRUPTCY_CASH_THRESHOLD) {
          val shouldReset = linksIncome.profit < 0
          if (shouldReset) {
            var resetBalance = Computation.getResetAmount(airline.id).overall
            if (resetBalance < 0) {
              resetBalance = 0
            }
            println(s"Resetting $airline due to negative cash and unprofitable flights")
            Airline.resetAirline(airline.id, newBalance = resetBalance)
          }
        }


        println(airline + " profit is: " + airlineProfit + " existing balance (not updated yet) " + airline.getBalance() + " reputation " +  airline.getReputation() + " cash flow " + totalCashFlow)
    }
    
    AirlineSource.saveAirlinesInfo(allAirlines)
    cashFlows.foreach { //for balance it's safer to use adjust instead of setting it directly
      case(airline, cashFlow) => AirlineSource.adjustAirlineBalance(airline.id, cashFlow)
    }
    IncomeSource.saveIncomes(allIncomes.toList)
    
    //purge previous entry of current year/month
    if (currentCycle % 4 != 0) { //clear previous entry for current month, if currentCycle % 4 == 0, it starts a new entry, so no previous entry for the same month to clear
      IncomeSource.deleteIncomes(currentCycle - 1, Period.MONTHLY)
    }
    if (currentCycle % 52 != 0) { //clear previous entry for current year, if currentCycle % 52 == 0, it starts a new entry, so no previous entry for the same years to clear
      IncomeSource.deleteIncomes(currentCycle - 1, Period.YEARLY)
    }
    
    //purge old entries, keep 10 entries of each Period
    IncomeSource.deleteIncomesBefore(currentCycle - 10, Period.WEEKLY);
    IncomeSource.deleteIncomesBefore(currentCycle - 10 * 4, Period.MONTHLY);
    IncomeSource.deleteIncomesBefore(currentCycle - 10 * 52, Period.YEARLY);
    
    CashFlowSource.saveCashFlows(allCashFlows.toList)
    //purge previous entry of current year/month
    if (currentCycle % 4 != 0) { //clear previous entry for current month, if currentCycle % 4 == 0, it starts a new entry, so no previous entry for the same month to clear
      CashFlowSource.deleteCashFlows(currentCycle - 1, Period.MONTHLY)
    }
    if (currentCycle % 52 != 0) { //clear previous entry for current year, if currentCycle % 52 == 0, it starts a new entry, so no previous entry for the same years to clear
      CashFlowSource.deleteCashFlows(currentCycle - 1, Period.YEARLY)
    }
    
    //purge old entries, keep 10 entries of each Period
    CashFlowSource.deleteCashFlowsBefore(currentCycle - 10, Period.WEEKLY);
    CashFlowSource.deleteCashFlowsBefore(currentCycle - 10 * 4, Period.MONTHLY);
    CashFlowSource.deleteCashFlowsBefore(currentCycle - 10 * 52, Period.YEARLY);
    
    //update Oil consumption history
    OilSource.saveOilConsumptionHistory(oilConsumptionEntries.toList)
    OilSource.deleteOilConsumptionHistoryBeforeCycle(currentCycle - 10)
  }
  
//  def getChampionReputationBoost(airlineId : Int) : Double = {
//    val topChampionsByCountryCode : List[(String, List[((Int, Long), Int)])]= CountrySource.loadMarketSharesByCriteria(List()).map {
//      case CountryMarketShare(countryCode, airlineShares) => (countryCode, airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(3).zipWithIndex)
//    }
//    
//    val championedCountryByThisAirline: List[(Country, Int)] = topChampionsByCountryCode.map { //(country, ranking)
//      case (countryCode, championAirlines) => (countryCode, championAirlines.find {
//        case((championAirlineId, passengerCount), ranking) => championAirlineId == airlineId
//      })
//    }.filter {
//      case (countryCode, thisAirlineRankingOption) => thisAirlineRankingOption.isDefined
//    }.map {
//      case (countryCode, thisAirlineRankingOption) => (CountrySource.loadCountryByCode(countryCode).get, thisAirlineRankingOption.get._2 + 1)
//    }
//    
//    championedCountryByThisAirline.foldLeft(0.0) {
//      case(sum, (country, ranking)) => sum + Computation.computeReputationBoost(country, ranking)
//    }
//  }
  
  
  
  /**
   * compute monthly and yearly income 
   * 
   * Returns Updating income entries
   */
  def computeAccumulateIncome(weeklyIncome : AirlineIncome) : List[AirlineIncome] = {
    //get existing entry
    val currentWeek = MainSimulation.currentWeek
    val airlineId = weeklyIncome.airlineId
    val currentMonthIncomeOption = if (currentWeek % 4 == 0) None else IncomeSource.loadIncomeByAirline(airlineId, currentWeek - 1, Period.MONTHLY)
    
    val updatedMonthIncome = currentMonthIncomeOption match {
      case Some(income) => {
        income.update(weeklyIncome)
      }
      case None => weeklyIncome.copy(period = Period.MONTHLY)//new month
    }
    val currentYearIncomeOption = if (currentWeek % 52 == 0) None else IncomeSource.loadIncomeByAirline(airlineId, currentWeek - 1, Period.YEARLY)
    val updatedYearIncome = currentYearIncomeOption match {
      case Some(income) => {
        income.update(weeklyIncome)
      }
      case None => weeklyIncome.copy(period = Period.YEARLY)//new year
    }
    
    List[AirlineIncome](updatedMonthIncome, updatedYearIncome)
  }
  
  /**
   * compute monthly and yearly cash flow 
   * 
   * Returns Updating cash flow entries
   */
  def computeAccumulateCashFlow(weeklyCashFlow : AirlineCashFlow) : List[AirlineCashFlow] = {
    //get existing entry
    val currentWeek = MainSimulation.currentWeek
    val airlineId = weeklyCashFlow.airlineId
    val currentMonthCashFlowOption = if (currentWeek % 4 == 0) None else CashFlowSource.loadCashFlowByAirline(airlineId, currentWeek - 1, Period.MONTHLY)
    
    val updatedMonthCashFlow = currentMonthCashFlowOption match {
      case Some(cashFlow) => {
        cashFlow.update(weeklyCashFlow)
      }
      case None => weeklyCashFlow.copy(period = Period.MONTHLY)//new month
    }
    val currentYearCashFlowOption = if (currentWeek % 52 == 0) None else CashFlowSource.loadCashFlowByAirline(airlineId, currentWeek - 1, Period.YEARLY)
    val updatedYearCashFlow = currentYearCashFlowOption match {
      case Some(cashFlow) => {
        cashFlow.update(weeklyCashFlow)
      }
      case None => weeklyCashFlow.copy(period = Period.YEARLY)//new year
    }
    
    List[AirlineCashFlow](updatedMonthCashFlow, updatedYearCashFlow)
  }
  
  /**
   * Returns a tuple of (totalLoanRepayment, totalLoanInterest)
   */
  def updateLoans(airline : Airline, paymentCycle : Int) : (Long, Long) = {
    val loans = BankSource.loadLoansByAirline(airline.id)
    var totalLoanPayment = 0L
    var totalLoanInterest = 0L
    loans.foreach { loan =>
      if (loan.lastPaymentCycle >= paymentCycle) { //something wrong with sim, avoid duplicated payment
        println(s"Skipping double payment on $loan")
      } else {
        val weeklyPayment = loan.weeklyPayment
        val weeklyInterest = loan.weeklyInterest(paymentCycle)
        totalLoanPayment = totalLoanPayment + weeklyPayment
        totalLoanInterest = totalLoanInterest + weeklyInterest
      }
      if (loan.remainingTerm(paymentCycle) <= 0) {
        BankSource.deleteLoan(loan.id)
      } else {
        BankSource.updateLoanLastPayment(loan.id, paymentCycle)
      }
    }
    
    (totalLoanPayment, totalLoanInterest)
  }
  
//  def getTargetQuality(serviceFunding : Int, links : List[Link]) : Double = {
//    var totalPassengerMileCapacity = links.map { link => link.frequency * link.getAssignedModel().fold(0L)(_.capacity.toLong) * link.distance }.sum
//    val MIN_PASSENGER_MILE_CAPACITY = 1000 * 1000
//    totalPassengerMileCapacity = Math.max(totalPassengerMileCapacity, MIN_PASSENGER_MILE_CAPACITY)
//
//    getTargetQuality(serviceFunding, totalPassengerMileCapacity) //50x to get 50 target quality, 200x to get max 100 target quality
//  }
//
//  val getTargetQuality : (Int, Long) => Double = (funding : Int, totalPassengerMileCapacity : Long) => {
//    val computedQuality = Math.pow(funding.toDouble / (totalPassengerMileCapacity.toDouble / 4000) / 30, 1 / 2.5) * 40  //40x capacity (assume average 4k distance) to get 50 target quality, 200x capacity to get max 100 target quality
//    if (computedQuality >= Airline.MAX_SERVICE_QUALITY) {
//      Airline.MAX_MAINTENANCE_QUALITY
//    } else {
//      computedQuality
//    }
//  }

  def getServiceFunding(targetQuality : Int, links : List[Link]) : Long = {
    val totalPassengerMileCapacity = links.map { link => link.frequency * link.getAssignedModel().fold(0L)(_.capacity.toLong) * link.distance }.sum
    getServiceFunding(targetQuality, totalPassengerMileCapacity)
  }

  val getServiceFunding : (Int, Long) => Long = (targetQuality : Int, totalPassengerMileCapacity : Long) => {
    val MIN_PASSENGER_MILE_CAPACITY = 1000 * 1000
    val passengerMileCapacity = Math.max(totalPassengerMileCapacity, MIN_PASSENGER_MILE_CAPACITY).toDouble

    val funding = Math.pow(targetQuality.toDouble / 40, 2.5) * (passengerMileCapacity / 4000) * 30
    funding.toLong
  }
  
  val getNewQuality : (Double, Double) => Double = (currentQuality, targetQuality) =>  {
    val delta = targetQuality - currentQuality
    val adjustment = 
      if (delta >= 0) { //going up, slower when current quality is already high
        MAX_SERVICE_QUALITY_INCREMENT * (1 - (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 1x; current quality 100, multiplier 0.1x
      } else { //going down, faster when current quality is already high
        -1 * MAX_SERVICE_QUALITY_DECREMENT * (0.1 + (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 0.1x; current quality 100, multiplier 1x
      }
    if (adjustment >= 0) {
      if (adjustment + currentQuality >= targetQuality) {
        targetQuality
      } else {
        adjustment + currentQuality
      }
    } else {
      if (currentQuality + adjustment <= targetQuality) {
        targetQuality
      } else {
        currentQuality + adjustment
      }
    } 
  }
  
//  def getChampions(allAirlines : scala.collection.immutable.Map[Int, Airline], allCountries : scala.collection.immutable.Map[String, Country]) : scala.collection.immutable.Map[Airline, List[(Country, Int)]] = {
//    val champions = Map[Airline, ListBuffer[(Country, Int)]]()
//     CountrySource.loadMarketSharesByCriteria(List.empty).foreach { 
//       case CountryMarketShare(countryCode : String, airlineShares) => {
//         val championsForThisCountry = airlineShares.toList.sortBy(_._2)(Ordering[Long].reverse).take(5)
//         for (x <- 0 until championsForThisCountry.size) {
//            val airline = allAirlines((championsForThisCountry(x)._1))
//            val ranking = x + 1
//            val airlineChampionedCountries = champions.getOrElseUpdate(airline, ListBuffer[(Country, Int)]())
//            airlineChampionedCountries += ((allCountries(countryCode), ranking))
//         }
//       }
//     }
//    
//    champions.mapValues(_.toList).toMap
//  }
}