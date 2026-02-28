package com.patson.model

case class Log(airline : Airline, message : String, category : LogCategory.Value, severity : LogSeverity.Value, cycle : Int, properties : Map[String, String] = Map.empty)

object Log {
  val RETENTION_CYCLE = 300
}

object LogCategory extends Enumeration {
    type LogCategory = Value
    val LINK, NEGOTIATION, AIRPORT_RANK_CHANGE, SELF_NOTE, LOUNGE = Value

    val getDescription : LogCategory.Value => String = {
      case LINK => "Flight Route"
      case NEGOTIATION => "Negotiation"
      case AIRPORT_RANK_CHANGE => "Airport Rank Change"
      case SELF_NOTE => "Self Note"
      case LOUNGE => "Lounge"
    }
}

object LogSeverity extends Enumeration {
    type LogSeverity = Value
    val WARN, INFO, FINE = Value
    
    val getDescription : LogSeverity.Value => String = {
      case WARN => "Warning"
      case INFO => "Information"
      case FINE => "Fine details"
    }
}

