package models

import com.patson.model.Airline
import com.patson.model.Airport

case class AirportFacility(airline : Airline, airport : Airport, name : String, level : Int, facilityType : FacilityType.Value)

object AirportFacility {
  def getNameRejection(name : String) : Option[String] = {
    if (name.length() < 1 || name.length() > MAX_NAME_LENGTH) {
      Some("Name should be between 1 - " + MAX_NAME_LENGTH + " characters")
    } else if (!name.forall(char => char.isLetter || char == ' ')) {
      Some("Alliance name can only contain space and characters")
    } else {
      None
    }
  }
  
  val MAX_NAME_LENGTH = 50
}

object FacilityType extends Enumeration {
  type FacilityType = Value
  val LOUNGE, SHUTTLE = Value
}