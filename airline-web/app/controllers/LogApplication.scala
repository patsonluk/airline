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
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import com.patson.data.BankSource
import com.patson.model.Loan
import play.api.data.Form
import play.api.data.Forms
import com.patson.data.LogSource



class LogApplication extends Controller {
  implicit object LogWrites extends Writes[Log] {
    def writes(log: Log): JsValue = JsObject(List(
      "airlineId" -> JsString(log.airline.name),
      "airlineName" -> JsNumber(log.airline.id),
      "message" -> JsString(log.message),
      "category" -> JsNumber(log.category.id),
      "categoryText" -> JsString(LogCategory.getDescription(log.category)),
      "severity" -> JsNumber(log.severity.id),
      "severityText" -> JsString(LogSeverity.getDescription(log.severity)),
      "cycle" -> JsNumber(log.cycle)
      ))
  }
  
  
  val LOG_RANGE = 100 //load 100 weeks worth of logs
  
  
  def getLogs(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(LogSource.loadLogsByAirline(request.user.id, CycleSource.loadCycle)))
  }
  
  

  
}
