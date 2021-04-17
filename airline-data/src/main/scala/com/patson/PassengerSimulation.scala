

package com.patson

import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

import com.patson.data.{AllianceSource, CountrySource, LinkSource}
import com.patson.model._

import scala.collection.mutable.{ListBuffer, Set}
import scala.util.Random
import scala.collection.parallel.CollectionConverters._

object PassengerSimulation {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  val countryOpenness : Map[String, Int] = CountrySource.loadAllCountries().map( country => (country.countryCode, country.openness)).toMap
  
  def testFlow() = {

    //val airportGroups = getAirportGroups(airportData)
    //println("Using " + airportData.size + " airport data");
    
    //val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)
    val demand = DemandGenerator.computeDemand(0)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

//    val airportData = AirportSource.loadAllAirports().filter( _.size >= 2)
//    val links = generateFlightLinks(airportData)
//    println("Generated " + links.size + " links")
    
    val links = LinkSource.loadAllLinks(LinkSource.FULL_LOAD)
    
    val consumptionResult = passengerConsume(demand, links)
    
    println("Consumption result: ")
    val soldLinks = links.filter{ link => link.getTotalSoldSeats > 0  }.map { link =>
      (link, link.getTotalSoldSeats)
      }.sortBy {
        case (_, soldSeats) => soldSeats 
      }
      
    soldLinks.foreach{ case(link, soldSeats) => println(link.airline.name + "($" + link.price + "; recommend $" + link.standardPrice(ECONOMY) + ") " + soldSeats  + " : " + link.from.name + " => " + link.to.name) }
    println("seats sold: " + soldLinks.foldLeft(0) {
      case (holder, (link, soldSeats)) => holder + soldSeats
    })
    
    
    //test
    //findShortestRoute(airportGroups(0)(0), airportGroups(4)(0), links.toList)
    //10 random
    //findRandomRoutes(airportGroups(0)(0), airportGroups(4)(0), links.toList, 10)
  }
  
