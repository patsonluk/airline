

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
import com.patson.model.AirportInfo
import com.patson.model.FlightPreference
import com.patson.model.SimplePreference
import com.patson.model.FlightPreference
import scala.concurrent.Future

object PassengerSimulation extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    val airportData = DataSource.loadAirportData()
    //val airportGroups = getAirportGroups(airportData)
    
    val demand = Await.result(DemandGenerator.computeDemand(airportData), Duration.Inf)
    println("DONE with demand")
    
    val links = generateFlightLinks(airportData)
    println("Generated " + links.size + " links")
    
    passengerConsume(demand, links)
    
    
    
    //test
    //findShortestRoute(airportGroups(0)(0), airportGroups(4)(0), links.toList)
    //10 random
    //findRandomRoutes(airportGroups(0)(0), airportGroups(4)(0), links.toList, 10)
  }
  
  def passengerConsume(demand : Map[AirportInfo, Map[AirportInfo, Int]], links : List[Link]) = {
     val demandChunkSize = 100
     println("splitting up demand")
     val demandListAllAirports = ListBuffer[(AirportInfo, AirportInfo, Int)]()
     val consumptionResult = ListBuffer[(AirportInfo, AirportInfo, Int, List[Link])]()
     
     demand.foreach {
       case (fromAirport, toAirportsWithDemand) =>
         val demandListFromThisAiport = toAirportsWithDemand.foreach {
           case (toAirport, demand) => 
             var remainingDemand = demand
             val demandChunks = ListBuffer[(AirportInfo, AirportInfo, Int)]()
             while (remainingDemand > demandChunkSize) {
               demandChunks.append((fromAirport, toAirport, demandChunkSize))
               remainingDemand -= demandChunkSize
             }
             demandListAllAirports.append((fromAirport, toAirport, remainingDemand))
         }
     }
     
     println("Finished splitting up the demand, chunk count " + demandListAllAirports.length)
    
     val consumptionCycleMax = 3; //try and rebuild routes 3 times
     var consumptionCycleCount = 0;
     
     //randomize consumption order
       var demandChunks = Random.shuffle(demandListAllAirports)
       
       //start consumption cycles
     while (consumptionCycleCount < consumptionCycleMax) {
       println("Run " + consumptionCycleCount + " demand chunk count " + demandChunks.size)
       
       
       //find out required routes
       val requiredRoutes = scala.collection.mutable.Map[AirportInfo, Set[AirportInfo]]()
       demandListAllAirports.foreach {
         case (fromAirport, toAirport, _) =>
           var toAirports : Set[AirportInfo] = requiredRoutes.getOrElseUpdate(fromAirport, scala.collection.mutable.Set[AirportInfo]())
           toAirports.add(toAirport)
       }
       
       //remove exhausted links
       val availableLinks = links.filter { _.capacity > 0 }
       
       val thriftyRoutesFuture = findAllRoutes(requiredRoutes.toMap, links, SimplePreference(0, 1)) //thrifty
       val speedyRoutesFuture = findAllRoutes(requiredRoutes.toMap, links, SimplePreference(1, 0)) //speedy
       val thriftyRouteMap = Await.result(thriftyRoutesFuture, Duration.Inf)
       val speedyRouteMap = Await.result(speedyRoutesFuture, Duration.Inf)
       
       
       //start consuming routes
       val remainingDemandChunks = ListBuffer[(AirportInfo, AirportInfo, Int)]()
       demandChunks.foreach {
         case (fromAirport, toAirport, chunkSize) => 
           val chosenRouteMap = if (Random.nextBoolean()) { thriftyRouteMap } else { speedyRouteMap }
           chosenRouteMap.get(fromAirport).foreach { toAirportRouteMap =>
             toAirportRouteMap.get(toAirport).foreach { pickedRoute =>
               val consumptionSize = pickedRoute.foldLeft(chunkSize)( (foldInt, link) => if (link.capacity < foldInt) { link.capacity } else { foldInt })
               //some capacity available on all the links, consume them NOMNOM NOM!
               if (consumptionSize > 0) {
                 pickedRoute.foreach { link => 
                   link.capacity -= consumptionSize
                   if (link.capacity == 0) {
                     println("EXHAUSED!! = " + link)
                   }
                 }
                 
                 consumptionResult.append((fromAirport, toAirport, consumptionSize, pickedRoute))
               }
               //update the remaining demand chunk list
               if (consumptionSize < chunkSize) { //not totally satisfied 
                 //put a updated demand chunk
                 remainingDemandChunks.append((fromAirport, toAirport, chunkSize - consumptionSize));
               }

             }
           }
        }
       
       //now process the remainingDemandChunks in next cycle 
       demandChunks = remainingDemandChunks     
       consumptionCycleCount += 1
     }
     
    
    
    
    
    
     println(consumptionResult.size)
  }
  
  def findAllRoutes(requiredRoutes : Map[AirportInfo, Set[AirportInfo]], links : List[Link], preference : FlightPreference) : Future[Map[AirportInfo, Map[AirportInfo, List[Link]]]] = {
    val totalRequiredRoutes = requiredRoutes.foldLeft(0){ case (currentCount, (fromAirport, toAirports)) => currentCount + toAirports.size }
    
    println("Total routes to compute : " + totalRequiredRoutes)
    
     //Step 0: find all vertex
    val allVertices = Set[AirportInfo]()
    links.foreach { link => 
      allVertices.add(link.from)
      allVertices.add(link.to)
    }
    println("Total active nodes: " + allVertices.size)
    
    //compute cost for each link
    val linksWithCost = links.collect{ case link => (link, preference.computeCost(link)) } 
    
    val demandSource = Source(requiredRoutes.iterator)
	  val computeFlow: Flow[(AirportInfo, Set[AirportInfo]), (AirportInfo, Map[AirportInfo, List[Link]])] = Flow[(AirportInfo, Set[AirportInfo])].map {
      case(fromAirport, toAirports) => 
        val routeMap = findShortestRoute(fromAirport, toAirports, allVertices, linksWithCost, 4)
        (fromAirport, routeMap)
    }
    //val resultSink = Sink.foreach { demandInfo : (AirportInfo, Map[AirportInfo, Int]) => println() }
    var counter = 0
    var progressCount = 0
    val progressChunk = requiredRoutes.size / 100
    
    val resultSink = Sink.fold(Map[AirportInfo, Map[AirportInfo, List[Link]]]()) {
      (map, demandInfo : (AirportInfo, Map[AirportInfo, List[Link]])) => 
         counter += 1
          if (counter % progressChunk == 0) {
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
  
  
  
  
  def getAirportGroups(airportSource : List[AirportInfo]) = {
    // group 0 <-> group1 <-> group 2 <-> group 3 <-> group 4
      
    val groupCount = 5
    val airportsPerGroup = 10;
    val airportGroups = ListBuffer[List[AirportInfo]]()
    
    var airportsPool = airportSource
    for (i <- 0 until groupCount) {
      val airportsInGroup = airportsPool.takeRight(airportsPerGroup)
      airportGroups.append(airportsInGroup) 
      airportsPool = airportsPool.dropRight(airportsPerGroup)
    }
    
    airportGroups
  }
  
  def generateFlightLinks(airports : List[AirportInfo]) = {
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
              Link(fromAirport, toAirport, price, distance, 10) :: list  
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
    cost
  }
  
  
  
  
//  def generateFlightLinks(airportGroups : List[List[AirportInfo]]) = {
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

 
  
  def findShortestRoute(from : AirportInfo, toAirports : Set[AirportInfo], allVertices: Set[AirportInfo], linksWithCost : List[(Link, Double)], maxHop : Int) : Map[AirportInfo, List[Link]] = {
   

    //     // Step 1: initialize graph
//   for each vertex v in vertices:
//       if v is source then distance[v] := 0
//       else distance[v] := inf
//       predecessor[v] := null

    val distanceMap = scala.collection.mutable.Map[AirportInfo, Double]()
    val predecessorMap = scala.collection.mutable.Map[AirportInfo, Link]()
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
    for (i <- 0 until maxHop - 1) {
      val updatingLinks = ListBuffer[(Link, Double)]()
      for ((link, cost) <- linksWithCost) {
        if (distanceMap(link.from) + cost < distanceMap(link.to)) {
//          distanceMap.put(link.to, distanceMap(link.from) + link.cost)
//          predecessorMap.put(link.to, link)
          updatingLinks.append((link, cost))
        }
      }
      updatingLinks.foreach { 
        case (updatingLink, cost) =>
          distanceMap.put(updatingLink.to, distanceMap(updatingLink.from) + cost)
          predecessorMap.put(updatingLink.to, updatingLink)
      }
      
    }
    
    //println("cost found : " + distanceMap(to))
    toAirports.foldLeft(Map[AirportInfo, List[Link]]()){ (map, to) =>  
      var walker = to
      var noSolution = false;
      var route = ListBuffer[Link]()
      while (walker != from && !noSolution) {
        predecessorMap.get(walker) match {
          case Some(link) =>
            route.prepend(link)
            walker = link.from
          case None => 
            noSolution = true
        }
      }
      if (noSolution) {
        map + Tuple2(to, List[Link]())
      } else {
//        route.foreach { link => print(link.from.name + " => ") }
//        println(route.last.to.name)
        map + Tuple2(to, route.toList)
      }  
    }
    
    
    
  }
  
//  def findRandomRoutes(from : AirportInfo, to : AirportInfo, links : List[Link], routeCount : Int) = {
//    val linkMap = scala.collection.mutable.Map[AirportInfo, ListBuffer[Link]]()
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