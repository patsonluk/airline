package com.patson

import scala.collection.mutable.ListBuffer
import com.patson.data._
import com.patson.model._
import com.patson.model.airplane._

import scala.collection.mutable
import scala.collection.mutable.Map
import scala.util.Random


object AirplaneSimulation {
  def airplaneSimulation(cycle: Int, links : List[Link]) : List[Airplane] = {
    println("starting airplane simulation")
    println("loading all airplanes")
    //do 2nd hand market adjustment
    secondHandAirplaneSimulate(cycle)
    
    //do decay
    val allAirplanes = AirplaneSource.loadAirplanesWithAssignedLinkByCriteria(List.empty)
    
    println("finished loading all airplanes")
    
    val updatingAirplanesListBuffer = ListBuffer[Airplane]()
    allAirplanes.groupBy { _._1.owner }.foreach {
      case (owner, airplanes) => {
        AirlineSource.loadAirlineById(owner.id, true) match {
          case Some(airline) =>
            val readyAirplanes = airplanes.filter(_._1.isReady(cycle))
            updatingAirplanesListBuffer ++= decayAirplanesByAirline(readyAirplanes, airline)
          case None => println("airline " + owner.id + " has airplanes but the airline cannot be loaded!")//invalid airline?
        }
      }
    }
    
    var updatingAirplanes = updatingAirplanesListBuffer.toList 
    AirplaneSource.updateAirplanesDetails(updatingAirplanes)
    println("Finished updating all airplanes")
    
    println("Start renewing airplanes")
    updatingAirplanes = renewAirplanes(updatingAirplanes, cycle)
    println("Finished renewing airplanes")
    
    println("Start retiring airplanes")
    removeAgingAirplaneFromLinks(links, updatingAirplanes) //need to pass the airplanes here as the airplanes in the `links` are not updated yet
    retireAgingAirplanes(updatingAirplanes.toList)
    println("Finished retiring airplanes")
    
    updatingAirplanes.toList
  }

  val SECOND_HAND_MAX_AIRPLANE_PER_MODEL_COUNT = 50 //only 50 max at a time
  def secondHandAirplaneSimulate(cycle : Int) = {
    var secondHandAirplanes = AirplaneSource.loadAirplanesCriteria(List(("is_sold", true)))
    val secondHandAirplanesByModelId = secondHandAirplanes.map(airplane => airplane.copy(dealerRatio = airplane.dealerRatio - DEALER_RATIO_DROP_RATE)).groupBy(_.model.id)
    

    val allUpdatingAirplanes = ListBuffer[Airplane]()
    val allRemovingAirplanes = ListBuffer[Airplane]()

    secondHandAirplanesByModelId.foreach {
      case(modelId, airplanesByModelId) =>
        var updatingAirplanesByModel = ListBuffer[Airplane]()
        airplanesByModelId.foreach { airplane =>
          if (airplane.dealerRatio >= DEALER_RATIO_LOWER_THRESHOLD) {
            updatingAirplanesByModel.append(airplane)
          } else {
            allRemovingAirplanes.append(airplane)
          }
        }
        if (updatingAirplanesByModel.length > SECOND_HAND_MAX_AIRPLANE_PER_MODEL_COUNT) {
          val removalCount = updatingAirplanesByModel.length - SECOND_HAND_MAX_AIRPLANE_PER_MODEL_COUNT
          updatingAirplanesByModel = Random.shuffle(updatingAirplanesByModel)
          allRemovingAirplanes.appendAll(updatingAirplanesByModel.take(removalCount))
          updatingAirplanesByModel = updatingAirplanesByModel.drop(removalCount)
        }

        allUpdatingAirplanes.appendAll(updatingAirplanesByModel)
    }

    AirplaneSource.updateAirplanesDetails(allUpdatingAirplanes.toList)
    allRemovingAirplanes.foreach { airplane =>
      AirplaneSource.deleteAirplanesByCriteria(List(("id", airplane.id), ("is_sold", true))) //need to be careful here, make sure it is still in 2nd hand market
    }

  }

