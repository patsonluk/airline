package com.patson

import java.util.Calendar

import com.patson.data._
import com.patson.model._

object UserSimulation {
  val PURGE_USER_THRESHOLD = 365 //in days


  def simulate(cycle: Int) = {
    val threshold = Calendar.getInstance()
    threshold.add(Calendar.DAY_OF_YEAR, -1 * PURGE_USER_THRESHOLD)
    println(s"starting resetting inactive user airlines - active before $threshold")

    UserSource.loadUsersByCriteria(List.empty).foreach { user =>
      if (user.status != UserStatus.INACTIVE && user.lastActiveTime.before(threshold)) {
        println(s"Resetting user $user")
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
}