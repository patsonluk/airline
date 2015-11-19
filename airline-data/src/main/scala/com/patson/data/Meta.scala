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
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CYCLE_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CITY_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP INDEX IF EXISTS " + AIRPORT_AIRLINE_INDEX)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_INFO_TABLE)
//     statement.execute()
//     statement.close()
//
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_APPEAL_TABLE)
     statement.execute()
     statement.close()
//     
//     statement = connection.prepareStatement("DROP INDEX IF EXISTS " + AIRPORT_CITY_SHARE_INDEX)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_CITY_SHARE_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_CONSUMPTION_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_ASSIGNMENT_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_MODEL_TABLE)
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE TABLE " + CYCLE_TABLE + "(cycle INTEGER PRIMARY KEY)")
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE TABLE " + CITY_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), population INTEGER, income INTEGER)")
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256), airport_size INTEGER, power LONG, population LONG, slots LONG, available_slots LONG)")
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(256), balance LONG)")
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_INFO_TABLE + "(" +
//                                             "airline INTEGER PRIMARY KEY REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "balance LONG)")
//     statement.execute()
//     statement.close()
//     
     statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_APPEAL_TABLE + "(" + 
                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "loyalty DOUBLE," + 
	                                           "awareness DOUBLE)")
     
     statement.execute()
     statement.close()
//     
//     statement = connection.prepareStatement("CREATE UNIQUE INDEX " + AIRPORT_AIRLINE_INDEX +  " ON " + AIRLINE_APPEAL_TABLE + "(airport, airline)")
//     statement.execute()
//     statement.close()
//     
//     
//     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_CITY_SHARE_TABLE + "(" + 
//                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "city INTEGER REFERENCES " + CITY_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "share DOUBLE," +
//                                             "PRIMARY KEY (airport, city))")
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE UNIQUE INDEX " + AIRPORT_CITY_SHARE_INDEX +  " ON " + AIRPORT_CITY_SHARE_TABLE + "(airport, city)")
//     statement.execute()
//     statement.close()
//     
//     
//     
//     
//     statement = connection.prepareStatement("CREATE TABLE " + LINK_TABLE + "(" +
//                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                             "from_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "to_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "price DOUBLE, " + 
//                                             "distance DOUBLE, " + 
//                                             "capacity INTEGER, " +
//                                             "quality INTEGER, " +
//                                             "duration INTEGER, " +
//                                             "frequency INTEGER)")
//     statement.execute()
//     statement.close()
//     
//     
//     statement = connection.prepareStatement("CREATE TABLE " + LINK_CONSUMPTION_TABLE + "(" +
//                                             "link INTEGER, " +
//	                                           "price INTEGER, " +
//                                             "capacity INTEGER, " +
//                                             "sold_seats INTEGER, " +
//                                             "fuel_cost INTEGER, " +
//                                             "crew_cost INTEGER, " +
//                                             "fixed_cost INTEGER, " +
//                                             "revenue INTEGER, " +
//                                             "profit INTEGER, " +
//                                             "from_airport INTEGER, " +
//                                             "to_airport INTEGER, " +
//                                             "airline INTEGER, " +
//                                             "distance INTEGER, " +
//                                             "cycle INTEGER, " +
//                                             "PRIMARY KEY (cycle, link))")
//     
//     statement.execute()
//     statement.close()
//     
//     statement = connection.prepareStatement("CREATE UNIQUE INDEX " + LINK_CONSUMPTION_INDEX +  " ON " + LINK_CONSUMPTION_TABLE + "(link, cycle)")
//     statement.execute()
//     statement.close()
//     
//     
//     statement = connection.prepareStatement("CREATE TABLE " + LINK_ASSIGNMENT_TABLE + "(" +
//                                             //"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                             "link INTEGER REFERENCES " + LINK_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "airplane INTEGER REFERENCES " + AIRPLANE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "PRIMARY KEY (link, airplane))")
//     statement.execute()
//     statement.close()
//     
//     
//     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_TABLE + "(" + 
//                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                             "name VARCHAR(256), " +
//                                             "capacity INTEGER, " + 
//                                             "fuel_burn INTEGER, " +
//                                             "speed INTEGER, " +
//                                             "range INTEGER, " +
//                                             "price INTEGER)")
//     statement.execute()
//     statement.close()
//     
//
//     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_TABLE + "(" + 
//                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                             "model INTEGER REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "owner INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "constructed_cycle INTEGER, " +
//                                             "condition DECIMAL(6,2))")
//
//     statement.execute()
//     statement.close()
     
     connection.close()
  }
}