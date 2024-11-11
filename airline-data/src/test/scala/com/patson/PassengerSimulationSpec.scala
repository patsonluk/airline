package com.patson

import java.util.Collections
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import com.patson.PassengerSimulation.RouteRejectionReason
import com.patson.model.FlightType._
import com.patson.model._
import com.patson.model.airplane.AirplaneMaintenanceUtil
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.jdk.CollectionConverters._
import scala.collection.mutable.Set
 
class PassengerSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override protected def beforeAll() : Unit = {
    super.beforeAll()
    AirplaneMaintenanceUtil.setTestFactor(Some(1))
  }

  override protected def afterAll() : Unit = {
    AirplaneMaintenanceUtil.setTestFactor(None)
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)


  val fromAirport = Airport.fromId(1).copy(baseIncome = 40000, basePopulation = 1) //income 40k . mid income country
  val airlineAppeal = AirlineAppeal(0)
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
  val toAirports = Set(toAirportsList : _*)

  val allAirportIds = Set[Int]()
  allAirportIds ++= toAirports.map {
    _.id
  }
  allAirportIds += fromAirport.id
  val LOOP_COUNT = 10000

  def assignLinkConsiderationIds(linkConsiderations : List[LinkConsideration]) = {
    var id = 1
    linkConsiderations.foreach { linkConsideration =>
      if (linkConsideration.link.id == 0) {
        linkConsideration.link.id = id
      }
      id += 1
    }
  }

  def assignLinkIds(links : List[Link]) = {
    var id = 1
    links.foreach { link =>
      if (link.id == 0) {
        link.id = id
      }
      id += 1
    }
  }


  //  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
  //  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  val passengerGroup = PassengerGroup(fromAirport, SimplePreference(homeAirport = fromAirport, priceSensitivity = 1, preferredLinkClass = ECONOMY), PassengerType.BUSINESS)
  //def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
  "Find shortest route".must {
    "find no route if there's no links".in {
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, List.empty[LinkConsideration].asJava, Collections.emptyMap[Int, Int](), 3)
      routes.size.shouldBe(0)
    }
    "find n route if there's 1 link to each target".in {
      val links = toAirports.foldRight(List[LinkConsideration]()) { (airport, foldList) =>
        LinkConsideration.getExplicit(Link(fromAirport, airport, testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false) :: foldList
      }
      assignLinkConsiderationIds(links)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.size.shouldBe(toAirports.size)
      toAirports.foreach { toAirport => routes.isDefinedAt(toAirport).shouldBe(true) }
    }
    "find route if there's a link chain to target within max hop".in {
      val links = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      assignLinkConsiderationIds(links)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find route if there's a reverse link chain to target within max hop".in {
      val links = List(LinkConsideration.getExplicit(Link(toAirportsList(2), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true),
        LinkConsideration.getExplicit(Link(toAirportsList(0), fromAirport, testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true))
      assignLinkConsiderationIds(links)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find no route if there's a link chain to target but exceed max hop".in {
      val links = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      assignLinkConsiderationIds(links)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
    "find a cheaper route even with connection flights (with frequent service)".in {
      val cheapLinks = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false))
      val allLinks = LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC), 13000, ECONOMY, false) :: cheapLinks
      assignLinkConsiderationIds(allLinks)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, allLinks.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(cheapLinks)
    }
    "use direct route even though it's more expensive as connection flight is not frequent enough".in {
      val cheapLinks = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 400, ECONOMY, false))
      val expensiveLink = LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC), 1400, ECONOMY, false)
      val allLinks = expensiveLink :: cheapLinks
      assignLinkConsiderationIds(allLinks)
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, allLinks.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(expensiveLink)
    }

    "use expensive route if cheaper route exceed max hop".in {
      val cheapLinks = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      val expensiveLink = LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 301, ECONOMY, false)
      val allLinks = expensiveLink :: cheapLinks
      assignLinkConsiderationIds(allLinks)
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, allLinks.asJava, Collections.emptyMap[Int, Int](), 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(List(expensiveLink))
    }
    "find no route if there's a link chain to target but one is not in correct direction".in {
      val links = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true), //wrong direction
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      assignLinkConsiderationIds(links)
      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
  }
  "findAllRoutes".must {
    "find routes if there're valid links".in {
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)

      result.isDefinedAt(economyPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(economyPassengerGroup).isDefinedAt(toAirport).shouldBe(true)

        //should be the right class
        result(economyPassengerGroup)(toAirport).links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == ECONOMY)
        }
      }
      result.isDefinedAt(businessPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(businessPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
        //should be the right class
        result(businessPassengerGroup)(toAirport).links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == BUSINESS)
        }
      }
      result.isDefinedAt(firstPassengerGroup).shouldBe(true)
      toAirports.foreach { toAirport =>
        result(firstPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
        //should be the right class
        result(firstPassengerGroup)(toAirport).links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == FIRST)
        }
      }
    }
    "find routes if there're valid links (from and to inverse)".in {
      val links = List(Link(toAirportsList(2), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(1), toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(0), fromAirport, testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)

      toAirports.foreach { toAirport =>
        val route = result(economyPassengerGroup)(toAirport)
        route.links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == ECONOMY)
        }
      }

      toAirports.foreach { toAirport =>
        val route = result(businessPassengerGroup)(toAirport)
        route.links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == BUSINESS)
        }
      }

      toAirports.foreach { toAirport =>
        val route = result(firstPassengerGroup)(toAirport)
        route.links.foreach { linkConsideration =>
          assert(linkConsideration.linkClass == FIRST)
        }
      }
    }
    "find route only if there's link with capacity left for class lower than or equal to specified class".in {
      //only business class left on first link
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(100, 100, 100), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(0, 100, 0), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(100, 0, 0), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)

      //for economy class it should only be able to find routes to 1st airports
      //1st airport 
      assert(result(economyPassengerGroup)(toAirportsList(0)).links(0).linkClass == ECONOMY)
      //2nd airport, 3rd airport - no route as 2nd link only have business class available
      assert(!result(economyPassengerGroup).contains(toAirportsList(1)))
      assert(!result(economyPassengerGroup).contains(toAirportsList(2)))


      //for business class it should only able to find routes to all airpots
      //1st airport no downgrade
      assert(result(businessPassengerGroup)(toAirportsList(0)).links(0).linkClass == BUSINESS)
      //2nd airport no downgrade
      assert(result(businessPassengerGroup)(toAirportsList(1)).links(0).linkClass == BUSINESS)
      assert(result(businessPassengerGroup)(toAirportsList(1)).links(1).linkClass == BUSINESS)
      //3rd airport last link downgrade to ECONOMY
      assert(result(businessPassengerGroup)(toAirportsList(2)).links(0).linkClass == BUSINESS)
      assert(result(businessPassengerGroup)(toAirportsList(2)).links(1).linkClass == BUSINESS)
      assert(result(businessPassengerGroup)(toAirportsList(2)).links(2).linkClass == ECONOMY)


      //for first class it should be able to find routes to 1st and 2nd airport. Last airport is not reachable as it requires downgrade of 2 classes
      //1st airport no downgrade
      assert(result(firstPassengerGroup)(toAirportsList(0)).links(0).linkClass == FIRST)
      //2nd airport, second link downgraded to business
      assert(result(firstPassengerGroup)(toAirportsList(1)).links(0).linkClass == FIRST)
      assert(result(firstPassengerGroup)(toAirportsList(1)).links(1).linkClass == BUSINESS)
      //3rd airport, no link as last link only has economy (downgrade 2 classes - forbidden)
      assert(!result(economyPassengerGroup).contains(toAirportsList(2)))
    }

    "find routes if there're valid links with sufficient country openness".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C2", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C3", "", "", 1, 0, 0, 0, id = 3)
      val airport4 = Airport("", "", "Airport 4", 0, 120, "C4", "", "", 1, 0, 0, 0, id = 4)

      val airline1 = Airline("airline 1", id = 1)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport4, "C4", 1, 1, headquarter = true)))
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))

      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports += airport2
      toAirports += airport3
      toAirports += airport4

      val countryOpenness = Map[String, Int](
        "C1" -> 10,
        "C2" -> 10,
        "C3" -> 10,
        "C4" -> 10
      )

      val activeAirports = scala.collection.mutable.Set(List.range(1, 5) : _*)
      val result : Map[PassengerGroup, Map[Airport, Route]] =
        PassengerSimulation.findAllRoutes(
          Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports),
          links,
          activeAirports,
          countryOpenness = countryOpenness)


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

    "find only routes with sufficient country openness".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C2", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C3", "", "", 1, 0, 0, 0, id = 3)
      val airport4 = Airport("", "", "Airport 4", 0, 120, "C4", "", "", 1, 0, 0, 0, id = 4)

      val airline1 = Airline("airline 1", id = 1)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport4, "C4", 1, 1, headquarter = true)))
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))

      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports += airport2
      toAirports += airport3
      toAirports += airport4

      val countryOpenness = Map[String, Int](
        "C1" -> 10,
        "C2" -> 10,
        "C3" -> 5,
        "C4" -> 10
      )

      val activeAirports = scala.collection.mutable.Set(List.range(1, 5) : _*)
      val result : Map[PassengerGroup, Map[Airport, Route]] =
        PassengerSimulation.findAllRoutes(
          Map(economyPassengerGroup -> toAirports),
          links,
          activeAirports,
          countryOpenness = countryOpenness)


      result.isDefinedAt(economyPassengerGroup).shouldBe(true)
      result(economyPassengerGroup).isDefinedAt(airport2).shouldBe(true)
      result(economyPassengerGroup).isDefinedAt(airport3).shouldBe(true)
      result(economyPassengerGroup).isDefinedAt(airport4).shouldBe(false) //cannot goto airport4 as C3 will block it
    }

    "find routes with low country openness if the original passenger is domestic or it's a domestic connection flight".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C1", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C2", "", "", 1, 0, 0, 0, id = 3)
      val airport4 = Airport("", "", "Airport 4", 0, 120, "C2", "", "", 1, 0, 0, 0, id = 4)
      val airport5 = Airport("", "", "Airport 5", 0, 150, "C3", "", "", 1, 0, 0, 0, id = 5)

      val airline1 = Airline("airline 1", id = 1)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport5, "C3", 1, 1, headquarter = true)))
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport5.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))

      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport4, airport5, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports += airport2
      toAirports += airport3
      toAirports += airport4
      toAirports += airport5

      val countryOpenness = Map[String, Int](
        "C1" -> 5,
        "C2" -> 5,
        "C3" -> 10
      )

      val activeAirports = scala.collection.mutable.Set(List.range(1, 5) : _*)
      val result : Map[PassengerGroup, Map[Airport, Route]] =
        PassengerSimulation.findAllRoutes(
          Map(economyPassengerGroup -> toAirports),
          links,
          activeAirports,
          countryOpenness = countryOpenness)

      result.isDefinedAt(economyPassengerGroup).shouldBe(true)
      result(economyPassengerGroup).isDefinedAt(airport2).shouldBe(true) //ok domestic flight
      result(economyPassengerGroup).isDefinedAt(airport3).shouldBe(true) //ok originate passenger is domestic
      result(economyPassengerGroup).isDefinedAt(airport4).shouldBe(true) //ok domestic connection flight
      result(economyPassengerGroup).isDefinedAt(airport5).shouldBe(false) //nope, C2 would block it as it needs 6th freedom here
    }

    "find routes (inversed Links) with low country openness if the original passenger is domestic or it's a domestic connection flight".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C1", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C2", "", "", 1, 0, 0, 0, id = 3)
      val airport4 = Airport("", "", "Airport 4", 0, 120, "C2", "", "", 1, 0, 0, 0, id = 4)
      val airport5 = Airport("", "", "Airport 5", 0, 150, "C3", "", "", 1, 0, 0, 0, id = 5)

      val airline1 = Airline("airline 1", id = 1)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport5, "C3", 1, 1, headquarter = true)))
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))
      airport5.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0)))

      val links = List(Link(airport5, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport4, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport3, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport1, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport]()
      toAirports += airport2
      toAirports += airport3
      toAirports += airport4
      toAirports += airport5

      val countryOpenness = Map[String, Int](
        "C1" -> 5,
        "C2" -> 5,
        "C3" -> 10
      )

      val activeAirports = scala.collection.mutable.Set(List.range(1, 5) : _*)
      val result : Map[PassengerGroup, Map[Airport, Route]] =
        PassengerSimulation.findAllRoutes(
          Map(economyPassengerGroup -> toAirports),
          links,
          activeAirports,
          countryOpenness = countryOpenness)

      result.isDefinedAt(economyPassengerGroup).shouldBe(true)
      result(economyPassengerGroup).isDefinedAt(airport2).shouldBe(true) //ok domestic flight
      result(economyPassengerGroup).isDefinedAt(airport3).shouldBe(true) //ok originate passenger is domestic
      result(economyPassengerGroup).isDefinedAt(airport4).shouldBe(true) //ok domestic connection flight
      result(economyPassengerGroup).isDefinedAt(airport5).shouldBe(false) //nope, C2 would block it as it needs 6th freedom here
    }

    "prefer routes with flights with same airline if available".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C1", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C2", "", "", 1, 0, 0, 0, id = 3)

      val airline1 = Airline("airline 1", id = 1)
      val airline2 = Airline("airline 2", id = 2)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport1, "C1", 1, 1, headquarter = true)))
      airline2.setBases(List[AirlineBase](AirlineBase(airline2, airport1, "C1", 1, 1, headquarter = true)))

      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0), airline2.id -> AirlineAppeal(0)))

      val countryOpenness = Map[String, Int](
        "C1" -> 10,
        "C2" -> 10)


      val links = List(Link(airport1, airport2, airline1, price = Pricing.computeStandardPriceForAllClass(2000, SHORT_HAUL_DOMESTIC), 2000, capacity = LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport3, airline1, price = Pricing.computeStandardPriceForAllClass(2000, SHORT_HAUL_DOMESTIC), 2000, capacity = LinkClassValues.getInstance(10000, 0, 0), 0, 600, 1, SHORT_HAUL_DOMESTIC),
        Link(airport2, airport3, airline2, price = Pricing.computeStandardPriceForAllClass(2000, SHORT_HAUL_DOMESTIC), 2000, capacity = LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      assignLinkIds(links)
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(airport1, AppealPreference(airport1, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)

      val toAirports = Set[Airport](airport3)

      val activeAirports = scala.collection.mutable.Set(List.range(1, 4) : _*)

      var economyAirline1 = 0
      var businessAirline1 = 0
      var firstAirline1 = 0
      val iterations = 1000

      for (i <- 0 until iterations) {
        val result : Map[PassengerGroup, Map[Airport, Route]] =
          PassengerSimulation.findAllRoutes(
            Map(economyPassengerGroup -> toAirports,
              businessPassengerGroup -> toAirports,
              firstPassengerGroup -> toAirports),
            links,
            activeAirports,
            countryOpenness = countryOpenness)
        assert(result(economyPassengerGroup)(airport3).links(0).link.airline == airline1)
        assert(result(businessPassengerGroup)(airport3).links(0).link.airline == airline1)
        assert(result(firstPassengerGroup)(airport3).links(0).link.airline == airline1)


        if (result(economyPassengerGroup)(airport3).links(1).link.airline == airline1) {
          economyAirline1 += 1
        }
        if (result(businessPassengerGroup)(airport3).links(1).link.airline == airline1) {
          businessAirline1 += 1
        }
        if (result(businessPassengerGroup)(airport3).links(1).link.airline == airline1) {
          firstAirline1 += 1
        }
      }

      assert(economyAirline1.toDouble / iterations > 0.7) //most pax should choose airline 1
      assert(businessAirline1.toDouble / iterations < 0.4) //most pax should choose airline 2 which has proper business class
      assert(firstAirline1.toDouble / iterations < 0.4) //most pax should choose airline 2 which has proper first class
    }
  }

//  val airport1 = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
//  val airport2 = Airport("", "", "", 0, 100, "", "", "", 0, 0, 0, 0, 0)
//  val airport3 = Airport("", "", "", 0, 200, "", "", "", 0, 0, 0, 0, 0)
  

  
  def isLoungePreference(preference: FlightPreference) : Boolean = {
    preference.isInstanceOf[AppealPreference] && preference.asInstanceOf[AppealPreference].loungeLevelRequired > 0
  }
  
  "IsLinkAffordable".must {
    "accept almost all route (single link) at 60% of suggested price and neutral quality and 50 loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val duration = Computation.computeStandardFlightDuration(distance)
      val price = suggestedPrice * 0.6
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration = duration, frequency = 14, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.99)
    }
    "accept some route (single link) at 70% of suggested price with 0 quality/loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 0.7
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration = duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
               }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.4)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.6)
    }
    "accept most route (single link) at suggested price with neutral quality and decent loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.7)
    }
    
    "accept some (single link) at suggested price with neutral quality and no loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
     assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.3)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.5)
    }
    "accept some (single link) at suggested price with neutral quality and no loyalty for low income country".in {
      val clonedFromAirport  = fromAirport.copy(baseIncome = Country.LOW_INCOME_THRESHOLD / 2)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.3)
       assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.5)
    }
    
    "accept some (single link) at suggested price with neutral quality and no loyalty for very low income country".in {
      val clonedFromAirport  = fromAirport.copy(baseIncome = Country.LOW_INCOME_THRESHOLD / 10)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.2)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.4)
    }
    
    "accept almost no link at 1.2 suggested price with 0 quality and no loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.05)
      
    }
    
    "accept very few link at 1.4 x suggested price with neutral quality and decent loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.4
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.1)
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0)
    }
    
    "accept almost no link at 3 x suggested price with max quality and max loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 100)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 3
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = 100
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val breakdown = flightPreference.computeCostBreakdown(newLink, preferredLinkClass)
              val cost = breakdown.cost
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
                //println(s"accepted $flightPreference -> $breakdown")
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes < 10)
    }

    "accept very few link at 2 x suggested price with max quality and max loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 100)))

      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = 100
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCostBreakdown(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost.cost, preferredLinkClass, false))


              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
                //println(s"$flightPreference to $cost")
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.3)
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0)
    }
    
    "accept very few link at 2 x suggested price with max quality and max loyalty but very low income country".in {
      val clonedFromAirport  = fromAirport.copy(baseIncome = 1000)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 100)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = 100
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.1)
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0)
    }
    
    "accept no link at 2 x suggested price with no quality and no loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes == 0)
    }
    
    

    "accept few link at suggested price with neutral quality and decent loyalty but downgrade in class (from business to econ)".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 0, 0), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.filter(_._1 == BUSINESS).foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, ECONOMY)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, ECONOMY, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.25)
    }

    "accept some links at 2 x suggested price with neutral quality and decent loyalty for SUPERSONIC flight of Speedy Eco pax".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50)))

      val toAirport = toAirportsList(2)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.calculateDuration(2000, distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, ECONOMY)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)

      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        val preferredLinkClass = ECONOMY
        val flightPreference = SpeedPreference(clonedFromAirport, ECONOMY)

        val cost = flightPreference.computeCost(newLink, preferredLinkClass)
        val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))

        val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
        if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
          totalAcceptedRoutes = totalAcceptedRoutes + 1
        }
        totalRoutes = totalRoutes + 1
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.5)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.8)
    }

    "accept some links at 1.4 x suggested price with neutral quality and decent loyalty for SUPERSONIC flight of Appeal First Class pax".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50)))

      val toAirport = toAirportsList(2)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.calculateDuration(2000, distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.4
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)

      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        val preferredLinkClass = FIRST
        val flightPreference = AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1.1, 0)

        val cost = flightPreference.computeCost(newLink, preferredLinkClass)
        val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))

        val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
        if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
          totalAcceptedRoutes = totalAcceptedRoutes + 1
        }
        totalRoutes = totalRoutes + 1
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.2)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.4)
    }
    
    "accept very few route with links are at 1.25 price with neutral quality and 0 loyalty".in { //will be less than single link cause each run fitler out some
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      var airportWalker = clonedFromAirport
      val links = toAirportsList.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val quality = fromAirport.expectedQuality(linkType, FIRST)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice * 1.25, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          newLink.setQuality(quality)
          airportWalker = toAirport
          newLink }

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              totalRoutes += 1
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirportsList.last, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes += 1
              }
            }
          }
        }
      }
      
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.1)
    }
    
    
    
    
    "reject route with one short link at 4X suggested price at min loyalty and quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //even if the last segment is really short
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          //make last link really expensive
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice * (if (toAirport == toAirports.last) 4 else 1), distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          airportWalker = toAirport
          newLink }
      
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 100 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              assert(!PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass).isEmpty)
            }
          }
        }
      }
    }


    "accept some routes with suggested price at good quality but no loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))

      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //the last segment is really short
      )

      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
        val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
        val duration = Computation.computeStandardFlightDuration(distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val quality = 70
        val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
        newLink.setQuality(quality)
        airportWalker = toAirport
        newLink }


      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }

              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })

              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass).isEmpty) {
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

    "accept most routes with suggested price at good quality and good loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 80)))

      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //the last segment is really short
      )

      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
        val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
        val duration = Computation.computeStandardFlightDuration(distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val quality = 70
        val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
        newLink.setQuality(quality)
        airportWalker = toAirport
        newLink }


      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }

              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })

              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes += 1
              }
              totalRoutes += 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.8)
    }


    
    "accept most routes with suggested price at neutral quality and decent loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //the last segment is really short
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val quality = fromAirport.expectedQuality(linkType, FIRST)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          newLink.setQuality(quality)
          airportWalker = toAirport
          newLink }
      
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes += 1
              }
              totalRoutes += 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.6)
    }
    
    
    
    "reject route that at 2X suggested price at min loyalty and quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      var airportWalker = clonedFromAirport
      val links = toAirportsList.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice * 2, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              
              assert(PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirportsList.last, preferredLinkClass) == Some(RouteRejectionReason.TOTAL_COST), route + " " + flightPreference)
            }
          }
        }
      }
    }
    
    "accept most route that all links are at 60% price and the total distance travel is 1.25x of the actual displacement with neutral quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 112.25, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 100, "", "", "", 1, 0, 0, 0, id = 3) //displacement is 100, while distance is 125
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport) * 0.6
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 50, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          newLink.setQuality(50)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) { 
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes += 1
              }
              totalRoutes += 1
              
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.8)
    }
    
    "reject route that all links are at suggested price and the total distance travel is 3x of the actual displacement".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 60, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 3) //displacement is 30, while distance is 90
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val duration = Computation.computeStandardFlightDuration(distance)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, cost, preferredLinkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              assert(PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirports.last, preferredLinkClass) == Some(RouteRejectionReason.DISTANCE), route + " " + flightPreference)
            }
          }
        }
      }
    }
   
    "reject most links at standard price if it does not fulfill lounge requirement (long flight)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      
      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT) //no lounge on the other side... so it's a no
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.2)
    }

    "reject some links at standard price if it does not fulfill lounge requirement (short flight)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))

      val toAirport = toAirportsList(0).copy(longitude = 10, size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT) //no lounge on the other side... so it's a no
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.7)
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.4)
    }
  
    "accept few links at standard price if it fulfill some lounge requirement (long flight level 1 at departing airport only)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      clonedFromAirport.initLounges(List(Lounge(airline = testAirline1, allianceId = None, airport = clonedFromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      
      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.2)
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.4)
    }
    
    "accept some links at standard price if it fulfill some lounge requirement (long flight level 1 at both airports)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      clonedFromAirport.initLounges(List(Lounge(airline = testAirline1, allianceId = None, airport = clonedFromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      
      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      toAirport.initLounges(List(Lounge(airline = testAirline1, allianceId = None, airport = toAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)    
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.2)
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.5)
    }
    
    "accept most links at standard price if it fulfill all lounge requirements (level 3 at both airports)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      clonedFromAirport.initLounges(List(Lounge(airline = testAirline1, allianceId  = None, airport = clonedFromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      
      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      toAirport.initLounges(List(Lounge(airline = testAirline1, allianceId  = None, airport = toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.9)
    }

    "accept some links at 1.3 * price if it fulfill all lounge requirements (level 3 at both airports)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      clonedFromAirport.initLounges(List(Lounge(airline = testAirline1, allianceId  = None, airport = clonedFromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it

      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      toAirport.initLounges(List(Lounge(airline = testAirline1, allianceId  = None, airport = toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) //only from airport has it
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.3
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)


      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.4)
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.6)
    }
    
    "accept most at standard price if it fulfill all lounge requirements (level 3 at both airports from alliance)".in {
      val clonedFromAirport  = fromAirport.copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50)))
      clonedFromAirport.initLounges(List(Lounge(airline = testAirline2, allianceId = Some(1), airport = clonedFromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) 
      testAirline1.setAllianceId(1)
      val toAirport = toAirportsList(2).copy(size = Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT)
      toAirport.initLounges(List(Lounge(airline = testAirline2, allianceId = Some(1), airport = toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0))) 
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val duration = Computation.computeStandardFlightDuration(distance)
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = fromAirport.expectedQuality(linkType, FIRST)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType)
      newLink.setQuality(quality)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(isLoungePreference(_)).foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink, preferredLinkClass)
              val linkConsiderations = List[LinkConsideration] (LinkConsideration.getExplicit(newLink, cost, preferredLinkClass, false))
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.getRouteRejection(route, clonedFromAirport, toAirport, preferredLinkClass).isEmpty) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.9)
    }
  }
  "Find affordable route".must {
    "accept a route if all links are reasonable".in {
      val distances = List(
        Computation.calculateDistance(fromAirport, toAirportsList(0)),
        Computation.calculateDistance(toAirportsList(0), toAirportsList(1)),
          Computation.calculateDistance(toAirportsList(1), toAirportsList(2))
      )
      val flightTypes = List(
        Computation.getFlightType(fromAirport, toAirportsList(0)),
        Computation.getFlightType(toAirportsList(0), toAirportsList(1)),
        Computation.getFlightType(toAirportsList(1), toAirportsList(2))
      )
      val standardPrices = List(
        Pricing.computeStandardPriceForAllClass(distances(0), flightTypes(0)),
        Pricing.computeStandardPriceForAllClass(distances(1), flightTypes(1)),
        Pricing.computeStandardPriceForAllClass(distances(2), flightTypes(2))
      )
      val durations = List(
        Computation.calculateDuration(900, distances(0)),
        Computation.calculateDuration(900, distances(0)),
        Computation.calculateDuration(900, distances(0)),
      )

      val links =
        List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, standardPrices(0), distances(0), LinkClassValues.getInstance(10000), 0, durations(0), 1, flightTypes(0)), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, standardPrices(1), distances(1), LinkClassValues.getInstance(10000), 0, durations(1), 1, flightTypes(1)), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, standardPrices(2), distances(2), LinkClassValues.getInstance(10000), 0, durations(2), 1, flightTypes(2)), 100, ECONOMY, false))
      assignLinkConsiderationIds(links)

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, Set(toAirportsList(2)), allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      assert(routes.size == 1)
      for (i <- 0 until 10000) {
        assert(PassengerSimulation.getRouteRejection(routes(toAirportsList(2)), fromAirport, toAirportsList(2), ECONOMY).isEmpty)
      }
    }
  }
}