  def passengerConsume[T <: Transport](demand : List[(PassengerGroup, Airport, Int)], links : List[T]) : (Map[(PassengerGroup, Airport, Route), Int], Map[(PassengerGroup, Airport), Int]) = {
     val consumptionResult = ListBuffer[(PassengerGroup, Airport, Int, Route)]()
    val missedDemandChunks = ListBuffer[(PassengerGroup, Airport, Int)]()
     val consumptionCycleMax = 10; //try and rebuild routes 10 times
     var consumptionCycleCount = 0;
     //start consumption cycles
     
     //find all active Airports
     val activeAirportIds = Set[Int]()
     links.foreach { link => 
       activeAirportIds.add(link.from.id)
       activeAirportIds.add(link.to.id)
     }
     println("Total active airports: " + activeAirportIds.size)
     
     println("Remove demand that is not covered by active airports, before " + demand.size);

     //randomize the demand chunks so later on it's consumed in a random (relatively even) manner
     var demandChunks = Random.shuffle(demand.filter { demandChunk =>
       val (passengerGroup, toAirport, chunkSize) = demandChunk
         val isConnected = activeAirportIds.contains(passengerGroup.fromAirport.id) && activeAirportIds.contains(toAirport.id)
         if (!isConnected) {
           missedDemandChunks.append(demandChunk)
         }
         isConnected
     }).sortWith((entry1, entry2) =>
       if (entry1._1.passengerType == PassengerType.OLYMPICS && entry2._1.passengerType == PassengerType.OLYMPICS) false else entry1._1.passengerType == PassengerType.OLYMPICS
     ) //olympics always come first
     
     println("After pruning : " + demandChunks.size);


     while (consumptionCycleCount < consumptionCycleMax) {
       println("Run " + consumptionCycleCount + " demand chunk count " + demandChunks.size)
       println("links: " + links.size)
       
       //find out required routes - which "to airports" does each passengerGroup has
       print("Find required routes...")
       val requiredRoutes = scala.collection.mutable.Map[PassengerGroup, Set[Airport]]()
       demandChunks.foreach {
         case (passengerGroup, toAirport, _) =>
           var toAirports : Set[Airport] = requiredRoutes.getOrElseUpdate(passengerGroup, scala.collection.mutable.Set[Airport]())
           toAirports.add(toAirport)
       }
       println("Done!")
       
       //remove exhausted links
       val availableLinks = links.filter { _.getTotalAvailableSeats > 0 }
       
       println("Available links: " + availableLinks.length)
       
//       val routesFuture = findAllRoutes(requiredRoutes.toMap, availableLinks, activeAirportIds)
//       val allRoutesMap = Await.result(routesFuture, Duration.Inf)
       val iterationCount = if (consumptionCycleCount < 3) 5 else 6
       val allRoutesMap = findAllRoutes(requiredRoutes.toMap, availableLinks, activeAirportIds, PassengerSimulation.countryOpenness, iterationCount)
       
       //start consuming routes
       println()
       print("Start to go through demand chunks and comsume...nom nom nom...")
       
       //we want to randomize the order and go chunk by chunk as we want to evenly/randomly distribute seats to each PassengerGroup
       val remainingDemandChunks = ListBuffer[(PassengerGroup, Airport, Int)]()

       demandChunks.foreach {
         case (passengerGroup, toAirport, chunkSize) => 
           allRoutesMap.get(passengerGroup).foreach { toAirportRouteMap =>
//             if (!toAirportRouteMap.isEmpty) {
//               println("to airport route map" + toAirportRouteMap)
//             }
             
             toAirportRouteMap.get(toAirport) match { 
               case Some(pickedRoute) =>
                 //println("picked route info" + passengerGroup + " " + pickedRoute.links(0).airline)
                 //val totalDistance = pickedRoute.links.foldLeft(0.0)(_ + _.link.distance)
                 val fromAirport = passengerGroup.fromAirport 
                 
                 
                 //println("RecommendedPrice  " +  Pricing.computeStandardPrice(totalDistance))
                 //add some randomness here
                 //val affordableCost = totalDistance * (1.25 - Random.nextFloat() / 2)
                 //val affordableCost = totalDistance * (Util.getBellRandom(1))
                 //val MIN_AIPLANE_SPEED = 300.0
                 //val linkClass = passengerGroup.preference.preferredLinkClass
                 
                 if (isRouteAffordable(pickedRoute, fromAirport, toAirport, passengerGroup.preference.preferredLinkClass)) {
                   val consumptionSize = pickedRoute.links.foldLeft(chunkSize) { (foldInt, linkConsideration) =>
                     val actualLinkClass = linkConsideration.linkClass
                     val availableSeats = linkConsideration.link.availableSeats(actualLinkClass) 
                     if (availableSeats < foldInt) { availableSeats } else { foldInt }
                   }
                   //some capacity available on all the links, consume them NOMNOM NOM!
                   if (consumptionSize > 0) {
                     pickedRoute.links.foreach { linkConsideration =>
                       val actualLinkClass = linkConsideration.linkClass
                       //val newAvailableSeats = linkConsideration.link.availableSeats(actualLinkClass) - consumptionSize
                       
                       linkConsideration.link.addSoldSeatsByClass(actualLinkClass, consumptionSize)
                       //linkConsideration.link.availableSeats = LinkClassValues(linkConsideration.link.availableSeats.map.+(actualLinkClass -> newAvailableSeats))
    //                   if (link.availableSeats == 0) {
    //                     println("EXHAUSED!! = " + link)
    //                   }
                     }
                     consumptionResult.append((passengerGroup, toAirport, consumptionSize, pickedRoute))
                   }
                   //update the remaining demand chunk list
                   if (consumptionSize < chunkSize) { //not totally satisfied 
                     //put a updated demand chunk
                     remainingDemandChunks.append((passengerGroup, toAirport, chunkSize - consumptionSize));
                   }
                 } else {
                   missedDemandChunks.append((passengerGroup, toAirport, chunkSize));
                 }
               case None => //no route
                 missedDemandChunks.append((passengerGroup, toAirport, chunkSize));
             }
           }
        }
       println("Done!")
       
       //now process the remainingDemandChunks in next cycle 
       demandChunks = remainingDemandChunks.toList     
       consumptionCycleCount += 1
     }
     
    println("Total chunks that consume something " + consumptionResult.size)
    println("Total missed chunks " + missedDemandChunks.size)
    
    //collapse it now
    val collapsedMap = consumptionResult.groupBy { 
      case(passengerGroup, toAirport, passengerCount, route) => (passengerGroup, toAirport, route)  
    }.mapValues { consumptions => consumptions.map(_._3).sum }
    
    
    println("Collasped consumption map size: " + collapsedMap.size)

    val missedMap = missedDemandChunks.groupBy {
      case(passengerGroup, toAirport, passengerCount) => (passengerGroup, toAirport)
    }.view.mapValues( missedChunks => missedChunks.map(_._3).sum).toMap

//    val soldLinks = links.filter{ link => link.availableSeats < link.capacity  }.map { link =>
//      (link, link.capacity - link.availableSeats)
//      }.sortBy {
//        case (_, soldSeats) => soldSeats 
//      }
//      
//    soldLinks.foreach{ case(link, soldSeats) => println(link.airline.name + "($" + link.price + "; recommend $" + Pricing.computeStandardPrice(link.distance) + ") " + soldSeats  + " : " + link.from.name + " => " + link.to.name) }
//    println("seats sold: " + soldLinks.foldLeft(0) {
//      case (holder, (link, soldSeats)) => holder + soldSeats
//    })
//    
//    LinkSource.saveLinkConsumptions(soldLinks)
    
    (collapsedMap.toMap, missedMap)
  }

