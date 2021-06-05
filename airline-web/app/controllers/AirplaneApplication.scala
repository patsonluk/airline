package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.{AirlineSource, AirplaneSource, CashFlowSource, CountrySource, CycleSource, LinkSource}
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.{Model, _}
import com.patson.model._
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.model.AirlineTransaction
import com.patson.model.AirlineCashFlow
import com.patson.model.CashFlowType
import com.patson.model.CashFlowType
import com.patson.model.AirlineCashFlowItem
import com.patson.model.airplane.Model.Category
import com.patson.util.{AirplaneOwnershipCache, CountryCache}

import javax.inject.Inject
import scala.collection.{MapView, mutable}


class AirplaneApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object LinkAssignmentWrites extends Writes[LinkAssignments] {
    def writes(linkAssignments: LinkAssignments) : JsValue = {
      var result = Json.arr()
      linkAssignments.assignments.foreach {
        case(linkId, assignment) =>
          val link = LinkSource.loadFlightLinkById(linkId, LinkSource.SIMPLE_LOAD).getOrElse(Link.fromId(linkId))
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

  implicit object AirplaneModelWithDiscountsWrites extends Writes[ModelWithDiscounts] {
    def writes(airplaneModelWithDiscounts : ModelWithDiscounts): JsValue = {
      if (airplaneModelWithDiscounts.discounts.isEmpty) {
        Json.toJson(airplaneModelWithDiscounts.originalModel)
      } else {
        val discountedModel = airplaneModelWithDiscounts.originalModel.applyDiscount(airplaneModelWithDiscounts.discounts)
        var result = Json.toJson(discountedModel).asInstanceOf[JsObject]
        if (discountedModel.price != airplaneModelWithDiscounts.originalModel.price) {
          result = result + ("originalPrice" -> JsNumber(airplaneModelWithDiscounts.originalModel.price))
        }
        if (discountedModel.constructionTime != airplaneModelWithDiscounts.originalModel.constructionTime) {
          result = result + ("originalConstructionTime" -> JsNumber(airplaneModelWithDiscounts.originalModel.constructionTime))
        }

        var discountsJson = Json.obj()
        airplaneModelWithDiscounts.discounts.groupBy(_.discountType).foreach {
          case (discountType, discounts) =>
            var discountsByTypeJson = Json.arr()
            discounts.foreach { discount =>
              discountsByTypeJson = discountsByTypeJson.append(Json.obj("discountDescription" -> discount.description, "discountPercentage" -> (discount.discount * 100).toInt))
            }
            val typeLabel = discountType.toString.toLowerCase
            discountsJson = discountsJson + (typeLabel -> discountsByTypeJson)
        }
        result = result + ("discounts" -> discountsJson)
        result
      }
    }
  }

  case class ModelWithDiscounts(originalModel : Model, discounts : List[ModelDiscount])

  sealed case class AirplanesByModel(model : Model, assignedAirplanes : List[Airplane], availableAirplanes : List[Airplane], constructingAirplanes: List[Airplane])
  
  object AirplanesByModelWrites extends Writes[List[AirplanesByModel]] {
    def writes(airplanesByModelList: List[AirplanesByModel]): JsValue = {
      var result = Json.obj()
      airplanesByModelList.foreach { airplanesByModel =>
        val airplaneJson = Json.obj(
          ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes)),
            ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes)),
            ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes)))
        result = result + (String.valueOf(airplanesByModel.model.id) -> airplaneJson)
      }
      result
    }
  }
  object AirplanesByModelSimpleWrites extends Writes[List[AirplanesByModel]] {
    def writes(airplanesByModelList: List[AirplanesByModel]): JsValue = {
      var result = Json.obj()
      airplanesByModelList.foreach { airplanesByModel =>
        val airplaneJson = Json.obj(
          ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes)(SimpleAirplanesWrites)),
          ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes)(SimpleAirplanesWrites)),
          ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes)(SimpleAirplanesWrites)))
        result = result + (String.valueOf(airplanesByModel.model.id) -> airplaneJson)
      }
      result
    }
  }




  object SimpleAirplanesWrites extends Writes[List[Airplane]] {
    override def writes(airplanes: List[Airplane]): JsValue = {
      var result = Json.arr()
      airplanes.foreach { airplane =>
        result = result.append(Json.toJson(airplane)(SimpleAirplaneWrite))
      }
      result
    }
  }

  def getAirplaneModels() = Action {
    Ok(Json.toJson(allAirplaneModels))
  }

  val MODEL_TOP_N = 10
  def getAirplaneModelStatsByAirline(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(getAirplaneModelStatsJson(modelId, Some(airlineId)))
  }

  def getAirplaneModelStats(modelId : Int) = Action {
    //load usage
    Ok(getAirplaneModelStatsJson(modelId, None))
  }

  def getAirplaneModelStatsJson(modelId : Int, airlineIdOption : Option[Int]) = {
    val airplanes = AirplaneSource.loadAirplanesCriteria(List(("a.model", modelId)))

    var result = Json.obj("total" -> airplanes.length)
    var topAirlinesJson = Json.arr()
    val airplanesCountByOwnerId : Map[Int, Int] = airplanes.filter(!_.isSold).groupBy(_.owner).view.map {
      case(airline, airplanes) => (airline.id, airplanes.length)
    }.toMap
    airplanesCountByOwnerId.toList.sortBy(_._2).reverse.take(MODEL_TOP_N).foreach {
      case (airlineId, airplaneCount) =>
        //load the airline name
        val airline = AirlineSource.loadAirlineById(airlineId)
        topAirlinesJson = topAirlinesJson.append(Json.obj("airline" -> Json.toJson(airline), "airplaneCount" -> airplaneCount))
    }

    result = result + ("topAirlines" -> topAirlinesJson)

    airlineIdOption.foreach { airlineId => //add favorite info for airline
      var favoriteJson = Json.obj()
      validateMakeFavorite(airlineId, modelId) match {
        case Left(rejection) => favoriteJson = favoriteJson + ("rejection" -> JsString(rejection))
        case Right(_) =>
      }
      result = result + ("favorite" -> favoriteJson)
    }
    result
  }
  
  def getAirplaneModelsByAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val originalModels = ModelSource.loadAllModels()
    val originalModelsById = originalModels.map(model => (model.id, model)).toMap
