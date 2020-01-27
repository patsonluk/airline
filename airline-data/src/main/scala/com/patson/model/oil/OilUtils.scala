package com.patson.model.oil

import com.patson.data.OilSource
import com.patson.model.Airline

object OilUtils {
  
  def getOilInventoryPolicy(airline : Airline) : OilInventoryPolicy = {
    OilSource.loadOilInventoryPolicyByAirlineId(airline.id) match {
      case Some(policy) => policy
      case None => OilInventoryPolicy.getDefaultPolicy(airline)
    }
  }
}