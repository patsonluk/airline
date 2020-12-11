package com.patson.model

import com.patson.model.airplane.{Airplane, LinkAssignment, Model}
import com.patson.model.Scheduling.TimeSlot
import java.util.concurrent.ConcurrentHashMap

/**
 * 
 * Frequency sum of all assigned plane
 */
case class Link(from : Airport, to : Airport, airline: Airline, price : LinkClassValues, distance : Int, var capacity: LinkClassValues, rawQuality : Int, duration : Int, var frequency : Int, flightType : FlightType.Value, var flightNumber : Int = 0, var id : Int = 0) extends IdObject{
  @volatile var availableSeats : LinkClassValues = capacity.copy()
  @volatile var soldSeats : LinkClassValues = LinkClassValues.getInstance()
  @volatile var cancelledSeats :  LinkClassValues = LinkClassValues.getInstance()
  @volatile var cancellationCount = 0
  @volatile var majorDelayCount = 0
  @volatile var minorDelayCount = 0
  @volatile private var assignedAirplanes : Map[Airplane, LinkAssignment] = Map.empty
  @volatile private var assignedModel : Option[Model] = None
  
  @volatile private var hasComputedQuality = false
  @volatile private var computedQualityStore : Int = 0

  var inServiceAirplanes : Map[Airplane, LinkAssignment] = Map.empty

  private val standardPrice : ConcurrentHashMap[LinkClass, Int] = new ConcurrentHashMap[LinkClass, Int]()

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
    setAssignedAirplanes(assignedAirplanes.toList.map {
      case (airplane, frequency) => (airplane, LinkAssignment(frequency, Computation.calculateFlightMinutesRequired(airplane.model, distance)))
    }.toMap)
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

  import FlightType._
  /**
   * Find seats at or below the requestedLinkClass (can only downgrade 1 level)
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

  def computedQuality : Int= {
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
  
  def capacityPerFlight() = {
    if (frequency > 0) {
      capacity / frequency
    } else { //error 
      LinkClassValues.getInstance()
    }
  }

  def standardPrice(linkClass : LinkClass) : Int = {
    var price = standardPrice.get(linkClass)
    if (price == null.asInstanceOf[Int]) {
      price = Pricing.computeStandardPrice(distance, flightType, linkClass)
      standardPrice.put(linkClass, price)
    }
    price
  }

  lazy val getOfficeStaffRequired = {
    Link.getOfficeStaffRequired(from, to)
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
}

object Link {
  val MAX_QUALITY = 100
  val HIGH_FREQUENCY_THRESHOLD = 14
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
  val getOfficeStaffRequired = (from : Airport, to : Airport) => {
    import FlightType._
    Computation.getFlightType(from, to) match {
      case SHORT_HAUL_DOMESTIC => 5
      case LONG_HAUL_DOMESTIC => 8
      case SHORT_HAUL_INTERNATIONAL => 10
      case LONG_HAUL_INTERNATIONAL => 12
      case SHORT_HAUL_INTERCONTINENTAL => 12
      case MEDIUM_HAUL_INTERCONTINENTAL => 20
      case LONG_HAUL_INTERCONTINENTAL => 30
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 30
  }
}
}



/**
 * Cost is the adjusted price
 */
case class LinkConsideration(link : Link, cost : Double, linkClass : LinkClass, inverted : Boolean, var id : Int = 0) extends IdObject {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " " + linkClass + " (inverted?) " + inverted + ")"
    }
}

sealed abstract class LinkClass(val code : String, val spaceMultiplier : Double, val resourceMultiplier : Double, val priceMultiplier : Double, val priceSensitivity : Double, val level : Int) {
  def label : String //level for sorting/comparison purpose
}
case object FIRST extends LinkClass("F", spaceMultiplier = 6, resourceMultiplier = 3, priceMultiplier = 9, priceSensitivity = 0.6, level = 3) {
  override def label = "first"
}
case object BUSINESS extends LinkClass("J", spaceMultiplier = 2.5, resourceMultiplier = 2, priceMultiplier = 3, priceSensitivity = 0.8, level = 2) {
  override def label = "business"
}
case object ECONOMY extends LinkClass("Y", spaceMultiplier = 1, resourceMultiplier = 1, priceMultiplier = 1, priceSensitivity = 1.0, level =1) {
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