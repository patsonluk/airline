package controllers

import com.patson.data.{AllianceSource, ColorSource}
import com.patson.model._
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import javax.inject.Inject
import scala.collection.mutable


class ColorApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val REVERT_COLOR = "REVERT"

  def getAirlineLabelColors(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val allianceLabelColors = internalGetAllianceLabelColors(request.user)

    val allAirlinesByAllianceId : Map[Int, List[Airline]] = AllianceSource.loadAllAlliances().map(entry => (entry.id, entry.members.map(_.airline))).toMap
    val airlineLabelColors : mutable.Map[Int, String] = mutable.Map()
    allianceLabelColors.foreach {
      case (allianceId, color) =>
        allAirlinesByAllianceId(allianceId).foreach {
          airline => airlineLabelColors.put(airline.id, color)
        }
    }

    Ok(Json.toJson(airlineLabelColors.filter {
      case (airlineId, color) => color != REVERT_COLOR
    }))
  }

  def getAllianceLabelColor(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { request =>
    val allianceLabelColors = internalGetAllianceLabelColors(request.user)
    allianceLabelColors.get(allianceId) match {
      case Some(color) => Ok(Json.obj("color" -> color))
      case None => Ok(Json.obj())
    }
  }

  def internalGetAllianceLabelColors(currentAirline : Airline) = {
    val allianceTextColors = mutable.HashMap[Int, String]()
    currentAirline.getAllianceId().foreach { currentAllianceId =>
      if (AllianceSource.loadAllianceMemberByAirline(currentAirline).get.role != AllianceRole.APPLICANT) { //an approved member
        allianceTextColors.addAll(ColorSource.loadAllianceLabelColorsFromAlliance(currentAllianceId))
      }
    }

    allianceTextColors.addAll(ColorSource.loadAllianceLabelColorsFromAirline(currentAirline.id))

    allianceTextColors
  }


  def setAllianceLabelColorAsAirline(airlineId: Int, targetAllianceId : Int, color : String) = AuthenticatedAirline(airlineId) { request =>
    ColorSource.saveAllianceLabelColorFromAirline(airlineId, targetAllianceId, color)
    Ok(Json.obj())
  }

  def setAllianceLabelColorAsAlliance(airlineId: Int, targetAllianceId : Int, color : String) = AuthenticatedAirline(airlineId) { request =>
    val currentAirline = request.user

    currentAirline.getAllianceId() match {
      case Some(currentAllianceId) =>
        val allianceRole = AllianceSource.loadAllianceMemberByAirline(currentAirline).get.role
        if (!AllianceRole.isAdmin(allianceRole)) {
          Forbidden(s"Cannot set alliance color : $currentAirline is not an admin of alliance")
        } else {
          ColorSource.saveAllianceLabelColorFromAlliance(currentAllianceId, targetAllianceId, color)
          Ok(Json.obj())
        }
      case None =>
        Forbidden(s"Cannot set alliance color: $currentAirline is not a part of any alliance")
    }
  }

  def deleteAllianceLabelColorAsAirline(airlineId: Int, targetAllianceId : Int) = AuthenticatedAirline(airlineId) { request => deleteColorAsAirlineBlock(request, airlineId, targetAllianceId) }

  def deleteColorAsAirlineBlock(request : AuthenticatedRequest[AnyContent, Airline], airlineId : Int, targetAllianceId : Int) : Result = {

    val currentAirline = request.user

    currentAirline.getAllianceId() match {
      case Some(currentAllianceId) =>
        if (AllianceSource.loadAllianceMemberByAirline(currentAirline).get.role != AllianceRole.APPLICANT) { //an approved member
          //check if there's existing color defined by alliance, if so, create an overwrite with color "REVERT"
          if (ColorSource.loadAllianceLabelColorsFromAlliance(currentAllianceId).get(targetAllianceId).isDefined) {
            ColorSource.saveAllianceLabelColorFromAirline(airlineId, targetAllianceId, REVERT_COLOR)
            return Ok(Json.obj())
          }
        }
      case None => //nothing
    }

    ColorSource.deleteAllianceLabelColorFromAirline(airlineId, targetAllianceId)
    return Ok(Json.obj())
  }

  def deleteAllianceLabelColorAsAlliance(airlineId: Int, targetAllianceId : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentAirline = request.user

    currentAirline.getAllianceId() match {
      case Some(currentAllianceId) =>
        val allianceRole = AllianceSource.loadAllianceMemberByAirline(currentAirline).get.role
        if (!AllianceRole.isAdmin(allianceRole)) {
          Forbidden(s"Cannot set alliance color : $currentAirline is not an admin of alliance")
        } else {
          ColorSource.deleteAllianceLabelColorFromAlliance(currentAllianceId, targetAllianceId)
          ColorSource.deleteAllianceLabelColorFromAirline(currentAirline.id, targetAllianceId) //otherwise admin can have revert stuck at airline level and hard to get out
          Ok(Json.obj())
        }
      case None =>
        Forbidden(s"Cannot set alliance color: $currentAirline is not a part of any alliance")
    }
  }
  


}
