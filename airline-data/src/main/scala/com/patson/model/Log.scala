package com.patson.model

case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)

object LogCategory extends Enumeration {
    type LogCategory = Value
    val LINK = Value
}

object LogSeverity extends Enumeration {
    type LogSeverity = Value
    val WARN, INFO, FINE = Value
}

