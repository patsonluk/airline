package controllers

import javax.inject.{Inject, Singleton}
import views._
import play.api.mvc._

@Singleton
class AboutApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  def about() = Action { implicit request =>
    Ok(html.about())
  }
}