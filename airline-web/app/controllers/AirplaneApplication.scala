package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import com.patson.data.CycleSource
import controllers.AuthenticationObject.AuthenticatedAirline


class AirplaneApplication extends Controller {
  implicit object AirplaneWithAssignedLinkWrites extends Writes[(Airplane, Option[Link])] {
    def writes(airplaneWithAssignedLink : (Airplane, Option[Link])): JsValue = {
      val airplane = airplaneWithAssignedLink._1
      val jsObject = Json.toJson(airplane).asInstanceOf[JsObject]
      airplaneWithAssignedLink._2.fold( jsObject ) { link =>
        jsObject + ("link", Json.toJson(link))
      }
    }
  }
  
  
  def getAirplaneModels() = Action {
    val models = ModelSource.loadAllModels()
    Ok(Json.toJson(models)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirplanes(airlineId : Int, getAssignedLink : Boolean) =  AuthenticatedAirline(airlineId) {
    if (!getAssignedLink) {
      val airplanes = AirplaneSource.loadAirplanesByOwner(airlineId)
      Ok(Json.toJson(airplanes)).withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      )
    } else {
      val airplanesWithLink : List[(Airplane, Option[Link])]= AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
      Ok(Json.toJson(airplanesWithLink)).withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      )
    }
  }
  
  def getAirplane(airlineId : Int, airplaneId : Int) =  AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId) match {
      case Some(airplaneWithLink) =>
        if (airplaneWithLink._1.owner.id == airlineId) {
          Ok(Json.toJson(airplaneWithLink))     
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def sellAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          val sellValue = Computation.calculateAirplaneSellValue(airplane)
          if (AirplaneSource.deleteAirplane(airplaneId) == 1) {
            AirlineSource.adjustAirlineBalance(airlineId, sellValue)
            Ok(Json.toJson(airplane))
          } else {
            NotFound
          }
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def addAirplane(airlineId : Int, model : Int, quantity : Int) = AuthenticatedAirline(airlineId) { request =>
    val modelGet = ModelSource.loadModelById(model)
    if (modelGet.isEmpty) {
      BadRequest("unknown model or airline")
    } else {
      val airline = request.user
      val airplane = Airplane(modelGet.get, airline, CycleSource.loadCycle(), Airplane.MAX_CONDITION, depreciationRate = 0, value = modelGet.get.price)
      if (airline.airlineInfo.balance < (airplane.model.price * quantity)) { //not enough money!
        BadRequest("Not enough money")   
      } else {
        
        AirlineSource.adjustAirlineBalance(airlineId,  -1 * airplane.model.price * quantity)
        val airplanes = ListBuffer[Airplane]()
        for (i <- 0 until quantity) {
          airplanes.append(airplane.copy())
        }
        
        val updateCount = AirplaneSource.saveAirplanes(airplanes.toList)
        if (updateCount > 0) {
            Accepted(Json.obj("updateCount" -> updateCount))
        } else {
            UnprocessableEntity("Cannot save airplane")
        }
      }
    }
  }

}
