package com.patson.model.alliance

import com.patson.model.LinkClassValues
import com.patson.model.Alliance

case class AllianceStats(alliance : Alliance, totalPax : LinkClassValues, cycle : Int) {
  lazy val properties = Map(
    "economyPax" -> totalPax.economyVal.toLong,
    "businessPax" -> totalPax.businessVal.toLong,
    "firstPax" -> totalPax.firstVal.toLong,
  )
}

object AllianceStats {
  def buildAllianceStats(alliance : Alliance, properties : Map[String, Long], cycle : Int) : AllianceStats = {
    val totalPax = LinkClassValues.getInstance(properties("economyPax").toInt, properties("businessPax").toInt, properties("firstPax").toInt)
    AllianceStats(alliance, totalPax, cycle)
  }
}
