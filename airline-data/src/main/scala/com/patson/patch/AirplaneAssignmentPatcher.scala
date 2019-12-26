package com.patson.patch

import java.sql.Connection

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirplaneSource, LinkSource, Meta}
import com.patson.init.actorSystem
import com.patson.model._
import com.patson.model.airplane._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v1.1
  */
object AirplaneAssignmentPatcher extends App {
  mainFlow
  
  def mainFlow() {
    createSchema()
    patchExistingLinks()
    patchUnassignedAirplanes()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def createSchema() = {
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
    val connection = dataSource.getConnection
    Meta.createAirplaneConfiguration(connection)
    connection.close
  }

  def patchExistingLinks() = {
    AirlineSource.loadAllAirlines().foreach { airline =>
      LinkSource.loadLinksByAirlineId(airline.id).foreach { link =>
        if (link.capacity.total > 0) {
          val configuration = link.capacity / link.frequency
          //update the configuration for airplanes
          var airplanes = link.getAssignedAirplanes().toList.map(_._1)

          //calculate new frequency and assignment
          val existingFrequency = link.frequency
          val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(link.getAssignedModel().get, link.distance)
          var remainingFrequency = existingFrequency

          val newAirplaneAssignments = new mutable.HashMap[Airplane, Int]()
          airplanes = airplanes.map { airplane =>
            val frequencyForThisAirplane =
              if (remainingFrequency > maxFrequencyPerAirplane) {
                maxFrequencyPerAirplane
              } else  {
                remainingFrequency
              }
            remainingFrequency -= frequencyForThisAirplane
            if (frequencyForThisAirplane > 0) {
              newAirplaneAssignments.put(airplane, frequencyForThisAirplane)
            }

            val availableFlightMinutes = Airplane.MAX_FLIGHT_MINUTES - Computation.calculateFlightMinutesRequired(airplane.model, link.distance) * frequencyForThisAirplane
            airplane.copy(availableFlightMinutes = availableFlightMinutes, configuration = configuration)
          }

          AirplaneSource.updateAirplanes(airplanes)

          System.out.println(link.id + " has remainingFrequency " + remainingFrequency)

          val newLink =
            if (remainingFrequency > 0) { //update frequency and capacity if we cannot accommodate everything
              val newFrequency = existingFrequency - remainingFrequency
              link.copy(capacity = configuration * newFrequency, frequency = newFrequency)
            } else {
              link
            }

          newLink.setAssignedAirplanes(newAirplaneAssignments.toMap)
          LinkSource.updateLink(newLink)
        }
      }


    }
  }

  def patchUnassignedAirplanes() = {
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    val unassignedAirplanes = AirplaneSource.loadAllAirplanes().filter(airplane => !linkAssignments.contains(airplane.id)).map { airplane =>
      airplane.copy(availableFlightMinutes = Airplane.MAX_FLIGHT_MINUTES, configuration = LinkClassValues.getInstance(economy = airplane.model.capacity))
    }
    AirplaneSource.updateAirplanes(unassignedAirplanes)
  }
  

}