package com.patson.model

case class Route(links: List[LinkConsideration], totalCost: Double, visitedAssets : List[AirportAsset] = List.empty, var id : Int = 0) extends IdObject
