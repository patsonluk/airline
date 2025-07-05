package com.patson

import com.patson.data._
import com.patson.model._
import scala.collection.{immutable, mutable}

object AviationHubSimulation {
  val AVIATION_HUB_DEMAND_RATIO_THRESHOLD = 2
  val AVIATION_HUB_PAX_THRESHOLD = 10000
  val AVIATION_HUB_MAX_STRENGTH = 100


  def main(args : Array[String]) : Unit = {
//    computeAviationHubStrength(Airport.fromId(0), 12749   , 236526  )
    println(computePaxRequirementByStrength(Airport.fromId(0), 1000000000, 99))
  }

  def simulate(allAirports : List[Airport], linkRidershipDetails : immutable.Map[(PassengerGroup, Airport, Route), Int], cycle : Int) : Map[Airport, Long] = {
    val airportDirectDemand = mutable.HashMap[Airport, Long]()

    DemandGenerator.computeDemand(cycle, allAirports, plainDemand = true).foreach {
      case (group, toAirport, pax) =>
        airportDirectDemand.put(group.fromAirport, airportDirectDemand.getOrElse(group.fromAirport, 0L) + pax)
        airportDirectDemand.put(toAirport, airportDirectDemand.getOrElse(toAirport, 0L) + pax)
    }

    val updatingAirports = computeUpdatingAirports(airportDirectDemand.toMap, getPaxByAirport(linkRidershipDetails))

    updatingAirports.foreach {
      case (airport, strength) =>
        if (strength > 0) {
          AirportSource.saveAirportFeature(airport.id, AviationHubFeature(strength))
        } else {
          AirportSource.deleteAirportFeature(airport.id, AirportFeatureType.AVIATION_HUB)
        }
    }

    airportDirectDemand.toMap
  }

  def computeUpdatingAirports(airportDirectDemand : Map[Airport, Long], paxByAirport: Map[Airport, Long]) : Map[Airport, Int] = {
    val updatingAirports = mutable.HashMap[Airport, Int]() //key is strength
    airportDirectDemand.foreach {
      case (airport, demand) =>
        val strength = computeAviationHubStrength(airport, demand, paxByAirport.getOrElse(airport, 0))
        if (strength > 0) {
          updatingAirports.put(airport, strength.toInt)
        } else if (airport.features.exists(_.featureType == AirportFeatureType.AVIATION_HUB)) {
          updatingAirports.put(airport, 0)
        }
    }
    updatingAirports.toMap
  }

  def getPaxByAirport(linkRidershipDetails : Map[(PassengerGroup, Airport, Route), Int]) : Map[Airport, Long] = {
    val paxByAirport = mutable.HashMap[Airport, Long]()

    linkRidershipDetails.foreach {
      case ((passengerGroup, toAirport, route), count) => route.links.foreach { link =>
        if (link.link.transportType == TransportType.FLIGHT) {
          paxByAirport.updateWith(link.from)(existingOption => Some(existingOption.map(_ + count).getOrElse(count)))
          paxByAirport.updateWith(link.to)(existingOption => Some(existingOption.map(_ + count).getOrElse(count)))
        }
      }
    }

    paxByAirport.toMap
  }

  def computeAviationHubStrength(airport : Airport, directDemand : Long, pax : Long) : Double = {
    if (pax < AVIATION_HUB_PAX_THRESHOLD) {
      return 0
    }

    val strengthRatio =
      if (directDemand == 0) {
        20
      } else {
        val ratio = pax * 1.0 / directDemand
        if (ratio < AVIATION_HUB_DEMAND_RATIO_THRESHOLD) {
          return 0
        }
        Math.min(20, Math.log(ratio - AVIATION_HUB_DEMAND_RATIO_THRESHOLD + 1) / Math.log(1.2)) //make it easier for less populated airports
      }


    //cases
    //X has 0 DD, 250k pax, max out 100 HUB strength
    //YXS has roughly 5428 DD, 50k actual traffic, around aviation hub 20
    //YVR has roughly
    val strength = strengthRatio * Math.min(500_000, pax) / 75_000

//    println(s"!!!!${airport.iata} demand $directDemand pax $pax ratio $strengthRatio final $strength")

    Math.min(AVIATION_HUB_MAX_STRENGTH, strength)
  }

  def computePaxRequirementByStrength(airport: Airport, directDemand : Long, targetStrength : Int) : Option[Long] = {
    // a bit hard to do reverse of the computation, let's just do binary search...
    if (targetStrength <= 0 || targetStrength > AVIATION_HUB_MAX_STRENGTH) {
      return None
    }

    var minPax = AVIATION_HUB_PAX_THRESHOLD.toLong

    //find maxPax
    var maxPax = 10_000_000L
    while (computeAviationHubStrength(airport, directDemand, maxPax) < targetStrength) {
      maxPax *= 2
    }

    for (i <- 0 until 100) { //just limit it just in case...
      val midPax = ((minPax + maxPax) / 2).toLong
      val strength = computeAviationHubStrength(airport, directDemand, midPax)
      if (midPax == minPax || midPax == maxPax) {
        if (strength < targetStrength) {
          return Some(midPax + 1)
        } else {
          return Some(midPax)
        }
      } else if (strength < targetStrength) {
        minPax = midPax
      } else {
        maxPax = midPax
      }
//      println(s"$midPax -> $strength")
    }
    None
  }
}