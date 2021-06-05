package com.patson.model

import java.util.concurrent.ConcurrentHashMap

abstract class Transport extends IdObject{
  var id : Int
  val from, to : Airport
  val airline: Airline
  val distance : Int
  var capacity: LinkClassValues
  val duration : Int
  var frequency : Int
  val transportType : TransportType.Value
  val price: LinkClassValues
  val flightType : FlightType.Value
  var availableSeats : LinkClassValues = capacity.copy()
  var minorDelayCount : Int
  var majorDelayCount : Int
  var cancellationCount : Int

  @volatile var soldSeats : LinkClassValues = LinkClassValues.getInstance()
  @volatile var cancelledSeats :  LinkClassValues = LinkClassValues.getInstance()
  private val standardPrice : ConcurrentHashMap[LinkClass, Int] = new ConcurrentHashMap[LinkClass, Int]()

  val cost: LinkClassValues //the cost of taking this transport, could just be the price, or with the hidden cost of taking it

  def computedQuality() : Int
  def getTotalCapacity : Int = {
    capacity.total
  }

  def getTotalAvailableSeats : Int = {
    availableSeats.total
  }

  def getTotalSoldSeats : Int = {
    soldSeats.total
  }


  def addSoldSeats(soldSeats : LinkClassValues) = {
    this.soldSeats = this.soldSeats + soldSeats;
    this.availableSeats = this.availableSeats - soldSeats;
  }

  def addSoldSeatsByClass(linkClass : LinkClass, soldSeats : Int) = {
    val soldSeatsClassValues = LinkClassValues.getInstanceByMap(Map(linkClass -> soldSeats))
    addSoldSeats(soldSeatsClassValues)
  }

  def addCancelledSeats(cancelledSeats : LinkClassValues) = {
    this.cancelledSeats = this.cancelledSeats + cancelledSeats;
    this.availableSeats = this.availableSeats - cancelledSeats;
  }

  def standardPrice(linkClass : LinkClass) : Int = {
    var price = standardPrice.get(linkClass)
    if (price == null.asInstanceOf[Int]) {
      price = Pricing.computeStandardPrice(distance, flightType, linkClass)
      standardPrice.put(linkClass, price)
    }
    price
  }

  import FlightType._
  /**
    * Find seats at or below the requestedLinkClass
    *
    * Returns the tuple of the matching class and seats available for that class
    */
  def availableSeatsAtOrBelowClass(targetLinkClass : LinkClass) : Option[(LinkClass, Int)] = {
    if (targetLinkClass == ECONOMY) {
      if (availableSeats(ECONOMY) > 0) {
        return Some(ECONOMY, availableSeats(ECONOMY))
      }
    } else {
      if (availableSeats(targetLinkClass) > 0) {
        return Some(targetLinkClass, availableSeats(targetLinkClass))
      } else  {
        if (targetLinkClass.level > ECONOMY.level) {
          val classDiff = flightType match {
            case SHORT_HAUL_DOMESTIC | SHORT_HAUL_INTERCONTINENTAL | SHORT_HAUL_INTERNATIONAL => targetLinkClass.level - 1//accept all classes
            case _ => 1
          }
          val lowestAcceptableLevel = targetLinkClass.level - classDiff
          var level = targetLinkClass.level - 1

          while (level >= lowestAcceptableLevel) {
            val lowerClass = LinkClass.fromLevel(level)
            val seatsAvailable = availableSeats(lowerClass)
            if (seatsAvailable > 0) {
              return Some(lowerClass, seatsAvailable)
            }
            level -= 1
          }
        }
      }
    }

    return None
  }
}

object TransportType extends Enumeration {
  type TransportType = Value
  val FLIGHT, SHUTTLE = Value
}


