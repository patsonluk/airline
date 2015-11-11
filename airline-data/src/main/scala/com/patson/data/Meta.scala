package com.patson.data

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

import org.sqlite.SQLiteConfig

import com.patson.data.Constants._

object Meta {
  def getConnection(enforceForeignKey : Boolean = true) = {
    Class.forName(DB_DRIVER);  
      
      
    val config = new SQLiteConfig();
    if (enforceForeignKey) {
      config.enforceForeignKeys(true);
    }

    val properties = config.toProperties()
    properties.put("user", DATABASE_USER);
    properties.put("password", "");
    DriverManager.getConnection(DATABASE_CONNECTION, properties);
  }
  
  def resetDatabase = {
    createSchema()   
  }
  
  def createSchema() {
    val connection = getConnection(false)
     var statement : PreparedStatement = null
     statement = connection.prepareStatement("DROP INDEX IF EXISTS " + AIRPORT_AIRLINE_INDEX)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_LOYALTY_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_CONSUMPTION_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_MODEL_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256), airport_size INTEGER, power LONG)")
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(256))")
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_LOYALTY_TABLE + "(" + 
                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "value INTEGER)")
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE UNIQUE INDEX " + AIRPORT_AIRLINE_INDEX +  " ON " + AIRPORT_LOYALTY_TABLE + "(airport, airline)")
     statement.execute()
     statement.close()
     
     
     
     statement = connection.prepareStatement("CREATE TABLE " + LINK_TABLE + "(" +
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "from_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "to_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "price DOUBLE, " + 
                                             "distance DOUBLE, " + 
                                             "capacity INTEGER)")
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE TABLE " + LINK_CONSUMPTION_TABLE + "(" +
                                             "link INTEGER PRIMARY KEY REFERENCES " + LINK_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "consumption INTEGER)")
     statement.execute()
     statement.close()
     
     
     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_TABLE + "(" + 
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "name VARCHAR(256), " +
                                             "capacity INTEGER, " + 
                                             "fuel_burn INTEGER, " +
                                             "speed INTEGER, " +
                                             "range INTEGER, " +
                                             "price INTEGER)")
     statement.execute()
     statement.close()
     

     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_TABLE + "(" + 
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "model INTEGER REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "owner INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)") 

     statement.execute()
     statement.close()
     
     connection.close()
  }
}