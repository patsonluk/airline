package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.{AirlineSource, AirplaneSource, CashFlowSource, CountrySource, CycleSource, LinkSource}
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.model.AirlineTransaction
import com.patson.model.AirlineCashFlow
import com.patson.model.CashFlowType
import com.patson.model.CashFlowType
import com.patson.model.AirlineCashFlowItem
import javax.inject.Inject


class AirplaneApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val BUY_AIRPLANCE_RELATIONSHIP_THRESHOLD = 0
  implicit object LinkAssignmentWrites extends Writes[LinkAssignments] {
    def writes(linkAssignments: LinkAssignments) : JsValue = {
      var result = Json.arr()
      linkAssignments.assignments.foreach {
        case(linkId, assignment) =>
          val link = LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD).getOrElse(Link.fromId(linkId))
          result = result.append(Json.obj("link" -> Json.toJson(link), "frequency" -> assignment.frequency))
      }
      result
    }
  }
  
  implicit object AirplaneWithAssignedLinkWrites extends Writes[(Airplane, LinkAssignments)] {
    def writes(airplaneWithAssignedLink : (Airplane, LinkAssignments)): JsValue = {
      val airplane = airplaneWithAssignedLink._1
      val jsObject = Json.toJson(airplane).asInstanceOf[JsObject]
      jsObject + ("links" -> Json.toJson(airplaneWithAssignedLink._2))
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
      case Some(homeCountry) => CountrySource.getCountryMutualRelationShips(homeCountry).toList.map {
        case ((_, otherCountry), relationship) => (otherCountry, relationship)
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
    if (airline.getHeadQuarter().isEmpty) { //no HQ
      return Some("Must build HQs before purchasing any airplanes")
    }
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
  
  def getUsedRejections(usedAirplanes : List[Airplane], model : Model, airline : Airline) : Map[Airplane, String] = {
    if (airline.getHeadQuarter().isEmpty) { //no HQ
      return usedAirplanes.map((_, "Must build HQs before purchasing any airplanes")).toMap
    }

    val countryRelationship = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationship(homeCountry, model.countryCode)
      case None => 0
    }
    
    if (countryRelationship < BUY_AIRPLANCE_RELATIONSHIP_THRESHOLD) {
      val rejection = "Cannot buy used airplane of " + model.name + " as your home country has bad relationship with manufacturer's country"
      return usedAirplanes.map((_, rejection)).toMap
    }
    
    val ownedModels = AirplaneSource.loadAirplanesByOwner(airline.id).map(_.model).toSet
    if (!ownedModels.contains(model) && ownedModels.size >= airline.airlineGrade.getModelsLimit) {
      val rejection = "Can only own up to " + airline.airlineGrade.getModelsLimit + " different airplane model(s) at current airline grade"
      return usedAirplanes.map((_, rejection)).toMap
    }
    
    val rejections = scala.collection.mutable.Map[Airplane, String]()
    usedAirplanes.foreach { airplane =>
      if (airplane.dealerValue > airline.getBalance()) {
         rejections.put(airplane, "Not enough cash to purchase this airplane")  
      }
    }
    return rejections.toMap
  }
  
  
  
  def getAirplanes(airlineId : Int, simpleResult : Boolean) = AuthenticatedAirline(airlineId) {
    val ownedAirplanes: List[Airplane] = AirplaneSource.loadAirplanesByOwner(airlineId)
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByOwner(airlineId)
    if (simpleResult) {
      //now split the list of airplanes by with and w/o assignedLinks
      val airplanesByModel: Map[Model, (List[Airplane], List[Airplane])] = ownedAirplanes.groupBy(_.model).view.mapValues {
        airplanes => airplanes.partition(airplane => linkAssignments.isDefinedAt(airplane.id))
      }.toMap

      val currentCycle = CycleSource.loadCycle()
      
      val airplanesByModelList = airplanesByModel.toList.map {
        case (model, (assignedAirplanes, freeAirplanes)) => AirplanesByModel(model, assignedAirplanes, availableAirplanes = freeAirplanes.filter(_.isReady(currentCycle)), constructingAirplanes=freeAirplanes.filter(!_.isReady(currentCycle)))
      }
      Ok(Json.toJson(airplanesByModelList))
    } else {
      val airplanesWithLink : List[(Airplane, LinkAssignments)]= ownedAirplanes.map { airplane =>
        (airplane, linkAssignments.getOrElse(airplane.id, LinkAssignments.empty))
      }
      Ok(Json.toJson(airplanesWithLink))
    }
  }
  
  def getUsedAirplanes(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
      ModelSource.loadModelById(modelId) match {
        case Some(model) => 
          val usedAirplanes = AirplaneSource.loadAirplanesCriteria(List(("a.model", modelId), ("is_sold", true)))
          
          val rejections = getUsedRejections(usedAirplanes, model, request.user)
          var result = Json.arr()
          usedAirplanes.foreach { airplane =>
            var airplaneJson = Json.toJson(airplane).asInstanceOf[JsObject]
            if (rejections.contains(airplane)) {
              airplaneJson = airplaneJson + ("rejection" -> JsString(rejections(airplane)))
            }
            result = result :+ airplaneJson
          }
          Ok(result)
        case None => BadRequest("model not found")
      }
  }
  
  def buyUsedAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
      this.synchronized {
        AirplaneSource.loadAirplaneById(airplaneId) match {
          case Some(airplane) =>
            val airline = request.user
            getUsedRejections(List(airplane), airplane.model, airline).get(airplane) match {
              case Some(rejection) => BadRequest(rejection)
              case None =>
                if (!airplane.isSold) {
                  BadRequest("Airplane is no longer for sale " + airlineId)
                } else {
                  val dealerValue = airplane.dealerValue
                  val actualValue = airplane.value
                  airplane.buyFromDealer(airline, CycleSource.loadCycle())
                  if (AirplaneSource.updateAirplanes(List(airplane)) == 1) {
                    val capitalGain = actualValue - dealerValue
                    AirlineSource.adjustAirlineBalance(airline.id, dealerValue * -1)
                    AirlineSource.saveTransaction(AirlineTransaction(airlineId = airline.id, transactionType = TransactionType.CAPITAL_GAIN, amount = capitalGain))
                    AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, dealerValue * -1))
                    Ok(Json.obj())
                  } else {
                    BadRequest("Failed to buy used airplane " + airlineId)
                  }

                }
            }

          case None => BadRequest("airplane not found")
        }
      }
  }
  
  
  
  def getAirplane(airlineId : Int, airplaneId : Int) =  AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          //load link assignments
          val airplaneWithLinkAssignments : (Airplane, LinkAssignments) = (airplane, AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id))
          Ok(Json.toJson(airplaneWithLinkAssignments))
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def sellAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id != airlineId || airplane.isSold) {
          Forbidden
        } else if (!airplane.isReady(CycleSource.loadCycle)) {
          BadRequest("airplane is not yet constructed or is sold")
        } else {
          val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplaneId)
          if (!linkAssignments.isEmpty) { //still assigned to some link, do not allow selling
            BadRequest("airplane " + airplane + " still assigned to link " + linkAssignments)
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)

            val updateCount =
              if (airplane.condition >= Airplane.BAD_CONDITION) { //then put in 2nd handmarket
                airplane.sellToDealer()
                AirplaneSource.updateAirplanes(List(airplane.copy()))
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
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def replaceAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          val currentCycle = CycleSource.loadCycle
          if (!airplane.isReady(currentCycle)) {
            BadRequest("airplane is not yet constructed")
          } else if (airplane.purchasedCycle > (currentCycle - airplane.model.constructionTime)) {
            BadRequest("airplane is not yet ready to be replaced")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)
            val replaceCost = airplane.model.price - sellValue
            if (request.user.airlineInfo.balance < replaceCost) { //not enough money!
              BadRequest("Not enough money")
            } else {
//               if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
//                  AirplaneSource.saveAirplanes(List(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, id = 0)))
//               }

              val replacingAirplane = airplane.copy(constructedCycle = currentCycle, purchasedCycle = currentCycle, condition = Airplane.MAX_CONDITION, value = airplane.model.price)

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
      
      val airplane = Airplane(modelGet.get, airline, constructedCycle = constructedCycle , purchasedCycle = constructedCycle, Airplane.MAX_CONDITION, depreciationRate = 0, value = modelGet.get.price, home = request.user.getHeadQuarter().map(_.airport).getOrElse(Airport.fromId(0)))
      
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
        airplanes.foreach(_.assignDefaultConfiguration())
        
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

  def updateAirplaneHome(airlineId : Int, airplaneId : Int, airportId: Int) = AuthenticatedAirline(airlineId) { request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id != airlineId) {
          BadRequest(s"Cannot update Home on airplane $airplane as it is not owned by ${request.user.name}")
        } else {
          if (!AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id).isEmpty) {
            BadRequest(s"Cannot update Home on airplane $airplane as it has assigned links")
          } else {
            request.user.getBases().find(_.airport.id == airportId) match {
              case Some(base) =>
                airplane.home = base.airport
                AirplaneSource.updateAirplanesDetails(List(airplane))
                Ok(Json.toJson(airplane))
              case None =>
                BadRequest(s"Cannot update Home on airplane $airplaneId as base $airportId is not found")
            }
          }
        }
      case None => BadRequest(s"Cannot update Configuration on airplane $airplaneId as it is not found")
    }
  }
}
