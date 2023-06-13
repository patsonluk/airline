package com.patson

import java.util.Calendar

import com.patson.data._
import com.patson.model._

object UserSimulation {
  val LARGE_AIRLINE_PURGE_USER_THRESHOLD = 150 //in days
  val SMALL_AIRLINE_PURGE_USER_THRESHOLD = 30 //in days


  def simulate(cycle: Int) = {
    val strictThreshold = Calendar.getInstance()
    strictThreshold.add(Calendar.DAY_OF_YEAR, -1 * SMALL_AIRLINE_PURGE_USER_THRESHOLD)

    val generousThreshold = Calendar.getInstance()
    generousThreshold.add(Calendar.DAY_OF_YEAR, -1 * LARGE_AIRLINE_PURGE_USER_THRESHOLD)

    println(s"starting resetting strict threshold - active before ${strictThreshold.getTime}; and generous threshold - active before ${generousThreshold.getTime}")


    UserSource.loadUsersByCriteria(List.empty).foreach { user =>
      if (shouldResetPlayer(user, strictThreshold, generousThreshold)) {
        println(s"Resetting user $user with airlines ${user.getAccessibleAirlines()}")
        user.getAccessibleAirlines.foreach { airline =>
          if (!airline.isGenerated) {
            val resetBalance = Computation.getResetAmount(airline.id).overall
            Airline.resetAirline(airline.id, newBalance = resetBalance) match {
              case Some(airline) =>
                println(s"Airline reset to $airline")
              case None =>
                println(s"Failed to reset airline $airline")
            }
          }
        }

        UserSource.updateUser(user.copy(status = UserStatus.INACTIVE))
      }
    }
  }

  def shouldResetPlayer(user : User, strictThreshold : Calendar, generousThreshold : Calendar): Boolean = {
    if (user.status != UserStatus.INACTIVE) {
      var hasLargeAirline = false
      user.getAccessibleAirlines().foreach { airline =>
        if (airline.airlineGrade.value >= AirlineGrade.LESSER_INTERNATIONAL.value) {
          hasLargeAirline = true
        }
      }
      val threshold = if (hasLargeAirline || user.level > 0) generousThreshold else strictThreshold

      if (user.lastActiveTime.before(threshold)) {
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}