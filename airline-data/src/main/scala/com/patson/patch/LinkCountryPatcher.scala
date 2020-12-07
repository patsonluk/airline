package com.patson.patch

import com.patson.data.{Constants, LinkSource, Meta}
import com.patson.init.actorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LinkCountryPatcher extends App {
  mainFlow

  def mainFlow() {
    val links = LinkSource.loadAllLinks()

    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("UPDATE " + Constants.LINK_TABLE + " SET from_country = ?, to_country = ? WHERE id = ?")
    connection.setAutoCommit(false)
    try {
      links.foreach { link =>
        preparedStatement.setString(1, link.from.countryCode)
        preparedStatement.setString(2, link.to.countryCode)
        preparedStatement.setInt(3, link.id)
        preparedStatement.addBatch()

      }

      preparedStatement.executeBatch()

      connection.commit()
    } finally {
      preparedStatement.close()
      connection.close()
    }

    Await.result(actorSystem.terminate(), Duration.Inf)
  }
}
