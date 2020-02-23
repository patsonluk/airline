package com.patson

import com.patson.data._
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model.Type.{JUMBO, LARGE, LIGHT, MEDIUM, REGIONAL, SMALL, X_LARGE}
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
//    airplanesByModel.foreach {
//      case (model, airplanes) =>
//        allModelDiscounts.appendAll(getModelDiscountsByAirplaneCount(model, airplanes.length))
//    }
    ModelSource.loadAllModels().foreach { model =>
      allModelDiscounts.appendAll(getModelDiscountsByAirplaneCount(model, airplanesByModel.get(model) match {
        case Some(airplanes) => airplanes.length
        case None => 0
      }))
    }


    ModelSource.updateModelDiscounts(allModelDiscounts.toList)
  }

  def getModelDiscountsByAirplaneCount(model : Model, airplaneCount : Int) = {
    val threshold = getModelDiscountThreshold(model)
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

  val MAX_PRICE_DISCOUNT_PERCENTAGE = 20
  val CONSTRUCTION_TIME_DISCOUNT = 0.5 //half the construction time

  val getModelDiscountThreshold = (model: Model) => { //smaller model has higher threshold. as the volume is supposed to be higher
    model.airplaneType match {
      case LIGHT => 2000
      case REGIONAL => 1000
      case SMALL => 800
      case MEDIUM => 600
      case LARGE => 400
      case X_LARGE => 200
      case JUMBO => 100
    }
  }

}