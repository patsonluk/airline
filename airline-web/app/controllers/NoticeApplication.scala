package controllers

import com.patson.data.{AirlineSource, NoticeSource}
import com.patson.model.notice.{Notice, NoticeCategory}
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject

class NoticeApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def saveCompletedNotice(airlineId : Int, noticeId: String, category : String) = AuthenticatedAirline(airlineId) { request =>
    NoticeSource.updateCompletedNotice(airlineId, Notice.fromCategoryAndId(NoticeCategory.withName(category), noticeId))
    Ok(Json.obj())
  }
}
