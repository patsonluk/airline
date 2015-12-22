package com.patson.model

case class LinkClassValues(map : Map[LinkClass, Int]) {
  def apply(linkClass : LinkClass) = { map.getOrElse(linkClass, 0) }
  
  val total = map.map(_._2).sum
}
object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkClassValues = {
    LinkClassValues(Map(ECONOMY -> economy, BUSINESS -> business, FIRST -> first))
  }
}

