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
//    patchLinkFrequency()
//    patchAirplaneConfiguration()
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

  def patchLinkFrequency() = {
    // frequency adjust, should be per airplane now
    AirlineSource.loadAllAirlines().foreach { airline =>
      LinkSource.loadLinksByAirlineId(airline.id).foreach { link =>
        if (link.capacity.total > 0) {
          val capacityPerAirplane = link.capacity / link.frequency
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
            airplane.copy(availableFlightMinutes = availableFlightMinutes)
          }

          AirplaneSource.updateAirplanes(airplanes)

          System.out.println(link.id + " has remainingFrequency " + remainingFrequency)

          val newLink =
            if (remainingFrequency > 0) { //update frequency and capacity if we cannot accommodate everything
              val newFrequency = existingFrequency - remainingFrequency
              link.copy(capacity = capacityPerAirplane * newFrequency, frequency = newFrequency)
            } else {
              link
            }

          newLink.setAssignedAirplanes(newAirplaneAssignments.toMap)
          LinkSource.updateLink(newLink)
        }
      }
    }

    println("Finished adjusting frequency")
  }

  def patchAirplaneConfiguration() = {
    println("Compute airplane configuration templates")

    AirlineSource.loadAllAirlines().foreach { airline =>
      val existingConfigurationsByModel = new mutable.HashMap[Model, mutable.HashSet[AirplaneConfiguration]]()
      val links = LinkSource.loadLinksByAirlineId(airline.id)
      links.foreach{ link =>
        if (link.capacity.total > 0) {
          val configurationsForThisModel = existingConfigurationsByModel.getOrElseUpdate(link.getAssignedModel().get, new mutable.HashSet[AirplaneConfiguration]())
          val seats = link.capacity / link.frequency
          val configuration = AirplaneConfiguration(seats.economyVal, seats.businessVal, seats.firstVal, link.airline, link.getAssignedModel().get)
          configurationsForThisModel.add(configuration)
        }
      }
      existingConfigurationsByModel.values.filter(_.size > AirplaneConfiguration.MAX_CONFIGURATION_TEMPLATE_COUNT).foreach { templatesOfThisModel => //print process the model that exceeds 5 configurations
        val trimmedTemplates = new mutable.HashSet[AirplaneConfiguration]()

        val sortedTemplates = templatesOfThisModel.toList.sortBy(computeLuxuryPoints)

        val indexStep = (sortedTemplates.length - 1).toDouble / (AirplaneConfiguration.MAX_CONFIGURATION_TEMPLATE_COUNT - 1)
        for (i <- 0 until AirplaneConfiguration.MAX_CONFIGURATION_TEMPLATE_COUNT) {
          val pickedIndex = Math.round(i * indexStep).toInt
          trimmedTemplates.add(sortedTemplates(pickedIndex))
        }
//        println("from ")
//        sortedTemplates.foreach(printConfiguration)
//        println("=> ")
//        trimmedTemplates.foreach(printConfiguration)
        templatesOfThisModel.clear()
        templatesOfThisModel.addAll(trimmedTemplates)
      }

      //save the configurations
      existingConfigurationsByModel.values.foreach { templatesOfThisModel =>
        val templates = templatesOfThisModel.toList.sortBy(computeLuxuryPoints)
        AirplaneSource.saveAirplaneConfigurations(templates)
      }


      //now assign the configuration id to link's airplanes
      links.foreach { link =>
        link.getAssignedModel().foreach { model => //if a model is defined for this link (ie has airplanes assigned)
          val existingConfiguration = link.capacity / link.frequency
          val availableConfigurations = existingConfigurationsByModel(model)
          //find the match
          val matchingConfiguration = availableConfigurations.find(option => option.economyVal == existingConfiguration.economyVal && option.businessVal == existingConfiguration.businessVal && option.firstVal == existingConfiguration.firstVal) match {
            case Some(configuration) => configuration //a perfect match
            case None => { //try to find one that is the closest
              findClosestConfiguration(availableConfigurations.toList, existingConfiguration)
            }
          }
          val updatingAirplanes = link.getAssignedAirplanes().toList.map(_._1).map { airplane => airplane.copy(configuration = matchingConfiguration) }
          AirplaneSource.updateAirplanes(updatingAirplanes)

          //update the link capacity
          val newCapacity = LinkClassValues(matchingConfiguration.economyVal * link.frequency, matchingConfiguration.businessVal * link.frequency, matchingConfiguration.firstVal * link.frequency)
          if (newCapacity != link.capacity) {
            println(s"Capacity : ${link.capacity} => $newCapacity")
          }
          LinkSource.updateLink(link.copy(capacity = newCapacity))
        }
      }

    }
  }

  def printConfiguration(configuration: AirplaneConfiguration) = {
    println(configuration.economyVal + "/" + configuration.businessVal + "/" + configuration.firstVal)
  }

  def computeLuxuryPoints(configuration: AirplaneConfiguration) = {
    configuration.businessVal + configuration.firstVal * 5
  }

  def computeLuxuryPoints(configuration: LinkClassValues) = {
    configuration.businessVal + configuration.firstVal * 5
  }

  def findClosestConfiguration(availableConfigurations: List[AirplaneConfiguration], existingConfiguration: LinkClassValues) : AirplaneConfiguration = {
    val existingLuxuryPoints = computeLuxuryPoints(existingConfiguration)
    val sortedDiff: Seq[(AirplaneConfiguration, Int)] = availableConfigurations.toList.map(configuration => (configuration, Math.abs(computeLuxuryPoints(configuration) - existingLuxuryPoints))).sortBy(_._2)
    val result = sortedDiff(0)._1 //the first one in the sorted luxury point diff is the closest match
    println("from " + existingConfiguration.economyVal + "/" + existingConfiguration.businessVal + "/" + existingConfiguration.firstVal)
    availableConfigurations.foreach { configuration =>
      if (result == configuration) {
        print("**")
      }
      printConfiguration(configuration)
    }

    result
  }


  def patchUnassignedAirplanes() = {
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    val unassignedAirplanes = AirplaneSource.loadAllAirplanes().filter(airplane => !linkAssignments.contains(airplane.id) && !airplane.isSold).map { airplane =>
      airplane.copy(availableFlightMinutes = Airplane.MAX_FLIGHT_MINUTES)
    }
    AirplaneSource.updateAirplanes(unassignedAirplanes)

    unassignedAirplanes.groupBy(airplane => (airplane.owner, airplane.model)).foreach {
      case ((owner, model), airplanes) =>
        //try to locate configuration for that model
        val configurations = AirplaneSource.loadAirplaneConfigurationsByCriteria(List(("airline", owner.id), ("model", model.id))).sortBy(computeLuxuryPoints)
        val pickedConfiguration =
          if (configurations.isEmpty) {
            //then create one all economy
            val newConfiguration = AirplaneConfiguration(economyVal = model.capacity, 0, 0, owner, model)
            AirplaneSource.saveAirplaneConfigurations(List(newConfiguration))
            newConfiguration
          } else {
            configurations(0)
          }
        val updatingAirplanes = airplanes.map(airplane => airplane.copy(configuration = pickedConfiguration))
        AirplaneSource.updateAirplanes(updatingAirplanes)
    }



  }
  

}