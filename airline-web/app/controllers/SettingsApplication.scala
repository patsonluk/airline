package controllers

import com.patson.data.SettingsSource
import controllers.AuthenticationObject.Authenticated
import play.api.libs.json.{JsString, Json}
import play.api.mvc._

import java.nio.file.Files
import javax.inject.Inject

class SettingsApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getWallpaper(userId : Int) = Authenticated { request =>
    if (!request.user.isPremium || request.user.id != userId) {
      Forbidden("Feature is only available for premium user")
    } else {
      Ok(SettingsSource.loadWallpaper(userId).get).as("image/png")
    }
  }

  def deleteWallpaper(userId : Int) = Authenticated { request =>
    if (!request.user.isPremium || request.user.id != userId) {
      Forbidden("Feature is only available for premium user")
    } else {
      SettingsSource.deleteWallpaper(userId)
      Ok(Json.obj("deleted" -> true))
    }
  }


  def uploadWallpaper(userId : Int) = Authenticated { request =>
    if (!request.user.isPremium || request.user.id != userId) {
      Forbidden("Feature is only available for premium user")
    } else {
      request.body.asMultipartFormData.map { data =>
        val file = data.file("wallpaperFile").get.ref.path
        WallpaperUtil.validateUpload(file) match {
          case Some(rejection) =>
            Ok(Json.obj("error" -> JsString(rejection))) //have to send ok as the jquery plugin's error cannot read the response
          case None =>
            val data =Files.readAllBytes(file)
            SettingsSource.saveWallpaper(request.user.id, data)

            println(s"Uploaded wallpaper for user ${request.user}")
            Ok(Json.obj("success" -> JsString("File uploaded")))
        }
      }.getOrElse {
        Ok(Json.obj("error" -> JsString("Cannot find uploaded contents"))) //have to send ok as the jquery plugin's error cannot read the response
      }
    }
  }
}
