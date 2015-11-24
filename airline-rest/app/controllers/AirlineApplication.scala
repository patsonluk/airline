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


object AirlineApplication extends Controller {
  implicit object AirlineFormat extends Format[Airline] {
    def reads(json: JsValue): JsResult[Airline] = {
      val airline = Airline.fromId((json \ "id").as[Int])
      JsSuccess(airline)
    }
    
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name)))
  }
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance)))
  }
  implicit object AirlineBaseFormat extends Format[AirlineBase] {
    def reads(json: JsValue): JsResult[AirlineBase] = {
      val airport = Airport.fromId((json \ "airportId").as[Int])
      val airline = Airline.fromId((json \ "airlineId").as[Int])
      val scale = (json \ "scale").as[Int]
      val headquarter = (json \ "headquarter").as[Boolean]
      JsSuccess(AirlineBase(airline, airport, scale, 0, headquarter))
    }
    
    def writes(base: AirlineBase): JsValue = JsObject(List(
      "airportId" -> JsNumber(base.airport.id),
      "airportName" -> JsString(base.airport.name),
      "airlineId" -> JsNumber(base.airline.id),
      "scale" -> JsNumber(base.scale),
      "headquarter" -> JsBoolean(base.headquarter),
      "foundedCycle" -> JsNumber(base.foundedCycle)))
  }

  
  def getAllAirlines() = Action {
     val airlines = AirlineSource.loadAllAirlines()
    Ok(Json.toJson(airlines)).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
    )
  }
  
  def getAirline(airlineId : Int) = Action {
     AirlineSource.loadAirlineById(airlineId, true) match {
       case Some(airline) =>
         var airlineJson = Json.toJson(airline)(OwnedAirlineWrites).asInstanceOf[JsObject]
         AirlineSource.loadAirlineHeadquarter(airlineId).foreach { headquarter => 
           airlineJson = airlineJson + ("headquarterAirport"-> Json.toJson(headquarter))
         }
         val bases = AirlineSource.loadAirlineBasesByAirline(airlineId)
         airlineJson = airlineJson + ("baseAirports"-> Json.toJson(bases))
         
         Ok(airlineJson).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*") //TODO make sure you really own the airline!
       case None => NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
     }
  }
  def getBases(airlineId : Int) = Action {
    Ok(Json.toJson(AirlineSource.loadAirlineBasesByAirline(airlineId))).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }
  def getBase(airlineId : Int, airportId : Int) = Action  {
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) => Ok(Json.toJson(base)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      case None => NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
  def deleteBase(airlineId : Int, airportId : Int) = Action {
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) if base.headquarter => //no deleting head quarter for now
        BadRequest("Not allowed to delete headquarter for now").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      case Some(base) =>
        AirlineSource.deleteAirlineBase(base)
        Ok(Json.toJson(base)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      case None => //
        NotFound.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*") 
    }
  }
  def putBase(airlineId : Int, airportId : Int) = Action { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val inputBase = request.body.asInstanceOf[AnyContentAsJson].json.as[AirlineBase]
      //TODO validations
      if (inputBase.headquarter) {
         AirlineSource.loadAirlineHeadquarter(airlineId) match {
           case Some(headquarter) =>
           if (headquarter.airport.id != airportId) {
             BadRequest("Not allowed to change headquarter for now").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
           } else {
             val updateBase = headquarter.copy(scale = inputBase.scale)
             AirlineSource.saveAirlineBase(updateBase)
             Created(Json.toJson(updateBase)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
           }
           case None => //ok to add then
             val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle())
             AirlineSource.saveAirlineBase(newBase)
             Created(Json.toJson(newBase)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         }
      } else {
        //TODO validations
        AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match { 
        case Some(base) => //updating
          val updateBase = base.copy(scale = inputBase.scale)
          AirlineSource.saveAirlineBase(updateBase)
          Created(Json.toJson(updateBase)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        case None => //
          val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle())
          AirlineSource.saveAirlineBase(newBase)
          Created(Json.toJson(newBase)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        } 
      }
    } else {
      BadRequest("Cannot insert base").withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }
}
