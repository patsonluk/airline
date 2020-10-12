package com.patson.patch

import java.sql.Connection

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.Meta

object Issue264OlympicsPatcher extends App {
  Class.forName(DB_DRIVER)
  val dataSource = new ComboPooledDataSource()
  dataSource.setUser(DATABASE_USER)
  dataSource.setPassword(DATABASE_PASSWORD)
  dataSource.setJdbcUrl(DATABASE_CONNECTION)
  dataSource.setMaxPoolSize(100)

  createSchema()

  def createSchema() = {
    val connection = dataSource.getConnection()
    try {
      Meta.createEvent(connection)
      Meta.createAirportAirlineBonus(connection)
    } finally {
      connection.close
    }
  }
}