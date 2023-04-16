package com.patson.stream

import com.patson.model.{Airport, Link, LinkClass}

object LinkEventHandler {
  var watchedLinkIds : Set[Int] = Set.empty

  def addWatchedLinkId(linkId : Int) = {
    watchedLinkIds = watchedLinkIds + linkId
  }
  def removeWatchedLinkId(linkId : Int) = {
    watchedLinkIds = watchedLinkIds - linkId
  }

  def isLinkWatched(linkId : Int) = {
    watchedLinkIds.contains(linkId)
  }

  def handleLinkEvent(link : Link, fromAirport : Airport, linkClass: LinkClass, paxCount: Int) = {
    SimulationEventStream.mainActor ! LinkConsumptionEvent(link.id, fromAirport.id, linkClass.code, paxCount * link.price(linkClass), link.soldSeats.toMap().map {
      case (linkClass, amount) => (linkClass.code, amount)
    })
  }
}
