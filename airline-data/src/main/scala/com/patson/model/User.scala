package com.patson.model

import java.util.Calendar
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

case class User(userName : String, email : String, creationTime : Calendar, lastActiveTime : Calendar, status : UserStatus.UserStatus, level : Int, var id : Int = 0) extends IdObject{
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

  import User._
  val isAdmin = level >= ADMIN_LEVEL
  val isSuperAdmin = level == SUPER_ADMIN_LEVEL
  val isChatBanned = status == UserStatus.CHAT_BANNED || status == UserStatus.BANNED
}

object User {
  val SUPER_ADMIN_LEVEL = 20
  val ADMIN_LEVEL = 10

  val fromId = (id : Int) => User("<unknown>", "", Calendar.getInstance(), Calendar.getInstance(), UserStatus.INACTIVE, 0, id)
}

case class UserSecret(userName : String, digest : String, salt : String)

object UserStatus extends Enumeration {
    type UserStatus = Value
    val ACTIVE, INACTIVE, CHAT_BANNED, BANNED = Value
}
