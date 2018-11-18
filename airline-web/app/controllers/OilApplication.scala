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
import com.patson.data.OilSource
import com.patson.model.oil.OilContract
import com.patson.model.oil.OilContract
import com.patson.model.oil.OilContract
import com.patson.data.IncomeSource
import com.patson.model.oil.OilPrice



class OilApplication extends Controller {
  implicit object OilContractWrites extends Writes[OilContractWithDetails] {
//case class OilContract(airline : Airline, contractPrice : Double, volume : Int, startCycle : Int, contractDuration : Int, var id : Int = 0) extends IdObject {
    def writes(contractWithDetails: OilContractWithDetails): JsValue = 
      val contract = contractWithDetails.contract
      val penalty = contractWithDetails.penalty
      val rejection = contractWithDetails.rejection
      
      var result = JsObject(List(
      "airlineId" -> JsNumber(contract.airline.id),
      "price" -> JsNumber(contract.contractPrice),
      "volume" -> JsNumber(contract.volume),
      "cost" -> JsNumber(contract.contractCost),
      "startCycle" -> JsNumber(contract.startCycle),
      "endCycle" -> JsNumber(contract.startCycle + contract.contractDuration),
      "terminationPenalty" -> JsNumber(penalty),
      "id" -> JsNumber(contract.id)))
      
      rejection.foreach { rejection =>
        result = result + ("rejection" -> JsString(rejection))
      }
      result
  }
  
  case class OilContractWithDetails(contract : OilContract, penalty : Long, rejection : Option[String])
  
  def getContracts(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(OilSource.loadOilContractsByAirline(airlineId))
  }
  
  def getContractConsideration(airlineId : Int, volume : Int, duration : Int) = AuthenticatedAirline(airlineId) { request =>
    OilSource.loadOilContractsByAirline(airlineId)
    Ok(Json.toJson(OilSource.loadOilContractsByAirline(airlineId))
  }
  
  def signContract(airlineId : Int, volume : Int, duration : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(OilSource.loadOilContractsByAirline(airlineId))
  }
  
  def terminateContract(airlineId : Int, contractId : Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    //ensure this is a contract this airline owns
    OilSource.loadOilContractById(contractId) match {
      case Some(contract) => 
        if (contract.airline.id == airlineId) {
          getTerminateContractRejection(airline, contract, currentCycle) match {
            case Some(rejection) => BadRequest(rejection)
            case None =>
              OilSource.deleteOilContract(contract)
              val penalty = contract.contractTerminationPenalty(currentCycle) * -1
              AirlineSource.adjustBalance(airlineId, penalty)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.OIL_CONTRACT, penalty))
              Ok(Json.toJson(contract))
          }
        } else {
          Forbidden("Cannot terminate contract that the airline does not own!")
        }
    
      case None => NotFound("contract with id " + contractId + " is not found!")
    }
    
  }
  
  def getTerminateContractRejection(airline : Airline, contract : OilContract, currentCycle : Int) : Option[String] = {
    val penalty = contract.contractTerminationPenalty(currentCycle)
    if (penalty > airline.balance) {
      return Some("Cannot termiante this contract - insufficient fund to pay the penalty")
    }
    
    return None
  }
  
  def getSignContractRejection(airline : Airline, contract : OilContract, existingContracts : List[OilContract],  currentCycle : Int) : Option[String] = {
    if (existingContracts.length >= OilContract.MAX_CONTRACTS_ALLOWED) {
      return Some("Cannot sign more contracts, only " + OilContract.MAX_CONTRACTS_ALLOWED + " active contracts allowed")
    }
    
    val extraBarrelsAllowed = getExtraBarrelsAllowed(airline, existingContracts)
    if (extraBarrelsAllowed < contract) {
      if (extraBarrelsAllowed > 0) {
        return Some("Can only sign a contract with maximum of " + existingBarrels + " barrels")
      } else {
        return Some("Cannot sign any new contracts as existing contracts already exceeded the barrels allowed according to your current usage")
      }
    }
    
    if (contract.contractCost > airline.balance) {
      return Some("Insufficient funds to sign this contract")
    }
    
    return None
  }
  
  def getBarrelsUsed(airline : Airline) : Long = {
    IncomeSource.loadIncomeByAirline(airlineId = airline.id, cycle = currentCycle - 1, period = Period.WEEKLY) match {
      case Some(airlineIncome) => (airlineIncome.links.fuelCost * -1 / OilPrice.DEFAULT_PRICE).toLong
      case None => 0
    }
  }
  
  def getExtraBarrelsAllowed(airline : Airline, existingContracts : List[OilContract]) = {
    val existingBarrels = existingContracts.map(_.volume).sum
    val barrelsUsed = getBarrelsUsed(airline)
    val totalBarrelsAllowed = (barrelsUsed * OilContract.MAX_VOLUME_FACTOR).toLong
    val extraBarrelsAllowed = totalBarrelsAllowed - existingBarrels
    extraBarrelsAllowed
  }
  
}
