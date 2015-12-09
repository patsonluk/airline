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
 
class PassengerSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1")
  val testAirline2 = Airline("airline 2")
  val fromAirport = Airport.fromId(1)
  val toAirportsList = List(Airport.fromId(2), Airport.fromId(3), Airport.fromId(4), Airport.fromId(5))
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
      val links = toAirports.foldRight(List[LinkWithCost]()) { (airport, foldList) =>
        LinkWithCost(Link(fromAirport, airport, testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false) :: foldList
      }
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.size.shouldBe(toAirports.size)
      toAirports.foreach { toAirport => routes.isDefinedAt(toAirport).shouldBe(true) }
    }
    "find route if there's a link chain to target within max hop".in {
      val links = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find route if there's a reverse link chain to target within max hop".in {
      val links = List(LinkWithCost(Link(toAirportsList(2), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, true),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(0), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, true),
          LinkWithCost(Link(toAirportsList(0), fromAirport, testAirline1, 100, 10000, 10000, 0, 600, 1), 100, true))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(links)
    }
    "find no route if there's a link chain to target but exceed max hop".in {
     val links = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
    "find a cheaper route even with connection flights (with frequent service)".in {
     val cheapLinks = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, distance = 3500, 10000, 0, duration = 200, frequency = 42), 3500, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, distance = 3500, 10000, 0, duration = 200, frequency = 42), 3500, false),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, distance = 3500, 10000, 0, duration = 200, frequency = 42), 3500, false))
     val allLinks = LinkWithCost(Link(fromAirport, toAirportsList(2), testAirline1, 100, 10000, 10000, 0, duration = 600, frequency = 1), 13000, false) :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      route.links.equals(cheapLinks)
    }
    "user direct route even though it's more expensive as connection flight is not frequent enough".in {
     val cheapLinks = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, 10000, 10000, 0, duration = 200, frequency = 1), 200, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, duration = 200, frequency = 1), 200, false),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, 10000, 10000, 0, duration = 200, frequency = 1), 200, false))
     val expensiveLink = LinkWithCost(Link(fromAirport, toAirportsList(2), testAirline1, 100, 10000, 10000, 0, duration = 600, frequency = 1), 1400, false)
     val allLinks =  expensiveLink :: cheapLinks
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(expensiveLink)
    }
    
    
    "use expensive route if cheaper route exceed max hop".in {
     val cheapLinks = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false))
     val expensiveLink = LinkWithCost(Link(fromAirport, toAirportsList(2), testAirline1, 100, 10000, 10000, 0, 600, 1), 301, false)
     val allLinks = expensiveLink :: cheapLinks
          
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, allLinks, 2)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      val route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(1)
      route.links.equals(List(expensiveLink))
    }
    "find no route if there's a link chain to target but one is not in correct direction".in {
     val links = List(LinkWithCost(Link(fromAirport, toAirportsList(0), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false),
          LinkWithCost(Link(toAirportsList(0), toAirportsList(1), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, true), //wrong direction
          LinkWithCost(Link(toAirportsList(1), toAirportsList(2), testAirline1, 100, 10000, 10000, 0, 600, 1), 100, false))
      
      val routes = PassengerSimulation.findShortestRoute(fromAirport, toAirports, allAirports, links, 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(false)
    }
  }
}
