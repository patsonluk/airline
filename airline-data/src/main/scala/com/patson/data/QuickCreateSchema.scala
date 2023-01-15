package com.patson.data

import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.data.airplane.ModelSource
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Meta.createAirlineSlogan

import java.sql.Connection
import java.sql.PreparedStatement

object QuickCreateSchema extends App {
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
  
  def getConnection(enforceForeignKey: Boolean = true) = {

    //sqlite start
    //    val config = new SQLiteConfig();
    //    if (enforceForeignKey) {
    //      config.enforceForeignKeys(true);
    //    }
    //    val properties = config.toProperties()
    //properties.put("password", "");
    //sqlite end

    //mysql start

    dataSource.getConnection()

  }
  
  def createSchema(connection : Connection) = {
//    Meta.createLog(connection)
//    Meta.createAlert(connection)
    Meta.createAirlineNameHistory(connection)
  }
  
  
  
}