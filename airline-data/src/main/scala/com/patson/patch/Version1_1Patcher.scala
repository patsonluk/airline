package com.patson.patch

import java.sql.Connection

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.CountrySimulation
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirplaneSource, LinkSource, Meta}
import com.patson.init.actorSystem
import com.patson.model._
import com.patson.model.airplane._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v1.1
  */
object Version1_1Patcher extends App {
  mainFlow

  def mainFlow() {
    createSchema()
    patchLinkFrequency()
    patchAirplaneConfiguration()
    patchUnassignedAirplanes()
    patchAirplaneHomeAirport()
    
    patchCountryAirlineTitles()

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
    Meta.createCountryAirlineTitle(connection)
    connection.close
  }

  def patchLinkFrequency() = {
    // frequency adjust, should be per airplane now
    AirlineSource.loadAllAirlines().foreach { airline =>
      val updatingLinks = ListBuffer[Link]()
      val updatingAssignedAirplanes = mutable.HashMap[Int, Map[Airplane, LinkAssignment]]()
      LinkSource.loadFlightLinksByAirlineId(airline.id).foreach { link =>
        if (link.capacity.total > 0) {
          val capacityPerAirplane = link.capacity / link.frequency
          var airplanes = link.getAssignedAirplanes().toList.map(_._1)

          //calculate new frequency and assignment
          val existingFrequency = link.frequency
          val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(link.getAssignedModel().get, link.distance)
          var remainingFrequency = existingFrequency

          val newAirplaneAssignments = new mutable.HashMap[Airplane, LinkAssignment]()
          airplanes.foreach { airplane =>
            val flightMinutesRequired = Computation.calculateFlightMinutesRequired(airplane.model, link.distance)
            val frequencyForThisAirplane =
              if (remainingFrequency > maxFrequencyPerAirplane) {
                maxFrequencyPerAirplane
              } else {
                remainingFrequency
              }
            remainingFrequency -= frequencyForThisAirplane
            if (frequencyForThisAirplane > 0) {
              newAirplaneAssignments.put(airplane, LinkAssignment(frequencyForThisAirplane, frequencyForThisAirplane * flightMinutesRequired))
            }
            //            val availableFlightMinutes = Airplane.MAX_FLIGHT_MINUTES - Computation.calculateFlightMinutesRequired(airplane.model, link.distance) * frequencyForThisAirplane
            //            airplane.copy(availableFlightMinutes = availableFlightMinutes)
          }

          //AirplaneSource.updateAirplanes(airplanes)
          if (remainingFrequency > 0) {
            System.out.println(s"${link.id} has remainingFrequency $remainingFrequency out of $existingFrequency . Distance is ${link.distance}")
          }

          val newLink =
            if (remainingFrequency > 0) { //update frequency and capacity if we cannot accommodate everything
              val newFrequency = existingFrequency - remainingFrequency
              link.copy(capacity = capacityPerAirplane * newFrequency, frequency = newFrequency)
            } else {
              link
            }
            updatingLinks.append(newLink)
            updatingAssignedAirplanes.put(link.id, newAirplaneAssignments.toMap)
//          LinkSource.updateLink(newLink)
//          LinkSource.updateAssignedPlanes(link.id, newAirplaneAssignments.toMap)
        }
        LinkSource.updateLinks(updatingLinks.toList)
        LinkSource.updateAssignedPlanes(updatingAssignedAirplanes.toMap)
      }
      println(s"Updated $airline")
    }

    println("Finished adjusting frequency")
  }

  def patchAirplaneConfiguration() = {
    println("Compute airplane configuration templates")

    AirlineSource.loadAllAirlines().foreach { airline =>
      val existingConfigurationsByModel = new mutable.HashMap[Model, mutable.HashSet[AirplaneConfiguration]]()
      val links = LinkSource.loadFlightLinksByAirlineId(airline.id)
      links.foreach{ link =>
        if (link.capacity.total > 0) {
          val configurationsForThisModel = existingConfigurationsByModel.getOrElseUpdate(link.getAssignedModel().get, new mutable.HashSet[AirplaneConfiguration]())
          val seats = link.capacity / link.frequency
          val configuration = AirplaneConfiguration(seats.economyVal, seats.businessVal, seats.firstVal, link.airline, link.getAssignedModel().get, false)
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
        if (templates.length > 0) {
          AirplaneSource.saveAirplaneConfigurations(templates)
          AirplaneSource.updateAirplaneConfiguration(templates(0).copy(isDefault = true)) //set the first one as default
        }
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
            val updateLink = link.copy(capacity = newCapacity)
            LinkSource.updateLink(updateLink)
          }
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
    println("Patching Unassigned airplanes (flight minutes, configuration)")
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    val unassignedAirplanes = AirplaneSource.loadAllAirplanes().filter(airplane => !linkAssignments.contains(airplane.id))

    unassignedAirplanes.filter(!_.isSold).groupBy(airplane => (airplane.owner, airplane.model)).foreach {
      case ((owner, model), airplanes) =>
        //try to locate configuration for that model
        val configurations = AirplaneSource.loadAirplaneConfigurationsByCriteria(List(("airline", owner.id), ("model", model.id))).sortBy(computeLuxuryPoints)
        val pickedConfiguration =
          if (configurations.isEmpty) {
            //then create one all economy
            val newConfiguration = AirplaneConfiguration(economyVal = model.capacity, 0, 0, owner, model, true)
            AirplaneSource.saveAirplaneConfigurations(List(newConfiguration))
            newConfiguration
          } else {
            configurations(0)
          }
        val updatingAirplanes = airplanes.map(airplane => airplane.copy(configuration = pickedConfiguration))
        AirplaneSource.updateAirplanes(updatingAirplanes)
    }
    println("Finished patching unassigned airplanes")
  }

  def patchAirplaneHomeAirport() = {
    println("Patching airplane home airports")
    val linkAssignments: Map[Int, LinkAssignments] = AirplaneSource.loadAirplaneLinkAssignmentsByCriteria(List.empty)
    val airlinesById = AirlineSource.loadAllAirlines(fullLoad = true).map(airline => (airline.id, airline)).toMap
    val updatingAirplanes = ListBuffer[Airplane]()
    AirplaneSource.loadAllAirplanes().foreach { airplane =>
      if (!airplane.isSold) { //only patched owned airplane
        val homeBase : Option[Airport] = linkAssignments.get(airplane.id) match {
          case Some(assignments) => LinkSource.loadFlightLinkById(assignments.assignedLinkIds(0), LinkSource.ID_LOAD).map(link => link.from)
          case None => airlinesById(airplane.owner.id).getHeadQuarter().map { _.airport } //unassigned airplanes, find HQ
        }
        homeBase.foreach { base => //if a home base can be determined
          airplane.home = base
          updatingAirplanes.append(airplane)
        }

        if (updatingAirplanes.length >= 1000) { //update in batches
          println("updating " + updatingAirplanes.length + " airplanes")
          AirplaneSource.updateAirplanes(updatingAirplanes.toList)
          updatingAirplanes.clear()
        }
      }
    }

    val updateCount = AirplaneSource.updateAirplanes(updatingAirplanes.toList)
    println("Finished patching airplane home airports, update count " + updateCount)
  }

  def patchCountryAirlineTitles() = {
    println("Simulating country airline titles")
    CountrySimulation.simulate(0)
    println("Finished country airline simulation")
  }
  

}