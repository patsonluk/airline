package controllers

import com.patson.data.{AirlineSource, AirportAssetSource, CycleSource}
import com.patson.model.AirportAssetType.PassengerCostModifier
import com.patson.model._
import com.patson.util.CountryCache
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject

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
        "id" -> entry.id,
        "baseBoosts" -> entry.baseBoosts,
        "publicProperties" -> computeAssetProperties(entry, entry.publicProperties)
      )

      entry.completionCycle.foreach { completionCycle =>
        result = result + ("completionDuration" -> JsNumber(completionCycle - CycleSource.loadCycle()))
      }

      if (entry.isInstanceOf[PassengerCostModifier] && entry.propertyHistoryLastCycle.isDefined) {
        result = result ++ Json.obj(
          "countryRanking" -> entry.paxByCountryCodeLastCycle.toList.sortBy(_._2)(Ordering[Long].reverse).map {
            case (countryCode, paxCount) => Json.obj("countryCode" -> countryCode, "countryName" -> CountryCache.getCountry(countryCode).get.name, "passengerCount" -> paxCount)
          },
          "transitPax" -> entry.transitPaxLastCycle,
          "destinationPax" -> entry.destinationPaxLastCycle)
      }
      result

    }
  }

  object OwnedAirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset) : JsValue = {
      var result = AirportAssetWrites.writes(entry).asInstanceOf[JsObject]
      val performanceApprox = Math.ceil(entry.performance.toDouble / 100 * 5).toInt
      result = result +
        ("expense" -> JsNumber(entry.expense)) +
        ("revenue" -> JsNumber(entry.revenue)) +
        ("performanceApprox" -> JsNumber(performanceApprox)) + //don't show the actual value. more fun if we hide some details :)
        ("privateProperties" -> Json.toJson(computeAssetProperties(entry, entry.privateProperties)))



      result
    }
  }

  implicit object AirportBoostHistoryWrites extends Writes[AirportAssetBoostHistory] {
    def writes(entry : AirportAssetBoostHistory): JsValue = {
      Json.obj(
        "level" -> entry.level,
        "boostType" -> entry.boostType.toString,
        "label" -> AirportBoostType.getLabel(entry.boostType),
        "value" ->  entry.value,
        "gain" -> entry.gain,
        "upgradeFactor" -> entry.upgradeFactor
      )
    }
  }




  def computeAssetProperties(asset : AirportAsset, rawProperties : Map[String, Long]) : Map[String, String] = {
    val result = collection.mutable.Map[String, String]()
    val formatter = java.text.NumberFormat.getIntegerInstance
    asset match {
      case hotel : HotelAsset =>
        rawProperties.get("occupancy").foreach { occupancy =>
          val occupancyString = formatter.format(occupancy) + " (" + (occupancy * 100 / hotel.capacity) + "%)"
          result.put("Occupancy", occupancyString)
        }
        rawProperties.get("rate").foreach { rate =>
          result.put("Room Rate", "$" + formatter.format(rate))
        }
      case asset : AdmissionAsset =>
        rawProperties.get("visitors").foreach { visitors =>
          val valueString = formatter.format(visitors)
          result.put("Visitors", valueString)
        }
        rawProperties.get("rate").foreach { rate =>
          result.put("Ticket Price", "$" + formatter.format(rate))
        }
      case asset : RentalAsset =>
        rawProperties.get("leasedSpace").foreach { leasedSpace =>
          val valueString = formatter.format(leasedSpace) + " (" + (leasedSpace * 100 / asset.space) + "%)"
          result.put("Leased Floor Space", valueString)
        }
        rawProperties.get("rate100Point").foreach { rate100Point =>
          result.put("Monthly Rent/sq. ft ", "$" + rate100Point.toDouble / 100)
        }
      case _ =>

    }
    result.toMap
  }

  def getAirportAssets(airportId : Int) = Action { request =>
    val assets = AirportAssetSource.loadAirportAssetsByAirport(airportId).map { asset =>

      asset.status match {
        case AirportAssetStatus.BLUEPRINT => { //for display purpose, set boosts for blueprints as well
          asset.boosts = asset.baseBoosts
          asset
        }
        case AirportAssetStatus.UNDER_CONSTRUCTION => { //for display purpose, if level 1, display the blueprint boosts
          if (asset.level == 1) {
            asset.boosts = asset.baseBoosts
            asset
          } else { //otherwise just display current boosts
            asset
          }
        }
        case _ => asset
      }
    }.sortBy(_.cost)
    Ok(Json.toJson(assets))
  }

  def getAirportAssetsWithAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val assets = AirportAssetSource.loadAirportAssetsByAirline(airlineId)
    Ok(Json.toJson(assets)(Writes.list(OwnedAirportAssetWrites)))
  }

  def getAirportAssetDetailsWithoutAirline(assetId : Int) = Action { request =>
    getAirportAssetDetails(None, assetId)
  }

  def getAirportAssetDetailsWithAirline(airlineId : Int, assetId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    getAirportAssetDetails(Some(airline), assetId)
  }

  private[this] def getAirportAssetDetails(airlineOption : Option[Airline], assetId : Int) = {
    AirportAssetSource.loadAirportAssetByAssetId(assetId) match {
      case Some(asset) =>

        asset.airline match {
          case Some(owner) =>
            var result : JsObject =
              if (airlineOption.isEmpty || owner.id != airlineOption.get.id) {
                Json.toJson(asset).asInstanceOf[JsObject]
              } else {
                var ownerResult = Json.toJson(asset)(OwnedAirportAssetWrites).asInstanceOf[JsObject]
                getUpgradeRejection(airlineOption.get, asset).foreach { rejection =>
                  ownerResult = ownerResult + ("rejection" -> JsString(rejection))
                }
                getDowngradeRejection(airlineOption.get, asset).foreach { rejection =>
                  ownerResult = ownerResult + ("downgradeRejection" -> JsString(rejection))
                }

                ownerResult
              }
            //load boost history
            result = result + ("boostHistory" -> Json.toJson(AirportAssetSource.loadAirportBoostHistoryByAssetId(assetId).sortBy(_.boostType.id).sortBy(_.level)(Ordering.Int.reverse)))

            Ok(result)
          case None =>
            var result = Json.toJson(asset).asInstanceOf[JsObject]
            airlineOption.foreach { airline =>
              getUpgradeRejection(airline, asset).foreach { rejection =>
                result = result + ("rejection" -> JsString(rejection))
              }
            }
            Ok(result)
        }
      case None => NotFound(s"Asset $assetId is not found")
    }
  }

  /**
   * Get rejection of building/upgrading the asset
   * @param airline
   * @param asset
   * @return
   */
  def getUpgradeRejection(airline : Airline, asset : AirportAsset) : Option[String] = {
    asset.airline match {
      case Some(owner) =>
        if (owner.id != airline.id) {
          Some(s"Your airline does not own ${asset.name}")
        } else if (airline.getBalance() < asset.cost) {
          Some(s"Not enough cash to upgrade ${asset.name}")
        } else if (asset.level >= AirportAsset.MAX_LEVEL) {
          Some(s"${asset.name} is already at max level")
        } else {
          val cooldownDelta = asset.completionCycle.get + asset.assetType.upgradeCooldown - CycleSource.loadCycle()
          if (cooldownDelta > 0) {
            Some(s"${asset.name} can only be upgraded again in $cooldownDelta week(s)")
          } else {
            None
          }
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


  def getDowngradeRejection(airline : Airline, asset : AirportAsset) : Option[String] = {
    val owner = asset.airline.get
    if (owner.id != airline.id) {
      Some(s"Your airline does not own ${asset.name}")
    } else if (asset.status != AirportAssetStatus.COMPLETED) {
      Some(s"Cannot downgrade while asset is under construction")
    } else if (asset.level <= 1) {
      Some(s"Cannot downgrade any further")
    } else {
      None
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

  val MAX_NAME_LENGTH = 30


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
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airline.id, CashFlowType.ASSET_TRANSACTION, asset.sellValue))
              Ok(Json.toJson(asset)(OwnedAirportAssetWrites))
            }
          case None =>
              Forbidden(s"Airline $airline cannot sell blueprint $asset")
        }
      case None => NotFound(s"Asset $assetId is not found")
    }
  }

  def downgradeAirportAsset(airlineId : Int, assetId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    AirportAssetSource.loadAirportAssetByAssetId(assetId) match {
      case Some(asset) =>
        asset.airline match {
          case Some(owner) =>
            if (owner.id != airline.id) {
              Forbidden(s"Airline $airline does not own $asset")
            } else  {
              getDowngradeRejection(airline, asset) match {
                case Some(rejection) => BadRequest(s"Rejected: $rejection")
                case None => //OK
                  val name = request.body.asInstanceOf[AnyContentAsJson].json.asInstanceOf[JsObject].value("name").as[String]
                  getNameRejection(name) match {
                    case Some(nameRejection) => Ok(Json.obj("nameRejection" -> nameRejection))
                    case None => //OK
                      val newAsset = asset.levelDown(name)
                      AirportAssetSource.updateAirportAsset(newAsset)
                      Ok(Json.toJson(newAsset)(OwnedAirportAssetWrites))
                  }
              }
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
        getUpgradeRejection(airline, asset) match {
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
                AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airline.id, CashFlowType.ASSET_TRANSACTION, -1 * newAsset.cost))
                Ok(Json.toJson(newAsset)(OwnedAirportAssetWrites))
            }
        }
      case None =>
        NotFound(s"Asset $assetId is not found")
    }
  }


}
