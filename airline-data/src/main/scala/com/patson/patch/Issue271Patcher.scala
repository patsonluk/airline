package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.Meta


object Issue271Patcher extends App {
  mainFlow


  def mainFlow() {
    com.patson.data.Patchers.airplaneModelPatcher()
    createSchema()
  }

  def createSchema() = {
    Class.forName(DB_DRIVER)
    val dataSource = new ComboPooledDataSource()
    dataSource.setUser(DATABASE_USER)
    dataSource.setPassword(DATABASE_PASSWORD)
    dataSource.setJdbcUrl(DATABASE_CONNECTION)
    dataSource.setMaxPoolSize(100)

    val connection = dataSource.getConnection()
    try {
      Meta.createAirplaneModelFavorite(connection)
      Meta.createAirplaneModelDiscount(connection)
    } finally {
      connection.close
    }

  }

}