package controllers

import com.patson.data.AirlineSource

object AirlineUtil {
  val MIN_AIRLINE_NAME_LENGTH = 1
  val MAX_AIRLINE_NAME_LENGTH = 50

  def checkAirlineName(airlineName : String) : Option[String] = {
    val length = airlineName.length
    if (length < MIN_AIRLINE_NAME_LENGTH || length > MAX_AIRLINE_NAME_LENGTH) {
      Some(s"Length should be $MIN_AIRLINE_NAME_LENGTH - $MAX_AIRLINE_NAME_LENGTH")
    } else if (!airlineName.forall(char => (char.isLetter && char <= 'z') || char == ' ')) {
      Some(s"Contains invalid character(s)")
    } else if (AirlineSource.loadAllAirlines(false).map { _.name.toLowerCase().replaceAll("\\s", "") }.contains(airlineName.replaceAll("\\s", "").toLowerCase())) {
      Some(s"Airline name is already taken")
    } else {
      None
    }
  }
}


