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
import com.patson.data.IncomeSource
import com.patson.model.Period
import com.patson.model.AirlineIncome
import com.patson.model.LinksIncome
import com.patson.model.TransactionsIncome
import com.patson.model.OthersIncome
import com.patson.AirlineSimulation


class AirlineApplication extends Controller {
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance),
      "reputation" -> JsNumber(BigDecimal(airline.airlineInfo.reputation).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)),
      "serviceQuality" -> JsNumber(airline.airlineInfo.serviceQuality),
      "serviceFunding" -> JsNumber(airline.airlineInfo.serviceFunding),
      "maintenanceQuality" -> JsNumber(airline.airlineInfo.maintenanceQuality),
      "gradeDescription" -> JsString(airline.airlineGrade.description)))
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
  def getBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) => Ok(Json.toJson(base))
      case None => { //create a base of scale 0 to indicate it's an non-existent base
        AirportSource.loadAirportById(airportId) match {
          case Some(airport) => {
            val emptyBase = AirlineBase(airline = request.user, airport = airport, countryCode = airport.countryCode, scale = 0, foundedCycle = 0, headquarter = false)
            val baseRejection = getBaseRejection(request.user, airport)
            var emptyBaseJson = Json.toJson(emptyBase).asInstanceOf[JsObject]
            baseRejection.foreach { rejection =>
              emptyBaseJson = emptyBaseJson. + ("rejection" -> JsString(rejection))
            }
              
            Ok(emptyBaseJson)
          }
          case None => NotFound
        }
      }
    }
  }
  
  def getBaseRejection(airline : Airline, airport : Airport) : Option[String] = {
    //it should first has link to it
    if (LinkSource.loadLinksByAirlineId(airline.id).find( link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
      return Some("No active flight route operated by your airline flying to this city yet")
    }
    
    val airlineCountryCode = airline.getCountryCode()
    if (airlineCountryCode == airport.countryCode) { //domestic airline
      val existingBaseCount = airline.getBases().filter(_.countryCode == airlineCountryCode).length
      val allowedBaseCount = airline.airlineGrade.value / 2 //up to 5 base max
      if (existingBaseCount >= allowedBaseCount) {
        Some("Only allow up to " + allowedBaseCount + " base(s) in home country with your current airline grade \"" + airline.airlineGrade.description + "\"")
      } else {
        None
      }
    } else { //foreign airline
      if (airline.getHeadQuarter().isEmpty) {
        return Some("Cannot build bases when there is no headquarter!")
      }
      val airlineZone = airline.getHeadQuarter().get.airport.zone
      if (airlineZone == airport.zone) { //no multiple bases in the same zone
        Some("No base allowed in foreign country of your home zone " + airlineZone); 
      } else { 
        val existingBaseCount = airline.getBases().filter(_.airport.zone == airport.zone).length
        val allowedBaseCount = airline.airlineGrade.value / 3; //max 3 bases per other zone
        if (existingBaseCount >= allowedBaseCount) {
          Some("Only allow up to " + allowedBaseCount + " base(s) in this zone " + airport.zone + " with your current airline grade \"" + airline.airlineGrade.description + "\"")
        } else {
          None
        } 
      }
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
        AirportSource.loadAirportById(inputBase.airport.id, true).fold {
          BadRequest("airport id " +  inputBase.airport.id + " not found!")
        } { airport =>
          getBaseRejection(request.user, airport) match {
            case Some(rejection) => BadRequest("Building base failed validation : " + rejection)
            case None =>
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
        }
      }
    } else {
      BadRequest("Cannot insert base")
    }
  }
  
  def getAirlineIncomes(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     val incomes = IncomeSource.loadIncomesByAirline(airlineId)   
     Ok(Json.toJson(incomes))
  }
  
  def getServicePrediction(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     val capacity = LinkSource.loadLinksByAirlineId(airlineId).map(_.capacity.total).sum
     val targetQuality = AirlineSimulation.getTargetQuality(airline.getServiceFunding(), capacity)
     
     val delta = targetQuality - airline.getServiceQuality()
     val prediction =  
       if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) {
         "Increase rapidly"
       } else if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT / 2) {
         "Increase steadily"
       } else if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT / 5) {
         "Increase slightly"
       } else if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT / -5) {
         "Steady"
       } else if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT / -2) {
         "Decrease slightly"
       } else if (delta >= AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT * -1) {
         "Decrease steadily"
       } else {
         "Decrease rapidly"
       }
     
     
     Ok(JsObject(List("prediction" -> JsString(prediction))))
  }
}
