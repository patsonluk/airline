package controllers

import com.patson.data.{AirplaneSource, LinkSource}
import com.patson.model.Link

import scala.collection.mutable.ListBuffer

object LinkUtil {
  def adjustLinksAfterConfigurationChanges(configurationId : Int) = {

    val affectedLinkIds = AirplaneSource.loadAirplanesCriteria(List(("configuration", configurationId))).flatMap { airplane =>
      val linkIdsFlownByThisAirplane = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id).assignments.keys
      linkIdsFlownByThisAirplane.toList
    }.toSet

    val affectedLinks = ListBuffer[Link]()
    affectedLinkIds.foreach { linkId =>
      LinkSource.loadLinkById(linkId).foreach { link =>
        affectedLinks.append(link)
      }
    }

    LinkSource.updateLinks(affectedLinks.toList)
  }

  def adjustLinksAfterAirplaneConfigurationChange(airplaneId : Int) = {
    val affectedLinkIds = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplaneId).assignedLinkIds

    val affectedLinks = ListBuffer[Link]()
    affectedLinkIds.foreach { linkId =>
      LinkSource.loadLinkById(linkId).foreach { link =>
        affectedLinks.append(link)
      }
    }

    LinkSource.updateLinks(affectedLinks.toList)
  }
}
