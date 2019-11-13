package com.patson

import com.patson.data.UserSource
import com.patson.model.UserSecret
import java.security.MessageDigest
import java.util.Arrays
import java.security.SecureRandom
import java.util.Base64

object Authentication {
  private[this] val ITERATION_NUMBER = 1000
  val decoder = Base64.getDecoder
  val encoder = Base64.getEncoder
  def base64ToByte(data : String) = {
     decoder.decode(data)
   }

   def byteToBase64(data : Array[Byte]) = {
     encoder.encode(data);
   }
  /**
    * Authenticates the user with a given login and password
    * If password and/or login is null then always returns false.
    * If the user does not exist in the database returns false.
    * @param con Connection An open connection to a databse
    * @param login String The login of the user
    * @param password String The password of the user
    * @return boolean Returns true if the user is authenticated, false otherwise
    *           (Two users with the same login, salt or digested password altered etc.)
    */
   def authenticate(inputUserName : String, inputPassword : String) : Boolean = {
     var userName = inputUserName
     var password = inputPassword
     var userExist = true;
     // INPUT VALIDATION
     if (userName == null || password == null){
       // TIME RESISTANT ATTACK
       // Computation time is equal to the time needed by a legitimate user
       userExist = false;
       userName = "";
       password = "";
     }
 
           
     val userSecret = UserSource.loadUserSecret(userName).getOrElse {
         val digest = "000000000000000000000000000=" // TIME RESISTANT ATTACK (Even if the user does not exist the
                                               // Computation time is equal to the time needed for a legitimate user
         val salt = "00000000000="
         userExist = false
         UserSecret(userName, digest, salt)
     }
           
     val bDigest = base64ToByte(userSecret.digest);
     val bSalt = base64ToByte(userSecret.salt);
 
     // Compute the new DIGEST
     val proposedDigest = getHash(ITERATION_NUMBER, password, bSalt);
 
     Arrays.equals(proposedDigest, bDigest) && userExist;
   }
   
   def createUserSecret(userName : String, password : String) : Boolean = {
      if (userName!=null && password!=null) {
         // Uses a secure Random not a simple Random
         val random = SecureRandom.getInstance("SHA1PRNG");
               // Salt generation 64 bits long
         val bSalt = new Array[Byte](8);
         random.nextBytes(bSalt);
         // Digest computation
         val bDigest = getHash(ITERATION_NUMBER,password,bSalt);
         val sDigest = byteToBase64(bDigest);
         val sSalt = byteToBase64(bSalt);
         UserSource.saveUserSecret(UserSecret(userName, new String(sDigest), new String(sSalt)))
      } else {
        false
      }
   }
 
 
 
  
   /**
    * From a password, a number of iterations and a salt,
    * returns the corresponding digest
    * @param iterationNb int The number of iterations of the algorithm
    * @param password String The password to encrypt
    * @param salt byte[] The salt
    * @return byte[] The digested password
    */
   def getHash(iterationNb : Int, password : String, salt : Array[Byte]) = {
       val digest = MessageDigest.getInstance("SHA-1");
       digest.reset();
       digest.update(salt);
       var input = digest.digest(password.getBytes("UTF-8"));
       for (i <- 0 until iterationNb) {
           digest.reset();
           input = digest.digest(input);
       }
       input
   }
}