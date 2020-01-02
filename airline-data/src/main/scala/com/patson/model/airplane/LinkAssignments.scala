package com.patson.model.airplane

/**
  * key - link id
  * value - frequency for that link
  * @param assignments
  */
case class LinkAssignments(assignments : Map[Int, LinkAssignment]){
  val getFrequencyByLink : (Int => Int) = { (linkId : Int) =>
    assignments.get(linkId) match {
      case Some(linkAssignment) => linkAssignment.frequency
      case None => 0
    }
  }
  val getFlightMinutesByLink : (Int => Int) = { (linkId : Int) =>
    assignments.get(linkId) match {
      case Some(linkAssignment) => linkAssignment.flightMinutes
      case None => 0
    }
  }

  val assignedLinkIds = assignments.keys.toList
  val isEmpty = assignments.isEmpty
}

object LinkAssignments {
  val empty = LinkAssignments(Map.empty)
}