  val DEALER_RATIO_DROP_RATE = 0.0025
  val DEALER_RATIO_LOWER_THRESHOLD = Computation.SELL_RATE //at this ratio, the dealer would just scrap the airplane
  
  
  def renewAirplanes(airplanes : List[Airplane], currentCycle : Int) : List[Airplane] = {
    val renewalThresholdsByAirline : scala.collection.immutable.Map[Int, Int] = AirlineSource.loadAirplaneRenewals()
    val costsByAirline : Map[Int, (Long, Long, Long, Long)] = Map[Int, (Long, Long, Long, Long)]()
    val airlinesById = AirlineSource.loadAllAirlines(false).map(airline => (airline.id, airline)).toMap
    val renewedAirplanes : ListBuffer[Airplane] = ListBuffer[Airplane]() 
    val secondHandAirplanes  = ListBuffer[Airplane]()

    val currentExplicitRenewals : scala.collection.immutable.Map[Int, Int] = AirplaneSource.loadAllExplicitRenewals()
    val updatingExplicitRenewals = mutable.HashMap[Int, Int]()
    val removingExplicitRenewals = mutable.HashSet[Int]()

    val updatingAirplanes = airplanes
      .sortBy(_.condition) //sort by - renewal lower condition ones first
      .sortWith((airplane1, airplane2) => currentExplicitRenewals.getOrElse(airplane1.id, Int.MaxValue) < currentExplicitRenewals.getOrElse(airplane2.id, Int.MaxValue)) //airplane with shortest explicit renewal get processed first
      .map { airplane => //final order. Shortest explicit renewal first than lowest condition
        val explicitRenewalOption = currentExplicitRenewals.get(airplane.id)

        val shouldRenew : Boolean =
        explicitRenewalOption match {
          case Some(remainingWeek) =>
            if (remainingWeek == -1) { //explicitly state that NO renewal
              false
            } else if (remainingWeek == 0) {
              true
            } else { //explicitly marked for renewal but not yet at the week
              false
            }
          case None => //no explicit renewal, check auto renewal
            renewalThresholdsByAirline.get(airplane.owner.id) match {
              case Some(threshold) =>
                airplane.condition < threshold && airplane.purchasedCycle <= currentCycle - airplane.model.constructionTime //only renew airplane if it has been purchased longer than the construction time required
              case None =>
                false
            }
        }

        var isRenewed = false

        var resultAirplane = airplane
        if (shouldRenew) { //then try to renew, this might fail if there's not enough cash
          val airlineId = airplane.owner.id
          val (existingCost, existingBuyPlane, existingSellPlane, existingCapitalLost) : (Long, Long, Long, Long) = costsByAirline.getOrElse(airlineId, (0, 0, 0, 0))
          val sellValue = Computation.calculateAirplaneSellValue(airplane)
          val renewCost = airplane.model.price - sellValue
          val newCost = existingCost + renewCost
          val newBuyPlane = existingBuyPlane + airplane.model.price
          val newSellPlane = existingSellPlane + sellValue

          if (newCost <= airlinesById(airplane.owner.id).getBalance()) {
            println("auto renewing " + airplane)
            val newCapitalLost = existingCapitalLost + (airplane.value - sellValue)
            costsByAirline.put(airlineId, (newCost, newBuyPlane, newSellPlane, newCapitalLost))
            if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
              secondHandAirplanes.append(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, id = 0))
            }
            val renewedAirplane = airplane.copy(constructedCycle = currentCycle, purchasedCycle = currentCycle, condition = Airplane.MAX_CONDITION, value = airplane.model.price)
            isRenewed = true
            renewedAirplanes.append(renewedAirplane)
            resultAirplane = renewedAirplane
          }
        }

