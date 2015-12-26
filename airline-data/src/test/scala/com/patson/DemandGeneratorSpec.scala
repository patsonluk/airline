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
 
class DemandGeneratorSpec extends WordSpecLike with Matchers {
  "generateDemand".must {
    "Generate more demand with higher pop airports".in {
       val highPopulation = 10000000
       val lowPopulation = highPopulation / 2
       val income : Long = 50000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 1)
       val highPopToAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 2)
       val lowPopToAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = lowPopulation * income, population = lowPopulation, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highPopToAirport.power + lowPopToAirport.power
       val highPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highPopToAirport, PassengerType.BUSINESS)
       val lowPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowPopToAirport, PassengerType.BUSINESS)
       
       highPopDemand(ECONOMY).should(be > lowPopDemand(ECONOMY))
       highPopDemand(BUSINESS).should(be > lowPopDemand(BUSINESS))
       highPopDemand(FIRST).should(be > lowPopDemand(FIRST))
    }
    "Generate more demand with higher income airports".in {
       val population = 10000000
       val highIncome : Long = 50000
       val lowIncome : Long = 10000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val highAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val lowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highAirport.power + lowAirport.power
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highAirport, PassengerType.BUSINESS)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowAirport, PassengerType.BUSINESS)
       
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
      
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, PassengerType.BUSINESS)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, PassengerType.BUSINESS)
       
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
      
       val closeDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, closeAirport, PassengerType.BUSINESS)
       val farDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, farAirport, PassengerType.BUSINESS)
       
       closeDemand.total.should(be > farDemand.total)
       closeDemand(ECONOMY).should(be > farDemand(ECONOMY))
       closeDemand(BUSINESS).should(be > farDemand(BUSINESS))
       closeDemand(FIRST).should(be < farDemand(FIRST)) //only long routes have first class demand
    }
    "Generate higher first class and business class ratio for business passengers".in {
       val population = 10000000
       val highIncome : Long = 50000
       
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val toAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       
       val businessDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.BUSINESS)
       val touristDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, PassengerType.TOURIST)
       
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
      
       val highBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, PassengerType.BUSINESS)
       val highTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromHighAirport, toAirport, PassengerType.TOURIST)
       
       val lowBusinessDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, PassengerType.BUSINESS)
       val lowTouristDemand = DemandGenerator.computeDemandBetweenAirports(fromLowAirport, toAirport, PassengerType.TOURIST)
       
       println((highTouristDemand.total.toDouble / highBusinessDemand.total) + " : " + (lowTouristDemand.total.toDouble / lowBusinessDemand.total))
       (highTouristDemand.total.toDouble / highBusinessDemand.total).should(be > lowTouristDemand.total.toDouble / lowBusinessDemand.total)
    }
  }
}
