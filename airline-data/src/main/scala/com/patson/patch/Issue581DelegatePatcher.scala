package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.{AirlineSource, CycleSource, DelegateSource}
import com.patson.data.Constants._
import com.patson.init.GenericTransitGenerator
import com.patson.model.{BusyDelegate, CountryDelegateTask, DelegateTask, DelegateTaskType}
import com.patson.util.CountryCache

import scala.collection.mutable.ListBuffer


object Issue581DelegatePatcher extends App {
  mainFlow


  def mainFlow() {
    val currentCycle = CycleSource.loadCycle()
    AirlineSource.loadAllAirlines(fullLoad = true).foreach { airline =>
      val delegateTaskCountry = airline.getDelegateInfo().busyDelegates.filter(_.assignedTask.getTaskType == DelegateTaskType.COUNTRY).map(_.assignedTask.asInstanceOf[CountryDelegateTask].country.countryCode)
      airline.getBases().groupBy(_.airport.countryCode).foreach {
        case (countryCode, bases) =>
          val countryDelegatesRequired = bases.map(_.delegatesRequired).sum
          val assignedDelegatesCount = delegateTaskCountry.count(c => c == countryCode)
          val missingDelegates = countryDelegatesRequired - assignedDelegatesCount
          if (missingDelegates > 0) {
            println(s"${airline.id} - ${airline.name} : missing $missingDelegates delegates for $countryCode")
            //patch
            val delegateTask = DelegateTask.country(currentCycle, CountryCache.getCountry(countryCode).get)
            val newDelegates = (0 until missingDelegates).map(_ => BusyDelegate(airline, delegateTask, None))
            DelegateSource.saveBusyDelegates(newDelegates.toList)
          }
      }
    }
    print("Done!")
  }


}