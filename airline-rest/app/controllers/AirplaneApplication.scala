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
import controllers.LinkApplication.LinkFormat
import com.patson.data.CycleSource


object AirplaneApplication extends Controller {
  implicit object AirplaneWithAssignedLinkWrites extends Writes[(Airplane, Option[Link])] {
    def writes(airplaneWithAssignedLink : (Airplane, Option[Link])): JsValue = {
      val airplane = airplaneWithAssignedLink._1
      val jsObject = Json.toJson(airplane).asInstanceOf[JsObject]
      airplaneWithAssignedLink._2.fold( jsObject ) { link =>
        jsObject + ("link", Json.toJson(link)(LinkFormat))
      }
    }
  }
  
  
  def getAirplaneModels() = Action {
    val models = ModelSource.loadAllModels()
    Ok(Json.toJson(models)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirplanes(airlineId : Int, getAssignedLink : Boolean) = Action {
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
  
  def getAirplane(airlineId : Int, airplaneId : Int) = Action {
    AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId) match {
      case Some(airplaneWithLink) =>
        if (airplaneWithLink._1.owner.id == airlineId) {
          Ok(Json.toJson(airplaneWithLink)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")     
        } else {
          Forbidden.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
      case None =>
        BadRequest("airplane not found").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  
  def sellAirplane(airlineId : Int, airplaneId : Int) = Action {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          val sellValue = Computation.calculateAirplaneValue(airplane)
          if (AirplaneSource.deleteAirplane(airplaneId) == 1) {
            AirlineSource.adjustAirlineBalance(airlineId, sellValue)
            Ok(Json.toJson(airplane)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          } else {
            NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          }
        } else {
          Forbidden.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        }
      case None =>
        BadRequest("airplane not found").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  
  def addAirplane(model: Int, quantity : Int, airlineId : Int) = Action {
    val modelGet = ModelSource.loadModelById(model)
    val airlineGet = AirlineSource.loadAirlineById(airlineId, true)
    if (modelGet.isEmpty || airlineGet.isEmpty) {
      BadRequest("unknown model or airline").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    } else {
      val airplane = Airplane(modelGet.get, airlineGet.get, CycleSource.loadCycle(), Airplane.MAX_CONDITION)
      val airline = airlineGet.get
      if (airline.airlineInfo.balance < (airplane.model.price * quantity)) { //not enough money!
        UnprocessableEntity("Not enough money").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")   
      } else {
        
        AirlineSource.adjustAirlineBalance(airlineId,  -1 * airplane.model.price * quantity)
        val airplanes = ListBuffer[Airplane]()
        for (i <- 0 until quantity) {
          airplanes.append(airplane.copy())
        }
        
        val updateCount = AirplaneSource.saveAirplanes(airplanes.toList)
        if (updateCount > 0) {
            Accepted(Json.obj("updateCount" -> updateCount)).withHeaders(
              ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
              )
        } else {
            UnprocessableEntity("Cannot save airplane").withHeaders(
              ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
              )
        }
      }
    }
  }

}
