package controllers

import com.patson.data.{CycleSource, DelegateSource}
import com.patson.model._
import com.patson.util.CountryCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.mvc.{request, _}
import play.api.libs.json._


class DelegateApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getDelegateInfo(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(request.user.getDelegateInfo()))
  }

  def getCountryDelegates(countryCode : String, airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val multiplier = AirlineCountryRelationship.getDelegateBonusMultiplier(CountryCache.getCountry(countryCode).get)
    implicit val writes = new CountryDelegateWrites(CycleSource.loadCycle())
    val delegatesRequired = request.user.getBases().filter(_.countryCode == countryCode).map(_.delegatesRequired).sum
    Ok(Json.obj("delegates" -> DelegateSource.loadCountryDelegateByAirlineAndCountry(airlineId, countryCode), "multiplier" -> multiplier, "delegatesRequired" -> delegatesRequired))
  }

  def updateCountryDelegates(countryCode : String, airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val delegateCount = request.body.asInstanceOf[AnyContentAsJson].json.\("delegateCount").as[Int]
    val delegatesRequired = request.user.getBases().filter(_.countryCode == countryCode).map(_.delegatesRequired).sum
    val existingDelegates = DelegateSource.loadCountryDelegateByAirlineAndCountry(airlineId, countryCode).sortBy(_.assignedTask.getStartCycle)
    if (delegateCount < delegatesRequired) {
      BadRequest(s"Require $delegatesRequired but trying to set to $delegateCount")
    } else {
      val delta = delegateCount - existingDelegates.length
      if (delegateCount >= 0 && delta <= airline.getDelegateInfo().availableCount) {
        if (delta < 0) { //unassign the most junior ones first
          existingDelegates.takeRight(delta * -1).foreach { unassigningDelegate =>
            DelegateSource.deleteBusyDelegateByCriteria(List(("id", "=", unassigningDelegate.id)))
          }
        } else if (delta > 0) {
          val delegateTask = DelegateTask.country(CycleSource.loadCycle(), CountryCache.getCountry(countryCode).get)
          val newDelegates = (0 until delta).map(_ => BusyDelegate(airline, delegateTask, None))

          DelegateSource.saveBusyDelegates(newDelegates.toList)
        }
        Ok(Json.obj())
      } else {
        BadRequest(s"Invalid delegate value $delegateCount")
      }
    }
  }

}

class CountryDelegateWrites(currentCycle : Int) extends Writes[BusyDelegate] {
  override def writes(countryDelegate: BusyDelegate): JsValue = {
    var countryDelegateJson = Json.toJson(countryDelegate)(new BusyDelegateWrites(currentCycle)).asInstanceOf[JsObject]
    val task = countryDelegate.assignedTask.asInstanceOf[CountryDelegateTask]
    countryDelegateJson = countryDelegateJson +
      ("countryCode" -> JsString(task.country.countryCode)) +
      ("startCycle" -> JsNumber(task.startCycle)) +
      ("level" -> JsNumber(task.level(currentCycle))) +
      ("levelDescription" -> JsString(task.levelDescription(currentCycle)))

    task.nextLevelCycleCount(currentCycle).foreach {
      value => countryDelegateJson = countryDelegateJson + ("nextLevelCycleCount" -> JsNumber(value))
    }

    countryDelegateJson
  }
}
