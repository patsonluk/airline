package com.patson

import scala.collection.mutable.ListBuffer
import com.patson.data._
import com.patson.data.airplane.ModelSource
import com.patson.model._
import com.patson.model.airplane._
import com.patson.util.AirlineCache

import scala.collection.mutable
import scala.util.Random


object AirplaneSimulation {
  def airplaneSimulation(cycle: Int) : List[Airplane] = {
    println("starting airplane simulation")
    println("loading all airplanes")
    //do 2nd hand market adjustment
    secondHandAirplaneSimulate(cycle)
    
    //do decay
    val allAirplanes = AirplaneSource.loadAirplanesCriteria(List.empty)
    val linkAssignments: Map[Int, LinkAssignments] = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    
    println("finished loading all airplanes")
    
    val updatingAirplanesListBuffer = ListBuffer[Airplane]()
    allAirplanes.groupBy { _.owner }.foreach {
      case (owner, airplanes) => {
        AirlineCache.getAirline(owner.id, true) match {
          case Some(airline) =>
            val readyAirplanes = airplanes.filter(_.isReady)
            val readyAirplanesWithAssignedLinks : Map[Airplane, LinkAssignments] = readyAirplanes.map { airplane =>
              (airplane, linkAssignments.getOrElse(airplane.id, LinkAssignments(Map.empty)))
            }.toMap
            updatingAirplanesListBuffer ++= decayAirplanesByAirline(readyAirplanesWithAssignedLinks, airline)
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
    //adjustLinksBasedOnAirplaneStatus(updatingAirplanes, cycle)
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
          if (airplane.dealerRatio >= DEALER_RATIO_LOWER_THERSHOLD) {
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
      AirplaneSource.deleteAirplanesByCriteria(List(("a.id", airplane.id), ("is_sold", true))) //need to be careful here, make sure it is still in 2nd hand market
    }
    
  }
  
  val DEALER_RATIO_DROP_RATE = 0.0025
  val DEALER_RATIO_LOWER_THERSHOLD = Computation.SELL_RATE //at this ratio, the dealer would just scrap the airplane
  
  
  def renewAirplanes(airplanes : List[Airplane], currentCycle : Int) : List[Airplane] = {
    val renewalThresholdsByAirline : scala.collection.immutable.Map[Int, Int] = AirlineSource.loadAirplaneRenewals()
    val costsByAirline : scala.collection.mutable.Map[Int, (Long, Long, Long, Long)] = mutable.HashMap[Int, (Long, Long, Long, Long)]()
    val airlinesByid = AirlineSource.loadAllAirlines(false).map(airline => (airline.id, airline)).toMap
    val renewedAirplanes : ListBuffer[Airplane] = ListBuffer[Airplane]() 
    val secondHandAirplanes  = ListBuffer[Airplane]()
    val fundsExhaustedAirlineIds = mutable.HashSet[Int]()

    val discountsByAirlineId = ModelSource.loadAllAirlineDiscounts().view.mapValues(_.groupBy(_.modelId))
    val discountsByModelId = ModelSource.loadAllModelDiscounts().groupBy(_.modelId)

    val updatingAirplanes = airplanes //this contains airplanes from all airlines
        .sortBy(_.condition) //lowest conditional airplane gets renewal first
        .map { airplane =>
      renewalThresholdsByAirline.get(airplane.owner.id) match {
        case Some(threshold) =>
          if (!fundsExhaustedAirlineIds.contains(airplane.owner.id)
            && airplane.condition < threshold
            && airplane.purchasedCycle <= currentCycle - airplane.model.constructionTime) { //only renew airplane if it has been purchased longer than the construction time required
             val airlineId = airplane.owner.id
             val (existingCost, existingBuyPlane, existingSellPlane, existingCapitalGain) : (Long, Long, Long, Long) = costsByAirline.getOrElse(airlineId, (0, 0, 0, 0))
             val sellValue = Computation.calculateAirplaneSellValue(airplane)

             val originalModel = airplane.model

             val discounts = ListBuffer[ModelDiscount]()
             discountsByAirlineId.get(airlineId).foreach { airlineDiscountsByModelId => //airline specific discounts
               airlineDiscountsByModelId.get(originalModel.id).foreach { airlineDiscounts =>
                 discounts.appendAll(airlineDiscounts)
               }
             }
             discountsByModelId.get(originalModel.id).foreach { modelDiscounts =>
               discounts.appendAll(modelDiscounts)
             }

             val adjustedModel = originalModel.applyDiscount(discounts.toList)
             val renewCost = adjustedModel.price - sellValue
             val newCost = existingCost + renewCost
             val newBuyPlane = existingBuyPlane + adjustedModel.price
             val newSellPlane = existingSellPlane + sellValue

             if (newCost <= airlinesByid(airplane.owner.id).getBalance()) {
               println("auto renewing " + airplane)
               val lossOnSelling = sellValue - airplane.value
               val gainOnDiscount = adjustedModel.price - originalModel.price
               val newCapitalGain = existingCapitalGain + lossOnSelling + gainOnDiscount
               costsByAirline.put(airlineId, (newCost, newBuyPlane, newSellPlane, newCapitalGain))
               if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
                 secondHandAirplanes.append(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, configuration = AirplaneConfiguration.empty, id = 0))
               }
               val renewedAirplane = airplane.copy(constructedCycle = currentCycle, purchasedCycle = currentCycle, condition = Airplane.MAX_CONDITION, value = airplane.model.price)
               renewedAirplanes.append(renewedAirplane)
               renewedAirplane
             } else { //not enough fund
               fundsExhaustedAirlineIds.add(airplane.owner.id) //funds exhausted, do not attempt to renew more airplanes for this airline, otherwise it might never have enough money for the worst condition one
               airplane
             }
          } else {
            airplane
          }
        case None => airplane
      }
    }
    
    //now deduct money
    costsByAirline.foreach {
      case(airlineId, (cost, buyAirplane, sellAirplane, capitalGain)) => {
        println("Deducting " + cost + " from " + airlinesByid(airlineId) + " for renewal")
        AirlineSource.adjustAirlineBalance(airlineId, cost * -1)
        AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, capitalGain))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellAirplane))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, buyAirplane * -1))
      }
      
    }
    //save the 2nd hand airplanes
    AirplaneSource.saveAirplanes(secondHandAirplanes.toList)
    //save the renewed airplanes
    AirplaneSource.updateAirplanes(renewedAirplanes.toList)
      
    updatingAirplanes
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
  
  def decayAirplanesByAirline(airplanesWithAssignedLink : Map[Airplane, LinkAssignments], owner : Airline) : List[Airplane] = {
    val updatingAirplanes = ListBuffer[Airplane]()
    
    
    airplanesWithAssignedLink.foreach { 
      case(airplane, linkAssignments) =>
        //val minDecay = Airplane.MAX_CONDITION.toDouble / airplane.model.lifespan //live the whole lifespan
        //val maxDecay = minDecay * 2
        //val baseDecayRate = maxDecay - (maxDecay - minDecay) * (owner.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY)
        val baseDecayRate = Airplane.MAX_CONDITION.toDouble / airplane.model.lifespan //live the whole lifespan

        val decayRate = baseDecayRate / 3 + baseDecayRate * (2.0 / 3) * airplane.utilizationRate

        val newCondition = airplane.condition - decayRate
        val depreciationRate = computeDepreciationRate(airplane.model, decayRate)
        val newValue = airplane.value - depreciationRate
        
        updatingAirplanes.append(airplane.copy(condition = newCondition, depreciationRate = depreciationRate, value = newValue))
    }
    updatingAirplanes.toList
  }
}