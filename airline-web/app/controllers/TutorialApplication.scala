package controllers

import com.patson.data.TutorialSource
import com.patson.model.tutorial.Tutorial
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._

class TutorialApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def saveCompletedTutorial(airlineId : Int, tutorialId: String, category : String) = AuthenticatedAirline(airlineId) { request =>
    TutorialSource.updateCompletedTutorial(airlineId, Tutorial(category, tutorialId))
    Ok(Json.obj())
  }

  def getCompletedTutorials(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(TutorialSource.loadCompletedTutorialsByAirline(airlineId).map(_.id)))
  }
}
