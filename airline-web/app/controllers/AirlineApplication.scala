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
import com.patson.model.Bank
import java.awt.Color
import com.patson.util.LogoGenerator
import java.nio.file.Files
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.patson.model.FlightCategory
import com.patson.data.AllianceSource


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
      "gradeValue" -> JsNumber(airline.airlineGrade.value),
      "airlineCode" -> JsString(airline.getAirlineCode()))
      
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
  
  def getAirline(airlineId : Int, extendedInfo : Boolean) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     var airlineJson = Json.toJson(airline)(OwnedAirlineWrites).asInstanceOf[JsObject]
     AirlineSource.loadAirlineHeadquarter(airlineId).foreach { headquarter => 
       airlineJson = airlineJson + ("headquarterAirport"-> Json.toJson(headquarter))
     }
     val bases = AirlineSource.loadAirlineBasesByAirline(airlineId)
     airlineJson = airlineJson + ("baseAirports"-> Json.toJson(bases))
     
     if (extendedInfo) {
       val links = LinkSource.loadLinksByAirlineId(airlineId)
       val airportsServed = links.flatMap {
         link => List(link.from, link.to)
       }.toSet.size
       
       val linkFlightCategories = links.map {
         link => Computation.getFlightCategory(link.from, link.to)
       }
       
       val airlineGrade = airline.airlineGrade
       airlineJson = airlineJson + 
       ("domesticLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.DOMESTIC))) +
       ("regionalLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.REGIONAL))) +
       ("intercontinentalLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.INTERCONTINENTAL))) +
       ("domesticLinkMax" -> JsNumber(airlineGrade.getLinkLimit(FlightCategory.DOMESTIC))) +
       ("regionalLinkMax" -> JsNumber(airlineGrade.getLinkLimit(FlightCategory.REGIONAL))) +
       ("intercontinentalLinkMax" -> JsNumber(airlineGrade.getLinkLimit(FlightCategory.INTERCONTINENTAL)))  
              
       
       
       val destinations = if (airportsServed > 0) airportsServed - 1 else 0 //minus home base
       
       val currentCycle = CycleSource.loadCycle()
       val airplanes = AirplaneSource.loadAirplanesByOwner(airlineId).filter(_.isReady(currentCycle))
       
       val fleetSize = airplanes.length
       val fleetAge = if (fleetSize > 0) airplanes.map(currentCycle - _.constructedCycle).sum / fleetSize else 0
       val assets = Bank.getAssets(airlineId)
       
       airlineJson = airlineJson + ("destinations"-> JsNumber(destinations)) + ("fleetSize"-> JsNumber(fleetSize)) + ("fleetAge"-> JsNumber(fleetAge)) + ("assets"-> JsNumber(assets))
     }
     
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
    val airport = targetBase.airport
    val cost = targetBase.getValue
    if (cost > airline.getBalance) {
      return Some("Not enough cash to build/upgrade the base")
    }
    
    if (targetBase.scale == 1) { //building something new
      if (airline.getHeadQuarter().isDefined) { //building non-HQ
            //it should first has link to it
        if (LinkSource.loadLinksByAirlineId(airline.id).find( link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
          return Some("No active flight route operated by your airline flying to this city yet")
        }
        
        if (airport.countryCode != airline.getCountryCode().get && CountrySource.loadCountryByCode(airport.countryCode).get.openness < Country.OPEN_DOMESTIC_MARKET_MIN_OPENNESS) {
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
      
      AllianceSource.loadAllianceMemberByAirline(request.user).foreach { allianceMember =>
        AllianceSource.deleteAllianceMember(airlineId)
      }  
      
      //reset balance
      val airline : Airline = request.user
      airline.setBalance(EntrepreneurProfile.INITIAL_BALANCE)
      
      //unset country code
      airline.removeCountryCode()
      //unset service investment
      airline.setServiceFunding(0)
      airline.setServiceQuality(0)
      
      AirlineSource.saveAirlineInfo(airline)
      Ok(Json.toJson(airline))
    }
  }
    
 def setAirlineCode(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      var airlineCode = request.body.asInstanceOf[AnyContentAsJson].json.\("airlineCode").as[String]
      
      if (airlineCode.length != 2) {
        BadRequest("Should be 2 characters") 
      } else if (airlineCode.filter(Character.isLetter(_)).length != 2) {
        BadRequest("Should be all letters")
      } 
      
      airlineCode = airlineCode.toUpperCase()
      
      val airline = request.user
      airline.setAirlineCode(airlineCode)
      AirlineSource.saveAirlineCode(airlineId, airlineCode)
      Ok(Json.toJson(airline))
    } else {
      BadRequest("Cannot Set airline Code")
    }
  }
 def getLogo(airlineId : Int) = Action {
   Ok(LogoUtil.getLogo(airlineId)).as("image/png").withHeaders(
    CACHE_CONTROL -> "max-age=3600"
   )
   //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
 }
 
 def setLogo(airlineId : Int, templateIndex : Int, color1 : String, color2 : String) = AuthenticatedAirline(airlineId) { request =>
   val logo = LogoGenerator.generateLogo(templateIndex, Color.decode(color1).getRGB, Color.decode(color2).getRGB)
   LogoUtil.saveLogo(airlineId, logo)
   println("Updated logo for airline " + request.user)
   Ok(Json.obj())
 }	 
 
  def uploadLogo(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.user.getReputation < 50) {
      Ok(Json.obj("error" -> JsString("Cannot upload img at current reputation"))) //have to send ok as the jquery plugin's error cannot read the response
    } else {
      request.body.asMultipartFormData.map { data =>
        import java.io.File
        val logoFile = data.file("logoFile").get.ref.file
        LogoUtil.validateUpload(logoFile) match {
          case Some(rejection) =>
            Ok(Json.obj("error" -> JsString(rejection))) //have to send ok as the jquery plugin's error cannot read the response
          case None =>
            val data =Files.readAllBytes(logoFile.toPath)
            LogoUtil.saveLogo(airlineId, data)
            
            println("Uploaded logo for airline " + request.user)
            Ok(Json.obj("success" -> JsString("File uploaded")))
        }
      }.getOrElse {
            Ok(Json.obj("error" -> JsString("Cannot find uploaded contents"))) //have to send ok as the jquery plugin's error cannot read the response
      }
    }
  }
 
 
  def setColor(airlineId : Int, color : String) = AuthenticatedAirline(airlineId) { request =>
   val decodedColor = Color.decode(color) //just for validation
   AirlineSource.saveColor(airlineId, color)
   println("Updated color for airline " + request.user)
   Ok(Json.obj())
  }
  
  def getColors() = Action {
   val colors : Map[Int, String] =  AirlineSource.getColors()
   var result = Json.obj()
      colors.foreach {
        case (airlineId, color) =>
          result = result + (airlineId.toString -> JsString(color))
      }
   
   Ok(result)
   //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
 } 
  
  def updateAirplaneRenewal(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val thresholdTry : Try[Int] = Try(request.body.asInstanceOf[AnyContentAsJson].json.\("threshold").as[Int])
      
      thresholdTry match {
        case Success(threshold) =>
          if (threshold < 0) { //disable
            AirlineSource.deleteAirplaneRenewal(airlineId)
            Ok(Json.obj())
          } else if (threshold >= 100) {
            BadRequest("Cannot set threshold to >= 100")
          } else {
            AirlineSource.saveAirplaneRenewal(airlineId, threshold)
            Ok(Json.obj("threshold" -> JsNumber(threshold)))
          }
        case Failure(_) =>
          BadRequest("Cannot Update airplane renewal")
      }
      
    } else {
      BadRequest("Cannot Update airplane renewal")
    }
  }
  
  def getAirplaneRenewal(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val threshold : Option[Int] = AirlineSource.loadAirplaneRenewal(airlineId)
     threshold match {
       case Some(threshold) => Ok(Json.obj("threshold" -> JsNumber(threshold)))
       case None => Ok(Json.obj())
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
