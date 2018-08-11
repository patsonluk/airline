package com.patson.model

case class Alliance(name : String, status : AllianceStatus.Value, creationCycle : Int,  var id : Int = 0)

object AllianceStatus extends Enumeration {
  type AllianceStatus = Value
  val ESTABLISHED, FORMING = Value
}

case class AllianceMember(alliance: Alliance, airline : Airline, role : AllianceRole.Value, joinedCycle : Int)

object AllianceRole extends Enumeration {
  type AllianceRole = Value
  val LEADER, FOUNDING_MEMBER, MEMBER, APPLICANT = Value
}

case class AllianceHistory(allianceName : String, airline : Airline, event : AllianceEvent.Value, cycle : Int, var id : Int = 0)


object AllianceEvent extends Enumeration {
  type AllianceEvent = Value
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE = Value
}