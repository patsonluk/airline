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
       val highPopulation = 1000000
       val lowPopulation = highPopulation / 2
       val income : Long = 50000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 1)
       val highPopToAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = highPopulation * income, population = highPopulation, 0, id = 2)
       val lowPopToAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = lowPopulation * income, population = lowPopulation, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highPopToAirport.power + lowPopToAirport.power
       val highPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highPopToAirport, totalWorldPower)
       val lowPopDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowPopToAirport, totalWorldPower)
       
       highPopDemand(ECONOMY).should(be > lowPopDemand(ECONOMY))
       highPopDemand(BUSINESS).should(be > lowPopDemand(BUSINESS))
       highPopDemand(FIRST).should(be > lowPopDemand(FIRST))
    }
    "Generate more demand with higher income airports".in {
       val population = 1000000
       val highIncome : Long = 50000
       val lowIncome : Long = 10000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val highAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val lowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highAirport.power + lowAirport.power
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highAirport, totalWorldPower)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowAirport, totalWorldPower)
       
       highDemand(ECONOMY).should(be > lowDemand(ECONOMY))
       highDemand(BUSINESS).should(be > lowDemand(BUSINESS))
       highDemand(FIRST).should(be > lowDemand(FIRST))
    }
    "Generate higher first class/business class ratio for higher income airports".in {
       val population = 1000000
       val highIncome : Long = 50000
       val lowIncome : Long = 10000
       val fromAirport = Airport("", "", "", latitude= 0, longitude = 0, "A", "", "", size = 3, power = population * highIncome, population = population, 0, id = 1)
       val highAirport = Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * highIncome, population = population, 0, id = 2)
       val lowAirport =  Airport("", "", "", latitude = 0, longitude = 180, "B", "", "", size = 3, power = population * lowIncome, population = population, 0, id = 3)
      
       val totalWorldPower = fromAirport.power + highAirport.power + lowAirport.power
       val highDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, highAirport, totalWorldPower)
       val lowDemand = DemandGenerator.computeDemandBetweenAirports(fromAirport, lowAirport, totalWorldPower)
       
       val totalHighDemand = highDemand(ECONOMY) + highDemand(BUSINESS) + highDemand(FIRST)
       val totalLowDemand = lowDemand(ECONOMY) + lowDemand(BUSINESS) + lowDemand(FIRST)
       
       (highDemand(ECONOMY).toDouble / totalHighDemand).should(be < (lowDemand(ECONOMY).toDouble / totalLowDemand)) //high income should have relatively lower economy ratio
       (highDemand(BUSINESS).toDouble / totalHighDemand).should(be > (lowDemand(BUSINESS).toDouble / totalLowDemand)) //high income should have relatively higher business ratio
       (highDemand(FIRST).toDouble / totalHighDemand).should(be > (lowDemand(FIRST).toDouble / totalLowDemand)) //high income should have relatively higher first ratio
    }
  }
}
