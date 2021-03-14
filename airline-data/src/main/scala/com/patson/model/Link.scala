package com.patson.model

import scala.collection.mutable.HashMap
import java.util.concurrent.ConcurrentHashMap
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.airplane.{Airplane, LinkAssignment, Model}
import com.patson.util.AirportCache
import FlightType._
import com.patson.model

/**
 * 
 * Frequency sum of all assigned plane
 */
case class Link(from : Airport, to : Airport, airline: Airline, price : LinkClassValues, distance : Int, var capacity: LinkClassValues, rawQuality : Int, duration : Int, var frequency : Int, flightType : FlightType.Value, var flightNumber : Int = 0, var id : Int = 0) extends Transport {
  override val transportType = TransportType.FLIGHT
  override val cost = price
  @volatile override var cancellationCount = 0
  @volatile override var majorDelayCount = 0
  @volatile override var minorDelayCount = 0
  @volatile private var assignedAirplanes : Map[Airplane, LinkAssignment] = Map.empty
  @volatile private var assignedModel : Option[Model] = None
  
  @volatile private var hasComputedQuality = false
  @volatile private var computedQualityStore : Int = 0

  var inServiceAirplanes : Map[Airplane, LinkAssignment] = Map.empty

  def setAssignedAirplanes(assignedAirplanes : Map[Airplane, LinkAssignment]) = {
    this.assignedAirplanes = assignedAirplanes
    if (!assignedAirplanes.isEmpty) {
      assignedModel = Some(assignedAirplanes.toList(0)._1.model)
    }
    inServiceAirplanes = this.assignedAirplanes.filter(_._1.isReady)
    recomputeCapacityAndFrequency()
  }

  /**
    * for testing only. would not recompute frequency and capacity
    * @param assignedAirplanes
    */
  def setTestingAssignedAirplanes(assignedAirplanes : Map[Airplane, Int]) = {
    this.assignedAirplanes = assignedAirplanes.toList.map {
      case (airplane, frequency) => (airplane, LinkAssignment(frequency, Computation.calculateFlightMinutesRequired(airplane.model, distance)))
    }.toMap

    if (!assignedAirplanes.isEmpty) {
      assignedModel = Some(assignedAirplanes.toList(0)._1.model)
    }
    inServiceAirplanes = this.assignedAirplanes.filter(_._1.isReady)
  }
  
  def getAssignedAirplanes() = {
    assignedAirplanes
  }
  
  def getAssignedModel() : Option[Model] = {
    assignedModel
  }

  def setAssignedModel(model : Model) = {
    this.assignedModel = Some(model)
  }

  override def computedQuality : Int= {
    if (!hasComputedQuality) {

      if (inServiceAirplanes.isEmpty) {
        0
      } else {
        val airplaneConditionQuality = inServiceAirplanes.toList.map {
          case ((airplane, assignmentPerAirplane)) => airplane.condition / Airplane.MAX_CONDITION * assignmentPerAirplane.frequency
        }.sum / frequency * 20
        computedQualityStore = (rawQuality.toDouble / Link.MAX_QUALITY * 30 + airline.airlineInfo.currentServiceQuality / Airline.MAX_SERVICE_QUALITY * 50 + airplaneConditionQuality).toInt
//        println("computed quality " + computedQualityStore)
        hasComputedQuality = true
        computedQualityStore
      }
    } else {
      computedQualityStore
    }
  }

  def setQuality(quality : Int) = {
    computedQualityStore = quality
    hasComputedQuality = true
  }

  def capacityPerFlight() = {
    if (frequency > 0) {
      capacity / frequency
    } else { //error 
      LinkClassValues.getInstance()
    }
  }

  lazy val getFutureOfficeStaffRequired : Int = {
    getOfficeStaffRequired(from, to, futureFrequency(), futureCapacity())
  }

  lazy val getFutureOfficeStaffBreakdown : StaffBreakdown = {
    getOfficeStaffBreakdown(from, to, futureFrequency(), futureCapacity())
  }

  lazy val getCurrentOfficeStaffRequired : Int = {
    getOfficeStaffRequired(from, to, frequency, capacity)
  }



  /**
    * Recomputes capacity base on assigned airplanes
    */
  private def recomputeCapacityAndFrequency() = {
    var newCapacity = LinkClassValues.getInstance()
    var newFrequency = 0
    inServiceAirplanes.foreach {
      case(airplane, assignment) =>
        newCapacity = newCapacity + (LinkClassValues(airplane.configuration.economyVal, airplane.configuration.businessVal, airplane.configuration.firstVal) * assignment.frequency)
        newFrequency += assignment.frequency
    }
    capacity = newCapacity
    frequency = newFrequency
  }

  def futureCapacity() = {
    var futureCapacity = LinkClassValues.getInstance()
    assignedAirplanes.foreach {
      case(airplane, assignment) => futureCapacity = futureCapacity + (LinkClassValues(airplane.configuration.economyVal, airplane.configuration.businessVal, airplane.configuration.firstVal) * assignment.frequency)
    }
    futureCapacity
  }

  def futureFrequency() = {
    assignedAirplanes.values.map(_.frequency).sum
  }
  
  override def toString() = {
    s"$id; ${airline.name}; ${from.city}(${from.iata}) => ${to.city}(${to.iata}); distance $distance; freq $frequency; capacity $capacity; price $price"
  }

  lazy val schedule : Seq[TimeSlot] = Scheduling.getLinkSchedule(this)

  lazy val getOfficeStaffRequired = (from : Airport, to : Airport, frequency : Int, capacity : LinkClassValues) => {
    getOfficeStaffBreakdown(from, to, frequency, capacity).total
  }

  lazy val getOfficeStaffBreakdown = (from : Airport, to : Airport, frequency : Int, capacity : LinkClassValues) => {
    val flightType = Computation.getFlightType(from, to)

    val airlineBaseModifier : Double = AirportCache.getAirport(from.id, true).get.getAirlineBase(airline.id).map(_.getStaffModifier(FlightType.getCategory(flightType))).getOrElse(1)
    if (frequency == 0) { //future flights
      StaffBreakdown(0, 0, 0, airlineBaseModifier)
    } else {
      val StaffSchemeBreakdown(basicStaff, perFrequencyStaff, per1000PaxStaff) = Link.staffScheme(flightType)
      StaffBreakdown(basicStaff, perFrequencyStaff * frequency, per1000PaxStaff * capacity.total / 1000, airlineBaseModifier)
    }
  }
}

