package com.patson

import com.patson.data._
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model.Type.{JUMBO, LARGE, LIGHT, MEDIUM, REGIONAL, SMALL, X_LARGE, SUPERSONIC}
import com.patson.model.airplane._

import scala.collection.mutable.ListBuffer


object AirplaneModelSimulation {



  def simulate(cycle: Int) = {
    println("starting airplane model simulation (discounts)")
    println("loading all airplanes")
    val allAirplanes = AirplaneSource.loadAirplanesCriteria(List.empty)
    simulateModelDiscounts(allAirplanes)
    println("Finished airplane model simulation")
  }


  def simulateModelDiscounts(allAirplanes: List[Airplane]) = {
    //simulate low demand
    //purge all the existing discounts due to low demand
    val airplanesByModel = allAirplanes.groupBy(_.model)
    val allModelDiscounts = ListBuffer[ModelDiscount]()
    ModelSource.loadAllModels().foreach { model =>
      allModelDiscounts.appendAll(getModelDiscountsByLowDemand(model, airplanesByModel.get(model) match {
        case Some(airplanes) => airplanes.length
        case None => 0
      }))
    }


    ModelSource.updateModelDiscounts(allModelDiscounts.toList)
  }

  def getModelDiscountsByLowDemand(model : Model, airplaneCount : Int) = {
    val threshold = getModelLowDemandDiscountThreshold(model)
    val thresholdDelta = threshold - airplaneCount
    val discounts = ListBuffer[ModelDiscount]()
    if (thresholdDelta > 0) { //then some discounts
      val priceDiscountPercentage = thresholdDelta * MAX_PRICE_DISCOUNT_PERCENTAGE / threshold
      if (priceDiscountPercentage > 0) {
        discounts.append(ModelDiscount(model.id, priceDiscountPercentage * 0.01, DiscountType.PRICE, DiscountReason.LOW_DEMAND, None))
      }
      discounts.append(ModelDiscount(model.id, CONSTRUCTION_TIME_DISCOUNT, DiscountType.CONSTRUCTION_TIME, DiscountReason.LOW_DEMAND, None))
    }
    discounts.toList
  }

  val MAX_PRICE_DISCOUNT_PERCENTAGE = 50
  val CONSTRUCTION_TIME_DISCOUNT = 0.5 //half the construction time

  val getModelLowDemandDiscountThreshold = (model: Model) => { //smaller model has higher threshold. as the volume is supposed to be higher
    model.airplaneType match {
      case LIGHT => 300
      case REGIONAL => 300
      case SMALL => 300
      case MEDIUM => 200
      case LARGE => 150
      case X_LARGE => 100
      case JUMBO => 50
      case SUPERSONIC => 100
    }
  }

}