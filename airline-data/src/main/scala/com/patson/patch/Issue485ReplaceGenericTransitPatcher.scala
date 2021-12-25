package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{AIRLINE_INFO_TABLE, DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.Meta
import com.patson.init.GenericTransitGenerator

import scala.collection.mutable.ListBuffer


object Issue485ReplaceGenericTransitPatcher extends App {
  mainFlow


  def mainFlow() {
    compensateShuttleService()
    GenericTransitGenerator.generateGenericTransit(4000, 50)
  }

  def compensateShuttleService() = {
    Class.forName(DB_DRIVER)
    val dataSource = new ComboPooledDataSource()
    dataSource.setUser(DATABASE_USER)
    dataSource.setPassword(DATABASE_PASSWORD)
    dataSource.setJdbcUrl(DATABASE_CONNECTION)
    dataSource.setMaxPoolSize(100)

    val connection = dataSource.getConnection()
    val shuttles = ListBuffer[(Int, Int, Int)]() //(airport, airline, levels)
    try {
      val statement = connection.prepareStatement("SELECT * FROM shuttle_service")
      val result = statement.executeQuery()
      while (result.next()) {
        shuttles.append((result.getInt("airport"), result.getInt("airline"), result.getInt("level")))
      }
      statement.close()

      connection.setAutoCommit(false)

      shuttles.foreach { case(airportId, airlineId, level) =>
        val patchStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET balance = balance + ? WHERE airline = ?")
        patchStatement.setInt(1, level * 25000000)
        patchStatement.setInt(2, airlineId)
        patchStatement.executeUpdate()
        patchStatement.close
      }
      connection.commit()

      val purgeStatement = connection.prepareStatement("DELETE FROM shuttle_service")
      purgeStatement.executeUpdate()
      purgeStatement.close()
    } finally {
      connection.close
    }
  }

}