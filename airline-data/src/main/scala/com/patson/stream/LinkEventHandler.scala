package com.patson.stream

import com.patson.model.{Link, LinkClass}

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

  def handleLinkEvent(link : Link, linkClass: LinkClass, amount: Int) = {
    SimulationEventStream.mainActor ! LinkConsumptionEvent(link.id, link.from.id, linkClass.code, amount, link.soldSeats.toMap().map {
      case (linkClass, amount) => (linkClass.code, amount)
    })
  }
}