  val LINK_COST_TOLERANCE_FACTOR = 0.9
  val LINK_COST_TOLERANCE_NOISE_RANGE = 0.4 //ie -0.2 to 0.2
  val random = new Random()
  def isRouteAffordable(pickedRoute: Route, fromAirport: Airport, toAirport: Airport, preferredLinkClass : LinkClass) : Boolean = {
    val ROUTE_DISTANCE_TOLERANCE_FACTOR = 2
    val routeDisplacement = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
    val routeDistance = pickedRoute.links.foldLeft(0)(_ + _.link.distance)
    if (routeDisplacement * ROUTE_DISTANCE_TOLERANCE_FACTOR <= routeDistance) { //a route that distance is too long (too indirect)
      return false
    }


//    val ROUTE_COST_TOLERANCE_FACTOR = 1.4
//    val routeAffordableCost = Pricing.computeStandardPrice(routeDisplacement, Computation.getFlightType(fromAirport, toAirport, routeDisplacement), linkClass) * ROUTE_COST_TOLERANCE_FACTOR   

//    println("affordable: " + routeAffordableCost + " cost : " + pickedRoute.totalCost + " => " + pickedRoute) 
    
//    if (pickedRoute.totalCost < routeAffordableCost) { //only consider individual ones for now
    

      val incomeAdjustedFactor : Double = 
        if (fromAirport.income < Country.LOW_INCOME_THRESHOLD) {
          1 - (Country.LOW_INCOME_THRESHOLD - fromAirport.income).toDouble / Country.LOW_INCOME_THRESHOLD * 0.2 //can reduce down to 0.8
        } else {
          1
        }

      val noise = (1 + (random.nextDouble() - 0.5) * LINK_COST_TOLERANCE_NOISE_RANGE)
      val unaffordableLink = pickedRoute.links.find { linkConsideration => {//find links that are too expensive
          val link = linkConsideration.link 
          
          
          val linkAffordableCost = Pricing.computeStandardPrice(link.distance, link.flightType, preferredLinkClass) * LINK_COST_TOLERANCE_FACTOR * incomeAdjustedFactor * noise
          
//          if (linkConsideration.linkClass == BUSINESS) {
//            println("affordable: " + linkAffordableCost + " cost : " + linkConsideration.cost + " => " + link) 
//          }
          linkConsideration.cost > linkAffordableCost
          
          
        }
      }
      return unaffordableLink.isEmpty
//    }  
  }
  
  
   
 
//  def findAllRoutes(requiredRoutes : Map[PassengerGroup, Set[Airport]], linksList : List[Link], activeAirportIds : Set[Int],  countryOpenness : Map[String, Int] = PassengerSimulation.countryOpenness) : Future[Map[PassengerGroup, Map[Airport, Route]]] = {
//    val totalRequiredRoutes = requiredRoutes.foldLeft(0){ case (currentCount, (fromAirport, toAirports)) => currentCount + toAirports.size }
//    
//    println("Total routes to compute : " + totalRequiredRoutes)
//    println("Total passenger groups : " + requiredRoutes.size)
//    
//    val links = linksList.toArray
//    
//    val demandSource = Source(requiredRoutes.iterator)
//    val computeFlow: Flow[(PassengerGroup, Set[Airport]), (PassengerGroup, Map[Airport, Route])] = Flow[(PassengerGroup, Set[Airport])].map {
//      case(passengerGroup, toAirports) =>
//        val linkClass = passengerGroup.preference.linkClass
//        //remove links that's unknown to this airport then compute cost for each link. Cost is adjusted by the PassengerGroup's preference
//        val linkConsiderations = ArrayBuffer[LinkConsideration]()
//        
//        var walker = 0
//        while (walker < links.length) {
//          val link = links(walker)
//          walker += 1
//          
//          //see if there are any seats for that class (or lower) left
//          link.availableSeatsAtOrBelowClass(linkClass).foreach { 
//            case(matchingLinkClass, seatsLeft) =>
//              //from the perspective of the passenger group, how well does it know each link
//              val airlineAwarenessFromCity = passengerGroup.fromAirport.getAirlineAwareness(link.airline.id)
//              val airlineAwarenessFromReputation = link.airline.getReputation() / 2 
//              //println("Awareness from reputation " + airlineAwarenessFromReputation)
//              val airlineAwareness = Math.max(airlineAwarenessFromCity, airlineAwarenessFromReputation)
//              
//              if (airlineAwareness > Random.nextInt(AirlineAppeal.MAX_AWARENESS)) {
//                var cost = passengerGroup.preference.computeCost(link) //cost should NOT be lower if seats available are lower than requested class, this reflect the unwillingness to downgrade
//                //2 instance of the link, one for each direction. Take note that the underlying link is the same, hence capacity and other params is shared properly!
//                val linkConsideration1 = LinkConsideration(link, cost, matchingLinkClass, false)
//                val linkConsideration2 = LinkConsideration(link, cost, matchingLinkClass, true)
//                if (hasFreedom(linkConsideration1, passengerGroup.fromAirport, countryOpenness)) {
//                  linkConsiderations += linkConsideration1
//                }
//                if (hasFreedom(linkConsideration2, passengerGroup.fromAirport, countryOpenness)) {
//                  linkConsiderations += linkConsideration2
//                }
//              }
//          }
//          
//        }
//        
//        //then find the shortest route based on the cost
//        
//        val routeMap = findShortestRoute(passengerGroup.fromAirport, toAirports, activeAirportIds, linkConsiderations, 4)
//        //if (!routeMap.isEmpty) { println(routeMap) }
//        (passengerGroup, routeMap)
//    }
//    //val resultSink = Sink.foreach { demandInfo : (Airport, Map[Airport, Int]) => println() }
//    var counter = 0
//    var progressCount = 0
//    val progressChunk = requiredRoutes.size / 100
//    
//    val resultSink = Sink.fold(Map[PassengerGroup, Map[Airport, Route]]()) {
//      (map, demandInfo : (PassengerGroup, Map[Airport, Route])) =>
//         counter += 1
//          if (progressChunk == 0 || counter % progressChunk == 0) {
//            progressCount += 1;
//            print(".")
//            if (progressCount % 10 == 0) {
//              print(progressCount + "% ")
//            }
//          }
//        map + demandInfo
//    }
//    
//    val completeFlow = demandSource.via(computeFlow).to(resultSink)
//    val materializedFlow = completeFlow.run()
//    materializedFlow.get(resultSink)
//  }
  
