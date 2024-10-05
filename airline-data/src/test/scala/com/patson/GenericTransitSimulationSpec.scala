package com.patson

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import com.patson.model.FlightType._
import com.patson.model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import java.util.Collections
import scala.collection.mutable
import scala.collection.mutable.Set
import scala.jdk.CollectionConverters._

class GenericTransitSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)
  val fromAirport = Airport.fromId(1).copy(baseIncome = 40000, basePopulation = 1, name = "F0") //income 40k . mid income country
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
  val LOOP_COUNT = 10000
  
  
//  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
//  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  val passengerGroup = PassengerGroup(fromAirport, SimplePreference(homeAirport = fromAirport, priceSensitivity = 1, preferredLinkClass = ECONOMY), PassengerType.BUSINESS) 
  //def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
  "Find shortest route".must {
    "find a route if there's generic transit offered".in {
      var links = List(LinkConsideration.getExplicit(GenericTransit(fromAirport, toAirportsList(0), 50, LinkClassValues.getInstance(10000), id = 1), 100, ECONOMY, false),
          LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 2), 100, ECONOMY, false),
          LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 3), 100, ECONOMY, false))
      
      var routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      var route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      assert(route.links.map(_.id).equals(links.map(_.id)))

      links = List(
        LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(GenericTransit(toAirportsList(0), toAirportsList(1), 50, LinkClassValues.getInstance(10000), id = 2), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 3), 100, ECONOMY, false))

      routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      assert(route.links.map(_.id).equals(links.map(_.id)))

      links = List(
        LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 2), 100, ECONOMY, false),
        LinkConsideration.getExplicit(GenericTransit(toAirportsList(1), toAirportsList(2), 50, LinkClassValues.getInstance(10000), id = 3), 100, ECONOMY, false))

      routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(2)).shouldBe(true)
      route = routes.get(toAirportsList(2)).get
      route.links.size.shouldBe(3)
      assert(route.links.map(_.id).equals(links.map(_.id)))

    }


    "direct flight more preferable than generic transit".in {
      val links = List(LinkConsideration.getExplicit(GenericTransit(fromAirport, toAirportsList(0), 50, LinkClassValues.getInstance(10000), id = 1), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 2), 100, ECONOMY, false),
        LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 3), 100, ECONOMY, false))

      val routes = PassengerSimulation.findShortestRoute(passengerGroup, toAirports, allAirportIds, links.asJava, Collections.emptyMap[Int, Int](), 3)
      routes.isDefinedAt(toAirportsList(1)).shouldBe(true)
      val route = routes.get(toAirportsList(1)).get
      route.links.size.shouldBe(1) //should take the direct flight only
    }
    "use direct route even though it's more expensive as connection flight is not frequent enough".in {
     val cheapLinks = List(LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC, id = 1), 400, ECONOMY, false),
          LinkConsideration.getExplicit(Link(toAirportsList(0), toAirportsList(1), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC, id = 2), 400, ECONOMY, false),
          LinkConsideration.getExplicit(Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 200, frequency = 1, SHORT_HAUL_DOMESTIC, id = 3), 400, ECONOMY, false))
     val expensiveLink = LinkConsideration.getExplicit(Link(fromAirport, toAirportsList(2), testAirline1, LinkClassValues.getInstance(100), 10000, LinkClassValues.getInstance(10000), 0, duration = 600, frequency = 1, SHORT_HAUL_DOMESTIC, id = 4), 1400, ECONOMY, false)
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
      val links = List(Link(fromAirport, toAirportsList(0), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 1),
                      GenericTransit(toAirportsList(0), toAirportsList(1), 50, LinkClassValues.getInstance(10000, 0, 0), id = 2),
                      Link(toAirportsList(1), toAirportsList(2), testAirline1, LinkClassValues.getInstance(100, 100, 100), 10000, LinkClassValues.getInstance(10000, 10000, 10000), 0, 600, 1, SHORT_HAUL_DOMESTIC, id = 3))
      
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

    "prefer direct flights but a few might still go for generic transit (from SFO to LAX , Transit LAX -> LGB, Flight SFO -> LAX, SFO -> LGB)".in {
      val sfo = fromAirport.copy(iata = "SFO", name = "SFO", latitude = 37.61899948120117, longitude = -122.375, id = 1)
      val sjc = fromAirport.copy(iata = "SJC", name = "SJC", latitude = 37.362598, longitude = -121.929001, id = 2)
      val lax = fromAirport.copy(iata = "LAX", name = "LAX", latitude = 33.942501, longitude = -118.407997, id = 3)
      val lgb = fromAirport.copy(iata = "LGB", name = "LGB", latitude = 33.817699, longitude = -118.152, id = 4) //long beach
      sfo.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      sjc.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lax.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lgb.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))

      val sjcToSfo = GenericTransit(sjc, sfo, Computation.calculateDistance(sfo, sjc), LinkClassValues.getInstance(economy = 10000), id = 1)
      val sfoToLax = {
        val fromAirport = sfo
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 2)
        newLink.setQuality(50)
        newLink
      }
      val sfoToLgb = {
        val fromAirport = sfo
        val toAirport = lgb
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 3)
        newLink.setQuality(50)
        newLink
      }

      val laxToLgb = GenericTransit(lax, lgb, Computation.calculateDistance(lax, lgb), LinkClassValues.getInstance(economy = 10000), id = 4)

      val links = List(sjcToSfo, sfoToLax, sfoToLgb, laxToLgb)
      val economyPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val toAirports = Set(lax)
      val paxDirectCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      val paxTransitCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      for (i <- 0 until 1000) {
        DemandGenerator.getFlightPreferencePoolOnAirport(sfo).pool.foreach {
          case(preferredLinkClass, flightPreferences) => {
            flightPreferences.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val result = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)
              result.foreach {
                case (paxGroup, routesByAirport) =>
                  //println(preferredLinkClass + " " + flightPreferences)
                  if (routesByAirport(lax).links.length == 1) {
                    paxDirectCount.put(paxGroup, paxDirectCount(paxGroup) + 1)
                  } else if (routesByAirport(lax).links.length == 2) {
                    paxTransitCount.put(paxGroup, paxTransitCount(paxGroup) + 1)
                  } else {
                    println(s"Unexpected route: ${routesByAirport(lax)}")
                  }
              }
            }
          }
        }
      }
      println(s"paxDirectCount $paxDirectCount")
      println(s"paxTransitCount $paxTransitCount")
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) < 0.2)
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) > 0.01)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) < 0.2)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) > 0.01)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) < 0.2)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) > 0.005)

    }

    "prefer direct flights but a few might still go for generic transit (from SFO to LAX , Transit SFO -> SJC, Flight SFO -> LAX, SJC -> LAX".in {
      val sfo = fromAirport.copy(iata = "SFO", name = "SFO", latitude = 37.61899948120117, longitude = -122.375, id = 1)
      val sjc = fromAirport.copy(iata = "SJC", name = "SJC", latitude = 37.362598, longitude = -121.929001, id = 2)
      val lax = fromAirport.copy(iata = "LAX", name = "LAX", latitude = 33.942501, longitude = -118.407997, id = 3)
      val lgb = fromAirport.copy(iata = "LGB", name = "LGB", latitude = 33.817699, longitude = -118.152, id = 4) //long beach
      sfo.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      sjc.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lax.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lgb.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))

      val sjcToSfo = GenericTransit(sjc, sfo, Computation.calculateDistance(sfo, sjc), LinkClassValues.getInstance(economy = 10000), id = 1)
      val sfoToLax = {
        val fromAirport = sfo
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 2)
        newLink.setQuality(50)
        newLink
      }
      val sjcToLax = {
        val fromAirport = sjc
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 3)
        newLink.setQuality(50)
        newLink
      }

      val laxToLgb = GenericTransit(lax, lgb, Computation.calculateDistance(lax, lgb), LinkClassValues.getInstance(economy = 10000), id = 4)

      val links = List(sjcToSfo, sfoToLax, sjcToLax, laxToLgb)
      val economyPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val toAirport = lax
      val toAirports = Set(toAirport)
      val paxDirectCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      val paxTransitCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      for (i <- 0 until 1000) {
        DemandGenerator.getFlightPreferencePoolOnAirport(sfo).pool.foreach {
          case(preferredLinkClass, flightPreferences) => {
            flightPreferences.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val result = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)
              result.foreach {
                case (paxGroup, routesByAirport) =>
                  //println(preferredLinkClass + " " + flightPreferences)
                  if (routesByAirport(toAirport).links.length == 1) {
                    paxDirectCount.put(paxGroup, paxDirectCount(paxGroup) + 1)
                  } else if (routesByAirport(toAirport).links.length == 2) {
                    paxTransitCount.put(paxGroup, paxTransitCount(paxGroup) + 1)
                  } else {
                    println(s"Unexpected route: ${routesByAirport(toAirport)}")
                  }
              }
            }
          }
        }
      }
      println(s"paxDirectCount $paxDirectCount")
      println(s"paxTransitCount $paxTransitCount")
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) < 0.2)
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) > 0.01)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) < 0.2)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) > 0.01)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) < 0.2)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) > 0.005)

    }

    "super cheap flight with generic transit can steal many pax but not all (from SFO to LAX , Transit SFO -> SJC, Flight SFO -> LAX, SJC -> LAX".in {
      val sfo = fromAirport.copy(iata = "SFO", name = "SFO", latitude = 37.61899948120117, longitude = -122.375, id = 1)
      val sjc = fromAirport.copy(iata = "SJC", name = "SJC", latitude = 37.362598, longitude = -121.929001, id = 2)
      val lax = fromAirport.copy(iata = "LAX", name = "LAX", latitude = 33.942501, longitude = -118.407997, id = 3)
      val lgb = fromAirport.copy(iata = "LGB", name = "LGB", latitude = 33.817699, longitude = -118.152, id = 4) //long beach
      sfo.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      sjc.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lax.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lgb.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))

      val sjcToSfo = GenericTransit(sjc, sfo, Computation.calculateDistance(sfo, sjc), LinkClassValues.getInstance(economy = 10000), id = 1)
      val sfoToLax = {
        val fromAirport = sfo
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
        val newLink = Link(fromAirport, toAirport, testAirline1, price = suggestedPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 2)
        newLink.setQuality(50)
        newLink
      }
      val sjcToLax = {
        val fromAirport = sjc
        val toAirport = lax
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        val duration = Computation.computeStandardFlightDuration(distance)
        val linkType = Computation.getFlightType(fromAirport, toAirport, distance)
        val cheapPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport) * 0.7
        val newLink = Link(fromAirport, toAirport, testAirline1, price = cheapPrice, distance = distance, LinkClassValues.getInstance(10000, 10000, 10000), rawQuality = 0, duration, frequency = Link.HIGH_FREQUENCY_THRESHOLD, linkType, id = 3)
        newLink.setQuality(50)
        newLink
      }

      val laxToLgb = GenericTransit(lax, lgb, Computation.calculateDistance(lax, lgb), LinkClassValues.getInstance(economy = 10000), id = 4)

      val links = List(sjcToSfo, sfoToLax, sjcToLax, laxToLgb)
      val economyPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val businessPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val firstPassengerGroup = PassengerGroup(sfo, AppealPreference(sfo, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0), PassengerType.BUSINESS)
      val toAirport = lax
      val toAirports = Set(toAirport)
      val paxDirectCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      val paxTransitCount = mutable.HashMap(economyPassengerGroup -> 0, businessPassengerGroup -> 0, firstPassengerGroup -> 0)
      for (i <- 0 until 1000) {
        DemandGenerator.getFlightPreferencePoolOnAirport(sfo).pool.foreach {
          case(preferredLinkClass, flightPreferences) => {
            flightPreferences.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val result = PassengerSimulation.findAllRoutes(Map(economyPassengerGroup -> toAirports, businessPassengerGroup -> toAirports, firstPassengerGroup -> toAirports), links, allAirportIds)
              result.foreach {
                case (paxGroup, routesByAirport) =>
                  //println(preferredLinkClass + " " + flightPreferences)
                  if (routesByAirport(toAirport).links.length == 1) {
                    paxDirectCount.put(paxGroup, paxDirectCount(paxGroup) + 1)
                  } else if (routesByAirport(toAirport).links.length == 2) {
                    paxTransitCount.put(paxGroup, paxTransitCount(paxGroup) + 1)
                  } else {
                    println(s"Unexpected route: ${routesByAirport(toAirport)}")
                  }
              }
            }
          }
        }
      }
      println(s"paxDirectCount $paxDirectCount")
      println(s"paxTransitCount $paxTransitCount")
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) > 2)
      assert(paxTransitCount(economyPassengerGroup).toDouble / paxDirectCount(economyPassengerGroup) < 5)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) > 0.7)
      assert(paxTransitCount(businessPassengerGroup).toDouble / paxDirectCount(businessPassengerGroup) < 1.5)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) > 0.2)
      assert(paxTransitCount(firstPassengerGroup).toDouble / paxDirectCount(firstPassengerGroup) < 0.5)

    }

  }
  
  
