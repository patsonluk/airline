package controllers

import com.patson.data.{AirportSource, CampaignSource, CycleSource, DelegateSource}
import com.patson.model.{Airport, BusyDelegate, CampaignDelegateTask, Computation, DelegateTask}
import com.patson.model.campaign._
import com.patson.util.{AirportCache, CountryCache}
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode

class CampaignApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object CampaignWrites extends Writes[Campaign] {
    def writes(entry : Campaign): JsValue = {
      //val countryCode : String = ranking.airline.getCountryCode().getOrElse("")
      Json.obj(
        "principalAirport" -> entry.principalAirport,
        "radius" -> entry.radius,
        "population" ->  entry.populationCoverage,
        "area" -> entry.area,
        "id" -> entry.id
      )
    }
  }

  implicit object CampaignDetailsWrites extends Writes[CampaignDetails] {
    def writes(entry : CampaignDetails): JsValue = {
      var delegatesJson = Json.arr()
      val currentCycle = CycleSource.loadCycle()
      val taskWrites = new CampaignDelegateTaskWrites(currentCycle)
      entry.delegateTasks.foreach { task =>
        delegatesJson = delegatesJson.append(Json.toJson(task)(taskWrites))
      }

      Json.toJson(entry.campaign).as[JsObject] + ("delegates" -> delegatesJson) + ("level" -> JsNumber(entry.delegateTasks.map(_.level(currentCycle)).sum))
    }
  }

  class CampaignDelegateTaskWrites(currentCycle : Int) extends Writes[CampaignDelegateTask] {
    def writes(entry : CampaignDelegateTask): JsValue = {
      var result = Json.obj(
        "level" -> entry.level(currentCycle),
        "description" -> entry.description,
        "levelDescription" -> entry.levelDescription(currentCycle),
      )

      entry.nextLevelCycleCount(currentCycle).foreach {
        value => result = result + ("nextLevelCycleCount" -> JsNumber(value))
      }
      result
    }
  }


  def getCampaigns(airlineId : Int, fullLoad : Boolean) = AuthenticatedAirline(airlineId) { request =>
    val campaigns = CampaignSource.loadCampaignsByCriteria(List(("airline", airlineId)), loadArea = fullLoad)
    val result = DelegateSource.loadBusyDelegatesByCampaigns(campaigns).map {
      case (campaign, delegates) => CampaignDetails(campaign, delegates.map(_.assignedTask.asInstanceOf[CampaignDelegateTask]))
    }

    Ok(Json.toJson(result))
  }

  val MAX_CAMPAIGN_RADIUS = 1000
  val MIN_CAMPAIGN_RADIUS = 100
  def getCampaignAirports(airlineId : Int, airportId : Int, radius : Int) = AuthenticatedAirline(airlineId) { request =>
    AirportCache.getAirport(airportId) match {
      case Some(principalAirport) =>
        val areaAirports = ListBuffer[Airport]()
        val candidateAirports = ListBuffer[Airport]()
        val candidateRadius = Math.min(MAX_CAMPAIGN_RADIUS, radius + 200)
        AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
          if (Computation.calculateDistance(principalAirport, airport) <= radius) {
            areaAirports.append(airport)
          } else if (Computation.calculateDistance(principalAirport, airport) <= candidateRadius){
            candidateAirports.append(airport)
          }
        }
        val population = areaAirports.map(_.population).sum
        val bonus = Campaign.getAirlineBonus(population, 1)
        val awarenessBonus = BigDecimal(bonus.awareness).setScale(2, RoundingMode.HALF_UP)
        val loyaltyBonus = BigDecimal(bonus.loyalty).setScale(2, RoundingMode.HALF_UP)

        Ok(Json.obj(
          "principalAirport" -> principalAirport,
          "radius" -> radius,
          "area" -> areaAirports.toList,
          "population" ->  population,
          "candidateArea" -> candidateAirports.toList,
          "bonus" -> Json.obj("awareness" -> awarenessBonus, "loyalty" -> loyaltyBonus),
          "costPerDelegate" -> CampaignDelegateTask.cost(principalAirport.income)))
      case None => NotFound(s"airport with id $airportId not found")
    }
  }

  def saveCampaign(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val radius = request.body.asInstanceOf[AnyContentAsJson].json.\("radius").as[Int]
    if (radius < MIN_CAMPAIGN_RADIUS || radius > MAX_CAMPAIGN_RADIUS) {
      BadRequest(s"Invalid radius $radius")
    } else {
      val delegateCount = request.body.asInstanceOf[AnyContentAsJson].json.\("delegateCount").as[Int]
      val savedCampaignResult : Either[Result, Campaign] = request.body.asInstanceOf[AnyContentAsJson].json.\("campaignId").asOpt[Int] match {
        case Some(campaignId) => { //update
          CampaignSource.loadCampaignById(campaignId) match {
            case Some(campaign) =>
              if (campaign.airline.id != airlineId) {
                Left(BadRequest(s"Trying to modify campaign with $campaignId that is NOT from this airline $airline"))
              } else {
                val newArea = getAreaAirportsInRadius(campaign.principalAirport, radius)
                val newPopulationCoverage = newArea.map(_.population).sum
                CampaignSource.updateCampaign(campaign.copy(radius = radius, populationCoverage = newPopulationCoverage, area = newArea))
                Right(campaign)
              }
            case None => Left(NotFound(s"Campaign with id $campaignId not found"))
          }
        }
        case None => { //new
          val principalAirportId = request.body.asInstanceOf[AnyContentAsJson].json.\("airportId").as[Int]
          AirportCache.getAirport(principalAirportId) match {
            case Some(principalAirport) =>
              val newArea = getAreaAirportsInRadius(principalAirport, radius)
              val newPopulationCoverage = newArea.map(_.population).sum
              val newCampaign = Campaign(airline, principalAirport, radius, newPopulationCoverage, newArea)
              CampaignSource.saveCampaign(newCampaign)
              Right(newCampaign)
            case None => Left(BadRequest(s"Airport $principalAirportId not found!"))
          }

        }
      }

      savedCampaignResult match {
        case Left(badResult) => badResult
        case Right(campaign) =>
          //save delegates
          val existingDelegates : List[BusyDelegate] = DelegateSource.loadBusyDelegatesByCampaigns(List(campaign)).getOrElse(campaign, List.empty).sortBy(_.assignedTask.getStartCycle)
          val delta = delegateCount - existingDelegates.length
          if (delegateCount >= 0 && delta <= airline.getDelegateInfo().availableCount) {
            if (delta < 0) { //unassign the most junior ones first
              existingDelegates.takeRight(delta * -1).foreach { unassigningDelegate =>
                DelegateSource.deleteBusyDelegateByCriteria(List(("id", "=", unassigningDelegate.id)))
              }
            } else if (delta > 0) {
              val delegateTask = DelegateTask.campaign(CycleSource.loadCycle(), campaign)
              val newDelegates = (0 until delta).map(_ => BusyDelegate(airline, delegateTask, None))

              DelegateSource.saveBusyDelegates(newDelegates.toList)
            }
            Ok(Json.obj())
          } else {
            BadRequest(s"Invalid delegate value $delegateCount")
          }
      }
    }
  }


  def deleteCampaign(airlineId : Int, campaignId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    CampaignSource.loadCampaignById(campaignId) match {
      case Some(campaign) =>
        if (campaign.airline.id != airlineId) {
          BadRequest(s"Trying to modify campaign with $campaignId that is NOT from this airline $airline")
        } else {
          CampaignSource.deleteCampaign(campaignId)
          Ok(Json.obj())
        }
      case None => NotFound(s"Campaign with id $campaignId not found")
    }
  }

  def getAreaAirportsInRadius(principalAirport : Airport, radius : Int) = {
    val areaAirports = ListBuffer[Airport]()

    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(principalAirport, airport) <= radius) {
        areaAirports.append(airport)
      }
    }
    areaAirports.toList
  }



  case class CampaignDetails(campaign: Campaign, delegateTasks : List[CampaignDelegateTask])

}
