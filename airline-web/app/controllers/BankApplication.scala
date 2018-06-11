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



class BankApplication extends Controller {
  implicit object LoanWrites extends Writes[Loan] {
    //case class Loan(airlineId : Int, borrowedAmount : Long, interest : Long, var remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject
    def writes(loan: Loan): JsValue = JsObject(List(
      "airlineId" -> JsNumber(loan.airlineId),
      "borrowedAmount" -> JsNumber(loan.borrowedAmount),
      "interest" -> JsNumber(loan.interest),
      "remainingAmount" -> JsNumber(loan.remainingAmount),
      "creationCycle" -> JsNumber(loan.creationCycle),
      "loanTerm" ->  JsNumber(loan.loanTerm),
      "id" -> JsNumber(loan.id)))
  }
  
  case class LoanRequest(requestedAmount: Long, requestedTerm: Int)
  val loanForm = Form(
    Forms.mapping(
      "requestedAmount" -> Forms.longNumber,
      "requestedTerm" -> Forms.number
    )(LoanRequest.apply)(LoanRequest.unapply)
  )
  
  
  def viewLoans(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.toJson(BankSource.loadLoansByAirline(request.user.id)))
  }
  
  def takeOutLoan(airlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    val LoanRequest(requestedAmount, requestedTerm) = loanForm.bindFromRequest.get
    val maxLoan = Bank.getMaxLoan(airlineId)
    if (requestedAmount > maxLoan) {
      BadRequest("Borrowing [" + requestedAmount + "] which is above limit [" + maxLoan + "]")
    } else {
      Bank.getLoanOptions(requestedAmount).find( loanOption => loanOption.loanTerm == requestedTerm) match {
        case Some(loan) =>
          BankSource.saveLoan(loan.copy(airlineId = request.user.id, creationCycle = CycleSource.loadCycle()))
          AirlineSource.adjustAirlineBalance(request.user.id, loan.borrowedAmount)
          Ok(Json.toJson(loan))
        case None => BadRequest("Bad loan term [" + requestedTerm + "]")
      }
    }
  }
  
  def getLoanOptions(airlineId : Int, loanAmount : Long) = AuthenticatedAirline(airlineId) { request =>
    val maxLoan = Bank.getMaxLoan(request.user.id)
    if (loanAmount <= maxLoan) {
      val options = Bank.getLoanOptions(loanAmount)
      Ok(Json.toJson(options))  
    } else {
      BadRequest("Borrowing [" + loanAmount + "] which is above limit [" + maxLoan + "]")
    }
    
  }
  
  def getMaxLoan(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(Json.obj("maxAmount" -> JsNumber(Bank.getMaxLoan(request.user.id))))
  }
  
  def repayLoan(airlineId : Int, loanId : Int) = AuthenticatedAirline(airlineId) { request =>
    BankSource.loadLoanById(loanId) match {
      case Some(loan) => { 
        if (loan.airlineId != request.user.id) {
          BadRequest("Cannot repay loan not owned by this airline") 
        } else {
          val balance = request.user.getBalance 
          if (balance < loan.remainingAmount) {
            BadRequest("Not enough cash to repay this loan")
          } else {
            AirlineSource.adjustAirlineBalance(request.user.id, -1 * loan.remainingAmount)
            BankSource.deleteLoan(loanId)
            Ok
          }
        }
      }
      case None => NotFound
    }
  }
  
}