object Link {
  val MAX_QUALITY = 100
  val HIGH_FREQUENCY_THRESHOLD = 14
  val LINK_NEGOTIATION_COOL_DOWN = 6
  def fromId(id : Int) : Link = {
    Link(from = Airport.fromId(0), to = Airport.fromId(0), Airline.fromId(0), price = LinkClassValues.getInstance(), distance = 0, capacity = LinkClassValues.getInstance(), rawQuality = 0, duration = 0, frequency = 0, flightType = FlightType.SHORT_HAUL_DOMESTIC, id = id)
  }
  
   //adjust by quality
//  import FlightType._
//  val neutralQualityOfClass = (linkClass : LinkClass, from : Airport, to : Airport, flightType : FlightType.Value) => {
//    val linkClassMultiplier = linkClass.level - 1
//    flightType match {
//      case SHORT_HAUL_DOMESTIC => 30 + linkClassMultiplier * 15
//      case SHORT_HAUL_INTERNATIONAL => 35 + linkClassMultiplier * 15
//      case SHORT_HAUL_INTERCONTINENTAL => 40 + linkClassMultiplier * 15
//      case LONG_HAUL_DOMESTIC => 45 + linkClassMultiplier * 15
//      case LONG_HAUL_INTERNATIONAL => 50 + linkClassMultiplier * 15
//      case LONG_HAUL_INTERCONTINENTAL => 55 + linkClassMultiplier * 15
//      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 60 + linkClassMultiplier * 15
//    }
//  }
  val staffScheme : Map[model.FlightType.Value, StaffSchemeBreakdown] = {
      val basicLookup = Map(
        SHORT_HAUL_DOMESTIC -> 8,
        MEDIUM_HAUL_DOMESTIC -> 10,
        LONG_HAUL_DOMESTIC -> 12,
        SHORT_HAUL_INTERNATIONAL -> 10,
        MEDIUM_HAUL_INTERNATIONAL -> 15,
        LONG_HAUL_INTERNATIONAL -> 20,
        SHORT_HAUL_INTERCONTINENTAL -> 15,
        MEDIUM_HAUL_INTERCONTINENTAL -> 25,
        LONG_HAUL_INTERCONTINENTAL -> 30,
        ULTRA_LONG_HAUL_INTERCONTINENTAL -> 30)


      val multiplyFactorLookup = Map(
        SHORT_HAUL_DOMESTIC -> 2,
        MEDIUM_HAUL_DOMESTIC -> 2,
        LONG_HAUL_DOMESTIC -> 2,
        SHORT_HAUL_INTERNATIONAL -> 2,
        MEDIUM_HAUL_INTERNATIONAL -> 2,
        LONG_HAUL_INTERNATIONAL -> 2,
        SHORT_HAUL_INTERCONTINENTAL -> 3,
        MEDIUM_HAUL_INTERCONTINENTAL -> 3,
        LONG_HAUL_INTERCONTINENTAL -> 4,
        ULTRA_LONG_HAUL_INTERCONTINENTAL -> 4)


      val lookup = FlightType.values.toList.map { flightType =>
        val basic = basicLookup(flightType)
        val multiplyFactor = multiplyFactorLookup(flightType)
        val staffPerFrequency = 2.0 / 5 * multiplyFactor
        val staffPer1000Pax = 1 * multiplyFactor
        (flightType, StaffSchemeBreakdown(basic, staffPerFrequency, staffPer1000Pax))
      }.toMap

      lookup.toMap
  }
}

