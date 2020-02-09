package com.patson

import com.patson.model.{PassengerType, _}
import org.scalatest.{Matchers, WordSpecLike}
 
class DemandGeneratorSpec extends WordSpecLike with Matchers {
  val DEFAULT_RELATIONSHIP = 0
  
  "generateDemand".must {
    "Generate more demand with higher pop to airports".in {
       val highPopulation = 10000000
       val lowPopulation = highPopulation / 2
       val income : Long = 50000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 1)
       val highPopToAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 2)
       val lowPopToAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = lowPopulation * income, population = lowPopulation, 0, id = 3)
      
       //val totalWorldPower = fromAirport.power + highPopToAirport.power + lowPopToAirport.power
       val highPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highPopToAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowPopToAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       highPopDemand(ECONOMY).should(be > lowPopDemand(ECONOMY))
       highPopDemand(BUSINESS).should(be > lowPopDemand(BUSINESS))
       highPopDemand(FIRST).should(be > lowPopDemand(FIRST))
    }
    "Generate more demand with higher pop from airports".in {
       val highPopulation = 10000000
       val lowPopulation = highPopulation / 2
       val income : Long = 50000
       val toAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 1)
       val highPopFromAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 2)
       val lowPopFromAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = lowPopulation * income, population = lowPopulation, 0, id = 3)
      
       //val totalWorldPower = fromAirport.power + highPopToAirport.power + lowPopToAirport.power
       val highPopDemand = DemandGenerator.computeDemandBetweenAirports(highPopFromAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowPopDemand = DemandGenerator.computeDemandBetweenAirports(lowPopFromAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       highPopDemand(ECONOMY).should(be > lowPopDemand(ECONOMY))
       highPopDemand(BUSINESS).should(be > lowPopDemand(BUSINESS))
       highPopDemand(FIRST).should(be > lowPopDemand(FIRST))
    }
    "Generate more demand with higher income from airports".in {
       val population = 10000000
       val highIncome : Long = 50000
       val lowIncome : Long = 10000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val highAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val lowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highAirport.power + lowAirport.power
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       highDemand(ECONOMY).should(be > lowDemand(ECONOMY))
       highDemand(BUSINESS).should(be > lowDemand(BUSINESS))
       highDemand(FIRST).should(be > lowDemand(FIRST))
    }
    "Generate more demand with higher income from toAirports".in {
       val population = 10000000
       val highIncome : Long = 50000
       val lowIncome : Long = 10000
       val toAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val highAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val lowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
      
       //val totalWorldPower = fromAirport.power + highAirport.power + lowAirport.power
       val highDemand = DemandGenerator.computeDemandBetweenAirports(highAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(lowAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       highDemand(ECONOMY).should(be > lowDemand(ECONOMY))
       highDemand(BUSINESS).should(be > lowDemand(BUSINESS))
       highDemand(FIRST).should(be > lowDemand(FIRST))
    }
    "Generate higher first class and business class ratio for higher income airports".in {
       val population = 10000000
       val highIncome : Long = 50000
       val lowIncome : Long = 30000
       
       val fromHighAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val fromLowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
       val toAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
      
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       val totalHighDemand = highDemand(ECONOMY) + highDemand(BUSINESS) + highDemand(FIRST)
       val totalLowDemand = lowDemand(ECONOMY) + lowDemand(BUSINESS) + lowDemand(FIRST)
       
       (highDemand(ECONOMY).toDouble / totalHighDemand).should(be < (lowDemand(ECONOMY).toDouble / totalLowDemand)) //high income should have relatively lower economy ratio
       (highDemand(BUSINESS).toDouble / totalHighDemand).should(be > (lowDemand(BUSINESS).toDouble / totalLowDemand)) //high income should have relatively higher business ratio
       (highDemand(FIRST).toDouble / totalHighDemand).should(be > (lowDemand(FIRST).toDouble / totalLowDemand)) //high income should have relatively higher first ratio
    }
    "Generate more passenger for shorter routes".in {
      val population = 10000000
       val highIncome : Long = 50000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val closeAirport = Airport("", "", "", latitude = 0, longitude = 10, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val farAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 3)
      
       val closeDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, closeAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val farDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, farAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       closeDemand.total.should(be > farDemand.total)
       closeDemand(ECONOMY).should(be > farDemand(ECONOMY))
       closeDemand(BUSINESS).should(be > farDemand(BUSINESS))
       closeDemand(FIRST).should(be > farDemand(FIRST))
    }
    "Generate more passenger for domestic routes".in {
      val population = 10000000
       val highIncome : Long = 50000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val toDomesticAirport = Airport("", "", "", latitude = 0, longitude = 10, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val toInternationalAirport =  Airport("", "", "", latitude = 0, longitude = 10, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 3)
      
       val domesticDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toDomesticAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val internationalDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toInternationalAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       
       domesticDemand.total.should(be > internationalDemand.total)
    }
    "Generate higher first class and business class ratio for business passengers".in {
       val population = 10000000
       val highIncome : Long = 50000
       
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val toAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       
       val businessDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val touristDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.TOURIST)
       
       val totalBusinessDemand = businessDemand(ECONOMY) + businessDemand(BUSINESS) + businessDemand(FIRST)
       val totalTouristDemand = touristDemand(ECONOMY) + touristDemand(BUSINESS) + touristDemand(FIRST)
       
       (businessDemand(ECONOMY).toDouble / totalBusinessDemand).should(be < (touristDemand(ECONOMY).toDouble / totalTouristDemand)) //business passenger should have relatively lower economy ratio
       (businessDemand(BUSINESS).toDouble / totalBusinessDemand).should(be > (touristDemand(BUSINESS).toDouble / totalTouristDemand)) //business passenger should have  should have relatively higher business ratio
       (businessDemand(FIRST).toDouble / totalBusinessDemand).should(be > (touristDemand(FIRST).toDouble / totalTouristDemand)) //business passenger should have  should have relatively higher first ratio
    }
    "Generate higher ratio of tourist in higher income from Airport".in {
      val population = 10000000
       val highIncome : Long = 50000
       val lowIncome : Long = 30000
       
       val fromHighAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val fromLowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
       val toAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
      
       val highBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val highTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.TOURIST)
       
       val lowBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.BUSINESS)
       val lowTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, DEFAULT_RELATIONSHIP, PassengerType.TOURIST)
       
       println((highTouristDemand.total.toDouble / highBusinessDemand.total) + " : " + (lowTouristDemand.total.toDouble / lowBusinessDemand.total))
       (highTouristDemand.total.toDouble / highBusinessDemand.total).should(be > lowTouristDemand.total.toDouble / lowBusinessDemand.total)
    }
    "Generate demand based on country relationships".in {
       val population = 10000000
       val highIncome : Long = 50000
       
       val fromAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val toAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
      
       val demandBusiness = scala.collection.mutable.Map[Int, LinkClassValues]()
       val demandTourist = scala.collection.mutable.Map[Int, LinkClassValues]()
       var relationship = -4
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = -3
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = -2
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = -1
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = 0
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = 1
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = 2
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = 3
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       relationship = 4
       demandBusiness(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
       demandTourist(relationship) = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
       
       assert(demandBusiness(-4).total == 0)
       assert(demandTourist(-4).total == 0)
       assert(demandBusiness(-3).total == 0)
       assert(demandTourist(-3).total == 0)
       
       for (relationship <- -3 until 4) {
         assert(demandBusiness(relationship).total < demandBusiness(relationship + 1).total)
         assert(demandTourist(relationship).total < demandTourist(relationship + 1).total)
       }
    }
    "Generate expected base demand for olympics".in {
      val population = 10000000
      val highIncome : Long = 50000
      val lowIncome : Long = 25000

      val fromAirport1 = Airport("", "", "", latitude = 0, longitude = 180, "f1", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
      val fromAirport2 = Airport("", "", "", latitude = 0, longitude = 180, "f2", "", "", size = 5, power = population * 5 * highIncome, population = population * 5, 0, id = 2)
      val fromAirport3 = Airport("", "", "", latitude = 0, longitude = 180, "f3", "", "", size = 5, power = population * 5 * lowIncome, population = population * 5, 0, id = 3)
      val olympicsAirport1 = Airport("", "", "", latitude= 0, longitude = 0, "o1", "", "", size = 5, power = population * highIncome, population = population, 0, id = 4)
      val olympicsAirport2 = Airport("", "", "", latitude= 0, longitude = 0, "o2", "", "", size = 7, power = population * 5 * highIncome, population = population * 5, 0, id = 5)

      //all 4 airports
      val result: List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])] = DemandGenerator.generateOlympicsDemand(cycle = 0, demandMultiplier = 1, olympicsAirports = List(olympicsAirport1, olympicsAirport2), allAirports = List(fromAirport1, fromAirport2, fromAirport3, olympicsAirport1, olympicsAirport2))

      val resultMap: Map[Airport, Map[Airport, (PassengerType.Value, LinkClassValues)]] = result.toMap.view.mapValues(_.toMap).toMap

      val listEntryCount = result.map(_._2.size).sum
      val mapEntryCount = resultMap.toList.map(_._2.size).sum
      println(s"result: $result")
      println(s"resultMap: $resultMap")

      assert(listEntryCount == mapEntryCount)

      assert(resultMap.get(olympicsAirport1) == None) //olympic airport as from airport should have no demand
      assert(resultMap.get(olympicsAirport2) == None)
      assert(resultMap(fromAirport1)(olympicsAirport1)._1 == PassengerType.OLYMPICS)
      assert(resultMap(fromAirport1)(olympicsAirport1)._2.total > 0)
      assert(resultMap(fromAirport1)(olympicsAirport2)._2.total > 0)
      assert(resultMap(fromAirport2)(olympicsAirport1)._2.total > 0)
      assert(resultMap(fromAirport2)(olympicsAirport2)._2.total > 0)
      assert(resultMap(fromAirport3)(olympicsAirport1)._2.total > 0)
      assert(resultMap(fromAirport3)(olympicsAirport2)._2.total > 0)

      assert(resultMap(fromAirport1)(olympicsAirport1)._2.total < resultMap(fromAirport2)(olympicsAirport1)._2.total) //bigger from city has higher demand
      assert(resultMap(fromAirport1)(olympicsAirport1)._2.total < resultMap(fromAirport1)(olympicsAirport2)._2.total) //bigger olympics city has higher demand
      assert(resultMap(fromAirport2)(olympicsAirport1)._2.total > resultMap(fromAirport3)(olympicsAirport1)._2.total) //higher income from city has higher demand

      //assert total, should be similar to the base
      val totalDemand = resultMap.toList.map(_._2.toList.map {
        case (airport, (passengerType, demand)) => demand.total
      }.sum).sum

      assert(totalDemand > DemandGenerator.OLYMPICS_DEMAND_BASE * 0.99 && totalDemand < DemandGenerator.OLYMPICS_DEMAND_BASE * 1.01) //should be similar

    }
  }
}
