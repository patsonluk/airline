package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import com.patson.data.CycleSource
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.model.AirlineTransaction
import com.patson.data.CountrySource
import com.patson.data.CashFlowSource
import com.patson.model.AirlineCashFlow
import com.patson.model.CashFlowType
import com.patson.model.CashFlowType
import com.patson.model.AirlineCashFlowItem


class AirplaneApplication extends Controller {
  val BUY_AIRPLANCE_RELATIONSHIP_THRESHOLD = 0
  
  implicit object AirplaneWithAssignedLinkWrites extends Writes[(Airplane, Option[Link])] {
    def writes(airplaneWithAssignedLink : (Airplane, Option[Link])): JsValue = {
      val airplane = airplaneWithAssignedLink._1
      val jsObject = Json.toJson(airplane).asInstanceOf[JsObject]
      airplaneWithAssignedLink._2.fold( jsObject ) { link =>
        jsObject + ("link", Json.toJson(link))
      }
    }
  }
  
  implicit object AirplanesByModelWrites extends Writes[AirplanesByModel] {
    def writes(airplanesByModel: AirplanesByModel): JsValue = {
      Json.toJson(airplanesByModel.model).asInstanceOf[JsObject] + 
        ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes)) + 
        ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes)) +
        ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes))
    }
  }
  
  
  def getAirplaneModels() = Action {
    val models = ModelSource.loadAllModels()
    
    Ok(Json.toJson(models))
  }
  
  def getAirplaneModelsByAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val models = ModelSource.loadAllModels()
    
    val modelWithRejections : Map[Model, Option[String]]= getRejections(models, request.user)
    
    Ok(Json.toJson(modelWithRejections.toList.map {
      case(model, rejectionOption) => {
        rejectionOption match {
          case Some(rejection) => Json.toJson(model).asInstanceOf[JsObject] + ("rejection" -> JsString(rejection))
          case None => Json.toJson(model)
        }
      }
    }))
  }
  
  def getRejections(models : List[Model], airline : Airline) : Map[Model, Option[String]] = {
     
    val countryRelations : Map[String, Int] = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationShips(homeCountry).map {
        case ((homeCountry, otherCountry), relationship) => (otherCountry, relationship)
      }.toMap
      case None => Map.empty
    }    
    
    val ownedModels = AirplaneSource.loadAirplanesByOwner(airline.id).map(_.model).toSet
    models.map { model =>
      (model, getRejection(model, countryRelations.getOrElse(model.countryCode, 0), ownedModels, airline))
    }.toMap
    
  }
  
  def getRejection(model: Model, airline : Airline) : Option[String] = {
    val countryRelation = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationship(homeCountry, model.countryCode)
      case None => 0
    }
    
    val ownedModels = AirplaneSource.loadAirplanesByOwner(airline.id).map(_.model).toSet
    getRejection(model, countryRelation, ownedModels, airline)
  }
  
  def getRejection(model: Model, countryRelationship : Int, ownedModels : Set[Model], airline : Airline) : Option[String]= {
    if (countryRelationship < BUY_AIRPLANCE_RELATIONSHIP_THRESHOLD) {
      return Some("The company refuses to sell " + model.name + " to your airline due to bad country relationship")
    }
    
    if (!ownedModels.contains(model) && ownedModels.size >= airline.airlineGrade.getModelsLimit) {
      return Some("Can only own up to " + airline.airlineGrade.getModelsLimit + " different airplane model(s) at current airline grade")
    }
    
    if (model.price > airline.getBalance()) {
      return Some("Not enough cash to purchase this airplane model")
    }
    
    return None
  }
  
  
  
  def getAirplanes(airlineId : Int, simpleResult : Boolean) = AuthenticatedAirline(airlineId) {
    if (simpleResult) {
      val airplanesWithLink : List[(Airplane, Option[Link])]= AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
      
      val airplanesByModel: Map[Model, (List[Airplane], List[Airplane])] = airplanesWithLink.groupBy( _._1.model ).mapValues { airplanesWithLink : List[(Airplane, Option[Link])] =>
        airplanesWithLink.partition {
          case (_, linkOption) => linkOption.isDefined
        }
      }.mapValues {
        case (assignedAirplanes, freeAirplanes) =>
          (assignedAirplanes.map(_._1), freeAirplanes.map(_._1)) //get rid of the Option[Link] now as we have 2 lists already
      }
      
      val currentCycle = CycleSource.loadCycle()
      
      val airplanesByModelList = airplanesByModel.map {
        case (model, (assignedAirplanes, freeAirplanes)) => AirplanesByModel(model, assignedAirplanes, availableAirplanes = freeAirplanes.filter(_.isReady(currentCycle)), constructingAirplanes=freeAirplanes.filter(!_.isReady(currentCycle)))
      }
      Ok(Json.toJson(airplanesByModelList))
    } else {
      val airplanesWithLink : List[(Airplane, Option[Link])]= AirplaneSource.loadAirplanesWithAssignedLinkByOwner(airlineId)
      Ok(Json.toJson(airplanesWithLink))
    }
  }
  
  def getUsedAirplanes(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
      ModelSource.loadModelById(modelId) match {
        case Some(model) => 
          val usedAirplanes = AirplaneSource.loadAirplanesCriteria(List(("model", modelId), ("is_sold", true)))
          var result =  Json.obj("airplanes" -> Json.toJson(usedAirplanes)).asInstanceOf[JsObject]
          getRejection(model, request.user).foreach { rejection =>
            result = result + ("rejection" -> JsString(rejection))
          }
          Ok(result)
        case None => BadRequest("model not found")
      }
    
      
  }
  
  def getAirplane(airlineId : Int, airplaneId : Int) =  AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId, AirplaneSource.LINK_FULL_LOAD) match {
      case Some(airplaneWithLink) =>
        if (airplaneWithLink._1.owner.id == airlineId) {
          Ok(Json.toJson(airplaneWithLink))     
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def sellAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId) match {
      case Some((airplane, Some(link))) => //still assigned to some link, do not allow selling
        BadRequest("airplane still assigned to link " + link)
      case Some((airplane, None)) =>
        if (airplane.owner.id == airlineId) {
          if (!airplane.isReady(CycleSource.loadCycle)) {
            BadRequest("airplane is not yet constructed or is sold")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)
            
            val updateCount = 
              if (airplane.condition >= Airplane.BAD_CONDITION) { //then put in 2nd handmarket
                airplane.isSold = true
                airplane.dealerRatio = Airplane.DEFAULT_DEALER_RATIO
                AirplaneSource.updateAirplanes(List(airplane))
              } else {
                AirplaneSource.deleteAirplane(airplaneId)
              }
            
            if (updateCount == 1) {
              AirlineSource.adjustAirlineBalance(airlineId, sellValue)
              
              AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellValue - airplane.value))
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellValue))
              
              
              Ok(Json.toJson(airplane))
            } else {
              NotFound
            }
          }
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def replaceAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) => 
        if (airplane.owner.id == airlineId) {
          if (!airplane.isReady(CycleSource.loadCycle)) {
            BadRequest("airplane is not yet constructed")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)
            val replaceCost = airplane.model.price - sellValue
            if (request.user.airlineInfo.balance < replaceCost) { //not enough money!
              BadRequest("Not enough money")   
            } else {
              if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
                 AirplaneSource.saveAirplanes(List(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, id = 0)))
              }
              
              val replacingAirplane = airplane.copy(constructedCycle = CycleSource.loadCycle(), condition = Airplane.MAX_CONDITION, value = airplane.model.price)
               
              AirplaneSource.updateAirplanes(List(replacingAirplane)) //TODO MAKE SURE SYNCHONRIZE WITH AIRPLANE UPDATE SIMULATION
              AirlineSource.adjustAirlineBalance(airlineId, -1 * replaceCost)
              AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellValue - airplane.value))
              
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellValue))
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, airplane.model.price * -1))
                
              Ok(Json.toJson(airplane))
            }
          }
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def addAirplane(airlineId : Int, model : Int, quantity : Int) = AuthenticatedAirline(airlineId) { request =>
    val modelGet = ModelSource.loadModelById(model)
    if (modelGet.isEmpty) {
      BadRequest("unknown model or airline")
    } else {
      val airline = request.user
      val currentCycle = CycleSource.loadCycle()
      val constructedCycle = currentCycle + modelGet.get.constructionTime
      
      val airplane = Airplane(modelGet.get, airline, constructedCycle = constructedCycle , Airplane.MAX_CONDITION, depreciationRate = 0, value = modelGet.get.price)
      
      val rejectionOption = getRejection(modelGet.get, airline)
      if (rejectionOption.isDefined) {
        BadRequest(rejectionOption.get)   
      } else {
        
        val amount = -1 * airplane.model.price * quantity
        AirlineSource.adjustAirlineBalance(airlineId, amount)
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, amount))
        
        val airplanes = ListBuffer[Airplane]()
        for (i <- 0 until quantity) {
          airplanes.append(airplane.copy())
        }
        
        val updateCount = AirplaneSource.saveAirplanes(airplanes.toList)
        if (updateCount > 0) {
            Accepted(Json.obj("updateCount" -> updateCount))
        } else {
            UnprocessableEntity("Cannot save airplane")
        }
      }
    }
  }

  sealed case class AirplanesByModel(model : Model, assignedAirplanes : List[Airplane], availableAirplanes : List[Airplane], constructingAirplanes: List[Airplane]) 
}
