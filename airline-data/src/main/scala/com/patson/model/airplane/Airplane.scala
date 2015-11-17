package com.patson.model.airplane

import com.patson.model.Airline
import com.patson.model.IdObject

case class Airplane(model : Model, owner : Airline, constructedCycle : Int, condition : BigDecimal) extends IdObject