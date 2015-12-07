package com.patson.model

object FlightType extends Enumeration {
    type FlightType = Value
    val SHORT_HAUL_DOMESTIC, LONG_HAUL_DOMESTIC, SHORT_HAUL_INTERNATIONAL, LONG_HAUL_INTERNATIONAL, ULTRA_LONG_HAUL_INTERNATIONAL = Value
}