package com.patson.model

case class Route(links: List[(LinkConsideration, Double)], totalCost: Double, var id : Int = 0) extends IdObject
