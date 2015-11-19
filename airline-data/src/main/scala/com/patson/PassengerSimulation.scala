

package com.patson

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import scala.util.Random
import com.patson.model.Link
import com.patson.model.Airport
import com.patson.model.FlightPreference
import com.patson.model.SimplePreference
import com.patson.model.FlightPreference
import scala.concurrent.Future
import com.patson.data.AirportSource
import com.patson.data.LinkSource
import com.patson.model.FlightPreferencePool
import com.patson.model.FlightPreferencePool
import com.patson.model.FlightPreference
import com.patson.model.FlightPreference
import com.patson.model.FlightPreference
import com.patson.model.FlightPreference
import com.patson.model.FlightPreference
import com.patson.model.PassengerGroup
import com.patson.model.PassengerGroup
import com.patson.model.PassengerGroup
import com.patson.model.PassengerGroup
import com.patson.model.Airline
import com.patson.model.Route
import com.patson.model.Pricing
import com.patson.model.AirlineAppeal
import com.patson.model.LinkWithCost

object PassengerSimulation extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  testFlow
  
  def testFlow() = {

    //val airportGroups = getAirportGroups(airportData)
    //println("Using " + airportData.size + " airport data");
    
    val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

//    val airportData = AirportSource.loadAllAirports().filter( _.size >= 2)
//    val links = generateFlightLinks(airportData)
//    println("Generated " + links.size + " links")
    
    val links = LinkSource.loadAllLinks(true)
    
    val consumptionResult = passengerConsume(demand, links)
    
    println("Consumption result: ")
    val soldLinks = links.filter{ link => link.availableSeats < link.capacity  }.map { link =>
      (link, link.capacity - link.availableSeats)
      }.sortBy {
        case (_, soldSeats) => soldSeats 
      }
      
    soldLinks.foreach{ case(link, soldSeats) => println(link.airline.name + "($" + link.price + "; recommend $" + Pricing.computeStandardPrice(link.distance) + ") " + soldSeats  + " : " + link.from.name + " => " + link.to.name) }
    println("seats sold: " + soldLinks.foldLeft(0) {
      case (holder, (link, soldSeats)) => holder + soldSeats
    })
    
    
    //test
    //findShortestRoute(airportGroups(0)(0), airportGroups(4)(0), links.toList)
    //10 random
    //findRandomRoutes(airportGroups(0)(0), airportGroups(4)(0), links.toList, 10)
  }
  
  def passengerConsume(demand : List[(PassengerGroup, Airport, Int)], links : List[Link]) : List[(PassengerGroup, Airport, Int, Route)] = {
     //randomize consumption order
     //convert demandChunks to array
     var demandChunks = Random.shuffle(demand).toArray
     
     val consumptionResult = ListBuffer[(PassengerGroup, Airport, Int, Route)]()
     val consumptionCycleMax = 3; //try and rebuild routes 3 times
     var consumptionCycleCount = 0;
     //start consumption cycles
     while (consumptionCycleCount < consumptionCycleMax) {
       println("Run " + consumptionCycleCount + " demand chunk count " + demandChunks.size)
       println("links: " + links.size)
       
       //find out required routes
       print("Find required routes...")
       val requiredRoutes = scala.collection.mutable.Map[PassengerGroup, Set[Airport]]()
       demandChunks.foreach {
         case (passengerGroup, toAirport, _) =>
           var toAirports : Set[Airport] = requiredRoutes.getOrElseUpdate(passengerGroup, scala.collection.mutable.Set[Airport]())
           toAirports.add(toAirport)
       }
       println("Done!")
       
       //remove exhausted links
       val availableLinks = links.filter { _.availableSeats > 0 }
       
       println("Available links: " + availableLinks.length)
       
       val routesFuture = findAllRoutes(requiredRoutes.toMap, availableLinks)
       val allRoutesMap = Await.result(routesFuture, Duration.Inf)
       
       //start consuming routes
       println()
       print("Start to go through demand chunks and comsume...nom nom nom...")
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
                 val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude) * 2 //at most double
                 
                 //println("RecommendedPrice  " +  Pricing.computeStandardPrice(totalDistance))
                 //add some randomness here
                 //val affordableCost = totalDistance * (1.25 - Random.nextFloat() / 2)
                 //val affordableCost = totalDistance * (Util.getBellRandom(1))
                 val affordableCost = distance
                 if (affordableCost >= pickedRoute.totalCost) { //OK!
                   val consumptionSize = pickedRoute.links.foldLeft(chunkSize)( (foldInt, linkWithDirection) => if (linkWithDirection.link.availableSeats < foldInt) { linkWithDirection.link.availableSeats } else { foldInt })
                   //some capacity available on all the links, consume them NOMNOM NOM!
                   if (consumptionSize > 0) {
                     pickedRoute.links.foreach { linkWithDirection => 
                       linkWithDirection.link.availableSeats -= consumptionSize
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
                 } else { //try next time!???
                   //println("rejected! affordableCost: " + affordableCost + " cost: " + pickedRoute.cost + " pref: " + passengerGroup.preference);
                   //remainingDemandChunks.append((passengerGroup, toAirport, chunkSize));
                 }
               case None => //no route

             }
           }
        }
       println("Done!")
       
       //now process the remainingDemandChunks in next cycle 
       demandChunks = remainingDemandChunks.toArray     
       consumptionCycleCount += 1
     }
     
    println("Total chunks that consume something " + consumptionResult.size)
        
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
    
    consumptionResult.toList
  }
  
   
  
  def findAllRoutes(requiredRoutes : Map[PassengerGroup, Set[Airport]], links : List[Link]) : Future[Map[PassengerGroup, Map[Airport, Route]]] = {
    val totalRequiredRoutes = requiredRoutes.foldLeft(0){ case (currentCount, (fromAirport, toAirports)) => currentCount + toAirports.size }
    
    println("Total routes to compute : " + totalRequiredRoutes)
    
     //Step 0: find all vertex
    val allVertices = Set[Airport]()
    links.foreach { link => 
      allVertices.add(link.from)
      allVertices.add(link.to)
    }
    println("Total active nodes: " + allVertices.size)
    
    val demandSource = Source(requiredRoutes.iterator)
    

	  val computeFlow: Flow[(PassengerGroup, Set[Airport]), (PassengerGroup, Map[Airport, Route])] = Flow[(PassengerGroup, Set[Airport])].map {
      case(passengerGroup, toAirports) =>
        //remove links that's unknown to this airport then compute cost for each link. Cost is adjusted by the PassengerGroup's preference
        val linksWithCost = links.filter{ link => 
          //from the perspective of the passenger group, how well does it know each link
            val airlineAwareness = passengerGroup.fromAirport.airlineAppeals.get(link.airline).map { _.awareness }.getOrElse(0.0) 
            airlineAwareness > Random.nextDouble() * AirlineAppeal.MAX_AWARENESS
          }.flatMap { link =>
            var cost = passengerGroup.preference.computeCost(link)
            //add extra cost for low frequency, this would add a small constant to make people less likely to take connection flight (even with high frequency)
            cost += 100  + (50.toDouble / link.frequency - 1) * 100
            List(LinkWithCost(link, cost, false), LinkWithCost(link, cost, true)) //2 instance of the link, one for each direction. Take note that the underlying link is the same, hence capacity and other params is shared properly! 
          }
        
//        linksWithCost.foreach {
//          case(link, cost) => println(link.airline.name + " price " + link.price + " cost " + cost + passengerGroup.preference)
//        }
//        val (cheapestLink, _) = linksWithCost.foldLeft((None, Double.MaxValue) : (Option[Link], Double)) { 
//          case(Tuple2(None, _), Tuple2(link, cost)) => (Some(link), cost)
//          case(Tuple2(Some(foldLink), foldDouble), Tuple2(link, cost)) =>
//            if (cost < foldDouble) { 
//              (Some(link), cost) 
//            } else { 
//              (Some(foldLink), foldDouble)   
//            }
//        }
//        
//        println(cheapestLink.get + " cheap!")
        
        
//        println()
        //then find the shortest route based on the cost
        
        val routeMap = findShortestRoute(passengerGroup.fromAirport, toAirports, allVertices, linksWithCost, 4)
        //if (!routeMap.isEmpty) { println(routeMap) }
        (passengerGroup, routeMap)
    }
    //val resultSink = Sink.foreach { demandInfo : (Airport, Map[Airport, Int]) => println() }
    var counter = 0
    var progressCount = 0
    val progressChunk = requiredRoutes.size / 100
    
    val resultSink = Sink.fold(Map[PassengerGroup, Map[Airport, Route]]()) {
      (map, demandInfo : (PassengerGroup, Map[Airport, Route])) =>
         counter += 1
          if (progressChunk == 0 || counter % progressChunk == 0) {
            progressCount += 1;
            print(".")
            if (progressCount % 10 == 0) {
              print(progressCount + "% ")
            }
          }
        map + demandInfo
    }
    
    val completeFlow = demandSource.via(computeFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
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
  
  def generateFlightLinks(airports : List[Airport]) = {
      val dummyAirline = Airline("dummy")
      val distinationCount = 100
      val sourcePermutation = (0 until airports.size).foldLeft(List[Int]())((list, integer) => integer :: list) //for random number
      val validFromAirportCount = 200
      airports.takeRight(validFromAirportCount).map { fromAirport =>
        val randomArray = Random.shuffle(sourcePermutation).take(distinationCount)
        randomArray.foldLeft(List[Link]()) { 
          case (list, randomNumber) => 
            val toAirport = airports(randomNumber)
            if (fromAirport != toAirport) {
              val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude)
              val price = computePrice(distance)
              //println(distance + " km, $" + price)
              Link(fromAirport, toAirport, dummyAirline, price, distance.toInt, 100, 10, distance.toInt * 60 / 500, 1) :: list  
            } else {
              list
            }
        }
      }.flatten
  }
  
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
  def findShortestRoute(from : Airport, toAirports : Set[Airport], allVertices: Set[Airport], linksWithCost : List[LinkWithCost], maxHop : Int) : Map[Airport, Route] = {
   

    //     // Step 1: initialize graph
//   for each vertex v in vertices:
//       if v is source then distance[v] := 0
//       else distance[v] := inf
//       predecessor[v] := null

    val distanceMap = scala.collection.mutable.Map[Airport, Double]()
    val predecessorMap = scala.collection.mutable.Map[Airport, LinkWithCost]()
    allVertices.foreach { vertex => 
      if (vertex == from) {
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
    for (i <- 0 until maxHop) {
      val updatingLinks = ListBuffer[LinkWithCost]()
      for (linkWithCost <- linksWithCost) {
        if (distanceMap(linkWithCost.from) + linkWithCost.cost < distanceMap(linkWithCost.to)) {
//          distanceMap.put(link.to, distanceMap(link.from) + link.cost)
//          predecessorMap.put(link.to, link)
          updatingLinks.append(linkWithCost)
        }
      }
      updatingLinks.foreach { updatingLink => 
          distanceMap.put(updatingLink.to, distanceMap(updatingLink.from) + updatingLink.cost)
          predecessorMap.put(updatingLink.to, updatingLink)
      }
      
    }
    
    //println("cost found : " + distanceMap(to))
    toAirports.foldLeft(Map[Airport, Route]()){ (map, to) =>  
      var walker = to
      var noSolution = false;
      var foundSolution = false
      var route = ListBuffer[LinkWithCost]()
      var hopCounter = 0
      while (!foundSolution && !noSolution && hopCounter < maxHop) {
        predecessorMap.get(walker) match {
          case Some(link) =>
            route.prepend(link)
            walker = link.from
            if (walker == from) {
              foundSolution = true
            }
          case None => 
            noSolution = true
        }
      }
      if (foundSolution) {
        map + Tuple2(to, Route(route.toList, distanceMap(to)))
      } else {
        map
      }  
    }
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