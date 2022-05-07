package controllers

import com.patson.data.NoticeSource
import com.patson.model.notice.{Notice, NoticeCategory}
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject

class BannerApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getBanner() = Action  {
    Ok(Json.obj("bannerUrl" -> GooglePhotoUtil.drawBannerUrl()))
  }
}
