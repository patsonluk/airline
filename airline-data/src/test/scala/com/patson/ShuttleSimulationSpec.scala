package com.patson

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.patson.model.FlightType._
import com.patson.model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import java.util.Collections
import scala.collection.mutable.Set
import scala.jdk.CollectionConverters._

class ShuttleSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)
  val fromAirport = Airport.fromId(1).copy(power = 40000, population = 1) //income 40k . mid income country
  val airlineAppeal = AirlineAppeal(0, 100)
  fromAirport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  fromAirport.initLounges(List.empty)
  val toAirportsList = List(
      Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
      Airport("", "", "To Airport", 0, 60, "", "", "", 1, 0, 0, 0, id = 3),
      Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 4))
  
  
  toAirportsList.foreach { airport =>
    airport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
    airport.initLounges(List.empty)
  }
  val toAirports = Set(toAirportsList  : _*)
  
  val allAirportIds = Set[Int]()
  allAirportIds ++= toAirports.map { _.id }
  allAirportIds += fromAirport.id
  val LOOP_COUNT = 10000
  
  
//  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
//  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  val passengerGroup = PassengerGroup(fromAirport, SimplePreference(homeAirport = fromAirport, priceSensitivity = 1, preferredLinkClass = ECONOMY), PassengerType.BUSINESS) 
  //def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
  "Find shortest route".must {
    "find a route if there's shuttle offered by same airline".in {
      var links = List(LinkConsideration(Shuttle(fromAirport, toAirportsList(0), testAirline1, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      
      var routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      var route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)

      links = List(
        LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Shuttle(toAirportsList(0), toAirportsList(1), testAirline1, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))

      routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)

      links = List(
        LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Shuttle(toAirportsList(1), toAirportsList(2), testAirline1, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false))

      routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)

    }
    "find a route if there's a shuttle offered by alliance member".in {
      val links = List(
        LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Shuttle(toAirportsList(1), toAirportsList(2), testAirline2, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false)) //shuttle by alliance member
      
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Map(testAirline1.id -> 1, testAirline2.id -> 1).asJava, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find no route if there's a shuttle offered by different airline".in {
      val links = List(
        LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Shuttle(toAirportsList(1), toAirportsList(2), testAirline2, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false)) //shuttle by different airline

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
    "find no route if there's a shuttle offered by different alliance".in {
      val links = List(
        LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Shuttle(toAirportsList(1), toAirportsList(2), testAirline2, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false)) //shuttle by different airline

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Map(testAirline1.id -> 1, testAirline2.id -> 2).asJava, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }

    "direct flight more preferable than shuttle".in {
      val links = List(LinkConsideration(Shuttle(fromAirport, toAirportsList(0), testAirline1, 50, LinkClassValues.getInstance(10000)), 100, ECONOMY, false),
        LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration(Link(fromAirport, toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(1)).shouldBe(true)
      val route = routes.get(toAirportsList(1)).get
      route.links.size.shouldBe(1) //should take the direct flight only
    }
    "use direct route even though it's more expensive as connection flight is not frequent enough".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false))
     val expensiveLink = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC), 1400, ECONOMY, false)
     val allLinks =  expensiveLink :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, allLinks.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(expensiveLink)
    }
  }
  "findAllRoutes".must {
    "find routes if there are valid links".in {
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Shuttle(toAirportsList(0), toAirportsList(1), testAirline1, 50, LinkClassValues.getInstance(10000, 0, 0)),
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)
      
      result.isDefinedAt(economyPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(economyPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
      }
      result.isDefinedAt(businessPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(businessPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
      }
      result.isDefinedAt(firstPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(firstPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
      }
    }

  }
  
  
//  val airport1 = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
//  val airport2 = Airport("", "", "", 0, 100, "", "", "", 0, 0, 0, 0, 0)
//  val airport3 = Airport("", "", "", 0, 200, "", "", "", 0, 0, 0, 0, 0)

  def isLoungePreference(preference: FlightPreference) : Boolean = {
    preference.isInstanceOf[AppealPreference] && preference.asInstanceOf[AppealPreference].loungeLevelRequired > 0
  }


  
  "IsLinkAffordable".must {
    "accept some routes with neutral conditions and shuttle as first and last link".in {
      val sfo = fromAirport.copy(latitude = 37.61899948120117, longitude = -122.375, id = 1)
      val sjc = fromAirport.copy(latitude = 37.362598, longitude = -121.929001, id = 2)
      val lax = fromAirport.copy(latitude = 33.942501, longitude = -118.407997, id = 3)
      val lgb = fromAirport.copy(latitude = 33.817699, longitude = -118.152, id = 4) //long beach
      sfo.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0, 0)))
      sjc.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0, 0)))
      lax.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0, 0)))
      lgb.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0, 0)))

      val sjcToSfo = Shuttle(sjc, sfo, testAirline1, Computation.calculateDistance(sfo, sjc), LinkClassValues.getInstance(economy = 1000))
      val sfoToLax = {
        val fromAirport = sfo
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
        newLink.setQuality(50)
        newLink
      }
      val laxToLgb = Shuttle(lax, lgb, testAirline1, Computation.calculateDistance(lax, lgb), LinkClassValues.getInstance(economy = 1000))

      val links = List(sjcToSfo, sfoToLax, laxToLgb)

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(sfo).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                new LinkConsideration(link, cost, preferredLinkClass, false)
              }

              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })

              if (PassengerSimulation.isRouteAffordable(route, sfo, lgb, preferredLinkClass)) {
                totalAcceptedRoutes += 1
              }
              totalRoutes += 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.3)
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.5)
    }
  }
}
