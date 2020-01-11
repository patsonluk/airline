package controllers

import com.patson.data.{AirportSource, AlertSource, ChristmasSource}
import com.patson.model._
import com.patson.model.christmas._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._



class ChristmasApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val possibleAirports = cachedAirportsByPower.filter(_.size >=  SantaClausInfo.AIRPORT_SIZE_THRESHOLD).map(_.id) //only top 300 airports are valid targets
  /**
    * Returns whether this airport is a valid target for Santa claus, and whether this airline is eligble for playing finding santa claus
    *
    * if yes, return attempts left on this airline, and whether it's found already
    *
    * If no, return empty result
    *
     * @param airportId
    * @param airlineId
    * @return
    */
  def getAttemptStatus(airportId: Int, airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    //only if it's one of the possible airports
    if (possibleAirports.contains(airportId)) {
      //only get info if there's a HQ
      val airline = request.user
      airline.getHeadQuarter() match {
        case Some(_) => ChristmasSource.loadSantaClausInfoByAirline(airline.id) match {
          case Some(entry) =>
            var result = Json.obj("found" -> entry.found, "attemptsLeft" -> entry.attemptsLeft)
            entry.pickedAward.foreach { pickedOption =>
              val pickedAward = SantaClausAward.getRewardByType(entry, pickedOption)
              result = result + ("pickedAwardDescription" -> JsString(pickedAward.description))
            }
            var guessesJson = Json.arr()
            entry.guesses.foreach { guess =>
              guessesJson = guessesJson.append(
                Json.obj(
                  "airportId" -> JsNumber(guess.airport.id),
                  "airportName" -> JsString(guess.airport.name),
                  "airportCode" -> JsString(guess.airport.iata),
                  "city" -> JsString(guess.airport.city),
                  "distanceText" -> JsString(getDistanceText(Computation.calculateDistance(entry.airport, guess.airport)))))
            }
            result = result + ("guesses" -> guessesJson)
            Ok(result)
          case None => Ok(Json.obj())
        }
        case None => Ok(Json.obj())
      }
    } else {
      Ok(Json.obj())
    }
  }


  /**
    * Make a guess
    *
    * if found, return found = true
    *
    * If not found, return found = false and description text on the distance
    *
    * @param airportId
    * @param airlineId
    * @return
    */
  def makeGuess(airportId: Int, airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    //only if it's one of the possible airports
    val airline = request.user
    ChristmasSource.loadSantaClausInfoByAirline(airline.id) match {
      case Some(entry) => {
        //check if there're any attempts left!
        if (entry.attemptsLeft <= 0) {
          BadRequest(Json.obj())
        } else {
          val santaClausAirport = entry.airport
          val selectedAirport = AirportCache.getAirport(airportId).get

          //check if it's a match!
          var found = false
          if (santaClausAirport.id == airportId) { //yay
            found = true
          }
          val newAttemptsLeft = entry.attemptsLeft - 1

          //update
          ChristmasSource.updateSantaClausInfo(entry.copy(attemptsLeft = newAttemptsLeft, found = found))
          ChristmasSource.saveSantaClausGuess(SantaClausGuess(selectedAirport, airline))

          if (found) {
            Ok(Json.obj("found" -> true, "attemptsLeft" -> entry.attemptsLeft))
          } else { //calculate how far
            Ok(Json.obj("found" -> false, "attemptsLeft" -> entry.attemptsLeft, "distanceText" -> getDistanceText(Computation.calculateDistance(santaClausAirport, selectedAirport))))
          }
        }
      }
      case None => NotFound
    }
  }

  def getAwardOptions(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    val airline = request.user
    ChristmasSource.loadSantaClausInfoByAirline(airline.id) match {
      case Some(entry) => {
        if (!entry.found || entry.pickedAward.isDefined) {
          BadRequest("Either not found yet or reward has already been redeemed!")
        } else {
          var result = Json.arr()
          SantaClausAward.getAllRewards(entry).foreach { option =>
            result = result.append(Json.obj("id" -> option.getType.id, "description" -> option.description))
          }
          Ok(result)
        }
      }
      case None => NotFound
    }
  }

  def pickAwardOption(airlineId : Int, optionId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    val airline = request.user
    ChristmasSource.loadSantaClausInfoByAirline(airline.id) match {
      case Some(entry) => {
        if (!entry.found || entry.pickedAward.isDefined) {
          BadRequest("Either not found yet or reward has already been redeemed!")
        } else {
          val pickedAward = SantaClausAward.getRewardByType(entry, SantaClausAwardType(optionId))
          pickedAward.apply

          ChristmasSource.updateSantaClausInfo(entry.copy(pickedAward = Some(pickedAward.getType)))

          Ok(Json.obj())
        }
      }
      case None => NotFound
    }
  }


  def getDistanceText(distance : Int) : String = {
    if (distance >= 10000) {
      "more than 10000 km away..."
    } else if (distance >= 5000) {
      "around 5000 - 10000 km away..."
    } else if (distance >= 2000) {
      "around 2000 - 5000 km away..."
    } else if (distance >= 1000) {
      "around 1000 - 2000 km away..."
    } else if (distance >= 500) {
      "around 500 - 1000 km away..."
    } else if (distance >= 250) {
      "around 250 - 500 km away..."
    } else if (distance >= 100) {
      "around 100 - 250 km away!"
    } else if (distance >= 50) {
      "around 50 - 100 km away!"
    } else if (distance > 0) {
      "less than 50 km away!"
    } else {
      "Santa Claus found!"
    }
  }


  
  

  
}