   /**
   * Return all routes if available, with destination defined in the input Map's value, the Input map key indicates various Passenger Group
   * 
   * Returned value is in form of Future[Map[PassengerGroup, Map[Airport, Route]]], which the Map key should always present even if no valid route is found at all
   * for that particular PassengerGroup, in such a case the map value will just me an empty map.
   * 
   * Take note that when finding routes, in order to be considered as a valid route, this method takes into consideration of:
   * 1. whether the link has available capacity left for the PassengerGroup's link Class, all the links in between 2 points should have capacity for the correct class
   * 2. whether the awareness/reputation makes the links "searchable" by the passenger group. There is some randomness to this, but at 0 awareness and reputation it simply cannot be found
   *    
   */
  def findAllRoutes(requiredRoutes : Map[PassengerGroup, Set[Airport]], linksList : List[Transport], activeAirportIds : Set[Int],  countryOpenness : Map[String, Int] = PassengerSimulation.countryOpenness, iterationCount : Int = 4) : Map[PassengerGroup, Map[Airport, Route]] = {
    val totalRequiredRoutes = requiredRoutes.foldLeft(0){ case (currentCount, (fromAirport, toAirports)) => currentCount + toAirports.size }
    
    println("Total routes to compute : " + totalRequiredRoutes)
    println("Total passenger groups : " + requiredRoutes.size)
    println(s"Iteration count : $iterationCount")
    
    //val links = linksList.toArray
    
    val counter = new AtomicInteger(0)
    val progressCount = new AtomicInteger(0)
    val progressChunk = requiredRoutes.size / 100
    
    val establishedAlliances = AllianceSource.loadAllAlliances().filter(_.status == AllianceStatus.ESTABLISHED)
    val establishedAllianceIdByAirlineId :java.util.Map[Int, Int] = new java.util.HashMap[Int, Int]()

    establishedAlliances.foreach { alliance => alliance.members.filter(_.role != AllianceRole.APPLICANT).foreach(member => establishedAllianceIdByAirlineId.put(member.airline.id, alliance.id)) }

//    val traceTimestampMap = new ConcurrentHashMap[Long, Long]()
//    val maxTraceDuration = 60 * 1000; //1 min
//    println("Agent ready? : " + AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS))
    val routeMaps : Map[PassengerGroup, Map[Airport, Route]] = requiredRoutes.toList.par.map {
      case((passengerGroup : PassengerGroup, toAirports)) => {
//        val currentThreadId = Thread.currentThread().getId
//        val currentTime = System.currentTimeMillis()
//        if (!traceTimestampMap.containsKey(currentThreadId) || (currentTime - traceTimestampMap.get(currentThreadId)) > maxTraceDuration) {
//          if (TraceContext.isSampled(Trace.getCurrentXTraceID)) {
//            println("ending " + currentThreadId)
//            Trace.endTrace("thread-" + currentThreadId)
//          }
//          Trace.startTrace("thread-" + currentThreadId).report()
//          println("tracing " + currentThreadId + " : " + Trace.getCurrentXTraceID)
//          traceTimestampMap.put(currentThreadId, currentTime)
//        }

        val preferredLinkClass = passengerGroup.preference.preferredLinkClass
        //remove links that's unknown to this airport then compute cost for each link. Cost is adjusted by the PassengerGroup's preference
        val linkConsiderations = new ArrayList[LinkConsideration]()

        linksList.foreach { link =>

          //see if there are any seats for that class (or lower) left
          link.availableSeatsAtOrBelowClass(preferredLinkClass).foreach { 
            case(matchingLinkClass, seatsLeft) =>
              //from the perspective of the passenger group, how well does it know each link
              val airlineAwarenessFromCity = passengerGroup.fromAirport.getAirlineAwareness(link.airline.id)
              val airlineAwarenessFromReputation = if (link.airline.getReputation() >= AirlineGrade.CONTINENTAL.reputationCeiling) AirlineAppeal.MAX_AWARENESS else link.airline.getReputation() * 2 //if reputation is 50+ then everyone will see it, otherwise reputation * 2
              //println("Awareness from reputation " + airlineAwarenessFromReputation)
              val airlineAwareness = Math.max(airlineAwarenessFromCity, airlineAwarenessFromReputation)
              
              if (airlineAwareness > Random.nextInt(AirlineAppeal.MAX_AWARENESS)) {
                var cost = passengerGroup.preference.computeCost(link, matchingLinkClass)
                
                //2 instance of the link, one for each direction. Take note that the underlying link is the same, hence capacity and other params is shared properly!
                val linkConsideration1 = LinkConsideration(link, cost, matchingLinkClass, false)
                val linkConsideration2 = LinkConsideration(link, cost, matchingLinkClass, true)
                if (hasFreedom(linkConsideration1, passengerGroup.fromAirport, countryOpenness)) {
                  linkConsiderations.add(linkConsideration1)
                }
                if (hasFreedom(linkConsideration2, passengerGroup.fromAirport, countryOpenness)) {
                  linkConsiderations.add(linkConsideration2)
                }
              }
          }
        }
        
        //then find the shortest route based on the cost
        
        val routeMap : Map[Airport, Route] = findShortestRoute(passengerGroup, toAirports, activeAirportIds, linkConsiderations, establishedAllianceIdByAirlineId, iterationCount)
        if (progressChunk == 0 || counter.incrementAndGet() % progressChunk == 0) {
          print(".")
          if (progressCount.incrementAndGet() % 10 == 0) {
            print(progressCount.get + "% ")
          }
        }
        //if (!routeMap.isEmpty) { println(routeMap) }
        (passengerGroup, routeMap)
      }
    }.toMap.seq
    
    routeMaps  
  }
  
  
  
