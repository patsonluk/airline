package controllers

import com.patson.data.{CycleSource, LogSource}
import com.patson.model._
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.math.BigDecimal.int2bigDecimal



class LogApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object LogWrites extends Writes[Log] {
    def writes(log: Log): JsValue = JsObject(List(
      "airlineName" -> JsString(log.airline.name),
      "airlineId" -> JsNumber(log.airline.id),
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
    Ok(Json.toJson(LogSource.loadLogsByAirline(request.user.id, CycleSource.loadCycle - LOG_RANGE)))
  }
  
  

  
}
