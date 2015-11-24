package com.patson.model

import com.patson.model.airplane._
import com.patson.data.CycleSource

object Computation {
  def calculateDuration(airplaneModel: Model, distance : Int) = {
    //assuming achieving max speed at 500km, otherwise half speed
    if (distance <= 500) {
      distance * 60 / (airplaneModel.speed / 2) 
    } else {
      (distance - 500) * 60 / airplaneModel.speed + 500 * 60 / (airplaneModel.speed / 2)  
    }
  }

  def calculateMaxFrequency(duration : Int) = {
    val roundTripTime = duration * 2 + 240 //240 constant turn-around for now - penalizing so long range flight is more profitable //TODO better calculation later
    val availableFlightTimePerWeek = 5 * 24 * 60 //assume per week only 5 days are "flyable"
    availableFlightTimePerWeek / roundTripTime
  }
  
  def calculateAge(fromCycle : Int) = {
    val currentCycle = CycleSource.loadCycle()
    currentCycle - fromCycle 
  }
  
  def calculateAirplaneValue(airplane : Airplane) : Int = {
    val maxAge = 30 * 52 //after age 30 no value...
    //80% off * condition * (age out of 30 years ratio)
    val value = airplane.model.price * 0.8 * airplane.condition / 100 * ((maxAge - calculateAge(airplane.constructedCycle)).toDouble / maxAge)
    if (value < 0) 0 else value.toInt
  }
}