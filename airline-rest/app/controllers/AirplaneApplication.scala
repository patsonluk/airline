package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer


object AirplaneApplication extends Controller {

  implicit object AirplaneModelWrites extends Writes[Model] {
    def writes(airplaneModel: Model): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplaneModel.id),
      "name" -> JsString(airplaneModel.name),
      "capacity" -> JsNumber(airplaneModel.capacity),
      "fuelBurn" -> JsNumber(airplaneModel.fuelBurn),
      "speed" -> JsNumber(airplaneModel.speed),
      "range" -> JsNumber(airplaneModel.range),
      "price" -> JsNumber(airplaneModel.price)))
      
    }
  }
  
  implicit object AirplaneWrites extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplane.id),
      "ownerId" -> JsNumber(airplane.owner.id), 
      "name" -> JsString(airplane.model.name),
      "capacity" -> JsNumber(airplane.model.capacity),
      "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
      "speed" -> JsNumber(airplane.model.speed),
      "range" -> JsNumber(airplane.model.range),
      "price" -> JsNumber(airplane.model.price)))
      
    }
  }
  
  def getAirplaneModels() = Action {
    val models = ModelSource.loadAllModels()
    Ok(Json.toJson(models)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirplanes(airlineId : Int) = Action {
    val airpalnes = AirplaneSource.loadAirplanesByOwner(airlineId)
    Ok(Json.toJson(airpalnes)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def addAirplane(model: Int, quantity : Int, airlineId : Int) = Action {
    val modelGet = ModelSource.loadModelById(model)
    val airlineGet = AirlineSource.loadAirlineById(airlineId)
    if (modelGet.isEmpty || airlineGet.isEmpty) {
      BadRequest("unknown model or airline").withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      )
    } else {
      val airplane = Airplane(modelGet.get, airlineGet.get)
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
