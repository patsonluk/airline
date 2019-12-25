package com.patson.model.airplane

import com.patson.model._

object AirplaneAssignmentUtil {
  def getAssignmentsToAirplanes(airline: Airline) : Map[Int, Map[Link, Int]] = ???
  def getAssignmentsToLinks(airline: Airline) : Map[Int, Map[Airplane, Int]] = ???

  def getAssignmentsToAirplane(airplane : Airplane) : Map[Link, Int] = ???
  def getAssignmentsToLink(link : Link) : Map[Link, Int] = ???
}
