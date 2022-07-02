package controllers

import com.patson.data.NoticeSource
import com.patson.model.notice.{Notice, NoticeCategory}
import com.typesafe.config.ConfigFactory
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject

class BannerApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val configFactory = ConfigFactory.load()
  val bannerEnabled = if (configFactory.hasPath("bannerEnabled")) configFactory.getBoolean("bannerEnabled") else false

  def getBanner() = Action  {
    if (bannerEnabled) {
      Ok(Json.obj("bannerUrl" -> GooglePhotoUtil.drawBannerUrl()))
    } else {
      Ok(Json.obj())
    }
  }
}