  def hasFreedom(linkConsideration : LinkConsideration, originatingAirport : Airport, countryOpenness : Map[String, Int]) : Boolean = {
    if (linkConsideration.from.countryCode == linkConsideration.to.countryCode) { //domestic flight is always ok
      true
    } else if (linkConsideration.from.countryCode == originatingAirport.countryCode) { //always ok if link flying out from same country as the originate airport
      true
    } else { //a foreign airline flying out carrying passengers originating from a foreign airport, decide base on openness
      countryOpenness(linkConsideration.from.countryCode) >= Country.SIXTH_FREEDOM_MIN_OPENNESS
    }
  }
  
  
  def getAirportGroups(airportSource : List[Airport]) = {
    // group 0 <-> group1 <-> group 2 <-> group 3 <-> group 4
      
    val groupCount = 5
    val airportsPerGroup = 10;
    val airportGroups = ListBuffer[List[Airport]]()
    
    var airportsPool = airportSource
    for (i <- 0 until groupCount) {
      val airportsInGroup = airportsPool.takeRight(airportsPerGroup)
      airportGroups.append(airportsInGroup) 
      airportsPool = airportsPool.dropRight(airportsPerGroup)
    }
    
    airportGroups
  }
  
//  def generateFlightLinks(airports : List[Airport]) = {
//      val dummyAirline = Airline("dummy")
//      val distinationCount = 100
//      val sourcePermutation = (0 until airports.size).foldLeft(List[Int]())((list, integer) => integer :: list) //for random number
//      val validFromAirportCount = 200
//      airports.takeRight(validFromAirportCount).map { fromAirport =>
//        val randomArray = Random.shuffle(sourcePermutation).take(distinationCount)
//        randomArray.foldLeft(List[Link]()) { 
//          case (list, randomNumber) => 
//            val toAirport = airports(randomNumber)
//            if (fromAirport != toAirport) {
//              val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude)
//              val price = computePrice(distance)
//              //println(distance + " km, $" + price)
//              Link(fromAirport, toAirport, dummyAirline, LinkClassValues(Map(ECONOMY -> price)), distance.toInt, LinkClassValues(Map(ECONOMY -> 100)), 10, distance.toInt * 60 / 500, 1) :: list  
//            } else {
//              list
//            }
//        }
//      }.flatten
//  }
  