//  val airport1 = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
//  val airport2 = Airport("", "", "", 0, 100, "", "", "", 0, 0, 0, 0, 0)
//  val airport3 = Airport("", "", "", 0, 200, "", "", "", 0, 0, 0, 0, 0)

  def isLoungePreference(preference: FlightPreference) : Boolean = {
    preference.isInstanceOf[AppealPreference] && preference.asInstanceOf[AppealPreference].loungeLevelRequired > 0
  }


  
  "IsLinkAffordable".must {
    "accept some routes with neutral conditions and generic transit as first and last link".in {
      val sfo = fromAirport.copy(iata = "SFO", name = "SFO", latitude = 37.61899948120117, longitude = -122.375, id = 1)
      val sjc = fromAirport.copy(iata = "SJC", name = "SJC", latitude = 37.362598, longitude = -121.929001, id = 2)
      val lax = fromAirport.copy(iata = "LAX", name = "LAX", latitude = 33.942501, longitude = -118.407997, id = 3)
      val lgb = fromAirport.copy(iata = "LGB", name = "LGB", latitude = 33.817699, longitude = -118.152, id = 4) //long beach
      sfo.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      sjc.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lax.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))
      lgb.initAirlineAppeals(Map(testAirline1.id -> AirlineAppeal(loyalty = 0)))

      val sjcToSfo = GenericTransit(sjc, sfo, Computation.calculateDistance(sfo, sjc), LinkClassValues.getInstance(economy = 1000))
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
      val laxToLgb = GenericTransit(lax, lgb, Computation.calculateDistance(lax, lgb), LinkClassValues.getInstance(economy = 1000))

      val links = List(sjcToSfo, sfoToLax, laxToLgb)

      //hmm kinda mix in flight preference here...might not be a good thing... loop 10000 times so result is more consistent
      var totalRoutes = 0
      var totalAcceptedRoutes = 0;
      for (i <- 0 until LOOP_COUNT) {
        DemandGenerator.getFlightPreferencePoolOnAirport(sfo).pool.foreach {
          case(preferredLinkClass, flightPreference) => {
            flightPreference.filter(!isLoungePreference(_)).foreach {  flightPreference =>
              val linkConsiderations = links.map { link =>
                val costBreakdown = flightPreference.computeCostBreakdown(link, preferredLinkClass)
                LinkConsideration.getExplicit(link, costBreakdown.cost, preferredLinkClass, false)
              }

              val route = Route(linkConsiderations, linkConsiderations.foldLeft(0.0) { _ + _.cost })

              PassengerSimulation.getRouteRejection(route, sfo, lgb, preferredLinkClass) match {
                case None => totalAcceptedRoutes += 1
                case Some(rejection) => //println(s"$flightPreference $rejection on $route")
              }
              totalRoutes += 1
            }
          }
        }
      }
      assert(totalAcceptedRoutes.toDouble / totalRoutes < 0.7)
      assert(totalAcceptedRoutes.toDouble / totalRoutes > 0.5)
    }
  }
}
