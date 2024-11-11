package controllers

import com.patson.data.{CycleSource, LogSource}
import com.patson.model._
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.math.BigDecimal.int2bigDecimal



class LogApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  case class LogWrites(currentCycle : Int) extends Writes[Log] {
    def writes(log: Log): JsValue = JsObject(List(
      "airlineName" -> JsString(log.airline.name),
      "airlineId" -> JsNumber(log.airline.id),
      "message" -> JsString(log.message),
      "category" -> JsNumber(log.category.id),
      "categoryText" -> JsString(LogCategory.getDescription(log.category)),
      "severity" -> JsNumber(log.severity.id),
      "severityText" -> JsString(LogSeverity.getDescription(log.severity)),
      "cycleDelta" -> JsNumber(log.cycle - currentCycle),
      "properties" -> Json.toJson(log.properties)
      ))
  }
  
  
  val LOG_RANGE = 100 //load 100 weeks worth of logs
  
  
  def getLogs(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val cycle = CycleSource.loadCycle()
    implicit val logWrites = LogWrites(cycle)
    Ok(Json.toJson(LogSource.loadLogsByAirline(request.user.id, cycle - Log.RETENTION_CYCLE).sortBy(_.cycle)(Ordering[Int].reverse)))
  }

  val MAX_NOTE_LENGTH : Int = 100

  def putSelfNote(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val cycle = CycleSource.loadCycle()
    val json = request.body.asInstanceOf[AnyContentAsJson].json
    json.\("note").asOpt[String] match {
      case Some(note) =>
        if (!note.trim.isEmpty) {
          if (LogSource.loadLogsByAirline(airlineId, cycle).filter(_.category == LogCategory.SELF_NOTE).length >= 5) { //max 5 notes per cycle
            Ok(Json.obj("error" -> "Exceeded note limit per cycle"))
          } else {
            LogSource.insertLogs(List(Log(request.user, note.substring(0, Math.min(note.length(), MAX_NOTE_LENGTH)), LogCategory.SELF_NOTE, LogSeverity.INFO, cycle)))
            Ok(Json.obj())
          }
        } else { //just ignore empty notes
          Ok(Json.obj())
        }
      case None =>
        BadRequest(Json.obj("error" -> "No note found"))
    }

  }
  
  

  
}
