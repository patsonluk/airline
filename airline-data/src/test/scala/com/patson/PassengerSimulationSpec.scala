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
 
class PassengerSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1", 1)
  val testAirline2 = Airline("airline 2", 2)
  val fromAirport = Airport.fromId(1)
  val airlineAppeal = AirlineAppeal(0, 100)
  fromAirport.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  val toAirportsList = List(Airport.fromId(2), Airport.fromId(3), Airport.fromId(4))
  toAirportsList.foreach {
    _.initAirlineAppeals(Map(testAirline1.id -> airlineAppeal, testAirline2.id -> airlineAppeal))
  }
  val toAirports = Set[Airport]()
  toAirports ++= toAirportsList
  val allAirports = Set[Airport]()
  allAirports ++= toAirports
  allAirports += fromAirport
  
  
//  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
//  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  
  //def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
  "Find shortest route".must {
    "find no route if there's no links".in {
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, List.empty, 3)
      routes.size.shouldBe(0)
    }
    "find n route if there's 1 link to each target".in {
      val links = toAirports.foldRight(List[LinkConsideration]()) { (airport, foldList) =>
        LinkConsideration(Link(fromAirport, airport, testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false) :: foldList
      }
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.size.shouldBe(toAirports.size)
      toAirports.foreach { toAirport => routes.isDefinedAt(toAirport).shouldBe(true) }
    }
    "find route if there's a link chain to target within max hop".in {
      val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find route if there's a reverse link chain to target within max hop".in {
      val links = List(LinkConsideration(Link(toAirportsList(2), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, true),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, true),
          LinkConsideration(Link(toAirportsList(0), fromAirport, testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, true))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find no route if there's a link chain to target but exceed max hop".in {
     val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
    "find a cheaper route even with connection flights (with frequent service)".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), distance = 3500, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 42), 3500, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), distance = 3500, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 42), 3500, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), distance = 3500, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 42), 3500, ECONOMY, false))
     val allLinks = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, duration = 600, frequency = 1), 13000, ECONOMY, false) :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(cheapLinks)
    }
    "user direct route even though it's more expensive as connection flight is not frequent enough".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 1), 200, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 1), 200, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, duration = 200, frequency = 1), 200, ECONOMY, false))
     val expensiveLink = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, duration = 600, frequency = 1), 1400, ECONOMY, false)
     val allLinks =  expensiveLink :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(expensiveLink)
    }
    
    "use expensive route if cheaper route exceed max hop".in {
     val cheapLinks = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false))
     val expensiveLink = LinkConsideration(Link(fromAirport, toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 301, ECONOMY, false)
     val allLinks = expensiveLink :: cheapLinks
          
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(List(expensiveLink))
    }
    "find no route if there's a link chain to target but one is not in correct direction".in {
     val links = List(LinkConsideration(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false),
          LinkConsideration(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, true), //wrong direction
          LinkConsideration(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100), 10000, LinkCapacity.getInstance(10000), 0, 600, 1), 100, ECONOMY, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
  }
  "findAllRoutes".must {
    "find routes if there're valid links".in {
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1),
                      Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1), 
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1))
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0))
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0))
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0))

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = Await.result(PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links), Duration.Inf)
      
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
    "find routes if there're valid links (from and to inverse)".in {
      val links = List(Link(toAirportsList(2), toAirportsList(1), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1),
                      Link(toAirportsList(1), toAirportsList(0), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1), 
                      Link(toAirportsList(0), fromAirport, testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1))
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0))
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0))
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0))

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = Await.result(PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links), Duration.Inf)
      
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
    "find no route if there's link but no capacity left for the specified class".in {
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(0, 10000, 0), 0, 600, 1),
                      Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(0, 10000, 0), 0, 600, 1), 
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(0, 10000, 0), 0, 600, 1))
      
      val economyPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, ECONOMY, 0))
      val businessPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, BUSINESS, 0))
      val firstPassengerGroup = PassengerGroup(fromAirport, AppealPreference(Map.empty, FIRST, 0))

      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = Await.result(PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links), Duration.Inf)
      
      result(economyPassengerGroup).isEmpty.shouldBe(true) //no seat for economy
      result(firstPassengerGroup).isEmpty.shouldBe(true) //no seat for first
      
      toAirports.foreach { toAirport =>
        result(businessPassengerGroup).isDefinedAt(toAirport).shouldBe(true)
      }
    }
    "find no route if the airline has no awareness at the fromAirport".in {
      val clonedFromAirport = fromAirport.copy()
      clonedFromAirport.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(0, 0)))
      val links = List(Link(clonedFromAirport, toAirportsList(0), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1),
                      Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1), 
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkPrice.getInstance(100, 100, 100), 10000, LinkCapacity.getInstance(10000, 10000, 10000), 0, 600, 1))
      
      val economyPassengerGroup = PassengerGroup(clonedFromAirport, AppealPreference(Map.empty, ECONOMY, 0))
      
      val toAirports = Set[Airport]()
      toAirports ++= toAirportsList
      val result : Map[PassengerGroup, Map[Airport, Route]] = Await.result(PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports), links), Duration.Inf)
      
      result(economyPassengerGroup).isEmpty.shouldBe(true) //no awareness
    }
  }
}
