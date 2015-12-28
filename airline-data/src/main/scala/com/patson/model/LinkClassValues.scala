package com.patson.model

case class LinkClassValues(map : Map[LinkClass, Int]) {
  def apply(linkClass : LinkClass) = { map.getOrElse(linkClass, 0) }
  
  val total = map.map(_._2).sum
  
  def +(otherValue : LinkClassValues) : LinkClassValues = {
    LinkClassValues(map.keySet.union(otherValue.map.keySet).foldLeft ( Map[LinkClass, Int]()) { (foldMap, key) =>
      foldMap + (key -> (map.getOrElse(key, 0) + otherValue.map.getOrElse(key, 0)))
    })
  }
  def *(multiplier : Double) : LinkClassValues = {
    LinkClassValues(map.mapValues { value => (value * multiplier).toInt })
  }
}
object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkClassValues = {
    LinkClassValues(Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first))
  }
}

