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
import com.patson.data.CountrySource
import com.patson.model.Country
import com.patson.model.CountryMarketShare
import com.patson.model.Computation
import com.patson.data.BankSource
import models.EntrepreneurProfile
import com.patson.data.AirplaneSource

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
     val airlines = AirlineSource.loadAllAirlines(fullLoad = true)
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
  def getBases(airlineId : Int) = Authenticated { implicit request =>
    Ok(Json.toJson(AirlineSource.loadAirlineBasesByAirline(airlineId)))
  }
  def getBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj().asInstanceOf[JsObject]
    AirportSource.loadAirportById(airportId) match {
      case Some(airport) => {
        val existingBase = AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId)
        if (existingBase.isDefined) {
          result = result + ("base" -> Json.toJson(existingBase)) 
        }
       
        val targetBase = existingBase match {
          case Some(base) => base.copy(scale = base.scale + 1)
          case None => AirlineBase(airline = request.user, airport = airport, countryCode = airport.countryCode, scale = 1, foundedCycle = CycleSource.loadCycle(), headquarter = request.user.getHeadQuarter.isEmpty)
        }
        
        result = result + ("targetBase" -> Json.toJson(targetBase))
        val baseRejection = getBaseRejection(request.user, targetBase)
        baseRejection.foreach { rejection => 
          result = result + ("rejection" -> JsString(rejection))
        }
        
        Ok(result)
      }
      case None => NotFound
    }
  }
  
  def getBaseRejection(airline : Airline, targetBase : AirlineBase) : Option[String] = {
    val airlineCountryCodeOption = airline.getCountryCode()
    val airport = targetBase.airport
    val cost = targetBase.getValue
    if (cost > airline.getBalance) {
      return Some("Not enough cash to build/upgrade the base")
    }
    
    if (targetBase.scale == 1) { //building something new
      if (airlineCountryCodeOption.isDefined) { //building non-HQ
            //it should first has link to it
        if (LinkSource.loadLinksByAirlineId(airline.id).find( link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
          return Some("No active flight route operated by your airline flying to this city yet")
        }
        
        if (CountrySource.loadCountryByCode(airport.countryCode).get.openness < Country.OPEN_DOMESTIC_MARKET_MIN_OPENNESS) {
          return Some("This country does not allow foreign airline base")
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
  def deleteBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) if base.headquarter => //no deleting head quarter for now
        BadRequest("Not allowed to delete headquarter for now")
      case Some(base) =>
        //remove all links from that base
        val linksFromThisAirport = LinkSource.loadLinksByAirlineId(airlineId).filter(_.from.id == airportId)
        linksFromThisAirport.foreach { link =>
          LinkSource.deleteLink(link.id)
        }
        
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
        val baseRejection = getBaseRejection(airline, inputBase)
        val cost = inputBase.getValue
        
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
                 AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                 Created(Json.toJson(updateBase))
               }
               case None => //ok to add then
                 AirportSource.loadAirportById(inputBase.airport.id, true).fold {
                   BadRequest("airport id " +  inputBase.airport.id + " not found!")
                 } { airport =>//TODO for now. Maybe update to Ad event later on
                   val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
                   AirlineSource.saveAirlineBase(newBase)
                   if (airport.getAirlineAwareness(airlineId) < 20) { //update to 10 for hq
                     airport.setAirlineAwareness(airlineId, 20)
                     AirportSource.updateAirlineAppeal(List(airport))
                   }
                   
                   airline.setCountryCode(newBase.countryCode)
                   AirlineSource.saveAirlineInfo(airline)
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
  
  def getChampionedCountries(airlineId : Int) = Authenticated { implicit request =>
    val topChampionsByCountryCode : List[(String, List[((Int, Long), Int)])]= CountrySource.loadMarketSharesByCriteria(List()).map {
      case CountryMarketShare(countryCode, airlineShares) => (countryCode, airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(3).zipWithIndex)
    }
    
    val championedCountryByThisAirline: List[(Country, Int, Long, Double)] = topChampionsByCountryCode.map { //(country, ranking, passengerCount, reputation boost)
      case (countryCode, championAirlines) => (countryCode, championAirlines.find {
        case((championAirlineId, passengerCount), ranking) => championAirlineId == airlineId
      })
    }.filter {
      case (countryCode, thisAirlineRankingOption) => thisAirlineRankingOption.isDefined
    }.map {
      case (countryCode, thisAirlineRankingOption) => {
        val country = CountrySource.loadCountryByCode(countryCode).get
        val ranking = thisAirlineRankingOption.get._2 + 1
        val passengerCount = thisAirlineRankingOption.get._1._2 
        (country, ranking, passengerCount, Computation.computeReputationBoost(country, ranking))
      }
    }.sortBy {
      case (countryCode, ranking, passengerCount, reputationBoost) => ranking
    }
    
    Ok(Json.toJson(championedCountryByThisAirline)(ChampionedCountriesWrites))
  }
  
  def resetAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (airlineId != request.user.id) {
      Forbidden
    } else {
      //remove all links
      LinkSource.loadLinksByAirlineId(airlineId).foreach { link =>
        LinkSource.deleteLink(link.id)
      }
      //remove all airplanes
      AirplaneSource.deleteAirplanesByCriteria(List(("owner", airlineId)));
      //remove all bases
      AirlineSource.deleteAirlineBaseByCriteria(List(("airline", airlineId)))
      //remove all loans
      BankSource.loadLoansByAirline(airlineId).foreach { loan =>
        BankSource.deleteLoan(loan.id)
      }
      //reset balance
      val airline : Airline = request.user
      airline.setBalance(EntrepreneurProfile.INITIAL_BALANCE)
      
      AirlineSource.saveAirlineInfo(airline)
      Ok(Json.toJson(airline))
    }
  }
  
  object ChampionedCountriesWrites extends Writes[List[(Country, Int, Long, Double)]] {
    def writes(championedCountries : List[(Country, Int, Long, Double)]): JsValue = {
      var arr = Json.arr()
      championedCountries.foreach {
        case (country, ranking, passengerCount, reputationBoost) =>
          arr = arr :+ Json.obj(
              "country" -> Json.toJson(country),
              "ranking" -> JsNumber(ranking),
              "passengerCount" -> JsNumber(passengerCount),
              "reputationBoost" -> JsNumber(reputationBoost))
      }
      arr
    }
  }
  
}
