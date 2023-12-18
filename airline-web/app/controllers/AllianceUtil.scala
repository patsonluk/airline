package controllers

import com.patson.model.AllianceEvent

object AllianceUtil {

  import AllianceEvent._

  val getAllianceEventText = (entry : AllianceEvent.Value) => {
    entry match {
      case FOUND_ALLIANCE => "founded"
      case APPLY_ALLIANCE => "applied for"
      case JOIN_ALLIANCE => "joined"
      case REJECT_ALLIANCE => "was rejected by"
      case LEAVE_ALLIANCE => "left"
      case BOOT_ALLIANCE => "was removed from"
      case PROMOTE_LEADER => "was promoted to leader of"
      case PROMOTE_CO_LEADER => "was promoted to co-leader of"
      case DEMOTE => "was demoted in"
    }
  }
}


