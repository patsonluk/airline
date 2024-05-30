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
        "quality" -> profile.quality,
        "airplanes" -> profile.airplanes,
        "reputation" -> profile.reputation,
        "airportText" -> profile.airport.displayText)
      profile.loan.foreach { loan =>
        result = result + ("loan" -> Json.toJson(loan)(new LoanWrites(CycleSource.loadCycle())))
      }
      result
    }
  }

  val BASE_CAPITAL = 80000000
  val BONUS_PER_DIFFICULTY_POINT = 1350000

  def generateAirplanes(value : Int, capacityRange : scala.collection.immutable.Range, quality : Double, homeAirport : Airport, condition : Double, airline : Airline, random : Random) : List[Airplane] =  {
    val eligibleModels = allAirplaneModels.filter(model => capacityRange.contains(model.capacity))
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((homeAirport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= value / 3)
      .filter(model => model.runwayRequirement <= homeAirport.runwayLength)
      .filter(model => model.quality <= quality + 0.5 && model.quality >= quality - 0.5)
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
        val amount = Math.min(value / model.price, 9)
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)  //not really that useful, just to fake a more reasonable number
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map(_ => Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0, value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = homeAirport)).toList
      case None =>
        List.empty
    }
  }

  val BASE_INTEREST_RATE = 0.1 //10%

  def generateProfiles(airline : Airline, airport : Airport) : List[Profile] = {
    val difficulty = airport.rating.overallDifficulty
    val capital = BASE_CAPITAL + difficulty * BONUS_PER_DIFFICULTY_POINT

    val profiles = ListBuffer[Profile]()

    val cashProfile = Profile(name = "Simple start", description = "You have sold all your assets to start the airline of your dreams! Recommended for new players.", cash = capital, airport = airport)
    profiles.append(cashProfile)

    val random = new Random(airport.id)
    val smallAirplanes = generateAirplanes(capital, (30 to 90), 2.5, airport, 90, airline, random)
    if (!smallAirplanes.isEmpty) {
      val smallAirplaneProfile = Profile(
        name = "A humble beginning",
        description = "A newly acquired airline with a modest fleet of almost-new aircraft. Grow this humble airline into the most powerful and respected brand in the aviation world!",
        cash = (capital * 1.7).toInt - smallAirplanes.map(_.value).sum +  difficulty * BONUS_PER_DIFFICULTY_POINT / 2,
        airport = airport,
        reputation = 10,
        quality = 15,
        airplanes = smallAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 1).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(smallAirplaneProfile)
    }

    val loanProfile = Profile(
      name = "Entrepreneurial spirit",
      description = "You and the bank are betting big that there's money in commercial aviation! Plan carefully but make bold moves to thrive in this brave new world!",
      cash = capital * 3,
      airport = airport,
      loan = Some(Bank.getLoanOptions((capital * 2.5).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id))
    )
    profiles.append(loanProfile)

    val largeAirplanes = generateAirplanes((capital * 4).toInt, (60 to 150), 2.0, airport, 70, airline, random)
    if (!largeAirplanes.isEmpty) {
      val largeAirplaneProfile = Profile(
        name = "Revival of past glory",
        description = "A once great airline now saddled with debt and aging airplanes. Can you turn this airline around?",
        cash = (capital * 4.6).toInt - largeAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT,
        airport = airport,
        reputation = 25,
        quality = 0,
        airplanes = largeAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 4).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(largeAirplaneProfile)
    }

    val cheapAirplanes = generateAirplanes((capital * 2.75).toInt, (32 to 150), 0.5, airport, 80, airline, random)
    if (!cheapAirplanes.isEmpty) {
      val cheapAirplaneProfile = Profile(
        name = "Economy Gamble",
        description = "You found a great deal on very low quality planes. Perfect to pack in the masses!",
        cash = (capital * 3.25).toInt - cheapAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT,
        airport = airport,
        reputation = 30,
        quality = 0,
        airplanes = cheapAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 2.25).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(cheapAirplaneProfile)
    }

    val fancyAirplanes = generateAirplanes((capital * 2.25).toInt, (0 to 80), 4.5, airport, 85, airline, random)
    if (!fancyAirplanes.isEmpty) {
      val fancyAirplaneProfile = Profile(
        name = "Luxury Startup",
        description = "A highly motivated team with high quality aircraft. Perfect for premium service!",
        cash = (capital * 2.75).toInt - fancyAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT,
        airport = airport,
        reputation = 25,
        quality = 99,
        airplanes = fancyAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 2).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(fancyAirplaneProfile)
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

  private[this] val buildHqWithProfileLock = new Object()
  def buildHqWithProfile(airlineId : Int, airportId : Int, profileId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    buildHqWithProfileLock.synchronized {
      if (!airline.isInitialized) {
        val airport = AirportCache.getAirport(airportId, true).get
        val profile = generateProfiles(airline, airport)(profileId)

        val base = AirlineBase(airline, airport, airport.countryCode, 1, CycleSource.loadCycle(), true)
        AirlineSource.saveAirlineBase(base)
        airline.setCountryCode(airport.countryCode)
        airline.setReputation(profile.reputation)
        airline.setCurrentServiceQuality(profile.quality)
        airline.setTargetServiceQuality(35)
        airline.setBalance(profile.cash)
        AirportSource.updateAirlineAppeal(airportId, airlineId, AirlineAppeal(loyalty = 0))

        profile.airplanes.foreach(_.assignDefaultConfiguration())
        AirplaneSource.saveAirplanes(profile.airplanes)

        profile.loan.foreach { loan =>
          BankSource.saveLoan(loan)
        }

        airline.setInitialized(true)
        AirlineSource.saveAirlineInfo(airline, true)
        val updatedAirline = AirlineSource.loadAirlineById(airlineId, true)

        Ok(Json.toJson(updatedAirline))
      } else {
        BadRequest(s"${request.user} was already initialized")
      }
    }
  }
}
