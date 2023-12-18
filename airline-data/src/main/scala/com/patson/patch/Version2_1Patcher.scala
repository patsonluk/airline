package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.CountrySimulation
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirplaneSource, AirportSource, Meta}
import com.patson.init.{AirplaneModelPatcher, AirportGeoPatcher, AssetBlueprintGenerator, actorSystem}
import com.patson.model._
import com.patson.model.airplane._

import java.sql.Connection
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v2.1
  */
object Version2_1Patcher extends App {
  mainFlow

  def mainFlow() {
    patchSchema()
    AirportGeoPatcher.mainFlow()
    patchAssetBlueprints()
    AirplaneModelPatcher.mainFlow()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def patchSchema() = {
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
    val connection = dataSource.getConnection
    Meta.createAirportAsset(connection)
    Meta.createAllianceMission(connection)

    connection.close
  }

  def patchAssetBlueprints(): Unit = {
    val airports = AirportSource.loadAllAirports(true, true).sortBy(_.power).reverse
    AssetBlueprintGenerator.generateAssets(airports)
  }
  

}