package com.patson.data

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import com.patson.data.Constants._
import java.util.Properties

object SqliteMeta {
  def getConnection(enforceForeignKey : Boolean = true) = {
    Class.forName(DB_DRIVER);  
      //sqlite start
//    val config = new SQLiteConfig();
//    if (enforceForeignKey) {
//      config.enforceForeignKeys(true);
//    }
//    val properties = config.toProperties()
    //properties.put("password", "");
    //sqlite end
    
    //mysql start
    val properties = new Properties()
    properties.put("user", DATABASE_USER);
    properties.put("password", "admin");
    //mysql end
    
    DriverManager.getConnection(DATABASE_CONNECTION, properties);
  }
  
  def resetDatabase = {
    createSchema()   
  }
  
  def createSchema() {
    val connection = getConnection(false)
     var statement : PreparedStatement = null
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CYCLE_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CITY_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_INFO_TABLE)
     statement.execute()
     statement.close()

     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_APPEAL_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_BASE_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_CITY_SHARE_TABLE)
     statement.execute()
     statement.close()
     
       statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_FEATURE_TABLE)
       statement.execute()
       statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_TABLE)
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_CONSUMPTION_TABLE)
     statement.execute()
     statement.close()

         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_STATISTICS_TABLE)
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + WATCHED_LINK_TABLE)
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + VIP_ROUTE_TABLE)
         statement.execute()
         statement.close()
         
              
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + VIP_ROUTE_ENTRY_TABLE)
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_ASSIGNMENT_TABLE)
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
           
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_TABLE)
         statement.execute()
         statement.close()
       
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_SECRET_TABLE)
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_AIRLINE_TABLE)
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + CYCLE_TABLE + "(cycle INTEGER PRIMARY KEY)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + CITY_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), population INTEGER, income INTEGER)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256), zone VARCHAR(16), airport_size INTEGER, power LONG, population LONG, slots LONG)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_TABLE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(256))")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_INFO_TABLE + "(" +
                                                 "airline INTEGER PRIMARY KEY REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "balance LONG," +
                                                 "service_quality DOUBLE," +
                                                 "service_funding INTEGER," +
                                                 "maintenance_quality DOUBLE," +
                                                 "reputation DOUBLE)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_APPEAL_TABLE + "(" + 
                                                 "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "loyalty DOUBLE," + 
    	                                           "awareness DOUBLE," +
                                                 "PRIMARY KEY (airport, airline))")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_APPEAL_INDEX_1 +  " ON " + AIRLINE_APPEAL_TABLE + "(airport)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_APPEAL_INDEX_2 +  " ON " + AIRLINE_APPEAL_TABLE + "(airline)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_BASE_TABLE + "(" + 
                                                 "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "scale INTEGER," + 
                                                 "founded_cycle INTEGER," +
    	                                           "headquarter INTEGER," +
                                                 "PRIMARY KEY (airport, airline))")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_BASE_INDEX_1 +  " ON " + AIRPORT_TABLE + "(id)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_BASE_INDEX_2 +  " ON " + AIRLINE_TABLE + "(id)")
         statement.execute()
         statement.close()
         
         
         
         statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_CITY_SHARE_TABLE + "(" + 
                                                 "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                                 "city INTEGER REFERENCES " + CITY_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                                 "share DOUBLE," +
                                                 "PRIMARY KEY (airport, city))")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_CITY_SHARE_INDEX_1 +  " ON " + AIRPORT_CITY_SHARE_TABLE + "(airport)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_CITY_SHARE_INDEX_2 +  " ON " + AIRPORT_CITY_SHARE_TABLE + "(city)")
         statement.execute()
         statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_FEATURE_TABLE + "(" +
                                            "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                            "feature_type VARCHAR(256)," +
                                            "strength DOUBLE," +
                                            "PRIMARY KEY (airport, feature_type))")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_FEATURE_INDEX_1 + " ON " + AIRPORT_FEATURE_TABLE + "(airport)")
    statement.execute()
    statement.close()

         
         
         statement = connection.prepareStatement("CREATE TABLE " + LINK_TABLE + "(" +
                                                 "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                 "from_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "to_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "price_economy INTEGER, " + 
                                                 "price_business INTEGER, " +
                                                 "price_first INTEGER, " +
                                                 "distance DOUBLE, " + 
                                                 "capacity_economy INTEGER, " +
                                                 "capacity_business INTEGER, " +
                                                 "capacity_first INTEGER, " +
                                                 "quality INTEGER, " +
                                                 "duration INTEGER, " +
                                                 "frequency INTEGER)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE UNIQUE INDEX " + LINK_INDEX_1 +  " ON " + LINK_TABLE + "(from_airport, to_airport, airline)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_2 +  " ON " + LINK_TABLE + "(from_airport)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_3 +  " ON " + LINK_TABLE + "(to_airport)")
         statement.execute()
         statement.close()
         statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_4 +  " ON " + LINK_TABLE + "(airline)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + LINK_CONSUMPTION_TABLE + "(" +
                                                 "link INTEGER, " +
    	                                           "price_economy INTEGER, " +
    	                                           "price_business INTEGER, " +
    	                                           "price_first INTEGER, " +
                                                 "capacity_economy INTEGER, " +
                                                 "capacity_business INTEGER, " +
                                                 "capacity_first INTEGER, " +
                                                 "sold_seats_economy INTEGER, " +
                                                 "sold_seats_business INTEGER, " +
                                                 "sold_seats_first INTEGER, " +
                                                 "quality INTEGER, " +
                                                 "fuel_cost INTEGER, " +
                                                 "crew_cost INTEGER, " +
                                                 "airport_fees INTEGER, " +
                                                 "inflight_cost INTEGER, " +
                                                 "maintenance_cost INTEGER, " +
                                                 "depreciation INTEGER, " +
                                                 "revenue INTEGER, " +
                                                 "profit INTEGER, " +
                                                 "from_airport INTEGER, " +
                                                 "to_airport INTEGER, " +
                                                 "airline INTEGER, " +
                                                 "distance INTEGER, " +
                                                 "cycle INTEGER, " +
                                                 "PRIMARY KEY (cycle, link))")
         
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + LINK_CONSUMPTION_INDEX_1 +  " ON " + LINK_CONSUMPTION_TABLE + "(link)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + LINK_CONSUMPTION_INDEX_2 +  " ON " + LINK_CONSUMPTION_TABLE + "(airline)")
         statement.execute()
         statement.close()
          
         statement = connection.prepareStatement("CREATE TABLE " + WATCHED_LINK_TABLE + "(" +
                                                 "airline INTEGER PRIMARY KEY, " +
    	                                           "watched_link INTEGER UNIQUE, " +
                                                 "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)")
         
         statement.execute()
         statement.close()
         
         //from_airport, to_airport, is_departure, is_destination, passenger_count, cycle
         statement = connection.prepareStatement("CREATE TABLE " + LINK_STATISTICS_TABLE + "(" +
                                                 "from_airport INTEGER, " +
    	                                           "to_airport INTEGER, " +
                                                 "is_departure INTEGER, " +
                                                 "is_destination INTEGER, " +
                                                 "passenger_count INTEGER, " +
                                                 "airline INTEGER, " +
                                                 "cycle INTEGER, " +
                                                 "PRIMARY KEY (from_airport, to_airport, is_departure, is_destination, cycle))")
         statement.execute()
         statement.close()
                                                 
         statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_1 +  " ON " + LINK_STATISTICS_TABLE + "(from_airport)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_2 +  " ON " + LINK_STATISTICS_TABLE + "(to_airport)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_3 +  " ON " + LINK_STATISTICS_TABLE + "(airline)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_4 +  " ON " + LINK_STATISTICS_TABLE + "(cycle)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + VIP_ROUTE_TABLE + "(" +
                                                 "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                 "cycle INTEGER)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + VIP_ROUTE_ENTRY_TABLE + "(" + 
                                                 "route INTEGER REFERENCES " + VIP_ROUTE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                                 "from_airport INTEGER ," +
                                                 "to_airport INTEGER ," +
                                                 "airline INTEGER)")
         statement.execute()
         statement.close()
         
         statement = connection.prepareStatement("CREATE TABLE " + LINK_ASSIGNMENT_TABLE + "(" +
                                                 //"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                 "link INTEGER REFERENCES " + LINK_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "airplane INTEGER REFERENCES " + AIRPLANE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                                 "PRIMARY KEY (link, airplane))")
         statement.execute()
         statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_ASSIGNMENT_INDEX_1 + " ON " + LINK_ASSIGNMENT_TABLE + "(link)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_ASSIGNMENT_INDEX_2 + " ON " + LINK_ASSIGNMENT_TABLE + "(airplane)")
    statement.execute()
    statement.close()
     
     
     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_TABLE + "(" + 
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "name VARCHAR(256), " +
                                             "capacity INTEGER, " + 
                                             "fuel_burn INTEGER, " +
                                             "speed INTEGER, " +
                                             "fly_range INTEGER, " +
                                             "price INTEGER)")
     statement.execute()
     statement.close()
     

     statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_TABLE + "(" + 
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "model INTEGER REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "owner INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "constructed_cycle INTEGER, " +
                                             "airplane_condition DOUBLE, " +
                                             "depreciation_rate INTEGER, " +
                                             "value INTEGER)")

     statement.execute()
     statement.close()
    
    statement = connection.prepareStatement("CREATE INDEX " + AIRPLANE_INDEX_1 + " ON " + AIRPLANE_TABLE + "(owner)")
    statement.execute()
    statement.close()

     
     
     statement = connection.prepareStatement("CREATE TABLE " + USER_TABLE + "(" +
                                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                             "user_name VARCHAR(100) UNIQUE, " +
                                             "email VARCHAR(256) NOT NULL, " +
                                             "status  VARCHAR(256) NOT NULL, " +
                                             "creation_time DATETIME DEFAULT CURRENT_TIMESTAMP)")
     statement.execute()
     statement.close()
       
     statement = connection.prepareStatement("CREATE TABLE " + USER_SECRET_TABLE + "(" + 
                                             "user_name VARCHAR(100) PRIMARY KEY REFERENCES " + USER_TABLE + "(user_name) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "digest VARCHAR(32) NOT NULL, " +
                                             "salt VARCHAR(32) NOT NULL)")
     statement.execute()
     statement.close()
     
     statement = connection.prepareStatement("CREATE TABLE " + USER_AIRLINE_TABLE + "(" +
                                             "airline INTEGER PRIMARY KEY," +
                                             "user_name VARCHAR(100) REFERENCES " + USER_TABLE + "(user_name) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)")
     statement.execute()
     statement.close()
     
     connection.close()
  }
}


//     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_SLOT_ASSIGNMENT_TABLE + "(" + 
//                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "assignment_value INTEGER," +
//                                             "PRIMARY KEY (airport, airline))")
//     statement.execute()
//     statement.close()
