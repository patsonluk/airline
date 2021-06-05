package com.patson.patch

import java.sql.Connection

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{Constants, LinkSource, Meta}
import com.patson.init.actorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Issue310_patcher extends App {
  mainFlow

  def mainFlow() {
    Class.forName(DB_DRIVER)
    val dataSource = new ComboPooledDataSource()
    dataSource.setUser(DATABASE_USER)
    dataSource.setPassword(DATABASE_PASSWORD)
    dataSource.setJdbcUrl(DATABASE_CONNECTION)
    dataSource.setMaxPoolSize(100)

    val connection = dataSource.getConnection()
    try {
      createSchema(connection)
      patchLinkAirplaneModel(connection : Connection)
    } finally {
      connection.close
    }

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def createSchema(connection : Connection): Unit = {
      Meta.createLinkChangeHistory(connection)
  }

  def patchLinkAirplaneModel(connection : Connection) = {
    connection.setAutoCommit(false)
    val statement = connection.prepareStatement("UPDATE " + Constants.LINK_TABLE + " SET airplane_model = ? WHERE id = ?")
    LinkSource.loadAllFlightLinks(LinkSource.FULL_LOAD).foreach { link =>
      link.getAssignedModel().foreach { model =>
        statement.setInt(1, model.id)
        statement.setInt(2, link.id)
        statement.executeUpdate()
      }
    }

    connection.commit()

  }
}