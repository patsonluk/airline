package com.patson

object ForeachTest extends App {
  val list = List(1, 2, 3)
  
  list.foreach { x => 
    Thread.sleep(10000) 
    println(x)  
  }
  
  println("hi")
}