package controllers

import com.patson.data.{AirlineSource, AirportAssetSource, AirportSource, CampaignSource, CycleSource, DelegateSource}
import com.patson.model.campaign._
import com.patson.model._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode

class AirportAssetApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object AirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset): JsValue = {
      val name = entry.status match {
        case AirportAssetStatus.BLUEPRINT => entry.assetType.label
        case _ => entry.name
      }
      var result = Json.obj(
        "airline" -> entry.airline,
        "airport" -> entry.blueprint.airport,
        "assetType" ->  entry.assetType,
        "assetTypeLabel" ->  entry.assetType.label,
        "level" -> entry.level,
        "name" -> name,
        "descriptions" -> entry.assetType.descriptions,
        "constructionDuration" -> entry.assetType.constructionDuration,
        "status" -> entry.status.toString,
        "cost" -> entry.cost,
        "sellValue" -> entry.sellValue,
        "boosts" -> entry.boosts,
        "id" -> entry.id
      )
      entry.completionCycle.foreach { completionCycle =>
        result = result + ("completionDuration" -> JsNumber(completionCycle - CycleSource.loadCycle()))
      }
      result
    }
  }

  object OwnedAirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset) : JsValue = {
      var result = AirportAssetWrites.writes(entry).asInstanceOf[JsObject]
      result = result + ("expense" -> JsNumber(entry.expense)) + ("revenue" -> JsNumber(entry.revenue))

      result
    }
  }

  def getAirportAssets(airportId : Int) = Action { request =>
    val assets = AirportAssetSource.loadAirportAssetsByAirport(airportId).map { asset =>
      //for display purpose, set boosts for blueprints as well
      asset.status match {
        case AirportAssetStatus.BLUEPRINT => {
          asset.boosts = asset.blueprint.assetType.baseBoosts
          asset
        }
        case _ => asset
      }
    }.sortBy(_.cost)
    Ok(Json.toJson(assets))
  }

  def getAirportAssetDetails(airlineId : Int, assetId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    AirportAssetSource.loadAirportAssetByAssetId(assetId) match {
      case Some(asset) =>

        asset.airline match {
          case Some(owner) =>
            if (owner.id != airline.id) {
              Forbidden(s"Airline $airline does not own $asset")
            } else {
              var result = Json.toJson(asset)(OwnedAirportAssetWrites).asInstanceOf[JsObject]
              getRejection(airline, asset).foreach { rejection =>
                result = result + ("rejection" -> JsString(rejection))
              }
              Ok(result)
            }
          case None =>
            var result = Json.toJson(asset).asInstanceOf[JsObject]
            getRejection(airline, asset).foreach { rejection =>
              result = result + ("rejection" -> JsString(rejection))
            }
            Ok(result)

        }
      case None => NotFound(s"Asset $assetId is not found")
    }

  }

  def getRejection(airline : Airline, asset : AirportAsset) : Option[String] = {
    asset.airline match {
      case Some(owner) =>
        if (owner.id != airline.id) {
          Some(s"Your airline does not own ${asset.name}")
        } else if (airline.getBalance() < asset.cost) {
          Some(s"Not enough cash to upgrade ${asset.name}")
        } else if (asset.level >= AirportAsset.MAX_LEVEL) {
          Some(s"${asset.name} is already at max level")
        } else {
          None
        }
      case None =>
        airline.getBases().find(_.airport.id == asset.blueprint.airport.id) match {
          case Some(base) =>
            if (airline.getBalance() >= asset.cost) {
              if (base.scale < asset.blueprint.assetType.baseRequirement) {
                Some(s"Requires Airport Base level ${asset.blueprint.assetType.baseRequirement} to build the ${asset.blueprint.assetType.label}")
              } else {
                //only 1 asset per base
                AirportAssetSource.loadAirportAssetsByAirline(airline.id).find(_.blueprint.airport.id == asset.blueprint.airport.id) match {
                  case Some(otherAsset) => Some(s"Cannot build more than 1 asset per airport. Already own ${otherAsset.name}")
                  case None => None //OK
                }
              }
            } else {
              Some(s"Not enough cash to build the ${asset.blueprint.assetType.label}")
            }
          case None => Some(s"Requires Airport Base to build the ${asset.blueprint.assetType.label}")
        }
    }
  }
  def getNameRejection(name : String) : Option[String] = {
    if (name.length() < 1 || name.length() > MAX_NAME_LENGTH) {
      Some("Name should be between 1 - " + MAX_NAME_LENGTH + " characters")
    } else if (!name.forall(char => char.isLetter || char == ' ')) {
      Some("Name can only contain space and characters")
    } else {
      None
    }
  }

  val MAX_NAME_LENGTH = 20


  def deleteAirportAsset(airlineId : Int, assetId : Int)= AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    AirportAssetSource.loadAirportAssetByAssetId(assetId) match {
      case Some(asset) =>
        asset.airline match {
          case Some(owner) =>
            if (owner.id != airline.id) {
              Forbidden(s"Airline $airline does not own $asset")
            } else {
              //OK
              AirportAssetSource.deleteAirportAsset(assetId)
              AirlineSource.adjustAirlineBalance(airline.id, asset.sellValue)
              Ok(Json.toJson(asset)(OwnedAirportAssetWrites))
            }
          case None =>
              Forbidden(s"Airline $airline cannot sell blueprint $asset")
        }
      case None => NotFound(s"Asset $assetId is not found")
    }
  }

  def putAirportAsset(airlineId : Int, assetId : Int)= AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    AirportAssetSource.loadAirportAssetByAssetId(assetId) match {
      case Some(asset) =>
        getRejection(airline, asset) match {
          case Some(rejection) => BadRequest(s"Cannot put $asset by $airline : $rejection")
          case None =>

            val name = request.body.asInstanceOf[AnyContentAsJson].json.asInstanceOf[JsObject].value("name").as[String]
            getNameRejection(name) match {
              case Some(nameRejection) =>  Ok(Json.obj("nameRejection" -> nameRejection))
              case None => //OK
                val newAsset = {
                  asset.airline match {
                    case Some(owner) => asset.levelUp(name)
                    case None => AirportAsset.buildNewAsset(airline, asset.blueprint, name)
                  }
                }

                AirportAssetSource.updateAirportAsset(newAsset)
                AirlineSource.adjustAirlineBalance(airline.id, -1 * newAsset.cost)
                Ok(Json.toJson(newAsset)(OwnedAirportAssetWrites))
            }
        }
      case None =>
        NotFound(s"Asset $assetId is not found")
    }
  }


}