  def computePrice(distance : Double) = {
    val priceBracket = 2000
    var multiplier = 1.0
    var cost = 0.0
    for (i <- 0 to (distance / priceBracket).toInt) {
        cost += (distance - priceBracket * i) * multiplier
        multiplier += 0.5 
    }
    cost.toInt
  }
  
  
  
  
//  def generateFlightLinks(airportGroups : List[List[Airport]]) = {
//    val links = ListBuffer[Link]()
//    
//    for (i <- 0 until (airportGroups.length - 1)) {
//      val iAirports = airportGroups(i)
//      val jAirports = airportGroups(i + 1)
//      links.appendAll(iAirports.collect{ 
//        case iAirport => jAirports.collect { 
//          case jAirport =>
//            val distance = Util.calculateDistance(iAirport.latitude, iAirport.longitude, jAirport.latitude, jAirport.longitude)
//            Link(iAirport, jAirport, 1, distance, distance) 
//        }
//      }.flatten)
//    }
//    
//    links.toList
//  }

 
  /**
   * Find the shortest routes from the fromAirport to ALL the toAirport
   * Returns a map with valid route in format of
   * Map[toAiport, Route]
   */
  def findShortestRoute(passengerGroup : PassengerGroup, toAirports : Set[Airport], allVertices : Set[Int], linkConsiderations : java.util.List[LinkConsideration], allianceIdByAirlineId : java.util.Map[Int, Int], maxIteration : Int) : Map[Airport, Route] = {
    val from = passengerGroup.fromAirport

    //     // Step 1: initialize graph
//   for each vertex v in vertices:
//       if v is source then distance[v] := 0
//       else distance[v] := inf
//       predecessor[v] := null
    //val allVertices = allVerticesSource.map { _.id }
    
    val distanceMap = new java.util.HashMap[Int, Double]()
    val predecessorMap = new java.util.HashMap[Int, LinkConsideration]()
    allVertices.foreach { vertex => 
      if (vertex == from.id) {
        distanceMap.put(vertex, 0)
      } else {
        distanceMap.put(vertex, 10000000)
      }
    }

   // Step 2: relax edges repeatedly
//   for i from 1 to size(vertices)-1:
//       for each edge (u, v) with weight w in edges:
//           if distance[u] + w < distance[v]:
//               distance[v] := distance[u] + w
//               predecessor[v] := u
    for (i <- 0 until maxIteration) {
      //val updatingLinks = ArrayBuffer[LinkConsideration]()
      val linkConsiderationsIterator = linkConsiderations.iterator()
      while (linkConsiderationsIterator.hasNext) {
        val linkConsideration = linkConsiderationsIterator.next()
        val predecessorLinkConsideration = predecessorMap.get(linkConsideration.from.id)
        if (linkConsideration.from.id == from.id || predecessorLinkConsideration != null) {
          var connectionCost = 0.0
          var isValid : Boolean = true
          if (linkConsideration.from.id != from.id) { //then it should be a connection flight
            val predecessorLink = predecessorLinkConsideration.link
            val previousLinkAirlineId = predecessorLink.airline.id
            val currentLinkAirlineId = linkConsideration.link.airline.id

            if (linkConsideration.link.id == predecessorLink.id) { //going back and forth on the same link
              isValid = false
            } else if (predecessorLink.transportType == TransportType.SHUTTLE || linkConsideration.link.transportType == TransportType.SHUTTLE) {
              if (previousLinkAirlineId == currentLinkAirlineId ||
                (allianceIdByAirlineId.containsKey(previousLinkAirlineId) &&
                  allianceIdByAirlineId.get(previousLinkAirlineId) == allianceIdByAirlineId.get(currentLinkAirlineId))) { //same airline or same alliance - shuttle okay
                connectionCost = 25
              } else {
                isValid = false //shuttle only allows same network
              }

              //THIS ONLY WORKS since the shuttle distance is less than min flight distance, if we introduce shuttle that overlaps flight distance, it will have issues
              //for example airport A -> B , 100 km , if covered by a long range shuttle, vertex B will have the shuttle as edge, but then it forbids all other airlines heading out from B
              //a more "correct" way would be to create shuttle assisted "flight" that is a Link combining shuttle and the actual link. Though this would require quite a bit of changes
            } else {
              connectionCost += 25 //base cost for connection
              //now look at the frequency of the link arriving at this FromAirport and the link (current link) leaving this FromAirport. check frequency
              val frequency = Math.max(predecessorLink.frequency, linkConsideration.link.frequency)
              //if the bigger of the 2 is less than 42, impose extra layover time (if either one is frequent enough, then consider that as ok)
              if (frequency < Link.HIGH_FREQUENCY_THRESHOLD) {
                connectionCost += (3.5 * 24 * 5) / frequency //each extra hour wait is like $5 more
              }

              if (previousLinkAirlineId != currentLinkAirlineId && (allianceIdByAirlineId.get(previousLinkAirlineId) == null.asInstanceOf[Int] || allianceIdByAirlineId.get(previousLinkAirlineId) != allianceIdByAirlineId.get(currentLinkAirlineId))) { //switch airline, impose extra cost
                connectionCost += 75
              }
            }
            connectionCost *= passengerGroup.preference.connectionCostRatio * passengerGroup.preference.preferredLinkClass.priceMultiplier //connection cost should take into consideration of preferred link class too
          }
          
          if (isValid) {
            val cost = linkConsideration.cost + connectionCost
            val fromCost = distanceMap.get(linkConsideration.from.id)
            val newCost = fromCost + cost


            if (newCost < distanceMap.get(linkConsideration.to.id)) {
              distanceMap.put(linkConsideration.to.id, newCost)
              predecessorMap.put(linkConsideration.to.id, linkConsideration.copy(cost = cost)) //clone it, do not modify the existing linkWithCost
            }
          }
        }
      }
    }

    val resultMap : scala.collection.mutable.Map[Airport, Route] = scala.collection.mutable.Map[Airport, Route]()
    val maxHop = maxIteration * (maxIteration + 1) / 2
    toAirports.foreach{ to =>  
      var walker = to.id
      var noSolution = false;
      var foundSolution = false
      var hasFlight = false
      var route = ListBuffer[LinkConsideration]()
      var hopCounter = 0
      while (!foundSolution && !noSolution && hopCounter < maxIteration) {
        val link = predecessorMap.get(walker)
        if (link != null) {
          route.prepend(link)
          if (link.link.transportType == TransportType.FLIGHT) {
            hasFlight = true
          }
          walker = link.from.id
          if (walker == from.id && hasFlight) { //at least 1 leg has to be a flight. We don't want route with no flights
            foundSolution = true
          }
        } else { 
            noSolution = true
        }
        hopCounter += 1        
      }
      if (foundSolution) {
        resultMap.put(to, Route(route.toList, distanceMap.get(to.id)))
      }  
    }
    
    resultMap.toMap
  }
  
  
//  def findRandomRoutes(from : Airport, to : Airport, links : List[Link], routeCount : Int) = {
//    val linkMap = scala.collection.mutable.Map[Airport, ListBuffer[Link]]()
//    
//    links.foreach { link =>
//      var linksFromThisAirport : ListBuffer[Link] = null
//      if (!linkMap.contains(link.from)) {
//        linksFromThisAirport = ListBuffer[Link]()
//        linkMap.put(link.from, linksFromThisAirport)
//      } else {
//        linksFromThisAirport = linkMap(link.from)
//      }
//      linksFromThisAirport.append(link)
//    }
//    
//    val random = new Random()
//    println
//    for (i <- 0 until routeCount) {
//      var walker = from
//      var cost = 0.0
//      for (j <- 0 until 3) { //from group 0 => .. => group 3
//        print(walker.name + " => ")
//        val nextLinks = linkMap(walker)
//        val nextLink = nextLinks(random.nextInt(nextLinks.length))
//        walker = nextLink.to
//        cost += nextLink.cost
//      }
//      
//      //last step, has to goto "to"
//      for (link <- linkMap(walker)) {
//        if (link.to == to) {
//          print(link.to.name + " COST " + (cost + link.cost))
//        }
//      }
//      println
//    }
//  }


}