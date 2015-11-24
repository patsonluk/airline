

import com.patson.model.airplane.Airplane
import play.api.libs.json.Writes
import com.patson.model.airplane.Model
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsValue
import com.patson.model.Computation
package object controllers {
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
      "modelId" -> JsNumber(airplane.model.id),
      "capacity" -> JsNumber(airplane.model.capacity),
      "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
      "speed" -> JsNumber(airplane.model.speed),
      "range" -> JsNumber(airplane.model.range),
      "price" -> JsNumber(airplane.model.price),
      "condition" -> JsNumber(airplane.condition),
      "age" -> JsNumber(Computation.calculateAge(airplane.constructedCycle)),
      "value" -> JsNumber(Computation.calculateAirplaneValue(airplane))))
    }
  }
  
  
}