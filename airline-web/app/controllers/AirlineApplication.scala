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
import play.api.mvc.Security.AuthenticatedRequest


class AirlineApplication extends Controller {
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue =  {
      var values = List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance),
      "reputation" -> JsNumber(BigDecimal(airline.airlineInfo.reputation).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)),
      "serviceQuality" -> JsNumber(airline.airlineInfo.serviceQuality),
      "serviceFunding" -> JsNumber(airline.airlineInfo.serviceFunding),
      "maintenanceQuality" -> JsNumber(airline.airlineInfo.maintenanceQuality),
      "gradeDescription" -> JsString(airline.airlineGrade.description),
      "gradeValue" -> JsNumber(airline.airlineGrade.value))
      
      airline.getCountryCode.foreach { countryCode =>
        values = values :+ ("countryCode" -> JsString(countryCode))
      }
      
      JsObject(values)
    }
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
      case Some(base) => {
        val baseRejection = getBaseRejection(request.user, base, base.scale + 1)
        if (baseRejection.isDefined) {
          Ok(Json.toJson(base).asInstanceOf[JsObject] + ("rejection" -> JsString(baseRejection.get)))
        } else {
          Ok(Json.toJson(base))          
        }
      }
      case None => { //create a base of scale 0 to indicate it's an non-existent base
        AirportSource.loadAirportById(airportId) match {
          case Some(airport) => {
            val emptyBase = AirlineBase(airline = request.user, airport = airport, countryCode = airport.countryCode, scale = 0, foundedCycle = 0, headquarter = false)
            val baseRejection = getBaseRejection(request.user, emptyBase, 1)
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
  
  def getBaseRejection(airline : Airline, base : AirlineBase, toScale : Int) : Option[String] = {
    val airport : Airport = base.airport
    val airlineCountryCodeOption = airline.getCountryCode()
    
    val cost = base.getUpgradeCost(toScale)
    if (cost > airline.getBalance) {
      return Some("Not enough cash to build/upgrade the base")
    }
    
    if (toScale == 1) { //building something new
      if (airlineCountryCodeOption.isDefined) { //building non-HQ
            //it should first has link to it
        if (LinkSource.loadLinksByAirlineId(airline.id).find( link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
          return Some("No active flight route operated by your airline flying to this city yet")
        }
        
        val existingBaseCount = airline.getBases().length
        val allowedBaseCount = airline.airlineGrade.getBaseLimit
        if (existingBaseCount >= allowedBaseCount) {
          return Some("Only allow up to " + allowedBaseCount + " bases for your current airline grade " + airline.airlineGrade.description)
        } 
      }  
    }
    

    
    return None
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
      //todo validate the user is the same
      val airline = request.user
      
      if (inputBase.airline.id != airline.id) {
        BadRequest("not the same user!")
      } else {
        val baseRejection = getBaseRejection(airline, inputBase, inputBase.scale)
        val cost = inputBase.getUpgradeCost(inputBase.scale)
        
        if (baseRejection.isDefined) {
          BadRequest("base request rejected: " + baseRejection.get)
        } else {
          if (inputBase.headquarter) {
             AirlineSource.loadAirlineHeadquarter(airlineId) match {
               case Some(headquarter) =>
               if (headquarter.airport.id != airportId) {
                 BadRequest("Not allowed to change headquarter for now")
               } else {
                 val updateBase = headquarter.copy(scale = inputBase.scale)
                 AirlineSource.saveAirlineBase(updateBase)
                 airline.setCountryCode(updateBase.countryCode)
                 AirlineSource.saveAirlineInfo(airline)
                 AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
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
                   AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                   Created(Json.toJson(newBase))
                 }
              }
          } else {
            AirportSource.loadAirportById(inputBase.airport.id, true).fold {
              BadRequest("airport id " +  inputBase.airport.id + " not found!")
            } { airport =>
                  AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match { 
                  case Some(base) => //updating
                    val updateBase = base.copy(scale = inputBase.scale)
                    AirlineSource.saveAirlineBase(updateBase)
                    AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                    Created(Json.toJson(updateBase))
                  case None => //ok to add
                    AirportSource.loadAirportById(inputBase.airport.id, true).fold {
                         BadRequest("airport id " +  inputBase.airport.id + " not found!")
                    } { airport =>
                      val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
                      AirlineSource.saveAirlineBase(newBase)
                      AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                      Created(Json.toJson(newBase))
                    }
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
       if (delta >= 20) {
         "Increase rapidly"
       } else if (delta >= 10) {
         "Increase steadily"
       } else if (delta >= 5) {
         "Increase slightly"
       } else if (delta >= -5) {
         "Steady"
       } else if (delta >= -10) {
         "Decrease slightly"
       } else if (delta >= -20) {
         "Decrease steadily"
       } else {
         "Decrease rapidly"
       }
     
     
     Ok(JsObject(List("prediction" -> JsString(prediction))))
  }
}
