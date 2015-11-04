package com.patson.model

import com.patson.model.AirportInfo

case class Link(from : AirportInfo, to : AirportInfo, price : Double, distance : Double, var capacity: Int) {   
}
