package com.patson.data

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import com.patson.data.Constants._
import java.util.Properties
import com.mchange.v2.c3p0.ComboPooledDataSource

object Meta {
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
  dataSource.setTestConnectionOnCheckout(true)

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

  def resetDatabase = {
    createSchema()
  }

  def createSchema() {
    val connection = getConnection(false)
    var statement: PreparedStatement = null

    statement = connection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + PASSENGER_HISTORY_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CYCLE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CITY_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_AIRLINE_RELATIONSHIP_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE)
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
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_PROJECT_TABLE)
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

    statement = connection.prepareStatement("CREATE TABLE " + CITY_TABLE + "(id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), population INTEGER, income INTEGER)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_TABLE + "(code CHAR(2) PRIMARY KEY, name VARCHAR(256), airport_population INTEGER, income INTEGER, openness INTEGER)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_TABLE + "( id INTEGER PRIMARY KEY AUTO_INCREMENT, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256), zone VARCHAR(16), airport_size INTEGER, power LONG, population LONG, slots LONG)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_INDEX_1 + " ON " + AIRPORT_TABLE + "(country_code)")
    statement.execute()
    statement.close()
    

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_TABLE + "( id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(256), is_generated TINYINT(1))")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_AIRLINE_RELATIONSHIP_TABLE + "(country CHAR(2), airline INTEGER, relationship INTEGER," +
                                            "PRIMARY KEY (country, airline)," +
	                                          "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
	                                          "FOREIGN KEY(country) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + COUNTRY_AIRLINE_RELATIONSHIP_INDEX_1 + " ON " + AIRLINE_TABLE + "(id)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + COUNTRY_AIRLINE_RELATIONSHIP_INDEX_2 + " ON " + COUNTRY_TABLE + "(code)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE + "(country_1 CHAR(2), country_2 CHAR(2), relationship INTEGER," +
                                            "PRIMARY KEY (country_1, country_2)," +
                                            "FOREIGN KEY(country_1) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE," +
                                            "FOREIGN KEY(country_2) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_INFO_TABLE + "(" +
      "airline INTEGER PRIMARY KEY, " +
      "balance LONG," +
      "service_quality DECIMAL(5,2)," +
      "service_funding INTEGER," +
      "maintenance_quality DECIMAL(5,2)," +
      "reputation DECIMAL(5,2)," +
      "country_code CHAR(2)," +
      "airline_code CHAR(2)," +
      "color CHAR(7)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_APPEAL_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "loyalty DECIMAL(5,2)," +
      "awareness DECIMAL(5,2)," +
      "PRIMARY KEY (airport, airline)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_APPEAL_INDEX_1 + " ON " + AIRLINE_APPEAL_TABLE + "(airport)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_APPEAL_INDEX_2 + " ON " + AIRLINE_APPEAL_TABLE + "(airline)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_BASE_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "scale INTEGER," +
      "founded_cycle INTEGER," +
      "headquarter INTEGER," +
      "country CHAR(2) NOT NULL, " +
      "PRIMARY KEY (airport, airline)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(country) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_BASE_INDEX_1 + " ON " + AIRPORT_TABLE + "(id)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_BASE_INDEX_2 + " ON " + AIRLINE_TABLE + "(id)")
    statement.execute()
    statement.close()
      statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_BASE_INDEX_3 + " ON " + COUNTRY_TABLE + "(code)")
      statement.execute()
      statement.close()

      
    createAirlineTransaction(connection)  
    createIncome(connection)
    createAirportImage(connection)
    createLoan(connection)
    createCountryMarketShare(connection)
    createAirlineLogo(connection)
    createAirplaneRenewal(connection)
    createAlliance(connection)
    createResetUser(connection)
      
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_CITY_SHARE_TABLE + "(" +
      "airport INTEGER," +
      "city INTEGER," +
      "share DOUBLE," +
      "PRIMARY KEY (airport, city)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(city) REFERENCES " + CITY_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_CITY_SHARE_INDEX_1 + " ON " + AIRPORT_CITY_SHARE_TABLE + "(airport)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_CITY_SHARE_INDEX_2 + " ON " + AIRPORT_CITY_SHARE_TABLE + "(city)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_FEATURE_TABLE + "(" +
      "airport INTEGER," +
      "feature_type VARCHAR(256)," +
      "strength DOUBLE," +
      "PRIMARY KEY (airport, feature_type)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_FEATURE_INDEX_1 + " ON " + AIRPORT_FEATURE_TABLE + "(airport)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_PROJECT_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airport INTEGER," +
      "project_type VARCHAR(256)," +
      "project_status VARCHAR(256)," +
      "progress DOUBLE," +
      "duration INTEGER," +
      "level INTEGER," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPORT_PROJECT_INDEX_1 + " ON " + AIRPORT_PROJECT_TABLE + "(airport)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LINK_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "airline INTEGER, " +
      "price_economy INTEGER, " +
      "price_business INTEGER, " +
      "price_first INTEGER, " +
      "distance DOUBLE, " +
      "capacity_economy INTEGER, " +
      "capacity_business INTEGER, " +
      "capacity_first INTEGER, " +
      "quality INTEGER, " +
      "duration INTEGER, " +
      "frequency INTEGER," +
      "flight_type INTEGER," + 
      "flight_number INTEGER," +
      "FOREIGN KEY(from_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(to_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE UNIQUE INDEX " + LINK_INDEX_1 + " ON " + LINK_TABLE + "(from_airport, to_airport, airline)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_2 + " ON " + LINK_TABLE + "(from_airport)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_3 + " ON " + LINK_TABLE + "(to_airport)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_4 + " ON " + LINK_TABLE + "(airline)")
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
      "delay_compensation INTEGER, " +
      "maintenance_cost INTEGER, " +
      "depreciation INTEGER, " +
      "revenue INTEGER, " +
      "profit INTEGER, " +
      "minor_delay_count INTEGER, " +
      "major_delay_count INTEGER, " +
      "cancellation_count INTEGER, " +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "airline INTEGER, " +
      "distance INTEGER, " +
      "frequency INTEGER, " +
      "cycle INTEGER, " +
      "PRIMARY KEY (cycle, link))")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_CONSUMPTION_INDEX_1 + " ON " + LINK_CONSUMPTION_TABLE + "(link)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_CONSUMPTION_INDEX_2 + " ON " + LINK_CONSUMPTION_TABLE + "(airline)")
    statement.execute()
    statement.close()

