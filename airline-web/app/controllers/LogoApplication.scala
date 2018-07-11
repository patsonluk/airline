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
import com.patson.data.CountrySource
import com.patson.data.AirportSource
import com.patson.util.LogoGenerator


class LogoApplication extends Controller {
  import scala.collection.JavaConverters._
  val templates : Map[Int, Array[Byte]] = LogoGenerator.getTemplates.asScala.map { case (key, value) => (key.intValue, value) }.toMap
  

  def getTemplates() = Action {
     Ok(Json.toJson(templates.keySet.toList))
  }
  
  def getTemplate(id : Int) = Action {
     Ok(templates(id)).as("image/bmp")
  }
}
