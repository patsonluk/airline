package controllers

import java.util.Random

import com.patson.data.{AirlineSource, AirplaneSource, AirportSource, BankSource, CycleSource, TutorialSource}
import com.patson.model._
import com.patson.model.airplane._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import models.Profile
import play.api.libs.json.{JsValue, Json, _}
import play.api.mvc._

import scala.collection.mutable.ListBuffer

class ProfileApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object ProfileWrites extends Writes[Profile] {
    def writes(profile: Profile): JsValue = {
      var result =Json.obj(
        "name" -> profile.name,
        "description" -> profile.description,
        "cash" -> profile.cash,
        "airplanes" -> profile.airplanes,
        "awareness" -> profile.awareness,
        "reputation" -> profile.reputation,
        "airportText" -> profile.airport.displayText)
      profile.loan.foreach { loan =>
        result = result + ("loan" -> Json.toJson(loan)(new LoanWrites(CycleSource.loadCycle())))
      }
      result
    }
  }

  val BASE_CAPITAL = 40000000
  val BONUS_PER_DIFFICULTY_POINT = 1000000

  def generateAirplanes(value : Int, capacityRange : scala.collection.immutable.Range, homeAirport : Airport, condition : Double, airline : Airline, random : Random) : List[Airplane] =  {
    val eligibleModels = allAirplaneModels.filter(model => capacityRange.contains(model.capacity))
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((homeAirport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= value / 3)
      .filter(model => model.runwayRequirement <= homeAirport.runwayLength)
    val countryModels = eligibleModels.filter(_.countryCode == homeAirport.countryCode)
    val currentCycle = CycleSource.loadCycle()

    val selectedModel =
      if (eligibleModels.isEmpty) {
        None
      } else {
        if (!countryModels.isEmpty) { //always go for airplanes from this country first
          Some(countryModels(random.nextInt(countryModels.length)))
        } else {
          Some(eligibleModels(random.nextInt(eligibleModels.length)))
        }
      }
    selectedModel match {
      case Some(model) =>
        val amount = value / model.price
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)  //not really that useful, just to fake a more reasonable number
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map(_ => Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0, value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = homeAirport)).toList
      case None =>
        List.empty
    }
  }

  val BASE_INTEREST_RATE = 0.1 //10%

  def generateProfiles(airline : Airline, airport : Airport) : List[Profile] = {
    val difficulty = AirportRating.rateAirport(airport).overallDifficulty
    val capital = BASE_CAPITAL + difficulty * BONUS_PER_DIFFICULTY_POINT

    val profiles = ListBuffer[Profile]()
    val cashProfile = Profile(name = "Entrepreneurial spirit", description = "You have sold all your assets to create this new airline of your dream! Plan carefully but make bold moves to thrive in this brave new world.", cash = capital, airport = airport)
    profiles.append(cashProfile)
    val random = new Random(airport.id)
    val smallAirplanes = generateAirplanes(capital, (15 to 50), airport, 90, airline, random)
    if (!smallAirplanes.isEmpty) {
      val smallAirplaneProfile = Profile(
        name = "A humble beginning",
        description = "A newly acquired airline with a modest aircraft fleet of young age. Grow this humble airline into the most powerful and respected brand in the aviation world!",
        cash = (capital * 1.5).toInt - smallAirplanes.map(_.value).sum +  difficulty * BONUS_PER_DIFFICULTY_POINT / 2,
        airport = airport,
        awareness = 20,
        reputation = 10,
        airplanes = smallAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 1).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))


      profiles.append(smallAirplaneProfile)
    }

    val largeAirplanes = generateAirplanes(capital * 3, (70 to 200), airport, 70, airline, random)
    if (!largeAirplanes.isEmpty) {
      val largeAirplaneProfile = Profile(
        name = "Revival of past glory",
        description = "An airline that has previously over-expanded by mismanagement of now retired CEO. It is left with some aging airplanes and heavy debt. Can you turn this airline around?",
        cash = (capital * 4).toInt - largeAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT,
        airport = airport,
        awareness = 50,
        reputation = 25,
        airplanes = largeAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 3.5).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(largeAirplaneProfile)
    }

    profiles.toList
  }

  def getProfiles(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    request.user.getHeadQuarter() match {
      case Some(headquarters) =>
        BadRequest("Cannot select profile with active HQ")
      case None =>
        Ok(Json.toJson(generateProfiles(request.user, AirportCache.getAirport(airportId, true).get)))
    }

  }

  def buildHqWithProfile(airlineId : Int, airportId : Int, profileId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airport = AirportCache.getAirport(airportId, true).get
    val airline = request.user
    val profile = generateProfiles(airline, airport)(profileId)

    val base = AirlineBase(airline, airport, airport.countryCode, 1, CycleSource.loadCycle(), true)
    AirlineSource.saveAirlineBase(base)
    airline.setCountryCode(airport.countryCode)
    airline.setReputation(profile.reputation)
    airline.setBalance(profile.cash)
    AirportSource.updateAirlineAppeal(airportId, airlineId, AirlineAppeal(loyalty = 0, awareness = profile.awareness))

    profile.airplanes.foreach(_.assignDefaultConfiguration())
    AirplaneSource.saveAirplanes(profile.airplanes)

    profile.loan.foreach { loan =>
      BankSource.saveLoan(loan)
    }

    airline.setInitialized(true)
    AirlineSource.saveAirlineInfo(airline, true)
    val updatedAirline = AirlineSource.loadAirlineById(airlineId, true)

    Ok(Json.toJson(updatedAirline))
  }
}