//    statement = connection.prepareStatement("CREATE TABLE " + WATCHED_LINK_TABLE + "(" +
//      "airline INTEGER PRIMARY KEY, " +
//      "watched_link INTEGER UNIQUE, " +
//      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)")
//
//    statement.execute()
//    statement.close()
//
//    statement = connection.prepareStatement("CREATE TABLE " + LINK_HISTORY_TABLE + "(" +
//      "watched_link INTEGER, " +
//      "inverted INTEGER, " +
//      "related_link INTEGER, " +
//      "from_airport INTEGER, " +
//      "to_airport INTEGER, " +
//      "airline INTEGER, " +
//      "passenger INTEGER," +
//      "FOREIGN KEY(watched_link) REFERENCES " + WATCHED_LINK_TABLE + "(watched_link) ON DELETE CASCADE ON UPDATE CASCADE" +
//      ")")
//
//    statement.execute()
//    statement.close()
//
//    statement = connection.prepareStatement("CREATE INDEX " + LINK_HISTORY_INDEX_1 + " ON " + LINK_HISTORY_TABLE + "(watched_link)")
//    statement.execute()
//    statement.close()

    //from_airport, to_airport, is_departure, is_destination, passenger_count, cycle
    statement = connection.prepareStatement("CREATE TABLE " + LINK_STATISTICS_TABLE + "(" +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "is_departure INTEGER, " +
      "is_destination INTEGER, " +
      "passenger_count INTEGER, " +
      "airline INTEGER, " +
      "cycle INTEGER)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_1 + " ON " + LINK_STATISTICS_TABLE + "(from_airport)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_2 + " ON " + LINK_STATISTICS_TABLE + "(to_airport)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_3 + " ON " + LINK_STATISTICS_TABLE + "(airline)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_STATISTICS_INDEX_4 + " ON " + LINK_STATISTICS_TABLE + "(cycle)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + VIP_ROUTE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "cycle INTEGER)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + VIP_ROUTE_ENTRY_TABLE + "(" +
      "route INTEGER," +
      "from_airport INTEGER ," +
      "to_airport INTEGER ," +
      "airline INTEGER," +
      "FOREIGN KEY(route) REFERENCES " + VIP_ROUTE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LINK_ASSIGNMENT_TABLE + "(" +
      //"id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "link INTEGER, " +
      "airplane INTEGER, " +
      "PRIMARY KEY (link, airplane)," +
      "FOREIGN KEY(link) REFERENCES " + LINK_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airplane) REFERENCES " + AIRPLANE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_ASSIGNMENT_INDEX_1 + " ON " + LINK_ASSIGNMENT_TABLE + "(link)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LINK_ASSIGNMENT_INDEX_2 + " ON " + LINK_ASSIGNMENT_TABLE + "(airplane)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "name VARCHAR(256), " +
      "capacity INTEGER, " +
      "fuel_burn INTEGER, " +
      "speed INTEGER, " +
      "fly_range INTEGER, " +
      "price INTEGER, " +
      "lifespan INTEGER, " +
      "construction_time INTEGER, " +
      "country_code CHAR(2), " +
      "image_url VARCHAR(256))")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "model INTEGER, " +
      "owner INTEGER, " +
      "constructed_cycle INTEGER, " +
      "airplane_condition DECIMAL(7,4), " +
      "depreciation_rate INTEGER, " +
      "value INTEGER," +
      "FOREIGN KEY(model) REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(owner) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPLANE_INDEX_1 + " ON " + AIRPLANE_TABLE + "(owner)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPLANE_INDEX_2 + " ON " + AIRPLANE_TABLE + "(model)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "user_name VARCHAR(100) UNIQUE, " +
      "email VARCHAR(256) NOT NULL, " +
      "status  VARCHAR(256) NOT NULL, " +
      "creation_time DATETIME DEFAULT CURRENT_TIMESTAMP, " + 
      "last_active DATETIME DEFAULT CURRENT_TIMESTAMP)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_SECRET_TABLE + "(" +
      "user_name VARCHAR(100) PRIMARY KEY," +
      "digest VARCHAR(32) NOT NULL, " +
      "salt VARCHAR(32) NOT NULL," +
      "FOREIGN KEY(user_name) REFERENCES " + USER_TABLE + "(user_name) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_AIRLINE_TABLE + "(" +
      "airline INTEGER PRIMARY KEY," +
      "user_name VARCHAR(100)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(user_name) REFERENCES " + USER_TABLE + "(user_name) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + PASSENGER_HISTORY_TABLE + "(" + 
                                            "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                                            "passenger_type INTEGER," + 
                                            "passenger_count INTEGER," +
                                            "route_id INTEGER," +
                                            "link INTEGER," +
                                            "link_class VARCHAR(2)," +
                                            "inverted INTEGER)")
                                            
    statement.execute()
    statement.close()
    
    connection.close()
  }
  
  def createAirlineTransaction(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_TRANSACTION_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_TRANSACTION_TABLE + "(" +
      "airline INTEGER, " +
      "transaction_type INTEGER, " +
      "amount LONG," +
      "cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_TRANSACTION_INDEX_1 + " ON " + AIRLINE_TRANSACTION_TABLE + "(airline)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRLINE_TRANSACTION_INDEX_2 + " ON " + AIRLINE_TRANSACTION_TABLE + "(cycle)")
    statement.execute()
    statement.close()
  }
  
  def createAirlineLogo(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_LOGO_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_LOGO_TABLE + "(" +
      "airline INTEGER, " +
      "logo BLOB, " +
      "PRIMARY KEY (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }
  
  def createIncome(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + INCOME_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + INCOME_TABLE + "(" +
      "airline INTEGER, " +
      "profit LONG, " +
      "revenue LONG, " +
      "expense LONG," +
      "period INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airline, period, cycle)" +
      ")")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINKS_INCOME_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + LINKS_INCOME_TABLE + "(" +
      "airline INTEGER, " +
      "profit LONG, " +
      "revenue LONG, " +
      "expense LONG," +
      "ticket_revenue LONG," +
      "airport_fee LONG," +
      "fuel_cost LONG," +
      "crew_cost LONG," +
      "inflight_cost LONG," +
      "delay_compensation LONG," +
      "maintenance_cost LONG," +
      "depreciation LONG," +
      "period INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airline, period, cycle)" +
      ")")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + TRANSACTIONS_INCOME_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + TRANSACTIONS_INCOME_TABLE + "(" +
      "airline INTEGER, " +
      "profit LONG, " +
      "revenue LONG, " +
      "expense LONG, " +
      "capital_gain LONG," +
      "create_link LONG," +
      "period INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airline, period, cycle)" +
      ")")

    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OTHERS_INCOME_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + OTHERS_INCOME_TABLE + "(" +
      "airline INTEGER, " +
      "profit LONG, " +
      "revenue LONG, " +
      "expense LONG, " +
      "loan_interest LONG," +
      "base_upkeep LONG," +
      "service_investment LONG," +
      "maintenance_investment LONG," +
      "advertisement LONG," +
      "depreciation LONG," +
      "period INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airline, period, cycle)" +
      ")")
    statement.execute()
    statement.close()
  }
  
  def createAirportImage(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_IMAGE_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_IMAGE_TABLE + "(" +
      "airport INTEGER, " +
      "city_url VARCHAR(1024), " +
      "airport_url  VARCHAR(1024), " +
      "PRIMARY KEY (airport)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }
  
  def createLoan(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOAN_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + LOAN_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "borrowed_amount LONG, " +
      "interest LONG, " +
      "remaining_amount LONG," +
      "creation_cycle INTEGER," +
      "loan_term LONG," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }
  
  def createCountryMarketShare(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_MARKET_SHARE_TABLE)
    statement.execute()
    statement.close()
   
    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_MARKET_SHARE_TABLE + "(country CHAR(2), airline INTEGER, passenger_count BIGINT(20)," +
                                            "PRIMARY KEY (country, airline)," +
                                            "FOREIGN KEY(country) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE," +
                                            "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)")
    statement.execute()
    statement.close()
  }
  
  def createAirplaneRenewal(connection : Connection) {
    var statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_RENEWAL_TABLE + "(" +
      "airline INTEGER, " +
      "threshold INTEGER, " +
      "PRIMARY KEY (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }
  
  def createAlliance(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "name VARCHAR(256), " +
      "creation_cycle INTEGER" + 
      ")")
    statement.execute()
    
    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MEMBER_TABLE + "(" +
      "alliance INTEGER," +
      "airline INTEGER, " +
      "role VARCHAR(256), " +
      "joined_cycle INTEGER, " + 
      "PRIMARY KEY (alliance, airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "FOREIGN KEY(alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    
    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_HISTORY_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "cycle INTEGER," +
      "airline INTEGER, " +
      "alliance_name VARCHAR(256)," +
      "event VARCHAR(256), " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    
    
    
    statement.close()
  }
  
  def createResetUser(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + RESET_USER_TABLE)
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + RESET_USER_TABLE + "(" +
      "user_name VARCHAR(100) PRIMARY KEY, " +
      "token VARCHAR(256) NOT NULL, " + 
      "FOREIGN KEY(user_name) REFERENCES " + USER_TABLE + "(user_name) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }
}


//     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_SLOT_ASSIGNMENT_TABLE + "(" + 
//                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "assignment_value INTEGER," +
//                                             "PRIMARY KEY (airport, airline))")
//     statement.execute()
//     statement.close()
