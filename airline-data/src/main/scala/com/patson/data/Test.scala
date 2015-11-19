package com.patson.data

import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.data.airplane.ModelSource
import com.patson.init.GeoDataGenerator
import scala.concurrent._
import scala.concurrent.duration.Duration
import com.patson.model.Runway
import scala.collection.mutable.Map
import scala.util.Random

object Test extends App {
  val idObject = getIdObject
  println(TestCase1(1) == TestCase1(1))
  println(TestCase2(2) == TestCase2(2))
  val obj1 = TestCase1(1)
  val obj2 = TestCase1(1)
  println(obj2 == obj1)
  obj1.id = 5
  println(obj2 == obj1)
  
  
  def getIdObject : TestIdObject = {
    if (Random.nextBoolean()) {
      TestCase1(1)
    } else {
      TestCase3(2)
    }
  }
}

case class TestCase1(i : Int, var id : Int = 0) extends TestIdObject
case class TestCase2(j : Double, var id : Int = 0) extends TestIdObject
case class TestCase3(k : Long, var id : Int = 0) extends TestIdObject
abstract class TestIdObject {
  def id : Int
}

