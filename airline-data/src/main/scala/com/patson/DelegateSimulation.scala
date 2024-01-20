package com.patson

import com.patson.data.{AirlineSource, DelegateSource}

object DelegateSimulation {
  def simulate(currentCycle : Int ) = {
    DelegateSource.deleteBusyDelegateByCriteria(List(("available_cycle", "<=", currentCycle)))


    // Commented out below as we should just let it go negative. Otherwise exploit to assign bonus delegates to country,
    // build base then let delegates expired/removed but bases remain
//    AirlineSource.loadAllAirlines().foreach { airline =>
//      val delegateInfo = airline.getDelegateInfo()
//      if (delegateInfo.availableCount < 0) {
//        println(s"Removing ${delegateInfo.availableCount} delegates from $airline")
//        val removingDelegates = delegateInfo.busyDelegates.sortBy(_.id).takeRight(delegateInfo.availableCount * -1) //take the newest ones away
//        DelegateSource.deleteBusyDelegates(removingDelegates)
//      }
//    }
  }

}
