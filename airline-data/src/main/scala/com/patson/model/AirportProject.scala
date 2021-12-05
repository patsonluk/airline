package com.patson.model


//import ProjectType.ProjectType
import ProjectStatus.ProjectStatus
import scala.collection.mutable.Map


abstract class AirportProject() extends IdObject {
    val airport : Airport
    val airline : Option[Airline]
    val projectType : ProjectType.Value
    val status : ProjectStatus.Value
    val level : Int
    var id : Int = 0
    val airportBoosts : List[AirportBoost]
}

case class AirportBoost(boostType : AirportBoostType.Value, value : Int)


object AirportBoostType extends Enumeration {
    type AirportBoostType = Value
    val POPULATION, INCOME, INTERNATIONAL_HUB, VACATION_HUB, FINANCIAL_HUB = Value

}

object ProjectType extends Enumeration {
    type ProjectType = Value
    val SKI_RESORT, RESIDENTIAL_COMPLEX = Value
}


object ProjectStatus extends Enumeration {
    type ProjectStatus = Value
    val BLUEPRINT, BUILDING, COMPLETED = Value
}

