package com.patson.model

case class Alliance(name : String, creationCycle : Int,  members : List[AllianceMember], var id : Int = 0) {
  val status = {
    if (members.length < Alliance.ESTABLISH_MIN_MEMBER_COUNT) {
      AllianceStatus.FORMING 
    } else {
      AllianceStatus.ESTABLISHED
    }
  }
}

object AllianceStatus extends Enumeration {
  type AllianceStatus = Value
  val ESTABLISHED, FORMING = Value
}

case class AllianceMember(allianceId: Int, airline : Airline, role : AllianceRole.Value, joinedCycle : Int)

object AllianceRole extends Enumeration {
  type AllianceRole = Value
  val LEADER, FOUNDING_MEMBER, MEMBER, APPLICANT = Value
}

case class AllianceHistory(allianceName : String, airline : Airline, event : AllianceEvent.Value, cycle : Int, var id : Int = 0)


object AllianceEvent extends Enumeration {
  type AllianceEvent = Value
  val FOUND_ALLIANCE, APPLY_ALLIANCE, JOIN_ALLIANCE, REJECT_ALLIANCE, LEAVE_ALLIANCE, BOOT_ALLIANCE = Value
}

object Alliance {
  val MAX_MEMBER_COUNT = 10
  val ESTABLISH_MIN_MEMBER_COUNT = 3
}