package models

import com.patson.model.Airline
import com.patson.model.Airport

case class Consideration[T](cost : Long, newFacility : T, rejection : Option[String] = None) {
  val isRejected = rejection.isDefined
  val rejectionReason = rejection.getOrElse("")
}
