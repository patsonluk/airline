package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirlineSimulation {
  private val AIRLINE_FIXED_COST = 0 //for now...
  private val REPUTATION_INCREMENT = 0.5 
  private[patson] val MAX_SERVICE_QUALITY_INCREMENT : Double = 1
  
  def airlineSimulation(cycle: Int, linkResult : List[LinkConsumptionDetails]) = {
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    val allLinks = LinkSource.loadAllLinks(LinkSource.ID_LOAD).groupBy { _.airline.id }
    val allTransactions = AirlineSource.loadTransactions(cycle).groupBy { _.airlineId }
    //purge the older transactions
    AirlineSource.deleteTransactions(cycle - 1)
    val linkResultByAirline = linkResult.groupBy { _.airlineId }
    allAirlines.foreach { airline =>
        val linksBalance = linkResultByAirline.get(airline.id) match { 
          case Some(linkConsumptions) => {
            val linksProfit = linkConsumptions.foldLeft(0L)(_ + _.profit)
            val linksAirportFee = linkConsumptions.foldLeft(0L)(_ + _.airportFees)
            val linksCrewCost = linkConsumptions.foldLeft(0L)(_ + _.crewCost)
            val linksDepreciation = linkConsumptions.foldLeft(0L)(_ + _.depreciation)
            val linksFuelCost = linkConsumptions.foldLeft(0L)(_ + _.fuelCost)
            val linksInflightCost = linkConsumptions.foldLeft(0L)(_ + _.inflightCost)
            val linksMaintenanceCost = linkConsumptions.foldLeft(0L)(_ + _.maintenanceCost)
            val linksRevenue = linkConsumptions.foldLeft(0L)(_ + _.revenue)
            val linksExpense = linksAirportFee + linksCrewCost + linksDepreciation + linksFuelCost + linksInflightCost + linksMaintenanceCost
            LinksBalanceData(airline.id, profit = linksProfit, revenue = linksRevenue, expense = linksExpense, ticketRevenue = linksRevenue, airportFee = linksAirportFee, fuelCost = linksFuelCost, crewCost = linksCrewCost, depreciation = linksDepreciation, inflightCost = linksInflightCost, maintenanceCost= linksMaintenanceCost)
          }
          case None => LinksBalanceData(airline.id, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0)
        }
        
        val transactionsBalance = allTransactions.get(airline.id) match {
          case Some(transactions) => {
            var expense = 0L
            var revenue = 0L
            val summary = Map[TransactionType.Value, Long]()
            transactions.foreach { transaction =>
              if (transaction.amount >= 0) { 
                revenue += transaction.amount
              } else {
                expense += transaction.amount
              }
              
              val existingAmount = summary.getOrElse(transaction.transactionType, 0L)
              summary.put(transaction.transactionType, existingAmount + transaction.amount)
            }
            TransactionsBalance(airline.id, revenue - expense, revenue, expense, summary.toMap)
          }
          case None => TransactionsBalance(airline.id, 0, 0, 0, scala.collection.immutable.Map.empty)
        }
        
        
        val othersSummary = Map[OtherBalanceItemType.Value, Long]()
        othersSummary.put(OtherBalanceItemType.SERVICE_INVESTMENT, airline.getServiceFunding() * -1)
        othersSummary.put(OtherBalanceItemType.BASE_UPKEEP, airline.bases.foldLeft(0L)((upkeep, base) => {
          upkeep - (if (base.headquarter) 100000 else 50000)
        }))
        
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
        
        val othersBalance = OthersBalance(airline.id, othersRevenue - othersExpense, othersRevenue, othersExpense, othersSummary.toMap)
        
        val airlineRevenue = linksBalance.revenue + transactionsBalance.revenue + othersBalance.revenue
        val airlineExpense = linksBalance.expense + transactionsBalance.expense + othersBalance.expense
        val airlineProfit = airlineRevenue - airlineExpense
        val airlineBalance = AirlineBalanceData(airline.id, airlineProfit, airlineRevenue, airlineExpense, linksBalance, transactionsBalance, othersBalance)
        

        airline.setBalance(airline.getBalance() + linksBalance.profit + othersBalance.profit) //do NOT use airlineProfit here directly as transactionProfit has already been updated immediately back then
          
        //update reputation
        linkResultByAirline.get(airline.id).foreach { linkConsumptions =>
          val totalPassengerKilometers = linkConsumptions.foldLeft(0L) { (foldLong, linkConsumption) =>
            foldLong + linkConsumption.soldSeats.total * linkConsumption.distance
          }
          
          //https://en.wikipedia.org/wiki/World%27s_largest_airlines
          var targetReputation = Math.log(totalPassengerKilometers / 5000) / Math.log(1.2)
          if (targetReputation > Airline.MAX_REPUTATION) {
            targetReputation = Airline.MAX_REPUTATION
          } else if (targetReputation < 10) {
            targetReputation = 10
          }
          
          val currentReputation = airline.getReputation() 
          if (currentReputation < targetReputation) {
            if (currentReputation + REPUTATION_INCREMENT >= targetReputation) {
              airline.setReputation(targetReputation)  
            } else {
              airline.setReputation(currentReputation + REPUTATION_INCREMENT)
            }
          }
        }
        
        //calculate service quality
        allLinks.get(airline.id).foreach {  links =>
          
           val totalCapacity = links.map { _.capacity.total }.sum
           if (totalCapacity > 0) {
             val targetServiceQuality = getTargetQuality(airline.getServiceFunding(), totalCapacity) //50x to get 50 target quality, 200x to get max 100 target quality
             val currentServiceQuality = airline.getServiceQuality()
             airline.setServiceQuality(getNewQuality(currentServiceQuality, targetServiceQuality)) 
           } 
        }
        
        
        
        println(airline + " profit is: " + airlineProfit + " new balance is " + airline.getBalance() + " reputation " +  airline.getReputation())
    }
    
    AirlineSource.saveAirlineInfo(allAirlines)
  }
  
  val getTargetQuality : (Int, Int) => Double = (funding : Int, capacity :Int) => {
    val computedQuality = Math.sqrt(funding.toDouble / capacity / 50 ) * 50  //50x capacity to get 50 target quality, 200x capacity to get max 100 target quality
    if (computedQuality >= Airline.MAX_SERVICE_QUALITY) {
      Airline.MAX_MAINTENANCE_QUALITY
    } else {
      computedQuality
    }
  }
  val getNewQuality : (Double, Double) => Double = (currentQuality, targetQuality) =>  {
    val delta = targetQuality - currentQuality
    val adjustment = 
      if (delta >= 0) { //going up, slower when current quality is already high
        MAX_SERVICE_QUALITY_INCREMENT * (1 - (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 1x; current quality 100, multiplier 0.1x
      } else { //going down, faster when current quality is already high
        -1 * MAX_SERVICE_QUALITY_INCREMENT * (0.1 + (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 0.1x; current quality 100, multiplier 1x
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
}