//    val airlineDiscountsByModelId = ModelSource.loadAirlineDiscountsByAirlineId(airlineId).groupBy(_.modelId)
//    val blanketDiscountsByModelId = ModelSource.loadAllModelDiscounts().groupBy(_.modelId)
    val discountsByModelId = ModelDiscount.getAllCombinedDiscountsByAirlineId(airlineId)

    val discountedModels = originalModels.map { originalModel =>
      discountsByModelId.get(originalModel.id) match {
        case Some(discounts) => originalModel.applyDiscount(discounts)
        case None => originalModel
      }

    }

    val discountedModelWithRejections : Map[Model, Option[String]]= getRejections(discountedModels, request.user)

    var result = Json.arr()
    val favoriteOption = ModelSource.loadFavoriteModelId(airlineId)
    discountedModelWithRejections.toList.foreach {
      case(discountedModel, rejectionOption) =>
        val originalModel = originalModelsById(discountedModel.id)

        var modelJson =
          discountsByModelId.get(originalModel.id) match {
            case Some(discounts) =>
              Json.toJson (ModelWithDiscounts (originalModel, discounts) ).asInstanceOf[JsObject]
            case None =>
              Json.toJson (originalModel).asInstanceOf[JsObject]
          }


        rejectionOption match {
          case Some(rejection) => modelJson = modelJson + ("rejection" -> JsString(rejection))
          case None => //
        }

        if (favoriteOption.isDefined && favoriteOption.get._1 == originalModel.id) {
          modelJson = modelJson + ("isFavorite" -> JsBoolean(true))
        }
        result = result.append(modelJson)
    }

    Ok(result)
  }
  
  def getRejections(models : List[Model], airline : Airline) : Map[Model, Option[String]] = {
    val allManufacturingCountries = models.map(_.countryCode).toSet

    val countryRelations : Map[String, AirlineCountryRelationship] = allManufacturingCountries.map { countryCode =>
      (countryCode, AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline))
    }.toMap

    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet


    models.map { model =>
      (model, getRejection(model, 1, countryRelations(model.countryCode), ownedModels, airline))
    }.toMap
    
  }
  
  def getRejection(model: Model, quantity : Int, airline : Airline) : Option[String] = {
    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(model.countryCode, airline)

    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet
    getRejection(model, quantity, relationship, ownedModels, airline)
  }
  
  def getRejection(model: Model, quantity : Int, relationship : AirlineCountryRelationship, ownedModels : Set[Model], airline : Airline) : Option[String]= {
    if (airline.getHeadQuarter().isEmpty) { //no HQ
      return Some("Must build HQs before purchasing any airplanes")
    }
    if (!model.purchasableWithRelationship(relationship.relationship)) {
      return Some(s"The manufacturer refuses to sell " + model.name + s" to your airline until your relationship with ${CountryCache.getCountry(model.countryCode).get.name} is improved to at least ${Model.BUY_RELATIONSHIP_THRESHOLD}")
    }


    val ownedModelFamilies = ownedModels.map(_.family)

    if (!ownedModelFamilies.contains(model.family) && ownedModelFamilies.size >= airline.airlineGrade.getModelFamilyLimit) {
      val familyToken = if (ownedModelFamilies.size <= 1) "family" else "families"
      return Some("Can only own up to " + airline.airlineGrade.getModelFamilyLimit + " different airplane " + familyToken + " at current airline grade")
    }

    val cost: Long = model.price.toLong * quantity
    if (cost > airline.getBalance()) {
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

    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(model.countryCode, airline)
    if (!model.purchasableWithRelationship(relationship.relationship)) {
      val rejection = s"Cannot buy used airplane of " + model.name + s" until your relationship with ${CountryCache.getCountry(model.countryCode).get.name} is improved to at least ${Model.BUY_RELATIONSHIP_THRESHOLD}"
      return usedAirplanes.map((_, rejection)).toMap
    }
    
    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet
    val ownedModelFamilies = ownedModels.map(_.family)
    if (!ownedModelFamilies.contains(model.family) && ownedModelFamilies.size >= airline.airlineGrade.getModelFamilyLimit) {
      val familyToken = if (ownedModelFamilies.size <= 1) "family" else "families"
      val rejection = "Can only own up to " + airline.airlineGrade.getModelFamilyLimit + " different airplane " + familyToken + " at current airline grade"
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

  def validateMakeFavorite(airlineId : Int, modelId : Int) : Either[String, Unit] = {
    val airplanes = AirplaneSource.loadAirplanesCriteria(List(("a.model", modelId)))
    val airplanesCountByOwner = airplanes.filter(!_.isSold).groupBy(_.owner).view.map {
      case (airline, airplanes) => (airline.id, airplanes.length)
    }.toMap
    val model = ModelSource.loadModelById(modelId).getOrElse(Model.fromId(modelId))
    airplanesCountByOwner.get(airlineId) match {
      case Some(count) =>
        val ownershipPercentage = count * 100.0 / airplanes.length
        if (ownershipPercentage >= ModelDiscount.MAKE_FAVORITE_PERCENTAGE_THRESHOLD) {
          ModelSource.loadFavoriteModelId(airlineId) match {
            case Some((favoriteModelId, startCycle)) =>
              if (favoriteModelId == modelId) {
                Right(()) //ok, already the favorite
              } else {
                val cycleSinceLastFavorite = CycleSource.loadCycle() - startCycle
                if (cycleSinceLastFavorite >= ModelDiscount.MAKE_FAVORITE_RESET_THRESHOLD) {
                  Right(())
                } else {
                  val remainingCycles = ModelDiscount.MAKE_FAVORITE_RESET_THRESHOLD - cycleSinceLastFavorite
                  Left(s"Can only reset favorite in $remainingCycles week(s)")
                }
              }
            case None => Right(()) //no favorite yet. ok
          }

        } else {

          Left(s"Cannot set ${model.name} as Favorite as you do not own at least ${ModelDiscount.MAKE_FAVORITE_PERCENTAGE_THRESHOLD}% of this model in circulation. You currently own ${BigDecimal(ownershipPercentage).setScale(2, BigDecimal.RoundingMode.HALF_UP)}%")
        }
      case None => Left(s"Cannot set ${model.name} as Favorite as you do not own any airplane of this model.")
    }

  }

  def getOwnedAirplanes(airlineId : Int, simpleResult : Boolean, groupedResult : Boolean) = {
    getAirplanes(airlineId, None, simpleResult, groupedResult)
  }

  def getOwnedAirplanesWithModelId(airlineId : Int, modelId : Int) = {
    getAirplanes(airlineId, Some(modelId), simpleResult = false, groupedResult = true)
  }


  private def getAirplanes(airlineId : Int, modelIdOption : Option[Int], simpleResult : Boolean, groupedResult : Boolean) = AuthenticatedAirline(airlineId) {
    val queryCriteria = ListBuffer(("owner", airlineId), ("is_sold", false))
    modelIdOption.foreach { modelId =>
      queryCriteria.append(("a.model", modelId))
    }

    val ownedAirplanes: List[Airplane] = AirplaneSource.loadAirplanesCriteria(queryCriteria.toList)
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByOwner(airlineId)
    if (groupedResult) {
      //now split the list of airplanes by with and w/o assignedLinks
      val airplanesByModel: Map[Model, (List[Airplane], List[Airplane])] = ownedAirplanes.groupBy(_.model).view.mapValues {
        airplanes => airplanes.partition(airplane => linkAssignments.isDefinedAt(airplane.id) && airplane.isReady) //for this list do NOT include assigned airplanes that are still under construction, as it's already under the construction list
          //TODO the front end should do the splitting...
      }.toMap

      val airplanesByModelList = airplanesByModel.toList.map {
        case (model, (assignedAirplanes, freeAirplanes)) => AirplanesByModel(model, assignedAirplanes, availableAirplanes = freeAirplanes.filter(_.isReady), constructingAirplanes=freeAirplanes.filter(!_.isReady))
      }
      var result =
        if (simpleResult) {
          Json.toJson(airplanesByModelList)(AirplanesByModelSimpleWrites)
        } else {
          Json.toJson(airplanesByModelList)(AirplanesByModelWrites)
        }
      Ok(result)
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
  
  def buyUsedAirplane(airlineId : Int, airplaneId : Int, homeAirportId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { request =>
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
                  val homeBase = request.user.getBases().find(_.airport.id == homeAirportId)
                  homeBase match {
                    case None =>
                      BadRequest(s"Home airport ID $homeAirportId is not valid")
                    case Some(homeBase) =>
                      val configuration: Option[AirplaneConfiguration] =
                        if (configurationId == -1) {
                          None
                        } else {
                          AirplaneSource.loadAirplaneConfigurationById(configurationId)
                        }

                      if (configuration.isDefined && (configuration.get.airline.id != airlineId || configuration.get.model.id != airplane.model.id)) {
                        BadRequest("Configuration is not owned by this airline/model")
                      } else {
                        val dealerValue = airplane.dealerValue
                        val actualValue = airplane.value
                        airplane.buyFromDealer(airline, CycleSource.loadCycle())
                        airplane.home = homeBase.airport
                        airplane.purchaseRate = 1 //no discount
                        configuration.foreach { configuration =>
                          airplane.configuration = configuration
                        }

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
        } else if (!airplane.isReady) {
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
          if (!airplane.isReady) {
            BadRequest("airplane is not yet constructed")
          } else if (airplane.purchasedCycle > (currentCycle - airplane.model.constructionTime)) {
            BadRequest("airplane is not yet ready to be replaced")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)

            val originalModel = airplane.model

            val model = originalModel.applyDiscount(ModelDiscount.getCombinedDiscountsByModelId(airlineId, originalModel.id))

            val replaceCost = model.price - sellValue
            val purchaseRate = model.price.toDouble / originalModel.price
            if (request.user.airlineInfo.balance < replaceCost) { //not enough money!
              BadRequest("Not enough money")
            } else {
//               if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
//                  AirplaneSource.saveAirplanes(List(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, id = 0)))
//               }

              val replacingAirplane = airplane.copy(constructedCycle = currentCycle, purchasedCycle = currentCycle, condition = Airplane.MAX_CONDITION, value = originalModel.price, purchaseRate = purchaseRate)

              AirplaneSource.updateAirplanes(List(replacingAirplane)) //TODO MAKE SURE SYNCHONRIZE WITH AIRPLANE UPDATE SIMULATION
              AirlineSource.adjustAirlineBalance(airlineId, -1 * replaceCost)

              val sellAirplaneLoss = sellValue - airplane.value
              val discountAirplaneGain = originalModel.price - model.price
              AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellAirplaneLoss + discountAirplaneGain))

              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellValue))
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, model.price * -1))

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
  
  def addAirplane(airlineId : Int, modelId : Int, quantity : Int, homeAirportId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { request =>
    ModelSource.loadModelById(modelId) match {
      case None =>
        BadRequest("unknown model or airline")
      case Some(originalModel) =>
        //now check for discounts
        val model = originalModel.applyDiscount(ModelDiscount.getCombinedDiscountsByModelId(airlineId, originalModel.id))

        val airline = request.user
        val currentCycle = CycleSource.loadCycle()
        val constructedCycle = currentCycle + model.constructionTime
        val homeBase = request.user.getBases().find(_.airport.id == homeAirportId)

        homeBase match {
          case None =>
            BadRequest(s"Home airport ID $homeAirportId is not valid")
          case Some(homeBase) =>
            val purchaseRate = model.price.toDouble / originalModel.price
            val airplane = Airplane(model, airline, constructedCycle = constructedCycle , purchasedCycle = constructedCycle, Airplane.MAX_CONDITION, depreciationRate = 0, value = originalModel.price, home = homeBase.airport, purchaseRate = purchaseRate)

            val rejectionOption = getRejection(model, quantity, airline)
            if (rejectionOption.isDefined) {
              BadRequest(rejectionOption.get)
            } else {
              val airplanes = ListBuffer[Airplane]()
              for (i <- 0 until quantity) {
                airplanes.append(airplane.copy())
              }

              val configuration : Option[AirplaneConfiguration] =
                if (configurationId == -1) {
                  None
                } else {
                  AirplaneSource.loadAirplaneConfigurationById(configurationId)
                }

              if (configuration.isDefined && (configuration.get.airline.id != airlineId || configuration.get.model.id != modelId)) {
                BadRequest("Configuration is not owned by this airline/model")
              } else {
                airplanes.foreach { airplane =>
                  configuration match {
                    case None => airplane.assignDefaultConfiguration()
                    case Some(configuration) => airplane.configuration = configuration
                  }
                }
                val updateCount = AirplaneSource.saveAirplanes(airplanes.toList)
                if (updateCount > 0) {
                  val amount: Long = -1 * airplane.model.price.toLong * updateCount
                  AirlineSource.adjustAirlineBalance(airlineId, amount)
                  AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, amount))

                  if (originalModel.price != model.price) { //if discounted, count as capital gain
                    AirlineSource.saveTransaction(AirlineTransaction(airlineId = airline.id, transactionType = TransactionType.CAPITAL_GAIN, amount = originalModel.price - model.price))
                  }
                  Accepted(Json.obj("updateCount" -> updateCount))
                } else {
                  UnprocessableEntity("Cannot save airplane")
                }
              }
            }
          }
    }
  }

  def swapAirplane(airlineId : Int, fromAirplaneId : Int, toAirplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
    val fromAirplaneOption = AirplaneSource.loadAirplaneById(fromAirplaneId)
    val toAirplaneOption = AirplaneSource.loadAirplaneById(toAirplaneId)

    if (fromAirplaneOption.isDefined && toAirplaneOption.isDefined) {
      val fromAirplane = fromAirplaneOption.get
      val toAirplane = toAirplaneOption.get
      if (fromAirplane.owner.id == airlineId && toAirplane.owner.id == airlineId && fromAirplane.model.id == toAirplane.model.id) {
        val fromConstructedCycle = fromAirplane.constructedCycle
        val fromPurchaseCycle = fromAirplane.purchasedCycle
        val fromCondition = fromAirplane.condition
        val fromValue = fromAirplane.value
        val fromPurchaseRate = fromAirplane.purchaseRate

        val toConstructedCycle = toAirplane.constructedCycle
        val toPurchaseCycle = toAirplane.purchasedCycle
        val toCondition = toAirplane.condition
        val toValue = toAirplane.value
        val toPurchaseRate = toAirplane.purchaseRate

        val swappedFromAirplane = fromAirplane.copy(constructedCycle = toConstructedCycle, purchasedCycle = toPurchaseCycle, condition = toCondition, value = toValue, purchaseRate = toPurchaseRate)
        val swappedToAirplane = toAirplane.copy(constructedCycle = fromConstructedCycle, purchasedCycle = fromPurchaseCycle, condition = fromCondition, value = fromValue, purchaseRate = fromPurchaseRate)

        AirplaneSource.updateAirplanes(List(swappedFromAirplane, swappedToAirplane))
        LinkUtil.adjustLinksAfterAirplaneConfigurationChange(swappedFromAirplane.id)
        LinkUtil.adjustLinksAfterAirplaneConfigurationChange(swappedToAirplane.id)

        Ok(Json.toJson(fromAirplane))
      } else {
        Forbidden
      }
    } else {
        BadRequest("airplane not found")
    }
  }

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

  def getFavoriteModelDetails(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
    ModelSource.loadModelById(modelId) match {
      case Some(model) =>
        var result = Json.obj()
        ModelDiscount.getFavoriteDiscounts(model).foreach { discount =>
          discount.discountType match {
            case DiscountType.PRICE => result = result + ("priceDiscount" -> JsNumber(discount.discount))
            case DiscountType.CONSTRUCTION_TIME => result = result + ("constructionTimeDiscount" -> JsNumber(discount.discount))
          }
        }

        ModelSource.loadFavoriteModelId(airlineId) match {
          case Some((existingFavoriteId, startCycle)) =>
            val existingFavoriteModel = ModelSource.loadModelById(existingFavoriteId).getOrElse(Model.fromId(existingFavoriteId))
            result = result + ("existingFavorite" -> Json.toJson(existingFavoriteModel))
          case None =>
        }
        Ok(result)
      case None =>
        NotFound
    }

  }

  def setFavoriteModel(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
    validateMakeFavorite(airlineId, modelId) match {
      case Left(rejection) =>
        BadRequest(rejection)
      case Right(_) =>
        //delete existing discount on another model
        ModelSource.loadFavoriteModelId(airlineId) match {
          case Some((exitingModelId, _)) =>
            ModelSource.deleteAirlineDiscount(airlineId, exitingModelId, DiscountReason.FAVORITE)
          case None => //
        }
        ModelSource.saveFavoriteModelId(airlineId, modelId, CycleSource.loadCycle())
        ModelDiscount.getFavoriteDiscounts(ModelSource.loadModelById(modelId).getOrElse(Model.fromId(modelId))).foreach {
          discount => ModelSource.saveAirlineDiscount(airlineId, discount)
        }
        Ok(Json.obj())
    }

  }

  def getPreferredSuppliers(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val ownedModelsByCategory : MapView[Model.Category.Value, List[Model]] = AirplaneOwnershipCache.getOwnership(airlineId).groupBy(_.model.category).view.mapValues(_.map(_.model).distinct)

    var categoryJson = Json.obj()
    val supplierDiscountInfo = ModelDiscount.getPreferredSupplierDiscounts(airlineId)

    Category.grouping.foreach {
      case (category, airplaneTypes) =>
        var categoryInfoJson = Json.obj("types" -> airplaneTypes.map(Model.Type.label(_)))
        var ownershipJson = Json.obj()
        ownedModelsByCategory.get(category) match {
          case Some(ownedModels) =>
            ownedModels.groupBy(_.manufacturer).foreach {
              case (manufacturer, ownedModelsByThisManufacturer) => ownershipJson = ownershipJson + (manufacturer.name -> Json.toJson(ownedModelsByThisManufacturer.map(_.family).distinct))
            }
          case None =>
        }
        categoryInfoJson = categoryInfoJson + ("ownership" -> ownershipJson)
        val categoryDiscount = supplierDiscountInfo(category)
        categoryInfoJson = categoryInfoJson + ("discount" -> JsString(categoryDiscount.description))
        val (minCapacity, maxCapacity) = Category.getCapacityRange(category)
        categoryInfoJson = categoryInfoJson + ("minCapacity" -> JsNumber(minCapacity)) + ("maxCapacity" -> JsNumber(maxCapacity))

        categoryJson = categoryJson + (category.toString -> categoryInfoJson)
    }
    Ok(categoryJson)
  }
}
