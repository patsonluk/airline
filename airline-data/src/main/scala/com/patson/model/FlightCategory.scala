package com.patson.model

object FlightCategory extends Enumeration {
    type FlightCategory = Value
    val DOMESTIC, INTERNATIONAL, INTERCONTINENTAL = Value

    val label = (category : FlightCategory.Value) => category match {
        case DOMESTIC => "Domestic"
        case INTERNATIONAL => "International"
        case INTERCONTINENTAL => "Intercontinental"
    }
}
