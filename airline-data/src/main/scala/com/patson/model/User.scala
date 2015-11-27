package com.patson.model

import java.util.Calendar
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

case class User(userName : String, email : String, creationTime : Calendar, status : UserStatus.UserStatus, var id : Int = 0) extends IdObject{
  private[this] val airlinesMap : Map[Int, Airline] = Map[Int, Airline]()
  
  def getAccessibleAirlines() = {
    airlinesMap.values.toList
  }
  def hasAccessToAirline(airlineId : Int) = {
    airlinesMap.keySet.contains(airlineId)  
  }
  
  def setAccesibleAirlines(airlines : List[Airline]) = {
    airlines.foreach { airline => airlinesMap.put(airline.id, airline) }
  }
  
  
}

case class UserSecret(userName : String, digest : String, salt : String)

object UserStatus extends Enumeration {
    type UserStatus = Value
    val ACTIVE, INACTIVE = Value
}
