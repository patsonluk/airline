package com.patson.model

case class HighSpeedRail(from : Airport, to : Airport, airline : Airline, distance : Int, var capacity: LinkClassValues, var frequency : Int, fundedCycle : Int, var id : Int = 0) extends Transport {
  val completionCycle : Int = fundedCycle + HighSpeedRail.CONSTRUCTION_DURATION

  def isCompleted(currentCycle : Int) : Boolean = currentCycle >= completionCycle

  //remaining cycles (weeks) before the construction completes, 0 if already completed
  def remainingConstructionCycles(currentCycle : Int) : Int = Math.max(0, completionCycle - currentCycle)

  override val transportType : TransportType.Value = TransportType.HIGH_SPEED_RAIL
  override val duration = (distance.toDouble / HighSpeedRail.SPEED * 60).toInt
  override def computedQuality() : Int = HighSpeedRail.QUALITY //constant quality for now
  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC //has to be defined before price, as standardPrice uses it

  //ticket price pax pay for HSR. Not a revenue of the airline that builds it
  override val price : LinkClassValues = LinkClassValues.getInstance(
    economy = (standardPrice(ECONOMY) * HighSpeedRail.PRICE_RATIO).toInt,
    business = (standardPrice(BUSINESS) * HighSpeedRail.PRICE_RATIO).toInt,
    first = (standardPrice(FIRST) * HighSpeedRail.PRICE_RATIO).toInt)

  override var minorDelayCount : Int = 0
  override var majorDelayCount : Int = 0
  override var cancellationCount : Int = 0

  override def toString() = {
    s"High speed rail $id; ${airline.name}; ${from.city}(${from.iata}) => ${to.city}(${to.iata}); distance $distance"
  }

  override val frequencyByClass  = (_ : LinkClass) =>  frequency
}

object HighSpeedRail {
  val QUALITY = 80 //high quality traffic
  val SPEED = 150 //km/h
  val PRICE_RATIO = 0.35 //HSR is a bit more expensive than generic transit
  val CONNECTION_COST = 10 //lower than generic transit, they are usually more convenient

  val MIN_BASE_SCALE = 13
  val MAX_DISTANCE = 100 //km
  val MIN_COUNTRY_RELATIONSHIP = 2 //same as generic transit, so it's always allowed within the same country
  val MIN_CARS = 3
  val MAX_CARS = 8
  val CAR_CAPACITY = 50000 //weekly capacity of a train car if it's all economy
  val COST_PER_KM_PER_CAPACITY = 1250
  val RECONFIGURE_COOLDOWN = 1000 //in cycles (weeks)
  val WEEKLY_FREQUENCY = 250
  val CONSTRUCTION_DURATION = 5 * 52 //in cycles (weeks). Not usable by pax until construction is completed

  //remaining cycles before the HSR can be reconfigured again. Counts from the cycle it was last built/reconfigured
  def reconfigureCooldown(lastUpdateCycle : Int, currentCycle : Int) : Int = Math.max(0, lastUpdateCycle + RECONFIGURE_COOLDOWN - currentCycle)

  //number of train cars is determined by the size of the from airport
  def carCount(fromAirport : Airport) : Int = Math.max(MIN_CARS, Math.min(MAX_CARS, fromAirport.size))

  def maxCapacity(fromAirport : Airport) : Int = carCount(fromAirport) * CAR_CAPACITY

  //each car can only be assigned to one class
  def seatsPerCar(linkClass : LinkClass) : Int = (CAR_CAPACITY / linkClass.spaceMultiplier).toInt

  def capacityByCars(cars : Map[LinkClass, Int]) : LinkClassValues = {
    LinkClassValues.getInstanceByMap(cars.map {
      case (linkClass, carCount) => (linkClass, carCount * seatsPerCar(linkClass))
    })
  }

  def carsByCapacity(capacity : LinkClassValues) : Map[LinkClass, Int] = {
    LinkClass.values.map(linkClass => (linkClass, capacity(linkClass) / seatsPerCar(linkClass))).toMap
  }

  def buildCost(distance : Int, fromAirport : Airport) : Long = distance.toLong * maxCapacity(fromAirport) * COST_PER_KM_PER_CAPACITY

  def carsRejection(cars : Map[LinkClass, Int], carCount : Int) : Option[String] = {
    if (cars.values.exists(_ < 0)) {
      Some("Number of train cars cannot be negative")
    } else if (cars.values.sum <= 0) {
      Some("Must assign at least 1 train car")
    } else if (cars.values.sum > carCount) {
      Some(s"Cannot assign more than $carCount train cars")
    } else {
      None
    }
  }
}
