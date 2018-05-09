package com.patson.model

case class Route(links: List[LinkConsideration], totalCost: Double, var id : Int = 0) extends IdObject
