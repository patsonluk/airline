package com.patson.model

//case class LinkClassValues(map : Map[LinkClass, Int]) {
case class LinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int) extends AbstractLinkClassValues(economyVal, businessVal, firstVal) {
//  val firstClassVal = map.getOrElse(FIRST, 0)
//  val businessClassVal = map.getOrElse(BUSINESS, 0)
//  val economyClassVal = map.getOrElse(ECONOMY, 0)
  override def toString() = {
    s"$economyVal / $businessVal / $firstVal"
  }
}

abstract class AbstractLinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int) {
  def apply(linkClass : LinkClass) = {
    //  map.getOrElse(linkClass, 0)
    linkClass match {
      case ECONOMY => economyVal
      case BUSINESS => businessVal
      case FIRST => firstVal
    }
  }

  //  val total = map.map(_._2).sum
  val total = economyVal + businessVal + firstVal

  def +(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value + otherValue(key))
    //    })
    LinkClassValues(economyVal + otherValue.economyVal, businessVal + otherValue.businessVal, firstVal + otherValue.firstVal)
  }

  def -(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value - otherValue(key))
    //    })
    LinkClassValues(economyVal - otherValue.economyVal, businessVal - otherValue.businessVal, firstVal - otherValue.firstVal)
  }

  def *(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value * otherValue(key))
    //    })
    LinkClassValues(economyVal * otherValue.economyVal, businessVal * otherValue.businessVal, firstVal * otherValue.firstVal)
  }

  def *(multiplier : Double) : LinkClassValues = {
    //    LinkClassValues(map.mapValues { value => (value * multiplier).toInt })
    LinkClassValues((economyVal * multiplier).toInt, (businessVal * multiplier).toInt, (firstVal * multiplier).toInt)
  }

  def /(divider : Int) : LinkClassValues = {
    //    LinkClassValues(map.mapValues { value => value / divider })
    LinkClassValues(economyVal / divider, businessVal / divider, firstVal / divider)
  }
}

object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkClassValues = {
    LinkClassValues(economy, business, first)
  }
  def getInstanceByMap(map : Map[LinkClass, Int]) : LinkClassValues = {
    LinkClassValues(map.getOrElse(ECONOMY, 0), map.getOrElse(BUSINESS, 0), map.getOrElse(FIRST, 0))
  }
}

