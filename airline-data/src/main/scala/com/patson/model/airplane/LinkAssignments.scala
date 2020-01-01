package com.patson.model.airplane

/**
  * key - link id
  * value - frequency for that link
  * @param assignments
  */
case class LinkAssignments(assignments : Map[Int, Int]){
  val getFrequencyByLink : (Int => Int) = { (linkId : Int) =>
    assignments.getOrElse(linkId, 0)
  }
  val assignedLinkIds = assignments.keys.toList
  val isEmpty = assignments.isEmpty
}

object LinkAssignments {
  val empty = LinkAssignments(Map.empty)
}
