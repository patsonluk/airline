package com.patson.patch

import com.patson.data.{Constants, LinkSource, Meta}
import com.patson.init.actorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MinimumRenewalPatcher extends App {
  mainFlow

  def mainFlow() {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("ALTER TABLE " + Constants.AIRLINE_INFO_TABLE + " ADD COLUMN minimum_renewal_balance BIGINT DEFAULT 0")
    preparedStatement.execute()
    preparedStatement.close()
    connection.close()
    }
  
}