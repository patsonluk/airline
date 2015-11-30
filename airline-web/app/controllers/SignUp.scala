package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._
import models._
import com.patson.data.UserSource
import com.patson.model._
import com.patson.Authentication
import java.util.Calendar

class SignUp extends Controller {
  
  /**
   * Sign Up Form definition.
   *
   * Once defined it handle automatically, ,
   * validation, submission, errors, redisplaying, ...
   */
  val signupForm: Form[NewUser] = Form(
    
    // Define a mapping that will handle User values
    mapping(
      "username" -> text(minLength = 4),
      "email" -> email,
      
      // Create a tuple mapping for the password/confirm
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
      (username, email, passwords) => NewUser(username, passwords._1, email) 
    } 
    {
      // Unbinding: Create the mapping values from an existing User value
      user => Some(user.username, user.email, (user.password, ""))
    }.verifying(
      // Add an additional constraint: The username must not be taken (you could do an SQL request here)
      "This username is not available",
      newUser => UserSource.loadUserByUserName(newUser.username).isEmpty
    )
  )
  
  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.signup(signupForm))
  }
  
  /**
   * Display a form pre-filled with an existing User.
   */
//  def editForm = Action {
//    val existingUser = NewUser("fakeuser", "secret", "fake@gmail.com") 
//    Ok(html.signup(signupForm.fill(existingUser)))
//  }
  
  /**
   * Handle form submission.
   */
  def submit = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      // Form has errors, redisplay it
      errors => BadRequest(html.signup(errors)), { user =>// We got a valid User value, display the summary
        UserSource.saveUser(User(user.username, user.email, Calendar.getInstance, UserStatus.ACTIVE))
        Authentication.createUserSecret(user.username, user.password)
        Ok(html.summary(user))
      }
    )
  }
  
}