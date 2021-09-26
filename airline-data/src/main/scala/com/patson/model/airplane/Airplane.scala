package com.patson.model.airplane

import com.patson.data.{AirplaneSource, LinkSource}
import com.patson.model.{Airline, Airport, Computation, IdObject, Link, LinkClassValues}

case class Airplane(model : Model, var owner : Airline, constructedCycle : Int, var purchasedCycle : Int, condition : Double, depreciationRate : Int, value : Int, var isSold : Boolean = false, var dealerRatio : Double = Airplane.DEFAULT_DEALER_RATIO, var configuration : AirplaneConfiguration = AirplaneConfiguration.empty, var home : Airport = Airport.fromId(0), isReady : Boolean = true, var purchaseRate : Double = 1, version : Int = 0,var id : Int = 0) extends IdObject {
  //val isReady = (currentCycle : Int) => currentCycle >= constructedCycle && !isSold
  val dealerValue = {
    (value * dealerRatio).toInt
  }
  
  def sellToDealer() = {
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = true
    configuration = AirplaneConfiguration.empty
    home = Airport.fromId(0)
  }
  
  def buyFromDealer(airline : Airline, currentCycle : Int) = {
    owner = airline
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = false

    purchasedCycle = currentCycle
    home = airline.getHeadQuarter().get.airport

    assignDefaultConfiguration()
  }

  def assignDefaultConfiguration() = {
    val configurationOptions = AirplaneSource.loadAirplaneConfigurationsByCriteria(List(("airline", owner.id), ("model", model.id)))
    val pickedConfiguration =
      if (configurationOptions.isEmpty) { //create one for this airline
        val newConfiguration = AirplaneConfiguration.default(owner, model)
        AirplaneSource.saveAirplaneConfigurations(List(newConfiguration))
        newConfiguration
      } else {
        configurationOptions(0) //just get the first one
      }
    configuration = pickedConfiguration
  }

  lazy val availableFlightMinutes : Int = {
    val occupiedFlightMinutes = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(id).assignments.toList.map(_._2.flightMinutes).sum

    Airplane.MAX_FLIGHT_MINUTES - occupiedFlightMinutes
  }

  lazy val utilizationRate : Double = {
    testUtilizationRate match {
      case Some(rate) => rate
      case None => (Airplane.MAX_FLIGHT_MINUTES - availableFlightMinutes).toDouble / Airplane.MAX_FLIGHT_MINUTES
    }
  }

  var testUtilizationRate : Option[Double] = None
  def setTestUtilizationRate(rate : Double): Unit = {
    testUtilizationRate = Some(rate)
  }

//  lazy val remainingFlightHour = usableFlightHour - linkAssignments.map {
//    case ((link, hours)) => hours
//  }.sum

//  private[this] var linkAssignments : Map[Link, Int] = Map.empty
//  def setLinkAssignments(linkAssignments : Map[Link, Int]) = this.linkAssignments = linkAssignments
//  def getLinkAssignments() = linkAssignments
}

object Airplane {
  val MAX_CONDITION = 100
  val BAD_CONDITION = 40
  val CRITICAL_CONDITION = 20
  val DEFAULT_DEALER_RATIO = 1.2
  val MAX_FLIGHT_MINUTES : Int = (24 * 60 * 4).toInt
}