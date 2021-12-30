package controllers

import com.patson.data.{AirportAssetSource, AirportSource, CampaignSource, CycleSource, DelegateSource}
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
  implicit object AirportAssetBoostWrites extends Writes[AirportBoost] {
    def writes(entry : AirportBoost): JsValue = {
      Json.obj(
        "boostType" -> entry.boostType.toString,
        "value" -> entry.value,
      )
    }
  }

  implicit object AirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset): JsValue = {
      val name = entry.status match {
        case AirportAssetStatus.BLUEPRINT => entry.assetType.label
        case _ => entry.name
      }
      Json.obj(
        "airline" -> entry.airline,
        "airport" -> entry.blueprint.airport,
        "assetType" ->  entry.assetType,
        "level" -> entry.level,
        "name" -> name,
        "status" -> entry.status.toString,
        "boosts" -> entry.boosts,
        "id" -> entry.id
      )
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
    }
    Ok(Json.toJson(assets))
  }
}
