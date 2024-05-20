package controllers

import com.patson.data.{AirlineSource, NoticeSource}
import com.patson.model.notice.{Notice, NoticeCategory, TrackingNotice}
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject

class NoticeApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def saveCompletedNotice(airlineId : Int, noticeId: String, category : String) = AuthenticatedAirline(airlineId) { request =>
    val notice = Notice.fromCategoryAndId(NoticeCategory.withName(category), noticeId)
    if (notice.isInstanceOf[TrackingNotice]) {
      NoticeSource.deleteTrackingNotice(airlineId, noticeId.toInt)
    } else {
      NoticeSource.updateCompletedNotice(airlineId, notice)
    }
    Ok(Json.obj())
  }
}
