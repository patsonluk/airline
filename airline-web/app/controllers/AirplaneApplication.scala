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


class AirplaneApplication extends Controller {
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
        ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes.map { _.id })) + 
        ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes.map {_.id})) +
        ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes.map {_.id}))
    }
  }
  
  
  
  
  def getAirplaneModels() = Action {
    val models = ModelSource.loadAllModels()
    
    val constructingAirplaneCounts : Map[Int, Int] = AirplaneSource.loadConstructingAirplanes.groupBy(_.model.id).mapValues(_.size) 
    
    Ok(Json.toJson(models))
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
            BadRequest("airplane is not yet constructed")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)
            if (AirplaneSource.deleteAirplane(airplaneId) == 1) {
              AirlineSource.adjustAirlineBalance(airlineId, sellValue)
              AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellValue - airplane.value))
              
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
  
  def addAirplane(airlineId : Int, model : Int, quantity : Int) = AuthenticatedAirline(airlineId) { request =>
    val modelGet = ModelSource.loadModelById(model)
    if (modelGet.isEmpty) {
      BadRequest("unknown model or airline")
    } else {
      val airline = request.user
      val currentCycle = CycleSource.loadCycle()
      val constructedCycle = currentCycle + modelGet.get.constructionTime
      
      val airplane = Airplane(modelGet.get, airline, constructedCycle = constructedCycle , Airplane.MAX_CONDITION, depreciationRate = 0, value = modelGet.get.price)
      if (airline.airlineInfo.balance < (airplane.model.price * quantity)) { //not enough money!
        BadRequest("Not enough money")   
      } else {
        
        val amount = -1 * airplane.model.price * quantity
        AirlineSource.adjustAirlineBalance(airlineId, amount)
        
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
