package com.patson.model

case class Loyalist(airport : Airport, airline : Airline, amount : Int)

case class LoyalistHistory(entry : Loyalist, cycle : Int)