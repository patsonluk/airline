package controllers

import com.patson.data.{AirlineSource, AirportSource, CountrySource}
import com.patson.model._
import com.patson.util.{AirlineCache, ChampionUtil, CountryCache}
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.math.BigDecimal.int2bigDecimal


class CountryApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def getAllCountries(airlineId : Option[Int]) = Action {
    val countries = CountrySource.loadAllCountries()

    airlineId match {
      case None => Ok(Json.toJson(countries))
      case Some(airlineId) => {
        AirlineCache.getAirline(airlineId, true) match {
          case None => Ok(Json.toJson(countries))
          case Some(airline) =>
            val baseCountByCountryCode = airline.getBases().groupBy(_.countryCode).view.mapValues(_.length)
            var result = Json.arr()

            val mutualRelationships: Map[String, Int] =
              airline.getCountryCode() match {
                case Some(homeCountryCode) => CountrySource.getCountryMutualRelationships(homeCountryCode)
                case None => Map.empty[String, Int]
              }

            val delegatesByCountryCode = airline.getDelegateInfo().busyDelegates.filter(_.assignedTask.getTaskType == DelegateTaskType.COUNTRY).map(_.assignedTask.asInstanceOf[CountryDelegateTask]).groupBy(_.country.countryCode)

            countries.foreach { country =>
              var countryJson : JsObject = Json.toJson(country).asInstanceOf[JsObject]
              baseCountByCountryCode.get(country.countryCode).foreach { baseCount =>
                countryJson = countryJson + ("baseCount" -> JsNumber(baseCount))
              }
              delegatesByCountryCode.get(country.countryCode).foreach { delegatesAssignedToThisCountry =>
                countryJson = countryJson + ("delegatesCount" -> JsNumber(delegatesAssignedToThisCountry.length))

              }

              if (airline.getHeadQuarter().isDefined) {
                val relationship : Int = mutualRelationships.get(country.countryCode).getOrElse(0)
                countryJson = countryJson + ("mutualRelationship" -> JsNumber(relationship))
              }

              result = result.append(countryJson)
            }

            Ok(result)
        }
      }
    }
  }

  def getCountryAirlineDetails(countryCode : String, airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, request.user)

    var result = Json.obj("relationship" -> relationship)
    val title = CountryAirlineTitle.getTitle(countryCode, request.user)
    result = result + ("title" -> Json.toJson(title))

    Ok(result)
  }

  def getCountry(countryCode : String) = Action {
    getCountryJson(countryCode) match {
      case Some(jsonObject) =>
        Ok(jsonObject)
      case None => NotFound
    } 
  }



  def getCountryAirlineTitleProgression(countryCode : String) = Action {
    var result = Json.arr()
    CountryCache.getCountry(countryCode) match {
      case Some(country) =>
        Title.values.toList.sortBy(_.id).reverse.foreach { title =>
          val titleJson = Json.obj("title" -> title.toString,
            "description" -> Title.description(title),
            "requirements" -> CountryAirlineTitle.getTitleRequirements(title, country),
            "bonus" -> CountryAirlineTitle.getTitleBonus(title, country))
          result = result.append(titleJson)
        }

        Ok(result)
      case None => NotFound
    }

  }

  def getCountryJson(countryCode : String) : Option[JsObject] = {
    CountryCache.getCountry(countryCode).map { country =>
      var jsonObject: JsObject = Json.toJson(country).asInstanceOf[JsObject]
      val airports = AirportSource.loadAirportsByCountry(countryCode)
      val smallAirportCount = airports.count { airport => airport.size <= 2 }
      val mediumAirportCount = airports.count { airport => airport.size >= 3 && airport.size <= 4 }
      val largeAirportCount = airports.count { airport => airport.size >= 5 }

      val allBases = AirlineSource.loadAirlineBasesByCountryCode(countryCode)

      val (headquarters, bases) = allBases.partition {
        _.headquarter
      }

      jsonObject = jsonObject.asInstanceOf[JsObject] ++
        Json.obj("smallAirportCount" -> smallAirportCount,
          "mediumAirportCount" -> mediumAirportCount,
          "largeAirportCount" -> largeAirportCount,
          "headquartersCount" -> headquarters.length,
          "basesCount" -> bases.length)
      //val allAirlines = AirlineSource.loadAllAirlines(false).map(airline => (airline.id, airline)).toMap
      CountrySource.loadMarketSharesByCountryCode(countryCode).foreach { marketShares => //if it has market share data
        val champions = ChampionUtil.getCountryChampionInfoByCountryCode(countryCode).sortBy(_.ranking)
        var championsJson = Json.toJson(champions)
        //        var x = 0
        //        for (x <- 0 until champions.size) {
        //          val airline = allAirlines(champions(x)._1)
        //          val passengerCount = champions(x)._2
        //          val ranking = x + 1
        //          val reputationBoost = Computation.computeReputationBoost(country, ranking)
        //          championsJson = championsJson :+ Json.obj("airline" -> Json.toJson(airline), "passengerCount" -> JsNumber(passengerCount), "ranking" -> JsNumber(ranking), "reputationBoost" -> JsNumber(reputationBoost))
        //        }

        jsonObject = jsonObject.asInstanceOf[JsObject] + ("champions" -> championsJson)

        var nationalAirlinesJson = Json.arr()
        var partneredAirlinesJson = Json.arr()
        CountryAirlineTitle.getTopTitlesByCountry(countryCode).foreach {
          case countryAirlineTitle =>
            val CountryAirlineTitle(country, airline, title) = countryAirlineTitle
            val share: Long = marketShares.airlineShares.getOrElse(airline.id, 0L)
            title match {
              case Title.NATIONAL_AIRLINE =>
                nationalAirlinesJson = nationalAirlinesJson.append(Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "passengerCount" -> share, "loyaltyBonus" -> countryAirlineTitle.loyaltyBonus))
              case Title.PARTNERED_AIRLINE =>
                partneredAirlinesJson = partneredAirlinesJson.append(Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "passengerCount" -> share, "loyaltyBonus" -> countryAirlineTitle.loyaltyBonus))
            }
        }

        jsonObject = jsonObject + ("nationalAirlines" -> nationalAirlinesJson) + ("partneredAirlines" -> partneredAirlinesJson)

        jsonObject = jsonObject.asInstanceOf[JsObject] + ("marketShares" -> Json.toJson(marketShares.airlineShares.map {
          case ((airlineId, passengerCount)) => (AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId)), passengerCount)
        }.toList)(AirlineSharesWrites))
      }
      jsonObject
    }

  }

  object AirlineSharesWrites extends Writes[List[(Airline, Long)]] {
    def writes(shares: List[(Airline, Long)]): JsValue = {
      var jsonArray = Json.arr()
      shares.foreach { share =>
        jsonArray = jsonArray :+ JsObject(List(
        "airlineId" -> JsNumber(share._1.id),
        "airlineName" -> JsString(share._1.name),
        "passengerCount" -> JsNumber(share._2)
        ))
      }
      jsonArray
    }
  }
}
