package com.patson

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model._
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.FlightType._
 
class PassengerSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)
  val fromAirport = Airport.fromId(1)
  val airlineAppeal = AirlineAppeal(0, 100)
  fromAirport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  val toAirportsList = List(
      Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
      Airport("", "", "To Airport", 0, 60, "", "", "", 1, 0, 0, 0, id = 3),
      Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 4))
  
  
  toAirportsList.foreach {
    _.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  }
  val toAirports = Set(toAirportsList  : _*)
  
  val allAirportIds = Set[Int]()
  allAirportIds ++= toAirports.map { _.id }
  allAirportIds += fromAirport.id
  
  
//  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
//  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  
  //def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
  "Find shortest route".must {
    "find no route if there's no links".in {
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, List.empty, 3)
      routes.size.shouldBe(0)
    }
    "find n route if there's 1 link to each target".in {
      val links = toAirports.foldRight(List[LinkConsideration]()) { (airport, foldList) =>
        LinkConsideration(Link(fromAirport, airport, testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false) :: foldList
      }
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, links, 3)
      routes.size.shouldBe(toAirports.size)
      toAirports.foreach { toAirport => routes.isDefinedAt(toAirport).shouldBe(true) }
    }
    "find route if there's a link chain to target within max hop".in {
      val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find route if there's a reverse link chain to target within max hop".in {
      val links = List(LinkConsideration(Link(toAirportsList(2), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true),
          LinkConsideration(Link(toAirportsList(0), fromAirport, testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find no route if there's a link chain to target but exceed max hop".in {
     val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, links, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
    "find a cheaper route even with connection flights (with frequent service)".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), distance = 3500, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 42, SHORT_HAUL_DOMESTIC), 3500, ECONOMY, false))
     val allLinks = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC), 13000, ECONOMY, false) :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(cheapLinks)
    }
    "use direct route even though it's more expensive as connection flight is not frequent enough".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 200, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 200, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC), 200, ECONOMY, false))
     val expensiveLink = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC), 1400, ECONOMY, false)
     val allLinks =  expensiveLink :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(expensiveLink)
    }
    
    "use expensive route if cheaper route exceed max hop".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
     val expensiveLink = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 301, ECONOMY, false)
     val allLinks = expensiveLink :: cheapLinks
          
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, allLinks, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(List(expensiveLink))
    }
    "find no route if there's a link chain to target but one is not in correct direction".in {
     val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, true), //wrong direction
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirportIds, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
  }
  "findAllRoutes".must {
    "find routes if there're valid links".in {
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0), PassengerType.BUSINESS)

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
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0), PassengerType.BUSINESS)

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
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0), PassengerType.BUSINESS)

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
    "find no route if the airline has no awareness at the fromAirport".in {
      val clonedFromAirport = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      val links = List(Link(clonedFromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(clonedFromAirport, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      
      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports), links, allAirportIds)
      
      result(economyPassengerGroup).isEmpty.shouldBe(true) //no awareness
    }
    
    "find routes if there're valid links with sufficient country openness".in {
      val airport1 = Airport("", "", "Airport 1", 0, 30, "C1", "", "", 1, 0, 0, 0, id = 1)
      val airport2 = Airport("", "", "Airport 2", 0, 60, "C2", "", "", 1, 0, 0, 0, id = 2)
      val airport3 = Airport("", "", "Airport 3", 0, 90, "C3", "", "", 1, 0, 0, 0, id = 3)
      val airport4 = Airport("", "", "Airport 4", 0, 120, "C4", "", "", 1, 0, 0, 0, id = 4)
      
      val airline1 = Airline("airline 1", id = 1)
      airline1.setBases(List[AirlineBase](AirlineBase(airline1, airport4, "C4", 1, 1, headquarter = true)))
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
          
      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, BUSINESS, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, FIRST, 0), PassengerType.BUSINESS)

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
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
          
      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      
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
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport5.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
          
      val links = List(Link(airport1, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport2, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(airport3, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport4, airport5, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      
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
      airport1.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport2.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport3.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport4.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
      airport5.initAirlineAppeals(Map(airline1.id -> AirlineAppeal(0, 100)))
          
      val links = List(Link(airport5, airport4, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport4, airport3, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC), 
                      Link(airport3, airport2, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC),
                      Link(airport2, airport1, airline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC))
      
      val economyPassengerGroup = PassengerGroup(airport1, AppealPreference(Map.empty, ECONOMY, 0), PassengerType.BUSINESS)
      
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
    
  }
  
  
//  val airport1 = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
//  val airport2 = Airport("", "", "", 0, 100, "", "", "", 0, 0, 0, 0, 0)
//  val airport3 = Airport("", "", "", 0, 200, "", "", "", 0, 0, 0, 0, 0)
  
  val LOOP_COUNT = 10000
  
  "IsLinkAffordable".must {
    "accept almost all route (single link) at 60% of suggested price and neutral quality and 50 loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 0.6
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.99)
    }
    "accept 80% (but not all) route (single link) at 60% of suggested price with 0 quality/loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 0.6
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, 600, 1, linkType)
          
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.8)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 1)
    }
     "accept at least 80% route (single link) at suggested price with neutral quality and decent loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(50, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.7)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.95)
    }
    
    "accept at around 30 - 50% (single link) at suggested price with neutral quality and no loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
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
    
    "accept almost no link at 1.5 suggested price with neutral quality and no loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.5
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.05)
      
    }
    
    "accept very few link at 1.5 x suggested price with neutral quality and decent loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.5
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.1)
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.0)
    }
    
    "accept 20 - 40% at 1.5 x suggested price with max quality and max loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 100, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 1.5
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = 100
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
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
    
    "accept no link at 2 x suggested price with neutral quality and decent loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes == 0)
    }
    
    
    "accept ver few link (<1%) at 2 x suggested price with max quality and max loyalty".in { 
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 100, 0)))
      
      val toAirport = toAirportsList(0)
      val distance = Util.calculateDistance(clonedFromAirport.latitude, clonedFromAirport.longitude, toAirport.latitude, toAirport.longitude).intValue()
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, clonedFromAirport, toAirport)
      val price = suggestedPrice * 2
      val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
      val quality = 100
      val newLink = Link(clonedFromAirport, toAirport, testAirline1, price = price, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val cost = flightPreference.computeCost(newLink)
              val linkConsiderations = List[LinkConsideration] (new LinkConsideration(newLink, cost, linkClass, false))
              
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirport, linkClass)) {
                totalAcceptedRoutes = totalAcceptedRoutes + 1
              }
              totalRoutes = totalRoutes + 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.01)
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0)
    }
    
    
    "accept at around 10 - 20% route with links are at suggested price with neutral quality and 0 loyalty".in { //will be less than single link cause each run fitler out some
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      var airportWalker = clonedFromAirport
      val links = toAirportsList.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          airportWalker = toAirport
          newLink }

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              totalRoutes += 1
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirportsList.last, linkClass)) {
                totalAcceptedRoutes += 1
              }
            }
          }
        }
      }
      
      assert(totalAcceptedRoutes / totalRoutes.toDouble > 0.1)
      assert(totalAcceptedRoutes / totalRoutes.toDouble < 0.2)
    }
    
    "reject route with one short link at 4X suggested price at min loyalty and quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //even if the last segment is really short
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          //make last link really expensive
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice * (if (toAirport == toAirports.last) 4 else 1), distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, linkType)
          airportWalker = toAirport
          newLink }
      
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 100 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              assert(!PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirports.last, linkClass))
            }
          }
        }
      }
      
       
    }
    
    "accept 50 - 70% routes with suggested price at neutral quality and decent loyalty".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 50, 0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 90, "", "", "", 1, 0, 0, 0, id = 3),
        Airport("", "", "To Airport", 0, 92, "", "", "", 1, 0, 0, 0, id = 4) //the last segment is really short
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val quality = Link.neutralQualityOfClass(FIRST, clonedFromAirport, toAirport, linkType)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = quality, 600, 1, linkType)
          airportWalker = toAirport
          newLink }
      
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirports.last, linkClass)) {
                totalAcceptedRoutes += 1
              }
              totalRoutes += 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.5)
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.7)
    }
    
    
    
    "reject route that at 2X suggested price at min loyalty and quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      var airportWalker = clonedFromAirport
      val links = toAirportsList.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice * 2, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, linkType)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              
              assert(!PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirportsList.last, linkClass), route + " " + flightPreference)
            }
          }
        }
      }
    }
    
    "accept most route that all links are at 60% price and the total distance travel is 1.25x of the actual displacement with neutral quality".in {
      val clonedFromAirport  = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 112.25, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 100, "", "", "", 1, 0, 0, 0, id = 3) //displacement is 100, while distance is 125
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport) * 0.6
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 50, 600, 1, linkType)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) { 
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              if (PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirports.last, linkClass)) {
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
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      
      val toAirports = List[Airport] (
        Airport("", "", "To Airport", 0, 60, "", "", "", 1, 0, 0, 0, id = 2),
        Airport("", "", "To Airport", 0, 30, "", "", "", 1, 0, 0, 0, id = 3) //displacement is 30, while distance is 90
      )
      
      var airportWalker = clonedFromAirport
      val links = toAirports.map { toAirport =>
          val distance = Util.calculateDistance(airportWalker.latitude, airportWalker.longitude, toAirport.latitude, toAirport.longitude).intValue()
          val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
          val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, airportWalker, toAirport)
          val newLink = Link(airportWalker, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, linkType)
          airportWalker = toAirport
          newLink }
      
      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport).pool.foreach {
          case(linkClass, flightPreference) => {
            flightPreference.foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val cost = flightPreference.computeCost(link)
                new LinkConsideration(link, cost, linkClass, false)
              }
              
              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })
              assert(!PassengerSimulation.isRouteAffordable(route, clonedFromAirport, toAirports.last, linkClass), route + " " + flightPreference)
            }
          }
        }
      }
    }
    
    
  }
}
