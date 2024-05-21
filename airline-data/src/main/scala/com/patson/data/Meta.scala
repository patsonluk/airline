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

    statement = connection.prepareStatement("CREATE TABLE " + CITY_TABLE + "(id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(256) CHARACTER SET 'utf8', latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256) CHARACTER SET 'utf8', population INTEGER, income INTEGER)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_TABLE + "(code CHAR(2) PRIMARY KEY, name VARCHAR(256) CHARACTER SET 'utf8', airport_population INTEGER, income INTEGER, openness INTEGER)")
    statement.execute()
    statement.close()
    
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_TABLE + "( id INTEGER PRIMARY KEY AUTO_INCREMENT, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256) CHARACTER SET 'utf8', latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256) CHARACTER SET 'utf8', zone VARCHAR(16), airport_size INTEGER, income BIGINT, population LONG, slots LONG, runway_length SMALLINT)")
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
      "target_service_quality INTEGER," +
      "maintenance_quality DECIMAL(5,2)," +
      "reputation DECIMAL(5,2)," +
      "country_code CHAR(2)," +
      "airline_code CHAR(2)," +
      "color CHAR(7)," +
      "skip_tutorial TINYINT," +
      "initialized TINYINT," +
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
    createAirlineCashFlowItem(connection)
    createCashFlow(connection)
    createAirportImage(connection)
    createLoan(connection)
    createCountryMarketShare(connection)
    createCountryAirlineTitle(connection)
    createAirlineLogo(connection)
    createAirlineLivery(connection)
    createAirlineSlogan(connection)
    createAirlineNameHistory(connection)
    createUserWallpaper(connection)
    createAirplaneRenewal(connection)
    createAirplaneConfiguration(connection)
    createAlliance(connection)
    createLounge(connection)
    createLoungeConsumption(connection)
    createShuttleService(connection)
    createOil(connection)
    createLoanInterestRate(connection)
    createResetUser(connection)
    createLog(connection)
    createLogProperty(connection)
    createAlert(connection)
    createEvent(connection)
    createSantaClaus(connection)
    createAirportAirlineBonus(connection)
    createAirplaneModelFavorite(connection)
    createAirplaneModelDiscount(connection)
    createLinkChangeHistory(connection)
    createGoogleResource(connection)
    createDelegate(connection)
    createAirportRunway(connection)
    createChatMessage(connection)
    createLoyalist(connection)
    createCampaign(connection)
    createLinkNegotiation(connection)
    createTutorial(connection)
    createNotice(connection)
    createAirportChampion(connection)
    createAirportAnimation(connection)
    createAirlineBaseSpecialization(connection)
    createAirlineBaseSpecializationLastUpdate(connection)
    createReputationBreakdown(connection)
    createIp(connection)
    createAdminLog(connection)
    createUserUuid(connection)
    createAirlineModifier(connection)
    createAirlineModifierProperty(connection)
    createUserModifier(connection)
    createAllianceLabelColor(connection)
    createAirportAsset(connection)

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
      "airplane_model SMALLINT," +
      "from_country CHAR(2)," +
      "to_country CHAR(2)," +
      "transport_type SMALLINT," +
      "last_update DATETIME DEFAULT CURRENT_TIMESTAMP," +
      "FOREIGN KEY(from_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(to_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
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
    statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_5 + " ON " + LINK_TABLE + "(from_country)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + LINK_INDEX_6 + " ON " + LINK_TABLE + "(to_country)")
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
      "quality SMALLINT, " +
      "fuel_cost INTEGER, " +
      "crew_cost INTEGER, " +
      "airport_fees INTEGER, " +
      "inflight_cost INTEGER, " +
      "delay_compensation INTEGER, " +
      "maintenance_cost INTEGER, " +
      "lounge_cost INTEGER, " +
      "depreciation INTEGER, " +
      "revenue INTEGER, " +
      "profit INTEGER, " +
      "minor_delay_count SMALLINT, " +
      "major_delay_count SMALLINT, " +
      "cancellation_count SMALLINT, " +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "airline INTEGER, " +
      "distance INTEGER, " +
      "frequency SMALLINT, " +
      "duration SMALLINT, " +
      "transport_type TINYINT, " +
      "flight_type TINYINT, " +
      "flight_number SMALLINT, " +
      "airplane_model SMALLINT, " +
      "raw_quality SMALLINT, " +
      "satisfaction DECIMAL(5,4), " +
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

    statement = connection.prepareStatement("CREATE INDEX " + LINK_CONSUMPTION_INDEX_3 + " ON " + LINK_CONSUMPTION_TABLE + "(cycle DESC)")
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
      "frequency INTEGER, " +
      "flight_minutes INTEGER, " +
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
      "family VARCHAR(256), " +
      "capacity INTEGER, " +
      "fuel_burn INTEGER, " +
      "speed INTEGER, " +
      "fly_range INTEGER, " +
      "price INTEGER, " +
      "lifespan INTEGER, " +
      "construction_time INTEGER, " +
      "country_code CHAR(2), " +
      "manufacturer VARCHAR(256)," +
      "image_url VARCHAR(256)," +
      "runway_requirement BIGINT)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "model INTEGER, " +
      "owner INTEGER, " +
      "constructed_cycle INTEGER, " +
      "purchased_cycle INTEGER, " +
      "airplane_condition DECIMAL(7,4), " +
      "depreciation_rate INTEGER, " +
      "value INTEGER," +
      "is_sold TINYINT(1)," +
      "dealer_ratio DECIMAL(7,6)," +
      "home INTEGER," +
      "purchase_rate DECIMAL(4,3) DEFAULT 1," +
      "version INTEGER DEFAULT 0," +
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
      "admin_status VARCHAR(256), " +
      "creation_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
      "level INTEGER NOT NULL DEFAULT 0, " +
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
                                            "passenger_type TINYINT," +
                                            "passenger_count INTEGER," +
                                            "route_id INTEGER," +
                                            "link INTEGER," +
                                            "link_class CHAR(1)," +
                                            "inverted TINYINT," +
                                            "home_country CHAR(2) NOT NULL DEFAULT ''," +
                                            "home_airport INT(11)," +
                                            "destination_airport INT(11)," +
                                            "preference_type TINYINT," +
                                            "preferred_link_class CHAR(1)," +
                                            "cost INT" +
                                            ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + PASSENGER_HISTORY_INDEX_1 + " ON " + PASSENGER_HISTORY_TABLE + "(link)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + PASSENGER_HISTORY_INDEX_2 + " ON " + PASSENGER_HISTORY_TABLE + "(route_id)")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + PASSENGER_HISTORY_INDEX_3 + " ON " + PASSENGER_HISTORY_TABLE + "(home_airport, destination_airport)")
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

  def createAirlineCashFlowItem(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_CASH_FLOW_ITEM_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_CASH_FLOW_ITEM_TABLE + "(" +
      "airline INTEGER, " +
      "cash_flow_type INTEGER, " +
      "amount BIGINT(20)," +
      "cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
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

  def createAirlineLivery(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_LIVERY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_LIVERY_TABLE + "(" +
      "airline INTEGER, " +
      "livery MEDIUMBLOB, " +
      "PRIMARY KEY (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirlineSlogan(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_SLOGAN_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_SLOGAN_TABLE + "(" +
      "airline INTEGER, " +
      "slogan VARCHAR(256), " +
      "PRIMARY KEY (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirlineNameHistory(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_NAME_HISTORY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_NAME_HISTORY_TABLE + "(" +
      "airline INTEGER, " +
      "name VARCHAR(256), " +
      "update_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
      "PRIMARY KEY (airline,name,update_timestamp)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createUserWallpaper(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_WALLPAPER_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_WALLPAPER_TABLE + "(" +
      "user INTEGER, " +
      "wallpaper MEDIUMBLOB, " +
      "PRIMARY KEY (user)," +
      "FOREIGN KEY(user) REFERENCES " + USER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
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
      "lounge_cost LONG," +
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
      "advertisement LONG," +
      "lounge_upkeep LONG, " +
      "lounge_cost LONG, " +
      "lounge_income LONG, " +
      "asset_expense LONG, " +
      "asset_revenue LONG, " +
      "fuel_profit LONG, " +
      "depreciation LONG," +
      "overtime_compensation LONG," +
      "period INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airline, period, cycle)" +
      ")")
    statement.execute()
    statement.close()
  }

  def createCashFlow(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CASH_FLOW_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + CASH_FLOW_TABLE + "(" +
      "airline INTEGER, " +
      "cash_flow BIGINT(20), " +
      "operation BIGINT(20), " +
      "loan_interest BIGINT(20), " +
      "loan_principle BIGINT(20)," +
      "base_construction BIGINT(20), " +
      "buy_airplane BIGINT(20), " +
      "sell_airplane BIGINT(20)," +
      "create_link BIGINT(20), " +
      "facility_construction BIGINT(20), " +
      "oil_contract BIGINT(20), " +
      "asset_transactions BIGINT(20), " +
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
      "principal LONG, " +
      "annual_rate DECIMAL(7,6), " +
      "creation_cycle INTEGER," +
      "last_payment_cycle INTEGER," +
      "term LONG," +
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

  def createCountryAirlineTitle(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_AIRLINE_TITLE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_AIRLINE_TITLE_TABLE + "(country CHAR(2), airline INT(11), title TINYINT," +
      "PRIMARY KEY (country, airline)," +
      "FOREIGN KEY(country) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE)")
    statement.execute()
    statement.close()
  }

  def createAirplaneRenewal(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_RENEWAL_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_RENEWAL_TABLE + "(" +
      "airline INTEGER, " +
      "threshold INTEGER, " +
      "PRIMARY KEY (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirplaneConfiguration(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_CONFIGURATION_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "model INTEGER, " +
      "economy INTEGER, " +
      "business INTEGER, " +
      "first INTEGER, " +
      "is_default TINYINT(1), " +
      "FOREIGN KEY(model) REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + AIRPLANE_CONFIGURATION_TEMPLATE_INDEX_1 + " ON " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + "(airline)")
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("CREATE INDEX " + AIRPLANE_CONFIGURATION_TEMPLATE_INDEX_2 + " ON " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + "(model)")
    statement.execute()
    statement.close()



    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_CONFIGURATION_TABLE + "(" +
      "airplane INTEGER, " +
      "configuration INTEGER, " +
      "PRIMARY KEY (airplane)," +
      "FOREIGN KEY(configuration) REFERENCES " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "FOREIGN KEY(airplane) REFERENCES " + AIRPLANE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
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

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MEMBER_TABLE)
    statement.execute()
    statement.close()

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

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_HISTORY_TABLE)
    statement.execute()
    statement.close()

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

  def createLounge(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOUNGE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LOUNGE_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "name VARCHAR(256), " +
      "level INTEGER," +
      "status VARCHAR(16)," +
      "founded_cycle INTEGER," +
      "PRIMARY KEY (airport, airline), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createShuttleService(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + SHUTTLE_SERVICE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + SHUTTLE_SERVICE_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "name VARCHAR(256), " +
      "level INTEGER," +
      "founded_cycle INTEGER," +
      "PRIMARY KEY (airport, airline), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createLoungeConsumption(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOUNGE_CONSUMPTION_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LOUNGE_CONSUMPTION_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "self_visitors INTEGER," +
      "alliance_visitors INTEGER," +
      "cycle INTEGER," +
      "PRIMARY KEY (airport, airline), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createLog(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOG_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LOG_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER, " +
      "message VARCHAR(512) CHARACTER SET 'utf8'," +
      "category INTEGER," +
      "severity INTEGER," +
      "cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + LOG_INDEX_1 + " ON " + LOG_TABLE + "(airline)")
    statement.execute()
    statement.close()
  }

  def createLogProperty(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOG_PROPERTY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LOG_PROPERTY_TABLE + "(" +
      "log INTEGER," +
      "property VARCHAR(256)," +
      "value VARCHAR(256)," +
      "FOREIGN KEY(log) REFERENCES " + LOG_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAlert(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALERT_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALERT_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER, " +
      "message VARCHAR(512) CHARACTER SET 'utf8'," +
      "category INTEGER," +
      "target_id INTEGER," +
      "duration INTEGER," +
      "cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE INDEX " + ALERT_INDEX_1 + " ON " + ALERT_TABLE + "(airline)")
    statement.execute()
    statement.close()
  }

  def createEvent(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_CANDIDATE_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_AFFECTED_AIRPORT_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_AIRLINE_VOTE_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_VOTE_ROUND_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_COUNTRY_STATS_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_AIRLINE_STATS_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OLYMPIC_AIRLINE_GOAL_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + EVENT_PICKED_REWARD_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + EVENT_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + EVENT_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "event_type INTEGER," +
      "start_cycle INTEGER, " +
      "duration INTEGER" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_CANDIDATE_TABLE + "(" +
      "event INTEGER," +
      "airport INTEGER," +
      "PRIMARY KEY (event, airport), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_AFFECTED_AIRPORT_TABLE + "(" +
      "event INTEGER," +
      "principal_airport INTEGER," +
      "affected_airport INTEGER," +
      "PRIMARY KEY (event, principal_airport, affected_airport), " +
      "FOREIGN KEY(principal_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(affected_airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_AIRLINE_VOTE_TABLE + "(" +
      "event INTEGER," +
      "airline INTEGER," +
      "airport INTEGER," +
      "vote_weight INTEGER," +
      "precedence INTEGER," +
      "PRIMARY KEY (event, airport, airline), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_VOTE_ROUND_TABLE + "(" +
      "event INTEGER," +
      "airport INTEGER," +
      "round INTEGER," +
      "vote INTEGER," +
      "PRIMARY KEY (event, airport, round), " +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_COUNTRY_STATS_TABLE + "(" +
      "event INTEGER," +
      "cycle INTEGER," +
      "country_code CHAR(2)," +
      "transported INTEGER," +
      "total INTEGER," +
      "PRIMARY KEY (event, cycle, country_code), " +
      "FOREIGN KEY(country_code) REFERENCES " + COUNTRY_TABLE + "(code) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_AIRLINE_STATS_TABLE + "(" +
      "event INTEGER," +
      "cycle INTEGER," +
      "airline INTEGER," +
      "score DECIMAL(15,2)," +
      "PRIMARY KEY (event, cycle, airline), " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OLYMPIC_AIRLINE_GOAL_TABLE + "(" +
      "event INTEGER," +
      "airline INTEGER," +
      "goal INTEGER," +
      "PRIMARY KEY (event, airline), " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + EVENT_PICKED_REWARD_TABLE + "(" +
      "event INTEGER," +
      "airline INTEGER," +
      "reward_category INTEGER," +
      "reward_option INTEGER," +
      "PRIMARY KEY (event, airline, reward_category), " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(event) REFERENCES " + EVENT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirportAirlineBonus(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE)
    statement.execute()
    statement.close()

    //case class AirlineAppealBonus(loyalty : Double, awareness : Double, bonusType: BonusType.Value, expirationCycle : Option[Int])
    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE + "(" +
      "airline INTEGER," +
      "airport INTEGER," +
      "bonus_type INTEGER," +
      "loyalty_bonus DECIMAL(5,2), " +
      "awareness_bonus DECIMAL(5,2)," +
      "expiration_cycle INTEGER," +
      "INDEX " + AIRPORT_AIRLINE_APPEAL_BONUS_INDEX_1 + " (airline,airport,bonus_type)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirplaneModelFavorite(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_MODEL_FAVORITE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_FAVORITE_TABLE + "(" +
      "airline INTEGER PRIMARY KEY," +
      "model INTEGER," +
      "start_cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirplaneModelDiscount(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE + "(" +
      "airline INTEGER," +
      "model INTEGER," +
      "discount DECIMAL(5,2)," +
      "discount_type INTEGER," +
      "discount_reason INTEGER," +
      "expiration_cycle INTEGER," +
      "PRIMARY KEY (airline, model, discount_type, discount_reason), " +
      "FOREIGN KEY(model) REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPLANE_MODEL_DISCOUNT_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPLANE_MODEL_DISCOUNT_TABLE + "(" +
      "model INTEGER," +
      "discount DECIMAL(5,2)," +
      "discount_type INTEGER," +
      "discount_reason INTEGER," +
      "expiration_cycle INTEGER," +
      "PRIMARY KEY (model, discount_type, discount_reason), " +
      "FOREIGN KEY(model) REFERENCES " + AIRPLANE_MODEL_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }


  def createSantaClaus(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + SANTA_CLAUS_INFO_TABLE)
    statement.execute()
    statement.close()

    //case class SantaClausInfo(airport : Airport, airline : Airline, attemptsLeft : Int, guesses : List[SantaClausGuess], found : Boolean, pickedAward : Option[SantaClausAwardType.Value], var id : Int = 0)
    statement = connection.prepareStatement("CREATE TABLE " + SANTA_CLAUS_INFO_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER, " +
      "airport  INTEGER," +
      "attempts_left INTEGER," +
      "found TINYINT(1)," +
      "picked_award INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + SANTA_CLAUS_GUESS_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + SANTA_CLAUS_GUESS_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER, " +
      "airport  INTEGER," +
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

  def createOil(connection : Connection) {
    //airline, price, volume, cost, start_cycle, duration
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OIL_CONTRACT_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OIL_CONTRACT_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "price DOUBLE, " +
      "volume INTEGER," +
      "start_cycle INTEGER," +
      "duration INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OIL_PRICE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OIL_PRICE_TABLE + "(" +
      "price DOUBLE, " +
      "cycle INTEGER," +
      "PRIMARY KEY (cycle)" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OIL_CONSUMPTION_HISTORY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OIL_CONSUMPTION_HISTORY_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "price DOUBLE, " +
      "volume INTEGER," +
      "consumption_type INTEGER," +
      "cycle INTEGER," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + OIL_INVENTORY_POLICY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + OIL_INVENTORY_POLICY_TABLE + "(" +
      "airline INTEGER, " +
      "factor DOUBLE," +
      "start_cycle INTEGER," +
      "PRIMARY KEY (airline), " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createLoanInterestRate(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOAN_INTEREST_RATE_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + LOAN_INTEREST_RATE_TABLE + "(" +
      "rate DECIMAL(5,2), " +
      "cycle INTEGER," +
      "PRIMARY KEY (cycle)" +
      ")")
    statement.execute()
    statement.close()
  }

  def createLinkChangeHistory(connection: Connection) = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_CHANGE_HISTORY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LINK_CHANGE_HISTORY_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "link INTEGER, " +
      "price_economy INTEGER, " +
      "price_business INTEGER, " +
      "price_first INTEGER, " +
      "price_economy_delta INTEGER, " +
      "price_business_delta INTEGER, " +
      "price_first_delta INTEGER, " +
      "capacity_economy INTEGER, " +
      "capacity_business INTEGER, " +
      "capacity_first INTEGER, " +
      "capacity INTEGER, " +
      "capacity_economy_delta INTEGER, " +
      "capacity_business_delta INTEGER, " +
      "capacity_first_delta INTEGER, " +
      "capacity_delta INTEGER, " +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "from_country CHAR(2), " +
      "to_country CHAR(2), " +
      "from_zone CHAR(2), " +
      "to_zone CHAR(2), " +
      "airline INTEGER, " +
      "alliance INTEGER, " +
      "frequency SMALLINT, " +
      "flight_number SMALLINT, " +
      "airplane_model SMALLINT, " +
      "raw_quality SMALLINT, " +
      "cycle INTEGER," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 1 + " (from_airport)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 2 + " (to_airport)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 3 + " (capacity_delta)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 4 + " (from_country)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 5 + " (to_country)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 6 + " (from_zone)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 7 + " (to_zone)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 8 + " (airline)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 9 + " (alliance)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 10 + " (capacity)," +
      "INDEX " + LINK_CHANGE_HISTORY_INDEX_PREFIX + 11 + " (cycle)" +
      ")")

    statement.execute()
    statement.close()
  }

  def createGoogleResource(connection: Connection) = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + GOOGLE_RESOURCE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + GOOGLE_RESOURCE_TABLE + "(" +
      "resource_id INTEGER," +
      "resource_type INTEGER, " +
      "url VARCHAR(1024)," +
      "max_age_deadline BIGINT(20)," +
      "PRIMARY KEY (resource_id, resource_type)" +
      ")")

    statement.execute()
    statement.close()
  }


  def createDelegate(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COUNTRY_DELEGATE_TASK_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_NEGOTIATION_TASK_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + BUSY_DELEGATE_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + BUSY_DELEGATE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "task_type TINYINT," +
      "available_cycle INTEGER, " +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")

    statement.execute()
    statement.close()



    statement = connection.prepareStatement("CREATE TABLE " + COUNTRY_DELEGATE_TASK_TABLE + "(" +
      "delegate INTEGER PRIMARY KEY, " +
      "country_code CHAR(2), " +
      "start_cycle INTEGER, " +
      "FOREIGN KEY(delegate) REFERENCES " + BUSY_DELEGATE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + LINK_NEGOTIATION_TASK_TABLE + "(" +
      "delegate INTEGER PRIMARY KEY , " +
      "from_airport INTEGER, " +
      "to_airport INTEGER, " +
      "start_cycle INTEGER, " +
      "FOREIGN KEY(delegate) REFERENCES " + BUSY_DELEGATE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

  }

  def createAirportRunway(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_RUNWAY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_RUNWAY_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airport INTEGER," +
      "code VARCHAR(16)," +
      "runway_type VARCHAR(256)," +
      "length SMALLINT," +
      "lighted TINYINT," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createChatMessage(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CHAT_MESSAGE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + CHAT_MESSAGE_TABLE + "(" +
      "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "user INTEGER, " +
      "room_id INTEGER, " +
      "text VARCHAR(512) CHARACTER SET 'utf8mb4'," +
      "time VARCHAR(128)" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LAST_CHAT_ID_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + LAST_CHAT_ID_TABLE + "(" +
      "user INTEGER PRIMARY KEY, " +
      "last_chat_id BIGINT" +
    ")")
    statement.execute()
    statement.close()

  }

  def createLoyalist(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOYALIST_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + LOYALIST_TABLE + "(" +
                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                                             "amount INTEGER," +
                                             "PRIMARY KEY (airport, airline)" +
                                             ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LOYALIST_HISTORY_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + LOYALIST_HISTORY_TABLE + "(" +
      "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "amount INTEGER," +
      "cycle INTEGER, " +
      "INDEX " + LOYALIST_HISTORY_INDEX_PREFIX + 1 + " (airline)," +
      "PRIMARY KEY (airport, airline, cycle)" +
      ")")

    statement.execute()
    statement.close()
  }


  def createCampaign(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CAMPAIGN_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + CAMPAIGN_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "principal_airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "population_coverage BIGINT, " +
      "radius INTEGER" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CAMPAIGN_AREA_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + CAMPAIGN_AREA_TABLE + "(" +
      "campaign INTEGER REFERENCES " + CAMPAIGN_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "PRIMARY KEY (campaign, airport)" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + CAMPAIGN_DELEGATE_TASK_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + CAMPAIGN_DELEGATE_TASK_TABLE + "(" +
      "delegate INTEGER PRIMARY KEY, " +
      "campaign INTEGER REFERENCES " + CAMPAIGN_TABLE + "(id) ON DELETE RESTRICT ON UPDATE CASCADE, " +
      "start_cycle INTEGER, " +
      "FOREIGN KEY(delegate) REFERENCES " + BUSY_DELEGATE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createLinkNegotiation(connection : Connection) = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_NEGOTIATION_COOL_DOWN_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement(s"CREATE TABLE $LINK_NEGOTIATION_COOL_DOWN_TABLE (" +
      s"airline INTEGER REFERENCES $AIRLINE_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      s"from_airport INTEGER REFERENCES $AIRPORT_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      s"to_airport INTEGER REFERENCES $AIRPORT_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "expiration_cycle INTEGER, " +
      "PRIMARY KEY (airline, from_airport, to_airport)" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + LINK_NEGOTIATION_DISCOUNT_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement(s"CREATE TABLE $LINK_NEGOTIATION_DISCOUNT_TABLE (" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      s"airline INTEGER REFERENCES $AIRLINE_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      s"from_airport INTEGER REFERENCES $AIRPORT_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      s"to_airport INTEGER REFERENCES $AIRPORT_TABLE(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "discount DECIMAL(3,2), " +
      "expiration_cycle INTEGER" +
      ")")

    statement.execute()
    statement.close()

  }



  def createTutorial(connection : Connection) = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COMPLETED_TUTORIAL_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + COMPLETED_TUTORIAL_TABLE + "(" +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "category VARCHAR(256)," +
      "id VARCHAR(256), " +
      "PRIMARY KEY (airline, category, id)" +
      ")")

    statement.execute()
    statement.close()

  }

  def createNotice(connection : Connection) = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + COMPLETED_NOTICE_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + COMPLETED_NOTICE_TABLE + "(" +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "category VARCHAR(256)," +
      "id VARCHAR(256), " +
      "PRIMARY KEY (airline, category, id)" +
      ")")

    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + TRACKING_NOTICE_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + TRACKING_NOTICE_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "category VARCHAR(256)" +
      ")")
    statement.execute()
    statement.close()

  }

  def createAirportChampion(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_CHAMPION_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_CHAMPION_TABLE + "(" +
      "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
      "loyalist INTEGER," +
      "ranking INTEGER," +
      "reputation_boost DECIMAL(5,2)," +
      "PRIMARY KEY (airport, airline)" +
      ")")

    statement.execute()
    statement.close()
  }


  def createAirportAnimation(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ANIMATION_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ANIMATION_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airport INTEGER," +
      "animation_type VARCHAR(256)," +
      "url VARCHAR(256)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }


  def createAirlineBaseSpecialization(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_BASE_SPECIALIZATION_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_BASE_SPECIALIZATION_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "specialization_type VARCHAR(256), " +
      "PRIMARY KEY (airport, airline, specialization_type)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirlineBaseSpecializationLastUpdate(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_BASE_SPECIALIZATION_LAST_UPDATE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_BASE_SPECIALIZATION_LAST_UPDATE_TABLE + "(" +
      "airport INTEGER, " +
      "airline INTEGER, " +
      "update_cycle INTEGER, " +
      "PRIMARY KEY (airport, airline, update_cycle)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createReputationBreakdown(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_REPUTATION_BREAKDOWN)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_REPUTATION_BREAKDOWN + "(" +
      "airline INTEGER, " +
      "reputation_type VARCHAR(256), " +
      "value DECIMAL(10, 2), " +
      "PRIMARY KEY (airline, reputation_type)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createIp(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_IP_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_IP_TABLE + "(" +
      "user INTEGER, " +
      "ip VARCHAR(256) NOT NULL, " +
      "occurrence INT NULL DEFAULT 0, " +
      "last_update DATETIME DEFAULT CURRENT_TIMESTAMP," +
      "PRIMARY KEY(user, ip)," +
      "FOREIGN KEY(user) REFERENCES " + USER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + BANNED_IP_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + BANNED_IP_TABLE + "(" +
      "ip VARCHAR(256) PRIMARY KEY" +
      ")")
    statement.execute()
    statement.close()
  }

  def createUserUuid(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_UUID_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_UUID_TABLE + "(" +
      "user INTEGER, " +
      "uuid VARCHAR(256) NOT NULL, " +
      "occurrence INT NULL DEFAULT 0, " +
      "last_update DATETIME DEFAULT CURRENT_TIMESTAMP," +
      "PRIMARY KEY(user, uuid)," +
      "FOREIGN KEY(user) REFERENCES " + USER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAdminLog(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ADMIN_LOG_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ADMIN_LOG_TABLE + "(" +
      "admin_user VARCHAR(256) NOT NULL, " +
      "admin_action VARCHAR(256) NOT NULL, " +
      "user_id INTEGER, " +
      "action_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAirlineModifier(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_MODIFIER_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_MODIFIER_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airline INTEGER, " +
      "modifier_name CHAR(20), " +
      "creation INTEGER," +
      "expiry INTEGER," +
      "INDEX " + AIRLINE_MODIFIER_INDEX_PREFIX + 1 + " (airline)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")"
    )
    statement.execute()
    statement.close()
  }

  def createAirlineModifierProperty(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRLINE_MODIFIER_PROPERTY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRLINE_MODIFIER_PROPERTY_TABLE + "(" +
      "id INTEGER," +
      "name VARCHAR(256), " +
      "value INTEGER," +
      "PRIMARY KEY(id, name)," +
      "FOREIGN KEY(id) REFERENCES " + AIRLINE_MODIFIER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")"
    )
    statement.execute()
    statement.close()
  }

  def createUserModifier(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + USER_MODIFIER_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + USER_MODIFIER_TABLE + "(" +
      "user INTEGER, " +
      "modifier_name CHAR(20), " +
      "creation INTEGER," +
      "PRIMARY KEY (user, modifier_name)," +
      "FOREIGN KEY(user) REFERENCES " + USER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")"
    )
    statement.execute()
    statement.close()
  }

  def createAirportAsset(connection : Connection): Unit = {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_PROPERTY_HISTORY_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_BOOST_HISTORY_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_PROPERTY_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_BOOST_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_ASSET_BLUEPRINT_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_BLUEPRINT_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
      "airport INTEGER," +
      "asset_type VARCHAR(256)," +
      "FOREIGN KEY(airport) REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_TABLE + "(" +
      "id INTEGER PRIMARY KEY, " +
      "airline INTEGER, " +
      "airport INTEGER," +
      "asset_type VARCHAR(256)," +
      "name VARCHAR(256)," +
      "level INTEGER, " +
      "completion_cycle INTEGER, " +
      "revenue BIGINT, " +
      "expense BIGINT, " +
      "roi DECIMAL(9,8), " +
      "upgrade_applied TINYINT, " +
      "FOREIGN KEY(id) REFERENCES " + AIRPORT_ASSET_BLUEPRINT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_BOOST_TABLE + "(" +
      "asset INTEGER, " +
      "boost_type VARCHAR(256), " +
      "value DECIMAL(12,3), " +
      "PRIMARY KEY (asset, boost_type)," +
      "FOREIGN KEY(asset) REFERENCES " + AIRPORT_ASSET_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_PROPERTY_TABLE + "(" +
      "asset INTEGER, " +
      "property VARCHAR(256), " +
      "value BIGINT, " +
      "PRIMARY KEY (asset, property)," +
      "FOREIGN KEY(asset) REFERENCES " + AIRPORT_ASSET_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_BOOST_HISTORY_TABLE + "(" +
      "asset INTEGER, " +
      "boost_type VARCHAR(256), " +
      "level SMALLINT, " +
      "cycle INT, " + //not strictly necessary but nice for debugging
      "value DECIMAL(12,3), " +
      "gain DECIMAL(12,3), " +
      "upgrade_factor DECIMAL(5,4), " +
      "PRIMARY KEY (asset, boost_type, level)," +
      "FOREIGN KEY(asset) REFERENCES " + AIRPORT_ASSET_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_ASSET_PROPERTY_HISTORY_TABLE + "(" +
      "asset INTEGER, " +
      "property VARCHAR(256), " +
      "cycle INT, " +
      "value VARCHAR(256), " +
      "PRIMARY KEY (asset, property, cycle)," +
      "FOREIGN KEY(asset) REFERENCES " + AIRPORT_ASSET_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }

  def createAllianceLabelColor(connection : Connection) {
    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_LABEL_COLOR_BY_ALLIANCE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_LABEL_COLOR_BY_AIRLINE_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_LABEL_COLOR_BY_AIRLINE_TABLE + "(" +
      "airline INTEGER, " +
      "target_alliance INTEGER," +
      "color CHAR(20)," +
      "PRIMARY KEY (airline, target_alliance)," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(target_alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")"
    )
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_LABEL_COLOR_BY_ALLIANCE_TABLE + "(" +
      "alliance INTEGER, " +
      "target_alliance INTEGER," +
      "color CHAR(20)," +
      "PRIMARY KEY (alliance, target_alliance)," +
      "FOREIGN KEY(alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(target_alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")"
    )
    statement.execute()
    statement.close()
  }

  def createAllianceMission(connection : Connection): Unit = {

    var statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_PROPERTY_TABLE)
    statement.execute()
    statement.close()
    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_REWARD_PROPERTY_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_REWARD_TABLE)
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_STATS_TABLE)
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("DROP TABLE IF EXISTS " + ALLIANCE_MISSION_STATS_TABLE)
    statement.execute()
    statement.close()




    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "start_cycle INTEGER, " +
      "mission_type VARCHAR(256), " +
      "duration INTEGER," +
      "alliance INTEGER, " +
      "status VARCHAR(256), " +
      "FOREIGN KEY(alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_PROPERTY_TABLE + "(" +
      "mission INTEGER, " +
      "property VARCHAR(256), " +
      "value BIGINT, " +
      "PRIMARY KEY (mission, property)," +
      "FOREIGN KEY(mission) REFERENCES " + ALLIANCE_MISSION_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()



    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE + "(" +
      "mission INTEGER, " +
      "property VARCHAR(256), " +
      "cycle INT, " +
      "value BIGINT, " +
      "PRIMARY KEY (mission, property, cycle)," +
      "FOREIGN KEY(mission) REFERENCES " + ALLIANCE_MISSION_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_REWARD_TABLE + "(" +
      "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
      "airline INTEGER, " +
      "mission INTEGER, " +
      "reward_type VARCHAR(256), " +
      "available TINYINT(1), " +
      "claimed TINYINT(1), " +
      "FOREIGN KEY(mission) REFERENCES " + ALLIANCE_MISSION_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE," +
      "FOREIGN KEY(airline) REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_REWARD_PROPERTY_TABLE + "(" +
      "reward INTEGER, " +
      "property VARCHAR(256), " +
      "value BIGINT, " +
      "PRIMARY KEY (reward, property)," +
      "FOREIGN KEY(reward) REFERENCES " + ALLIANCE_MISSION_REWARD_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()


    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_STATS_TABLE + "(" +
      "alliance INTEGER, " +
      "cycle INTEGER, " +
      "property VARCHAR(256), " +
      "value BIGINT, " +
      "PRIMARY KEY (alliance, cycle, property)," +
      "FOREIGN KEY(alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()

    statement = connection.prepareStatement("CREATE TABLE " + ALLIANCE_MISSION_STATS_TABLE + "(" +
      "alliance INTEGER, " +
      "cycle INTEGER, " +
      "property VARCHAR(256), " +
      "value BIGINT, " +
      "PRIMARY KEY (alliance, cycle, property)," +
      "FOREIGN KEY(alliance) REFERENCES " + ALLIANCE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
      ")")
    statement.execute()
    statement.close()
  }



  def isTableExist(connection : Connection, tableName : String): Boolean = {
    val tables = connection.getMetaData.getTables(null, null, tableName, null)
    tables.next()
  }

}


//     statement = connection.prepareStatement("CREATE TABLE " + AIRPORT_SLOT_ASSIGNMENT_TABLE + "(" + 
//                                             "airport INTEGER REFERENCES " + AIRPORT_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "airline INTEGER REFERENCES " + AIRLINE_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
//                                             "assignment_value INTEGER," +
//                                             "PRIMARY KEY (airport, airline))")
//     statement.execute()
//     statement.close()
