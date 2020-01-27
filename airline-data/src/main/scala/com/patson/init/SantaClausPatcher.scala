package com.patson.init

import java.sql.Connection

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirportSource, ChristmasSource, LinkSource, Meta}
import com.patson.model.christmas.SantaClausInfo
import com.patson.model.{ECONOMY, Link, LinkClassValues}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

object SantaClausPatcher extends App {
  Class.forName(DB_DRIVER)
  val dataSource = new ComboPooledDataSource()
  //    val properties = new Properties()
  //    properties.put("user", DATABASE_USER);
  //    properties.put("password", "admin");
  //DriverManager.getConnection(DATABASE_CONNECTION, properties);
  //mysql end

  //dataSource.setProperties(properties)
  dataSource.setUser(DATABASE_USER)
  dataSource.setPassword(DATABASE_PASSWORD)
  dataSource.setJdbcUrl(DATABASE_CONNECTION)
  dataSource.setMaxPoolSize(100)
  val connection = getConnection(false)
  createSchema(connection)
  connection.close

  //init the data
  val airports = AirportSource.loadAllAirports().filter(_.size > SantaClausInfo.AIRPORT_SIZE_THRESHOLD)
  val entries = ListBuffer[SantaClausInfo]()
  AirlineSource.loadAllAirlines(true).foreach { airline =>
    val candidateAirports = airline.getHeadQuarter() match {
      case Some(hq) => airports.filterNot( _.id == hq.airport.id) //do not include hq. otherwise bonus is too powerful
      case None => airports
    }
    val targetAirport = candidateAirports(Random.nextInt(candidateAirports.size))
    entries.append(SantaClausInfo(targetAirport, airline, SantaClausInfo.MAX_ATTEMPTS, List.empty, false, None))
  }

  ChristmasSource.saveSantaClausInfo(entries.toList)

  def getConnection(enforceForeignKey: Boolean = true) = {
    dataSource.getConnection()

  }

  def createSchema(connection : Connection) = {
    //    Meta.createLog(connection)
    //    Meta.createAlert(connection)
    Meta.createSantaClaus(connection)
  }



}