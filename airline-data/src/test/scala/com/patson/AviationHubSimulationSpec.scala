package com.patson

import com.patson.model._
import com.patson.model.airplane._
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer

class AviationHubSimulationSpec extends WordSpecLike with Matchers {
  val airport1 = Airport("", "", "Test Airport 1", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 10000L, 0, id = 1)
  val airport2 = Airport("", "", "Test Airport 2", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 100000L, 0, id = 2)
  val airport3 = Airport("", "", "Test Airport 3", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 100000L, 0, id = 3)
  val airline1 = Airline.fromId(1)
  val airline2 = Airline.fromId(2)
  val airline3 = Airline.fromId(3)
  val airline4 = Airline.fromId(4)
  val airline1Link1 = Link(airport1, airport2, airline1, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1)
  val badAirline1Link1 = LinkConsideration.getExplicit(airline1Link1, 100000, ECONOMY, false, 0)
  val goodAirline1Link1 = LinkConsideration.getExplicit(airline1Link1, 0, ECONOMY, false, 0)
  val airline2Link2 = Link(airport2, airport3, airline2, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 2)
  val badAirline2Link2 = LinkConsideration.getExplicit(airline2Link2, 100000, ECONOMY, false, 0)
  val goodAirline2Link2 = LinkConsideration.getExplicit(airline2Link2, 0, ECONOMY, false, 0)
  val badRoute = Route(List(badAirline1Link1, badAirline2Link2), 0)
  val goodRoute = Route(List(goodAirline1Link1, goodAirline2Link2), 0)
  val passengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, 0, 1, 1), PassengerType.BUSINESS)
  val allAirports = List(airport1, airport2, airport3)


  "navigationHubSimulation should accurately update paxByAirport based on link ridership details" in {
    // Create mock data for Airport and Route

//    val airline1Link1 = Link(airport1, airport2, airline1, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1)
    val linkConsideration1 = LinkConsideration.getExplicit(Link(airport1, airport2, airline1, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1), 0, ECONOMY, false, 0)
    val linkConsideration2 = LinkConsideration.getExplicit(Link(airport2, airport3, airline2, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1), 0, ECONOMY, false, 0)
    val linkConsideration3 = LinkConsideration.getExplicit(Link(airport3, airport2, airline1, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1), 0, ECONOMY, true, 0)

    val route1 = Route(links = List(linkConsideration1, linkConsideration2), 0)
    val route2 = Route(links = List(linkConsideration1, linkConsideration3), 0)


    // Create mock data for PassengerGroup and linkRidershipDetails
    val linkRidershipDetails = immutable.Map(
      (passengerGroup, airport3, route1) -> 100,
      (passengerGroup, airport3, route2) -> 150
    )


    // Call the method
    val airportDirectDemand = mutable.HashMap[Airport, Long]()

    DemandGenerator.computeDemand(1, allAirports, plainDemand = true).foreach {
      case (group, toAirport, pax) =>
        airportDirectDemand.put(group.fromAirport, airportDirectDemand.getOrElse(group.fromAirport, 0L) + pax)
        airportDirectDemand.put(toAirport, airportDirectDemand.getOrElse(toAirport, 0L) + pax)
    }

    AviationHubSimulation.computeUpdatingAirports(airportDirectDemand.toMap, linkRidershipDetails, 1)

    // Validate results
//    result should contain allOf(
//      (airport1 -> 100L),
//      (airport2 -> 250L),
//      (airport3 -> 150L)
//    )
  }


}
