package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import com.patson.data.CycleSource
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.CountrySource
import com.patson.data.AirportSource
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import com.patson.data.BankSource
import com.patson.model.Loan
import play.api.data.Form
import play.api.data.Forms
import com.patson.data.OilSource
import com.patson.model.oil.OilContract
import com.patson.model.oil.OilPrice
import com.patson.data.IncomeSource
import com.patson.data.OilSource
import com.patson.model.oil.OilPrice



class OilApplication extends Controller {
  implicit object OilContractWrites extends Writes[OilContractWithDetails] {
//case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
    def writes(contractWithDetails: OilContractWithDetails): JsValue = {
      val contract = contractWithDetails.contract
      val penalty = contractWithDetails.penalty
      val rejection = contractWithDetails.rejection
      val remainingDuration = contractWithDetails.remainingDuration
      
      var result = JsObject(List(
      "airlineId" -> JsNumber(contract.airline.id),
      "price" -> JsNumber(contract.contractPrice),
      "volume" -> JsNumber(contract.volume),
      "cost" -> JsNumber(contract.contractCost),
      "startCycle" -> JsNumber(contract.startCycle),
      "remainingDuration" -> JsNumber(remainingDuration),
      "terminationPenalty" -> JsNumber(penalty),
      "id" -> JsNumber(contract.id)))
      
      rejection.foreach { rejection =>
        result = result + ("rejection" -> JsString(rejection))
      }
      result
    }
  }
  
  implicit object OilPriceWrites extends Writes[OilPrice] {
//case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
    def writes(oilPrice: OilPrice): JsValue = {
      
      JsObject(List(
      "price" -> JsNumber(oilPrice.price),
      "cycle" -> JsNumber(oilPrice.cycle)))
      
    }
  }
  
  case class OilContractWithDetails(contract : OilContract, remainingDuration : Int, penalty : Long, rejection : Option[String])
  
  def getContracts(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    Ok(Json.toJson(OilSource.loadOilContractsByAirline(airlineId).map(contract => wrapContract(request.user, contract, currentCycle))))
  }
  
  def wrapContract(airline : Airline, contract : OilContract, currentCycle : Int) : OilContractWithDetails = {
    val penalty = contract.contractTerminationPenalty(currentCycle)
    
    val rejection = getTerminateContractRejection(airline, contract, currentCycle)
    val remainingDuration = contract.startCycle + contract.contractDuration - currentCycle
    OilContractWithDetails(contract, remainingDuration, penalty, rejection)
  }
  
  def getContractSuggestion(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    val currentCycle = CycleSource.loadCycle()
    val existingContracts = OilSource.loadOilContractsByAirline(airlineId)
    val existingContractBarrels = existingContracts.map(_.volume).sum
    val barrelsUsed = getBarrelsUsed(airline, currentCycle)
    val totalBarrelsAllowed = (barrelsUsed * OilContract.MAX_VOLUME_FACTOR).toLong
    val extraBarrelsAllowed = totalBarrelsAllowed - existingContractBarrels
    val suggestedBarrels =
      if (existingContractBarrels > barrelsUsed) {
        0
      } else {
        barrelsUsed - existingContractBarrels //top up
      }
    
    val currentPrice : Double = OilSource.loadOilPriceByCycle(currentCycle) match {
      case Some(price) => price.price
      case None => OilPrice.DEFAULT_PRICE
    }
    val contractPrice = currentPrice + OilContract.EXTRA_PER_BARREL_CHARGE
    Ok(Json.obj("barrelsUsed" -> JsNumber(barrelsUsed), 
        "suggestedBarrels" -> JsNumber(suggestedBarrels), 
        "extraBarrelsAllowed" -> JsNumber(extraBarrelsAllowed), 
        "suggestedDuration" -> OilContract.MIN_DURATION, 
        "maxDuration" -> OilContract.MAX_DURATION, 
        "contractPrice" -> contractPrice,
        "barrelsUsed" -> barrelsUsed))     
  }
  
  def getContractConsideration(airlineId : Int, volume : Int, duration : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    val existingContracts = OilSource.loadOilContractsByAirline(airlineId)
    val currentPrice : Double = OilSource.loadOilPriceByCycle(currentCycle) match {
      case Some(price) => price.price
      case None => OilPrice.DEFAULT_PRICE
    }
    
    val contractPrice = currentPrice + OilContract.EXTRA_PER_BARREL_CHARGE
    
    val airline = request.user
    val newContract = OilContract(airline = airline, contractPrice = contractPrice, volume = volume, startCycle = currentCycle, contractDuration = duration)
    val penalty = newContract.contractTerminationPenalty(currentCycle)
    val rejection = getSignContractRejection(airline, newContract, existingContracts, currentCycle)
    
    Ok(Json.toJson(OilContractWithDetails(newContract, duration, penalty, rejection)))
  }
  
