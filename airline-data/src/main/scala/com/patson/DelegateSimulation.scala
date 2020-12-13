package com.patson

import com.patson.data.{AirlineSource, DelegateSource}

object DelegateSimulation {
  def simulate(currentCycle : Int ) = {
    DelegateSource.deleteBusyDelegateByCriteria(List(("available_cycle", "<=", currentCycle)))

    //update/refresh delegates
    AirlineSource.loadAllAirlines().foreach { airline =>
      val delegateInfo = airline.getDelegateInfo()
      if (delegateInfo.availableCount < 0) {
        println(s"Removing ${delegateInfo.availableCount} delegates from $airline")
        val removingDelegates = delegateInfo.busyDelegates.sortBy(_.id).takeRight(delegateInfo.availableCount * -1) //take the newest ones away
        DelegateSource.deleteBusyDelegates(removingDelegates)
      }
    }

  }

}
