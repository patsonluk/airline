package controllers

import com.patson.data.CycleSource
import com.patson.data.ConsumptionHistorySource
import com.patson.model.PassengerType
import com.patson.model.Route
import com.patson.model.Link
import com.patson.model.LinkHistory
import com.patson.model.LinkConsideration
import com.patson.model.RelatedLink
import com.patson.model.Airport
import com.patson.data.AirlineSource
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object LogoUtil {
  val logos : scala.collection.mutable.Map[Int, Array[Byte]] = collection.mutable.Map(AirlineSource.loadLogos().toSeq: _*) 
  val blank = getBlankLogo
  val imageHeight = 12
  val imageWidth = 24
  
  def getLogo(airlineId : Int) : Array[Byte]= {
    logos.get(airlineId) match {
      case Some(logo) => logo
      case None => blank
    }
  }
  
  def saveLogo(airlineId : Int, logo : Array[Byte]) = {
    AirlineSource.saveLogo(airlineId, logo)
    logos.put(airlineId, logo) //update cache
  }
  
  def validateUpload(logoFile : File) : Option[String] = {
    val image = ImageIO.read(logoFile)
    println("!!!!!!!!!!!!!!!!!!!!" + image.getHeight + " X " + image.getWidth)
    if (image.getHeight() != imageHeight || image.getWidth() != imageWidth) {
      Some("Image should be " + imageWidth + "px wide and " + imageHeight + "px tall") 
    } else {
      None
    }
  }
  
  def getBlankLogo() = {
    val buffer = new ByteArrayOutputStream();
    val is = play.Play.application().resourceAsStream("/logo/blank.png")

    val targetArray = new Array[Byte](is.available());
    is.read(targetArray);

    targetArray
  }
}