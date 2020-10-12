package controllers

import com.patson.data.{AirplaneSource, LinkSource}
import com.patson.data.airplane.ModelSource
import com.patson.model._
import com.patson.model.airplane._
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.int2bigDecimal


class AirplaneConfigurationApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object AirplaneConfigurationWrites extends Writes[AirplaneConfiguration] {
    //case class Loan(airlineId : Int, borrowedAmount : Long, interest : Long, var remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject
    def writes(configuration: AirplaneConfiguration): JsValue = JsObject(List(
      "id" -> JsNumber(configuration.id),
      "airlineId" -> JsNumber(configuration.airline.id),
      "model" -> Json.toJson(configuration.model),
      "economy" -> JsNumber(configuration.economyVal),
      "business" -> JsNumber(configuration.businessVal),
      "first" -> JsNumber(configuration.firstVal),
      "isDefault" -> JsBoolean(configuration.isDefault)
      ))
  }

  

  def getConfigurations(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request : AuthenticatedRequest[Any, Airline] =>
    ModelSource.loadModelById(modelId) match {
      case Some(model) =>
        val configurations = AirplaneSource.loadAirplaneConfigurationsByCriteria(List(("airline", airlineId), ("model", modelId)))

        Ok(Json.obj("model" -> model, "configurations" -> configurations, "spaceMultipliers" -> Json.obj("economy" -> ECONOMY.spaceMultiplier, "business" -> BUSINESS.spaceMultiplier, "first" -> FIRST.spaceMultiplier), "maxConfigurationCount" -> AirplaneConfiguration.MAX_CONFIGURATION_TEMPLATE_COUNT))
      case None =>
        BadRequest(s"Model $modelId for configuration is not found")
    }


  }

  def getConfiguration(airlineId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { request : AuthenticatedRequest[Any, Airline] =>
    AirplaneSource.loadAirplaneConfigurationById(configurationId) match {
      case Some(configuration) =>
        Ok(Json.obj("configuration" -> configuration, "spaceMultipliers" -> Json.obj("economy" -> ECONOMY.spaceMultiplier, "business" -> BUSINESS.spaceMultiplier, "first" -> FIRST.spaceMultiplier)))
      case None =>
        BadRequest(s"Configuration $configurationId is not found")
    }
  }
  
  def putConfiguration(airlineId : Int, modelId : Int, configurationId : Int, economy : Int, business : Int, first : Int, isDefault : Boolean) = AuthenticatedAirline(airlineId) { implicit request =>
    if (economy < 0 || business < 0 || first < 0) {
       BadRequest("cannot have negative values for configurations")
    } else {
      ModelSource.loadModelById(modelId) match {
        case Some(model) =>
          if (economy * ECONOMY.spaceMultiplier + business * BUSINESS.spaceMultiplier + first * FIRST.spaceMultiplier > model.capacity) {
            BadRequest("configuration is not within capacity limit!")
          } else {
            val existingConfigurations: Map[Int, AirplaneConfiguration] = AirplaneSource.loadAirplaneConfigurationsByCriteria(List(("airline", airlineId), ("model", modelId))).map(config => (config.id, config)).toMap
            if (configurationId == 0) { // new config, check if there's still space
              if (existingConfigurations.size >= AirplaneConfiguration.MAX_CONFIGURATION_TEMPLATE_COUNT) {
                 BadRequest("configuration count exceeds limit!")
              } else {
                val newConfig = AirplaneConfiguration(economy, business, first, request.user, model, isDefault)
                AirplaneSource.saveAirplaneConfigurations(List(newConfig))
                Ok(Json.toJson(newConfig))
              }
            } else {
              existingConfigurations.get(configurationId) match {
                case Some(existingConfig) =>
                  //ensure it is owned by this airline
                  if (existingConfig.airline.id != airlineId) {
                    BadRequest(s"configuration update failed, as the configuration with not owned by $airlineId !")
                  } else {
                    val updatingConfig = existingConfig.copy(economyVal = economy, businessVal = business, firstVal = first, isDefault = isDefault)
                    //check if this is set as default
                    if (!existingConfig.isDefault && isDefault) { //then unset the old default
                      AirplaneSource.loadAirplaneConfigurationById(airlineId, modelId).filter(_.isDefault).foreach { oldDefault =>
                        AirplaneSource.updateAirplaneConfiguration(oldDefault.copy(isDefault = false))
                      }
                    }

                    AirplaneSource.updateAirplaneConfiguration(updatingConfig)
                    LinkUtil.adjustLinksAfterConfigurationChanges(configurationId)
                    Ok(Json.toJson(updatingConfig))
                  }
                case None =>
                  BadRequest(s"configuration update failed, as the configuration with id $configurationId is not found!")
              }
            }
          }
        case None => BadRequest("model is not found")
      }
    }
  }

  def deleteConfiguration(airlineId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AirplaneSource.loadAirplaneConfigurationById(configurationId) match {
      case Some(configuration) =>
        if (configuration.airline.id != airlineId) {
          BadRequest(s"Configuration deletion failed, as the configuration with not owned by $airlineId !")
        } else {
          if (configuration.isDefault) { //cannot delete default
            BadRequest(s"Configuration deletion failed, as the configuration is a default $configuration !")
          } else {
            AirplaneSource.loadAirplaneConfigurationById(airlineId, configuration.model.id).find(_.isDefault) match {
              case Some(defaultConfiguration) =>
              //assign all existing airplanes with this configuration to default
                val airplanes = AirplaneSource.loadAirplanesCriteria(List(("configuration", configurationId)))
                airplanes.foreach(_.configuration = defaultConfiguration)
                AirplaneSource.updateAirplanes(airplanes)
                //adjust capacity on links
                LinkUtil.adjustLinksAfterConfigurationChanges(defaultConfiguration.id)
                //can safely delete the configuration
                AirplaneSource.deleteAirplaneConfiguration(configuration)
                Ok(Json.toJson(configuration))
              case None =>    BadRequest(s"Configuration deletion failed on $configuration, as there's no default configuration !")

            }
          }
        }
      case None =>
        BadRequest(s"Configuration with id $configurationId is not found")
    }
  }

  def updateAirplaneConfiguration(airlineId : Int, airplaneId : Int, configurationId: Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id != airlineId) {
          BadRequest(s"Cannot update Configuration on airplane $airplane as it is not owned by ${request.user.name}")
        } else {
          AirplaneSource.loadAirplaneConfigurationById(configurationId) match {
            case Some(configuration) =>
              if (configuration.airline.id != airlineId || configuration.model.id != airplane.model.id) {
                BadRequest(s"Cannot update Configuration on airplane $airplane as configuration $configurationId is not valid")
              } else {
                airplane.configuration = configuration
                AirplaneSource.updateAirplanes(List(airplane))
                LinkUtil.adjustLinksAfterAirplaneConfigurationChange(airplaneId)
                Ok(Json.toJson(airplane))
              }

            case None =>
              BadRequest(s"Cannot update Configuration on airplane $airplaneId as configuration $configurationId is not found")
          }

        }
      case None => BadRequest(s"Cannot update Configuration on airplane $airplaneId as it is not found")
    }

  }
}
