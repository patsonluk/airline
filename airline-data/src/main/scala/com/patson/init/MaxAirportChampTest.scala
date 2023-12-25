package com.patson.init

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.AirlineSimulation.MAX_AIRPORT_CHAMPION_BOOST_ENTRIES
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
    case (airline, entries) => (airline,
      entries.filter(entry => entry.ranking <= ChampionUtil.getAirportChampionCount(entry.loyalist.airport) - 2).map(_.reputationBoost).sum,
      entries.map(_.reputationBoost).sorted.takeRight(MAX_AIRPORT_CHAMPION_BOOST_ENTRIES).sum,
      entries.filter(entry => entry.ranking <= ChampionUtil.getAirportChampionCount(entry.loyalist.airport) - 2).length,
      entries.map(_.reputationBoost).sorted.takeRight(MAX_AIRPORT_CHAMPION_BOOST_ENTRIES).length
      )
  }.sortBy(_._2).reverse

  sorted.foreach {
    case (airline, before, after, beforeCount, afterCount) =>
      println(s"${airline.name},$before,$after,$beforeCount,$afterCount")
  }
}