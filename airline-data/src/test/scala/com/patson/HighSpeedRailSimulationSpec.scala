package com.patson

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import com.patson.model.FlightType._
import com.patson.model.FlightPreferenceType.{APPEAL, BUDGET, LOYAL, SPEED}
import com.patson.model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import java.util.Collections
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable.Set
import scala.jdk.CollectionConverters._

class HighSpeedRailSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)
  val fromAirport = Airport.fromId(1).copy(baseIncome = 40000, basePopulation = 1, name = "F0")
  val airlineAppeal = AirlineAppeal(0)
  fromAirport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  fromAirport.initLounges(List.empty)
  val toAirportsList = List(
      Airport("", "", "T0", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
      Airport("", "", "T1", 0, 60, "", "", "", 1, 0, 0, 0, id = 3),
      Airport("", "", "T2", 0, 90, "", "", "", 1, 0, 0, 0, id = 4))

  toAirportsList.foreach { airport =>
    airport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
    airport.initLounges(List.empty)
  }
  val toAirports = Set(toAirportsList  : _*)

  val allAirportIds = Set[Int]()
  allAirportIds ++= toAirports.map { _.id }
  allAirportIds += fromAirport.id

  val passengerGroup = PassengerGroup(fromAirport, SimplePreference(homeAirport = fromAirport, priceSensitivity = 1, preferredLinkClass = ECONOMY), PassengerType.BUSINESS)

  def hsr(from : Airport, to : Airport, airline : Airline, capacity : LinkClassValues = LinkClassValues.getInstance(10000, 10000, 10000), id : Int) =
    HighSpeedRail(from, to, airline, 50, capacity, frequency = 100, id = id)

  def flight(from : Airport, to : Airport, airline : Airline, id : Int) =
    Link(from, to, airline, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = id)

  "Find shortest route".must {
    "find a route with HSR connecting to/from flights".in {
      var links = List(
        LinkConsideration.getExplicit(hsr(fromAirport, toAirportsList(0), testAirline1, id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(flight(toAirportsList(0), toAirportsList(1), testAirline1, id = 2), 100, ECONOMY, false))

      var routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(1)).shouldBe(true)
      assert(routes(toAirportsList(1)).links.map(_.id).equals(links.map(_.id)))

      links = List(
        LinkConsideration.getExplicit(flight(fromAirport, toAirportsList(0), testAirline1, id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(hsr(toAirportsList(0), toAirportsList(1), testAirline1, id = 2), 100, ECONOMY, false))

      routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(1)).shouldBe(true)
      assert(routes(toAirportsList(1)).links.map(_.id).equals(links.map(_.id)))
    }

    "not find a route with HSR only (no flights)".in {
      val links = List(
        LinkConsideration.getExplicit(hsr(fromAirport, toAirportsList(0), testAirline1, id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(hsr(toAirportsList(0), toAirportsList(1), testAirline1, id = 2), 100, ECONOMY, false))

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(0)).shouldBe(false)
      routes.isDefinedAt(toAirportsList(1)).shouldBe(false)
    }

    "not find a route with back to back non flights (generic transit + HSR)".in {
      val transit = GenericTransit(fromAirport, toAirportsList(0), 50, LinkClassValues.getInstance(10000), id = 1)
      val links = List(
        LinkConsideration.getExplicit(transit, 100, ECONOMY, false),
        LinkConsideration.getExplicit(hsr(toAirportsList(0), toAirportsList(1), testAirline1, id = 2), 100, ECONOMY, false),
        LinkConsideration.getExplicit(flight(toAirportsList(1), toAirportsList(2), testAirline1, id = 3), 100, ECONOMY, false))

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }

    "have no connection cost if HSR is operated by the same airline or alliance as the connecting flight".in {
      def routeCost(hsrAirline : Airline, flightAirline : Airline, allianceIdByAirlineId : java.util.Map[Int, Int]) : Double = {
        val links = List(
          LinkConsideration.getExplicit(hsr(fromAirport, toAirportsList(0), hsrAirline, id = 1), 100, ECONOMY, false),
          LinkConsideration.getExplicit(flight(toAirportsList(0), toAirportsList(1), flightAirline, id = 2), 100, ECONOMY, false))
        PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, allianceIdByAirlineId, 3)(toAirportsList(1)).totalCost
      }

      val noAlliance = Collections.emptyMap[Int, Int]()
      val sameAlliance = Map(testAirline1.id -> 1, testAirline2.id -> 1).asJava
      val differentAlliance = Map(testAirline1.id -> 1, testAirline2.id -> 2).asJava

      val otherAirlineCost = routeCost(testAirline1, testAirline2, noAlliance)
      val sameAirlineCost = routeCost(testAirline1, testAirline1, noAlliance)
      val sameAllianceCost = routeCost(testAirline1, testAirline2, sameAlliance)
      val differentAllianceCost = routeCost(testAirline1, testAirline2, differentAlliance)

      val connectionCost = HighSpeedRail.CONNECTION_COST * passengerGroup.preference.connectionCostRatio * ECONOMY.priceMultiplier
      otherAirlineCost.shouldBe(200 + connectionCost)
      sameAirlineCost.shouldBe(200)
      sameAllianceCost.shouldBe(sameAirlineCost)
      differentAllianceCost.shouldBe(otherAirlineCost)
    }
  }

  "findRoutesByPassengerGroup".must {
    "use the matching HSR class, and downgrade if the class is not available".in {
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val toAirport = Set(toAirportsList(1))

      def hsrLinkClass(hsrCapacity : LinkClassValues) : LinkClass = {
        val links = List(hsr(fromAirport, toAirportsList(0), testAirline1, hsrCapacity, id = 1), flight(toAirportsList(0), toAirportsList(1), testAirline1, id = 2))
        val route = PassengerSimulation.findRoutesByPassengerGroup(businessPassengerGroup, toAirport, links, allAirportIds, countryOpenness = Map.empty)(toAirportsList(1))
        route.links.find(_.link.transportType == TransportType.HIGH_SPEED_RAIL).get.linkClass
      }

      hsrLinkClass(LinkClassValues.getInstance(10000, 10000, 10000)).shouldBe(BUSINESS)
      hsrLinkClass(LinkClassValues.getInstance(10000, 0, 10000)).shouldBe(ECONOMY)
    }

    "have hidden cost on HSR for all classes".in {
      val rail = hsr(fromAirport, toAirportsList(0), testAirline1, id = 1)
      LinkClass.values.foreach { linkClass =>
        rail.cost(linkClass).shouldBe((rail.standardPrice(linkClass) * HighSpeedRail.COST_RATIO).toInt)
        assert(rail.cost(linkClass) > 0)
      }
    }
  }

  "find routes".must {
    //~111.19 km per degree at the equator
    val countryOpenness = Map[String, Int]("C1" -> 10, "C2" -> 10)
    val iterations = 1000

    def airport(name : String, latitude : Double, longitude : Double, countryCode : String, id : Int) : Airport = {
      val airport = fromAirport.copy(iata = name, name = name, latitude = latitude, longitude = longitude, countryCode = countryCode, id = id)
      airport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0), testAirline2.id -> AirlineAppeal(0)))
      airport.initLounges(List.empty)
      airport
    }

    //flight with default pricing and neutral quality
    def standardFlight(from : Airport, to : Airport, airline : Airline, frequency : Int, id : Int) : Link = {
      val distance = Computation.calculateDistance(from, to)
      val duration = Computation.computeStandardFlightDuration(distance)
      val flightType = Computation.getFlightType(from, to, distance)
      val price = Pricing.computeStandardPriceForAllClass(distance, from, to)
      val link = Link(from, to, airline, price, distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency, flightType, id = id)
      link.setQuality(50)
      link
    }

    def standardHsr(from : Airport, to : Airport, airline : Airline, id : Int) : HighSpeedRail = {
      val cars : Map[LinkClass, Int] = Map(ECONOMY -> 6, BUSINESS -> 1, FIRST -> 1)
      HighSpeedRail(from, to, airline, Computation.calculateDistance(from, to), HighSpeedRail.capacityByCars(cars), HighSpeedRail.WEEKLY_FREQUENCY, id = id)
    }

    def genericTransit(from : Airport, to : Airport, id : Int) : GenericTransit = {
      GenericTransit(from, to, Computation.calculateDistance(from, to), LinkClassValues.getInstance(economy = 10000), id = id)
    }

    /**
      * Simulates pax (with preference from drawPreference) and returns the HSR percentage (0 - 100), out of all pax that found a route
      */
    def hsrPercentage(links : List[Transport], from : Airport, to : Airport, label : String)(drawPreference : () => FlightPreference) : Double = {
      val activeAirports = links.flatMap(link => List(link.from.id, link.to.id)).to(collection.mutable.Set)
      val toAirports = Set(to)

      var routeCount = 0
      var hsrCount = 0
      for (i <- 0 until iterations) {
        val group = PassengerGroup(from, drawPreference(), PassengerType.BUSINESS)
        val result = PassengerSimulation.findAllRoutes(Map(group -> toAirports), links, activeAirports, countryOpenness = countryOpenness)
        result(group).get(to).foreach { route =>
          routeCount += 1
          if (route.links.exists(_.link.transportType == TransportType.HIGH_SPEED_RAIL)) {
            hsrCount += 1
          }
        }
      }
      println(s"$label : HSR $hsrCount / routes $routeCount / iterations $iterations")
      withClue(s"$label found no route") {
        assert(routeCount > 0)
      }
      hsrCount * 100.0 / routeCount
    }

    /**
      * Draws pax of the preference type from the pool (across all link classes)
      */
    def hsrPercentageByPreferenceType(links : List[Transport], from : Airport, to : Airport, preferenceType : FlightPreferenceType.Value) : Double = {
      val preferences = DemandGenerator.getFlightPreferencePoolOnAirport(from).pool.values.flatten.filter { preference =>
        preference.isApplicable(from, to) && preference.getPreferenceType == preferenceType
      }.toList
      withClue(s"No preference of type $preferenceType in the pool") {
        assert(preferences.nonEmpty)
      }
      hsrPercentage(links, from, to, preferenceType.title)(() => preferences(ThreadLocalRandom.current().nextInt(preferences.length)))
    }

    /**
      * Assert the percentage (0 - 100) is within [min, max] inclusively
      */
    def assertProbablyRange(label : String, percentage : Double, min : Double, max : Double) = {
      withClue(s"$label HSR percentage $percentage not within [$min, $max]") {
        assert(percentage >= min && percentage <= max)
      }
    }

    "HSR over generic transit (50km) to connect to a long haul flight".in {
      val airportA = airport("A", 0, 0, "C1", 1)
      val airportB = airport("B", 0.45, 0, "C1", 2) //~50km from A
      val airportC = airport("C", 0.45, 70, "C2", 3) //long haul from B

      val links = List(
        genericTransit(airportA, airportB, id = 1),
        standardHsr(airportA, airportB, testAirline2, id = 2), //not the same airline/alliance as the long haul flight
        standardFlight(airportB, airportC, testAirline1, frequency = 7, id = 3))

      //simulate all pax groups first, so all results are printed even if one of them fails
      val budgetPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, BUDGET)
      val comprehensivePercentage = hsrPercentageByPreferenceType(links, airportA, airportC, APPEAL)
      val speedPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, SPEED)

      assertProbablyRange(BUDGET.title, budgetPercentage, 0.1, 10) //most will use generic transit as it's cheaper
      assertProbablyRange(APPEAL.title, comprehensivePercentage, 80, 99) //most but not all would use HSR
      assertProbablyRange(SPEED.title, speedPercentage, 100, 100) //always HSR
    }

    "HSR over flight (100km) to connect to a long haul flight".in {
      val airportA = airport("A", 0, 0, "C1", 1)
      val airportB = airport("B", 0.89, 0, "C1", 2) //~99km from A
      val airportC = airport("C", 0.89, 70, "C2", 3) //long haul from B

      val links = List(
        standardFlight(airportA, airportB, testAirline1, frequency = Link.HIGH_FREQUENCY_THRESHOLD, id = 1),
        standardHsr(airportA, airportB, testAirline2, id = 2), //not the same airline/alliance as the long haul flight
        standardFlight(airportB, airportC, testAirline1, frequency = 7, id = 3))

      //simulate all pax groups first, so all results are printed even if one of them fails
      val budgetPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, BUDGET)
      val comprehensivePercentage = hsrPercentageByPreferenceType(links, airportA, airportC, APPEAL)
      val speedPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, SPEED)

      //HSR for all
      assertProbablyRange(BUDGET.title, budgetPercentage, 100, 100)
      assertProbablyRange(APPEAL.title, comprehensivePercentage, 100, 100)
      assertProbablyRange(SPEED.title, speedPercentage, 100, 100)
    }

    "prefer HSR over connection flight (100km) between 2 flights".in {
      val airportA = airport("A", 0, 0, "C1", 1)
      val airportB = airport("B", 0, 9, "C1", 2) //~1000km from A
      val airportC = airport("C", 0.89, 9, "C1", 3) //~99km from B
      val airportD = airport("D", 0.89, 18, "C2", 4) //~1000km from C

      val links = List(
        standardFlight(airportA, airportB, testAirline1, frequency = 14, id = 1),
        standardFlight(airportB, airportC, testAirline1, frequency = 28, id = 2),
        standardHsr(airportB, airportC, testAirline2, id = 3), //not the same airline/alliance as the flights
        standardFlight(airportC, airportD, testAirline1, frequency = 14, id = 4))

      //simulate all pax groups first, so all results are printed even if one of them fails
      val budgetPercentage = hsrPercentageByPreferenceType(links, airportA, airportD, BUDGET)
      val comprehensivePercentage = hsrPercentageByPreferenceType(links, airportA, airportD, APPEAL)
      val speedPercentage = hsrPercentageByPreferenceType(links, airportA, airportD, SPEED)

      //HSR is just faster and cheaper in all aspect
      assertProbablyRange(BUDGET.title, budgetPercentage, 100, 100)
      assertProbablyRange(APPEAL.title, comprehensivePercentage, 100, 100)
      assertProbablyRange(SPEED.title, speedPercentage, 100, 100)
    }

    "prefer HSR (100km) + short haul flight (300km) over direct short haul flight (400km)".in {
      val airportA = airport("A", 0, 0, "C1", 1)
      val airportB = airport("B", 0.89, 0, "C1", 2) //~99km from A
      val airportC = airport("C", 3.6, 0, "C1", 3) //~301km from B, ~400km from A
      //pax from A are very loyal to the airline operating the flights
      airportA.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(AirlineAppeal.MAX_LOYALTY), testAirline2.id -> AirlineAppeal(0)))

      val links = List(
        standardHsr(airportA, airportB, testAirline2, id = 1), //not the same airline/alliance as the flights
        standardFlight(airportB, airportC, testAirline1, frequency = 14, id = 2),
        standardFlight(airportA, airportC, testAirline1, frequency = 14, id = 3))

      //simulate all pax groups first, so all results are printed even if one of them fails
      val budgetPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, BUDGET)
      val comprehensivePercentage = hsrPercentageByPreferenceType(links, airportA, airportC, APPEAL)
      val speedPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, SPEED)
      val loyalPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, LOYAL)

      assertProbablyRange(BUDGET.title, budgetPercentage, 100, 100)
      assertProbablyRange(APPEAL.title, comprehensivePercentage, 100, 100)
      assertProbablyRange(SPEED.title, speedPercentage, 100, 100)
      assertProbablyRange(LOYAL.title, loyalPercentage, 50, 99) //some would take the direct flight of the airline they are loyal to
    }

    "prefer HSR (100km) + long haul flight (10000km) over direct long haul flight (10100km)".in {
      val airportA = airport("A", 0, 0, "C1", 1)
      val airportB = airport("B", 0, 0.89, "C1", 2) //~99km from A
      val airportC = airport("C", 0, 90.83, "C2", 3) //~10000km from B, ~10100km from A
      //pax from A are very loyal to the airline operating the flights
      airportA.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(AirlineAppeal.MAX_LOYALTY), testAirline2.id -> AirlineAppeal(0)))

      val links = List(
        standardHsr(airportA, airportB, testAirline2, id = 1), //not the same airline/alliance as the flights
        standardFlight(airportB, airportC, testAirline1, frequency = 14, id = 2),
        standardFlight(airportA, airportC, testAirline1, frequency = 14, id = 3))

      //simulate all pax groups first, so all results are printed even if one of them fails
      val budgetPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, BUDGET)
      val comprehensivePercentage = hsrPercentageByPreferenceType(links, airportA, airportC, APPEAL)
      val speedPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, SPEED)
      val loyalPercentage = hsrPercentageByPreferenceType(links, airportA, airportC, LOYAL)

      assertProbablyRange(BUDGET.title, budgetPercentage, 100, 100)
      assertProbablyRange(APPEAL.title, comprehensivePercentage, 100, 100)
      assertProbablyRange(SPEED.title, speedPercentage, 100, 100)
      assertProbablyRange(LOYAL.title, loyalPercentage, 50, 99) //some would take the direct flight of the airline they are loyal to
    }
  }
}
