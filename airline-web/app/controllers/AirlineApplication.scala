package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json.Json
import com.patson.model.Airport
import com.patson.model.Airline
import com.patson.data.AirportSource
import com.patson.Util
import com.patson.model.Link
import com.patson.data.LinkSource
import com.patson.data.AirlineSource
import com.patson.data.CycleSource
import com.patson.model.AirlineBase
import com.patson.model.AirlineBase
import controllers.AuthenticationObject.Authenticated
import controllers.AuthenticationObject.AuthenticatedAirline


class AirlineApplication extends Controller {
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance),
      "reputation" -> JsNumber(BigDecimal(airline.airlineInfo.reputation).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)),
      "serviceQuality" -> JsNumber(airline.airlineInfo.serviceQuality),
      "serviceFunding" -> JsNumber(airline.airlineInfo.serviceFunding),
      "maintenanceQuality" -> JsNumber(airline.airlineInfo.maintenanceQuality)))
  }
  
  def getAllAirlines() = Authenticated { implicit request =>
     val airlines = AirlineSource.loadAllAirlines()
    Ok(Json.toJson(airlines)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "http://localhost:9000",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
  
  def getAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     var airlineJson = Json.toJson(airline)(OwnedAirlineWrites).asInstanceOf[JsObject]
     AirlineSource.loadAirlineHeadquarter(airlineId).foreach { headquarter => 
       airlineJson = airlineJson + ("headquarterAirport"-> Json.toJson(headquarter))
     }
     val bases = AirlineSource.loadAirlineBasesByAirline(airlineId)
     airlineJson = airlineJson + ("baseAirports"-> Json.toJson(bases))
     
     Ok(airlineJson)
  }
  def getBases(airlineId : Int) = AuthenticatedAirline(airlineId) {
    Ok(Json.toJson(AirlineSource.loadAirlineBasesByAirline(airlineId)))
  }
  def getBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) {
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) => Ok(Json.toJson(base))
      case None => NotFound
    }
  }
  def deleteBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) {
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) if base.headquarter => //no deleting head quarter for now
        BadRequest("Not allowed to delete headquarter for now")
      case Some(base) =>
        AirlineSource.deleteAirlineBase(base)
        Ok(Json.toJson(base))
      case None => //
        NotFound 
    }
  }
  def putBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val inputBase = request.body.asInstanceOf[AnyContentAsJson].json.as[AirlineBase]
      //TODO validations
      if (inputBase.headquarter) {
         AirlineSource.loadAirlineHeadquarter(airlineId) match {
           case Some(headquarter) =>
           if (headquarter.airport.id != airportId) {
             BadRequest("Not allowed to change headquarter for now")
           } else {
             val updateBase = headquarter.copy(scale = inputBase.scale)
             AirlineSource.saveAirlineBase(updateBase)
             Created(Json.toJson(updateBase))
           }
           case None => //ok to add then
             AirportSource.loadAirportById(inputBase.airport.id, true).fold {
               BadRequest("airport id " +  inputBase.airport.id + " not found!")
             } { airport =>//TODO for now. Maybe update to Ad event later on
               val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
               AirlineSource.saveAirlineBase(newBase)
               if (airport.getAirlineAwareness(airlineId) < 10) { //update to 10 for hq
                 airport.setAirlineAwareness(airlineId, 10)
                 AirportSource.updateAirlineAppeal(List(airport))
               }
               Created(Json.toJson(newBase))
             }
          }
      } else {
        //TODO validations
        AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match { 
        case Some(base) => //updating
          val updateBase = base.copy(scale = inputBase.scale)
          AirlineSource.saveAirlineBase(updateBase)
          Created(Json.toJson(updateBase))
        case None => //ok to add
          AirportSource.loadAirportById(inputBase.airport.id, true).fold {
               BadRequest("airport id " +  inputBase.airport.id + " not found!")
          } { airport =>
            val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
            AirlineSource.saveAirlineBase(newBase)
            Created(Json.toJson(newBase))
          }
        } 
      }
    } else {
      BadRequest("Cannot insert base")
    }
  }
}
