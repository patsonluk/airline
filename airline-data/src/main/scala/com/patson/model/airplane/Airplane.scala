package com.patson.model.airplane

import com.patson.model.{Airline, IdObject, Link, LinkClassValues}

case class Airplane(model : Model, var owner : Airline, constructedCycle : Int, var purchasedCycle : Int, condition : Double, depreciationRate : Int, value : Int, var isSold : Boolean = false, var dealerRatio : Double = Airplane.DEFAULT_DEALER_RATIO, availableFlightMinutes : Int = Airplane.MAX_FLIGHT_MINUTES, configuration : LinkClassValues = LinkClassValues.getInstance(), var id : Int = 0) extends IdObject {
  val isReady = (currentCycle : Int) => currentCycle >= constructedCycle && !isSold
  val dealerValue = {
    (value * dealerRatio).toInt
  }
  
  def sellToDealer() = {
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = true
  }
  
  def buyFromDealer(airline : Airline, currentCycle : Int) = {
    owner = airline
    dealerRatio = Airplane.DEFAULT_DEALER_RATIO
    isSold = false
    purchasedCycle = currentCycle
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
  val MAX_FLIGHT_MINUTES : Int = (24 * 60 * 3.5).toInt //TODO make it a bit higher ...right now assume each airplane can fly for 3.5 days per week
}