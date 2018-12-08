package com.patson.model

import scala.collection.mutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.Util
import scala.collection.mutable.ListBuffer
import com.patson.OilSimulation
import com.patson.model.oil.OilPrice
 
class LoanSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  "early payment".must {
    "compute right values".in {
      val loan = Loan(airlineId = 0, borrowedAmount = 1000000, interest = 200000, remainingAmount = 600000, creationCycle = 0, loanTerm = 100) //1.2mill total 600k paid, so remaining 50 weeks
      assert(loan.weeklyPayment == 12000)
      assert(loan.principalWeeklyPayment == 10000)
      assert(loan.interestWeeklyPayment == 2000)
      assert(loan.remainingPrincipal == 500000)
      assert(loan.earlyRepayment < loan.remainingAmount)
      assert(loan.earlyRepayment > loan.remainingPrincipal)
    }
  }
}
