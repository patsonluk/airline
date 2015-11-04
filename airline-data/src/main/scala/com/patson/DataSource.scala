package com.patson
import com.patson.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.AirportInfo

object DataSource {
  def loadAirportData() = {
      //open the hsqldb
      Class.forName(DB_DRIVER);
      val connection = DriverManager.getConnection(DATABASE_CONNECTION, DATABASE_USER, "");
      
      val preparedStatement = connection.prepareStatement("SELECT iata, icao, name, latitude, longitude, country_code, city, airport_size, power FROM airport")
      
      val resultSet = preparedStatement.executeQuery()
      
      val airportData = new ListBuffer[AirportInfo]()        
      while (resultSet.next()) {
        airportData += AirportInfo( 
          resultSet.getString(1),
          resultSet.getString(2),
          resultSet.getString(3),
          resultSet.getDouble(4),
          resultSet.getDouble(5),
          resultSet.getString(6),
          resultSet.getString(7),
          resultSet.getInt(8),
          resultSet.getLong(9))
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      println("Loaded " + airportData.length + " airport records")
      
      
      
      airportData.toList
  }
}