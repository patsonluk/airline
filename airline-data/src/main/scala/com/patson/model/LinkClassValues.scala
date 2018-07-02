package com.patson.model

case class LinkClassValues(map : Map[LinkClass, Int]) {
  def apply(linkClass : LinkClass) = { map.getOrElse(linkClass, 0) }
  
  val total = map.map(_._2).sum
  
  def +(otherValue : LinkClassValues) : LinkClassValues = {
//    map.foreach {
//      case (key, value) => map.update(key, map(key) + otherValue(key)) 
//    }
//    this
    LinkClassValues(map.map { 
      case (key, value) => (key, value + otherValue(key)) 
    })
  }
  
  def -(otherValue : LinkClassValues) : LinkClassValues = {
//    map.foreach {
//      case (key, value) => map.update(key, map(key) - otherValue(key)) 
//    }
//    this
    LinkClassValues(map.map { 
      case (key, value) => (key, value - otherValue(key)) 
    })
  }
  
  def *(otherValue : LinkClassValues) : LinkClassValues = {
//    map.foreach {
//      case (key, value) => map.update(key, (map(key) * multiplier).toInt) 
//    }
//    this
    LinkClassValues(map.map { 
      case (key, value) => (key, value * otherValue(key)) 
    })
  }
  
  def *(multiplier : Double) : LinkClassValues = {
//    map.foreach {
//      case (key, value) => map.update(key, (map(key) * multiplier).toInt) 
//    }
//    this
    LinkClassValues(map.mapValues { value => (value * multiplier).toInt })
  }
  
  def /(divider : Int) : LinkClassValues = {
//    map.foreach {
//      case (key, value) => map.update(key, map(key) / divider) 
//    }
//    this
    LinkClassValues(map.mapValues { value => value / divider })
  }
}
object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkClassValues = {
    LinkClassValues(Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first))
  }
}

