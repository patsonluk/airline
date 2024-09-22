package com.patson.model

object FlightType extends Enumeration {
    type FlightType = Value
    val ULTRA_SHORT_HAUL_DOMESTIC, SHORT_HAUL_DOMESTIC, MEDIUM_HAUL_DOMESTIC, LONG_HAUL_DOMESTIC, ULTRA_SHORT_HAUL_INTERNATIONAL, SHORT_HAUL_INTERNATIONAL, MEDIUM_HAUL_INTERNATIONAL, LONG_HAUL_INTERNATIONAL, ULTRA_SHORT_HAUL_INTERCONTINENTAL, SHORT_HAUL_INTERCONTINENTAL, MEDIUM_HAUL_INTERCONTINENTAL, LONG_HAUL_INTERCONTINENTAL, ULTRA_LONG_HAUL_INTERCONTINENTAL = Value

    val getCategory = (flightType : FlightType.Value) => flightType match {
        case ULTRA_SHORT_HAUL_DOMESTIC | SHORT_HAUL_DOMESTIC | MEDIUM_HAUL_DOMESTIC | LONG_HAUL_DOMESTIC => FlightCategory.DOMESTIC
        case ULTRA_SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERNATIONAL | MEDIUM_HAUL_INTERNATIONAL | LONG_HAUL_INTERNATIONAL => FlightCategory.INTERNATIONAL
        case ULTRA_SHORT_HAUL_INTERCONTINENTAL | SHORT_HAUL_INTERCONTINENTAL | MEDIUM_HAUL_INTERCONTINENTAL | LONG_HAUL_INTERCONTINENTAL | ULTRA_LONG_HAUL_INTERCONTINENTAL => FlightCategory.INTERCONTINENTAL
    }

    val label = (flightType : FlightType.Value) => flightType match {
        case ULTRA_SHORT_HAUL_DOMESTIC => "Ultra short-haul Domestic"
        case SHORT_HAUL_DOMESTIC => "Short-haul Domestic"
        case MEDIUM_HAUL_DOMESTIC => "Medium-haul Domestic"
        case LONG_HAUL_DOMESTIC => "Long-haul Domestic"
        case ULTRA_SHORT_HAUL_INTERNATIONAL => "Ultra short-haul International"
        case SHORT_HAUL_INTERNATIONAL => "Short-haul International"
        case MEDIUM_HAUL_INTERNATIONAL => "Medium-haul International"
        case LONG_HAUL_INTERNATIONAL => "Long-haul International"
        case ULTRA_SHORT_HAUL_INTERNATIONAL => "Ultra short-haul Intercontinental"
        case SHORT_HAUL_INTERCONTINENTAL => "Short-haul Intercontinental"
        case MEDIUM_HAUL_INTERCONTINENTAL => "Medium-haul Intercontinental"
        case LONG_HAUL_INTERCONTINENTAL => "Long-haul Intercontinental"
        case ULTRA_LONG_HAUL_INTERCONTINENTAL => "Ultra long-haul Intercontinental"
    }
}