case class StaffBreakdown(basicStaff : Int, frequencyStaff : Double, capacityStaff : Double, modifier : Double) {
  val total = ((basicStaff + frequencyStaff + capacityStaff) * modifier).toInt
}
case class StaffSchemeBreakdown(basic : Int, perFrequency : Double, per1000Pax : Double)




/**
 * Cost is the adjusted price
 */
case class LinkConsideration(link : Transport, cost : Double, linkClass : LinkClass, inverted : Boolean, var id : Int = 0) extends IdObject {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " " + linkClass + " (inverted?) " + inverted + ")"
    }
}

sealed abstract class LinkClass(val code : String, val spaceMultiplier : Double, val resourceMultiplier : Double, val priceMultiplier : Double, val priceSensitivity : Double, val level : Int) {
  def label : String //level for sorting/comparison purpose
}
case object FIRST extends LinkClass("F", spaceMultiplier = 6, resourceMultiplier = 3, priceMultiplier = 9, priceSensitivity = 0.8, level = 3) {
  override def label = "first"
}
case object BUSINESS extends LinkClass("J", spaceMultiplier = 2.5, resourceMultiplier = 2, priceMultiplier = 3, priceSensitivity = 0.9, level = 2) {
  override def label = "business"
}
case object ECONOMY extends LinkClass("Y", spaceMultiplier = 1, resourceMultiplier = 1, priceMultiplier = 1, priceSensitivity = 1, level =1) {
  override def label = "economy"
}
object LinkClass {
  val values = List(FIRST, BUSINESS, ECONOMY)
  
  val fromCode : String => LinkClass = (code : String) => {
    values.find { _.code == code }.get
  }
  
  val fromLevel : Int => LinkClass = (level : Int) => {
    values.find { _.level  == level}.get 
  }
}