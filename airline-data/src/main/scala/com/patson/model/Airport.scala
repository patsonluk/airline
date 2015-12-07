package com.patson.model

import com.patson.model.airplane.Model
import com.patson.model.airplane.Model.Type

case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, var size : Int, var power : Long, var population : Long, var slots : Int, var id : Int = 0) extends IdObject {
  val citiesServed = scala.collection.mutable.MutableList[(City, Double)]()
  private[this] val airlineAppeals = scala.collection.mutable.Map[Int, AirlineAppeal]()
  private[this] var airlineAppealsLoaded = false
  private[this] val slotAssignments = scala.collection.mutable.Map[Int, Int]()
  private[this] var slotAssignmentsLoaded = false
  
  def availableSlots : Int = {
    if (slotAssignmentsLoaded) {
      slots - slotAssignments.foldLeft(0)(_ + _._2)
    } else {
      throw new IllegalStateException("airline slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
  }
  
  
  
  def addCityServed(city : City, share : Double) {
    citiesServed += Tuple2(city, share)
  }
  
  def getAirlineAppeals() : Map[Int, AirlineAppeal] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAppeals.toMap
  }
  
  def setAirlineLoyalty(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrElse(airlineId, AirlineAppeal(0, 0))
    airlineAppeals.put(airlineId, AirlineAppeal(value, oldAppeal.awareness))
  }
  def setAirlineAwareness(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrElse(airlineId, AirlineAppeal(0, 0))
    airlineAppeals.put(airlineId, AirlineAppeal(oldAppeal.loyalty, value))
  }
  def getAirlineLoyalty(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAppeals.get(airlineId).fold(0.0)(_.loyalty)
  }
  def getAirlineAwareness(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAppeals.get(airlineId).fold(0.0)(_.awareness)
  }
  
  def isAirlineAppealsInitialized = airlineAppealsLoaded
  def isSlotAssignmentsInitialized = slotAssignmentsLoaded
  
  def getAirlineSlotAssignment(airlineId : Int) = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    slotAssignments.getOrElse(airlineId, 0)
  }
  
  /**
   * Get max slots that can be assigned to this airline (including existing ones)
   */
  def getMaxSlotAssignment(airlineId : Int) : Int = {
    val reservedSlots = (slots * 0.1).toInt //airport always keep 10% spare
    val currentAssignedSlotToThisAirline = getAirlineSlotAssignment(airlineId)
    if (availableSlots < reservedSlots) { //at reserved range already...cannot assign any new slots to existing airline
      if (currentAssignedSlotToThisAirline > 0) {
        currentAssignedSlotToThisAirline
      } else if (availableSlots > 0) { //some hope for new airline...
        1
      } else { //sry all full
        0
      } 
    } else { //calculate how many can be assigned
      val maxSlotsByLoyalty = ((slots - reservedSlots) * (getAirlineLoyalty(airlineId) / AirlineAppeal.MAX_LOYALTY)).toInt //base on loyalty, at 100% get all the available (minus reserved)
      val maxSlotsByAwareness = (getAirlineAwareness(airlineId) * 5 / AirlineAppeal.MAX_AWARENESS).toInt + 1// +5 (total 6) at max awareness
      val maxSlots = Math.max(maxSlotsByLoyalty, maxSlotsByAwareness)
        
      //now see whether this new max slot would violate anything
      if (maxSlots <= currentAssignedSlotToThisAirline) { //you can keep what you have but we cannot give u more as we don't like you anymore than before
        currentAssignedSlotToThisAirline
      } else {
        var increment = maxSlots - currentAssignedSlotToThisAirline
        if (availableSlots - increment < reservedSlots) { //nah cannot assign into the reserved range
           increment = availableSlots - reservedSlots  //can give u what is left before reserved range at most   
        }
        currentAssignedSlotToThisAirline + increment
      }
    }
  }
  
  def setAirlineSlotAssignment(airlineId : Int, value : Int) = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    val maxAssignment = getMaxSlotAssignment(airlineId)
    if (value > maxAssignment) {
      throw new IllegalArgumentException("Cannot assign that many slots to this airline!") 
    }
    slotAssignments.put(airlineId, value)
  }
  
  def getAirlineSlotAssignments() : Map[Int, Int] = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    slotAssignments.toMap
  }
  
  
  def initAirlineAppeals(airlineAppeals : Map[Int, AirlineAppeal]) = {
    this.airlineAppeals.clear()
    this.airlineAppeals ++= airlineAppeals
    airlineAppealsLoaded = true
  }
  def initSlotAssignments(slotAssignments : Map[Int, Int]) = {
    this.slotAssignments.clear()
    this.slotAssignments ++= slotAssignments
    slotAssignmentsLoaded = true
  }
  
  //fixed fee for all airplane type
  val slotFee : Int = { 
    size match {
      case 1 => 50 //small
      case 2 => 50 //medium
      case 3 => 50 //large
      case 4 => 300  //international class
      case 5 => 500
      case 6 => 700 
      case _ => 1000 //mega airports - not suitable for tiny jets
    }
  }
  
  def landingFee(airplaneModel : Model) : Int = {
    val perSeat = 
      if (size <= 3) {
        2
      } else if (size == 4) {
        4
      } else {
        8
      }
    
    airplaneModel.capacity * perSeat
  }
}

case class AirlineAppeal(loyalty : Double, awareness : Double)
object AirlineAppeal {
  val MAX_LOYALTY = 100
  val MAX_AWARENESS = 100
}

object Airport {
  def fromId(id : Int) = {
    val airportWithJustId = Airport("", "", "", 0, 0, "", "", 0, 0, 0, 0, 0)
    airportWithJustId.id = id
    airportWithJustId
  }
}

case class Runway(length : Int, runwayType : RunwayType.Value)

object RunwayType extends Enumeration {
    type RunwayType = Value
    val Asphalt, Concrete, Gravel = Value
}


