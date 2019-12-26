package com.patson.model.airplane

case class LinkAssignments(assignments : Map[Int, Int]){
  val getFrequencyByLink : (Int => Int) = { (linkId : Int) =>
    assignments.getOrElse(linkId, 0)
  }
  val isEmpty = assignments.isEmpty
}

object LinkAssignments {
  val empty = LinkAssignments(Map.empty)
}
