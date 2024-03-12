package com.patson.model

object FlightType extends Enumeration {
    type FlightType = Value
    val SHORT_HAUL_DOMESTIC, MEDIUM_HAUL_DOMESTIC, LONG_HAUL_DOMESTIC, ULTRA_LONG_HAUL_DOMESTIC, SHORT_HAUL_INTERNATIONAL, MEDIUM_HAUL_INTERNATIONAL, LONG_HAUL_INTERNATIONAL, ULTRA_LONG_HAUL_INTERCONTINENTAL = Value

    val getCategory = (flightType : FlightType.Value) => flightType match {
        case SHORT_HAUL_DOMESTIC | MEDIUM_HAUL_DOMESTIC | LONG_HAUL_DOMESTIC | ULTRA_LONG_HAUL_DOMESTIC => FlightCategory.DOMESTIC
        case SHORT_HAUL_INTERNATIONAL | MEDIUM_HAUL_INTERNATIONAL | LONG_HAUL_INTERNATIONAL | ULTRA_LONG_HAUL_INTERCONTINENTAL => FlightCategory.INTERNATIONAL
    }

    val label = (flightType : FlightType.Value) => flightType match {
        case SHORT_HAUL_DOMESTIC => "Short-haul Home Market"
        case MEDIUM_HAUL_DOMESTIC => "Medium-haul Home Market"
        case LONG_HAUL_DOMESTIC => "Long-haul Home Market"
        case ULTRA_LONG_HAUL_DOMESTIC => "Ultra long-haul Home Market"
        case SHORT_HAUL_INTERNATIONAL => "Short-haul International"
        case MEDIUM_HAUL_INTERNATIONAL => "Medium-haul International"
        case LONG_HAUL_INTERNATIONAL => "Long-haul International"
        case ULTRA_LONG_HAUL_INTERCONTINENTAL => "Ultra long-haul Intercontinental"
    }
}