        //update explicit renewals
        explicitRenewalOption.foreach { remainingWeek =>
          if (remainingWeek > 0) {
            updatingExplicitRenewals.put(airplane.id, remainingWeek - 1)
          } else if (remainingWeek == 0 && isRenewed) { //successfully renewed, remove from explicit renewals
            removingExplicitRenewals.add(airplane.id)
          }
        }
        resultAirplane
      }
    
    //now deduct money
    costsByAirline.foreach {
      case(airlineId, (cost, buyAirplane, sellAirplane, capitalLoss)) => {
        println("Deducting " + cost + " from " + airlinesById(airlineId) + " for renewal")
        AirlineSource.adjustAirlineBalance(airlineId, cost * -1)
        AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, capitalLoss * -1))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellAirplane))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, buyAirplane * -1))
      }
      
    }
    //save the 2nd hand airplanes
    AirplaneSource.saveAirplanes(secondHandAirplanes.toList)
    //save the renewed airplanes
    AirplaneSource.updateAirplanes(renewedAirplanes.toList)

    AirplaneSource.saveExplicitRenewals(updatingExplicitRenewals.toMap)
    AirplaneSource.deleteExplicitRenewals(removingExplicitRenewals.toSet)
      
    updatingAirplanes
  }
  
   def removeAgingAirplaneFromLinks(links : List[Link], airplanes : List[Airplane]) = {
    val updatingLinks = ListBuffer[Link]()
    val updatedAirplanesById = airplanes.map( airplane => (airplane.id, airplane)).toMap
    links.foreach {
      link => {
        val updatedAssignedAirplanes : List[Airplane] = link.getAssignedAirplanes().map( airplane => updatedAirplanesById.getOrElse(airplane.id, airplane)) //update the list of assigned airplanes
        
        val okAirplanes : List[Airplane] = updatedAssignedAirplanes.filter( _.condition > 0)
        
        val retiringAirplanesCount = updatedAssignedAirplanes.size - okAirplanes.size
        
        if (retiringAirplanesCount > 0) {
           println("retiring " + retiringAirplanesCount + " airplanes for link " + link)
           //now see if frequency should be reduced
           val maxFrequency = if (okAirplanes.isEmpty) 0 else { Computation.calculateMaxFrequency(okAirplanes(0).model, link.distance, airplaneCount = okAirplanes.length) }
           val updatingLink = 
             if (maxFrequency < link.frequency) {
               val capacityPerFlight = link.capacity / link.frequency
               link.copy(capacity = capacityPerFlight * maxFrequency, frequency = maxFrequency)
             } else {
               link
             }
          
           updatingLinks.append(updatingLink)
        }
      }
    }
    
    LinkSource.updateLinks(updatingLinks.toList)
  }
   
  def retireAgingAirplanes(airplanes : List[Airplane]) {
    airplanes.filter(_.condition <= 0).foreach { airplane =>
      println("Deleting airplane " + airplane)
      AirplaneSource.deleteAirplane(airplane.id)
    }
  }
  
  def computeDepreciationRate(model : Model, decayRate : Double) = {
    val depreciationRate = (model.price * (decayRate / 100)).toInt
    depreciationRate
  }
  
  def decayAirplanesByAirline(airplanesWithAssignedLink : List[(Airplane, Option[Link])], owner : Airline) : List[Airplane] = {
    val updatingAirplanes = ListBuffer[Airplane]()
    
    
    airplanesWithAssignedLink.foreach { 
      case(airplane, assignedLink) =>
        val minDecay = Airplane.MAX_CONDITION.toDouble / airplane.model.lifespan //live the whole lifespan
        val maxDecay = minDecay * 2
        val baseDecayRate = maxDecay - (maxDecay - minDecay) * (owner.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY)
        var decayRate =
          if (assignedLink.isEmpty) { //not assigned to any links, decay slower
            baseDecayRate / 3 
          } else {
            baseDecayRate
          }
        if (decayRate > airplane.condition) {
          decayRate = airplane.condition
        }
        
        val newCondition = airplane.condition - decayRate
        val depreciationRate = computeDepreciationRate(airplane.model, decayRate)
        val newValue = airplane.value - depreciationRate
        
        updatingAirplanes.append(airplane.copy(condition = newCondition, depreciationRate = depreciationRate, value = newValue))
    }
    updatingAirplanes.toList
  }
}