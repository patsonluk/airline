package com.patson.model

//case class LinkClassValues(map : Map[LinkClass, Int]) {
case class LinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int, discountVal : Int = 0) extends AbstractLinkClassValues(economyVal, businessVal, firstVal, discountVal) {
//  val firstClassVal = map.getOrElse(FIRST, 0)
//  val businessClassVal = map.getOrElse(BUSINESS, 0)
//  val economyClassVal = map.getOrElse(ECONOMY, 0)
  override def toString() = {
    val economy = discountVal + economyVal
    s"$discountVal / $economyVal / $businessVal / $firstVal"
  }
}

abstract class AbstractLinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int, discountVal : Int = 0) {
  def apply(linkClass : LinkClass) = {
    //  map.getOrElse(linkClass, 0)
    linkClass match {
      case DISCOUNT_ECONOMY => discountVal
      case ECONOMY => economyVal
      case BUSINESS => businessVal
      case FIRST => firstVal
    }
  }

  //  val total = map.map(_._2).sum
  val total = economyVal + businessVal + firstVal + discountVal
  val totalwithSeatSize = economyVal + businessVal * BUSINESS.spaceMultiplier + firstVal * FIRST.spaceMultiplier + discountVal

  def +(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value + otherValue(key))
    //    })
    LinkClassValues(economyVal + otherValue.economyVal, businessVal + otherValue.businessVal, firstVal + otherValue.firstVal, discountVal + otherValue.discountVal)
  }

  def -(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value - otherValue(key))
    //    })
    LinkClassValues(economyVal - otherValue.economyVal, businessVal - otherValue.businessVal, firstVal - otherValue.firstVal, discountVal - otherValue.discountVal)
  }

  def *(otherValue : LinkClassValues) : LinkClassValues = {
    //    LinkClassValues(map.map {
    //      case (key, value) => (key, value * otherValue(key))
    //    })
    LinkClassValues(economyVal * otherValue.economyVal, businessVal * otherValue.businessVal, firstVal * otherValue.firstVal, discountVal * otherValue.discountVal)
  }

  def *(multiplier : Double) : LinkClassValues = {
    //    LinkClassValues(map.mapValues { value => (value * multiplier).toInt })
    LinkClassValues((economyVal * multiplier).toInt, (businessVal * multiplier).toInt, (firstVal * multiplier).toInt, (discountVal * multiplier).toInt)
  }

  def /(divider : Int) : LinkClassValues = {
    //    LinkClassValues(map.mapValues { value => value / divider })
    LinkClassValues(economyVal / divider, businessVal / divider, firstVal / divider, discountVal / divider)
  }
}

object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0, discount : Int = 0) : LinkClassValues = {
    LinkClassValues(economy, business, first, discount)
  }
  def getInstanceByMap(map : Map[LinkClass, Int]) : LinkClassValues = {
    LinkClassValues(map.getOrElse(ECONOMY, 0), map.getOrElse(BUSINESS, 0), map.getOrElse(FIRST, 0), map.getOrElse(DISCOUNT_ECONOMY, 0))
  }
}

