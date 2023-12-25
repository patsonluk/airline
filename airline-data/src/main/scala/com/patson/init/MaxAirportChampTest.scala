package com.patson.init

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirportSource, ChristmasSource, Meta}
import com.patson.model.christmas.SantaClausInfo
import com.patson.util.ChampionUtil

import java.sql.Connection
import scala.collection.mutable.ListBuffer
import scala.util.Random

object MaxAirportChampTest extends App {
  val allInfo = ChampionUtil.loadAirportChampionInfo()
  val sorted = allInfo.groupBy(_.loyalist.airline).toList.map {
    case (airline, entries) => (airline, (entries.map(_.reputationBoost).sum, entries.length))
  }.sortBy(_._2._2).reverse

  sorted.foreach(println)

}