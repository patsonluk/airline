package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import javax.inject._
import views._
import models._
import com.patson.data.UserSource
import com.patson.model._
import com.patson.Authentication
import java.util.Calendar
import com.patson.data.AirlineSource
import play.api.libs.ws.WS
import play.api.libs.ws.WSClient
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import play.api.libs.json.Writes
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString

class AccountApplication extends Controller {
  /**
   * Sign Up Form definition.
   *
   * Once defined it handle automatically, ,
   * validation, submission, errors, redisplaying, ...
   */
  val form: Form[PasswordReset] = Form(
    
    // Define a mapping that will handle User values
    mapping(
      "resetToken" -> text,
      "password" -> tuple(
        "main" -> text(minLength = 4),
        "confirm" -> text
      ).verifying(
        // Add an additional constraint: both passwords must match
        "Passwords don't match", passwords => passwords._1 == passwords._2
      )
    )
    // The mapping signature doesn't match the User case class signature,
    // so we have to define custom binding/unbinding functions
    {
      // Binding: Create a User from the mapping result (ignore the second password and the accept field)
      (token, passwords) => PasswordReset(token, passwords._1) 
    } 
    {
      // Unbinding: Create the mapping values from an existing User value
      passwordReset => Some(passwordReset.token, (passwordReset.password, ""))
    }
  )
  
  val forgotForm : Form[Forgot] =  Form(
    
    // Define a mapping that will handle User values
    mapping(
      "email" -> text.verifying(
        "Email address is not found",  
        email => !(UserSource.loadUsersByCriteria(List(("email", email))).isEmpty)
      )
    )
    // The mapping signature doesn't match the User case class signature,
    // so we have to define custom binding/unbinding functions
    {
      // Binding: Create a User from the mapping result (ignore the second password and the accept field)
      (email) => Forgot(email) 
    } 
    {
      // Unbinding: Create the mapping values from an existing User value
      forgot => Some(forgot.email)
    }
  )
  
  /**
   * Display an empty form.
   */
  def passwordResetForm(resetToken : String) = Action {
    UserSource.loadResetUser(resetToken) match {
    case Some(username) => {
      Ok(html.passwordReset(form.fill(PasswordReset(resetToken, ""))))
    }
      case None => Forbidden
    }
    
    
  }
  
  def forgotIdForm() = Action {
    Ok(html.forgotId(forgotForm.fill(Forgot(""))))
  }
  
  def forgotPasswordForm() = Action {
    Ok(html.forgotPassword(forgotForm.fill(Forgot(""))))
  }
  
  def sendEmail() = Action {
    EmailUtil.sendEmail("patson_luk@hotmail.com", "info@airline-club.com", "testing", "testing");
    Ok(Json.obj())
  }
  
  
  
  /**
   * Handle form submission.
   */
  def passwordResetSubmit = Action { implicit request =>
    form.bindFromRequest.fold(
      // Form has errors, redisplay it
      errors => {
        println(errors)
        BadRequest(html.passwordReset(errors))
      }, 
      userInput => {
          UserSource.loadResetUser(userInput.token) match {
            case Some(username) => {
              println("Resetting user for " + username)
              Authentication.createUserSecret(username, userInput.password)
              UserSource.deleteResetUser(userInput.token)
              Redirect("/")
            }
            case None => {
              println("TOKEN " + userInput.token)
              Forbidden
            }
          }
      }
    )
  }
  
  /**
   * Handle form submission.
   */
  def forgotIdSubmit = Action { implicit request =>
    forgotForm.bindFromRequest.fold(
      // Form has errors, redisplay it
      errors => {
        println(errors)
        BadRequest(html.forgotId(errors))
      }, 
      userInput => {
          val users = UserSource.loadUsersByCriteria(List(("email", userInput.email)))
          val user = users(0)
          println("Sending email for forgot ID " + user)
          EmailUtil.sendEmail(user.email, "info@airline-club.com", "Forgot User ID from airline-club.com", getForgotIdMessage(user))
          Redirect("/")
      }
    )
  }
  
   def forgotPasswordSubmit = Action { implicit request =>
    forgotForm.bindFromRequest.fold(
      // Form has errors, redisplay it
      errors => {
        println(errors)
        BadRequest(html.forgotId(errors))
      }, 
      userInput => {
          val users = UserSource.loadUsersByCriteria(List(("email", userInput.email)))
          val user = users(0)
          println("Sending email for reset password " + user)
          EmailUtil.sendEmail(user.email, "info@airline-club.com", "Reset password for airline-club.com", getResetPasswordMessage(user))
          Redirect("/")
      }
    )
  }
  
  def getForgotIdMessage(user : User) = {
    "Your user ID registered with this email address is : " + user.id
  }
  
  def getResetPasswordMessage(user : User) = {
    val resetLink = generateResetLink(user)
    "Please follow this link " + resetLink + " to reset your password."
  }
  
  def generateResetLink(user : User) = {
    "https://www.airline-club.com/password-reset?resetToken=123"
  }
}