  def signContract(airlineId : Int, volume : Int, duration : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    val existingContracts = OilSource.loadOilContractsByAirline(airlineId)
    val currentPrice = OilSource.loadOilPriceByCycle(currentCycle) match {
      case Some(price) => price.price
      case None => OilPrice.DEFAULT_PRICE
    }
    val contractPrice = currentPrice + OilContract.EXTRA_PER_BARREL_CHARGE
    
    val airline = request.user
    val newContract = OilContract(airline = airline, contractPrice = contractPrice, volume = volume, startCycle = currentCycle, contractDuration = duration)
    getSignContractRejection(airline, newContract, existingContracts, currentCycle) match {
      case Some(rejection) => BadRequest(rejection)
      case None =>
        OilSource.saveOilContract(newContract)
        val cost = newContract.contractCost * -1
        AirlineSource.adjustAirlineBalance(airlineId, cost)
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.OIL_CONTRACT, cost))
        Ok(Json.toJson(OilContractWithDetails(newContract, duration, 0, None)))
    }
  }
  
  def terminateContract(airlineId : Int, contractId : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    val airline = request.user
    //ensure this is a contract this airline owns
    OilSource.loadOilContractById(contractId) match {
      case Some(contract) => 
        if (contract.airline.id == airlineId) {
          getTerminateContractRejection(airline, contract, currentCycle) match {
            case Some(rejection) => BadRequest(rejection)
            case None =>
              OilSource.deleteOilContract(contract)
              val penalty = contract.contractTerminationPenalty(currentCycle) * -1
              AirlineSource.adjustAirlineBalance(airlineId, penalty)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.OIL_CONTRACT, penalty))
              Ok(Json.obj("id" -> contract.id))
          }
        } else {
          Forbidden("Cannot terminate contract that the airline does not own!")
        }
    
      case None => NotFound("contract with id " + contractId + " is not found!")
    }
    
  }
  
  def getOilPrices() = Action {
    val currentCycle = CycleSource.loadCycle()
    val prices = OilSource.loadOilPricesFromCycle(currentCycle - 50).sortBy(_.cycle)
    Ok(Json.toJson(prices))
  }
  
  def getTerminateContractRejection(airline : Airline, contract : OilContract, currentCycle : Int) : Option[String] = {
    val penalty = contract.contractTerminationPenalty(currentCycle)
    if (penalty > airline.getBalance()) {
      return Some("Cannot termiante this contract - insufficient fund to pay the penalty")
    }
    
    return None
  }
  
  def getSignContractRejection(airline : Airline, contract : OilContract, existingContracts : List[OilContract],  currentCycle : Int) : Option[String] = {
    if (existingContracts.length >= OilContract.MAX_CONTRACTS_ALLOWED) {
      return Some("Cannot sign more contracts, only " + OilContract.MAX_CONTRACTS_ALLOWED + " active contracts allowed")
    }
    
    if (contract.contractDuration > OilContract.MAX_DURATION) {
      return Some("Contract cannot be longer than " + OilContract.MAX_DURATION + " weeks")
    }
    
    if (contract.contractDuration < OilContract.MIN_DURATION) {
      return Some("Contract cannot be shorter than " + OilContract.MIN_DURATION + " weeks")
    }
    
    
    if (contract.volume <= 0) {
      return Some("Barrels should be positive")
    }
    
    val extraBarrelsAllowed = getExtraBarrelsAllowed(airline, existingContracts, currentCycle)
    if (extraBarrelsAllowed < contract.volume) {
      if (extraBarrelsAllowed > 0) {
        return Some("Can only sign a contract with maximum of " + extraBarrelsAllowed + " barrels")
      } else {
        return Some("Cannot sign any new contracts as existing contracts already exceeded the barrels allowed according to your current usage")
      }
    }
    
    if (contract.contractCost > airline.getBalance()) {
      return Some("Insufficient funds to sign this contract")
    }
    
    return None
  }
  
  def getBarrelsUsed(airline : Airline, currentCycle : Int) : Long = {
    IncomeSource.loadIncomeByAirline(airlineId = airline.id, cycle = currentCycle - 1, period = Period.WEEKLY) match {
      case Some(airlineIncome) => (airlineIncome.links.fuelCost * -1 / OilPrice.DEFAULT_PRICE).toLong
      case None => 0
    }
  }
  
  def getExtraBarrelsAllowed(airline : Airline, existingContracts : List[OilContract], currentCycle : Int) = {
    val existingBarrels = existingContracts.map(_.volume).sum
    val barrelsUsed = getBarrelsUsed(airline, currentCycle)
    val totalBarrelsAllowed = (barrelsUsed * OilContract.MAX_VOLUME_FACTOR).toLong
    val extraBarrelsAllowed = totalBarrelsAllowed - existingBarrels
    extraBarrelsAllowed
  }
  
}
