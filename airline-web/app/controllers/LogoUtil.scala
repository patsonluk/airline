package controllers

import java.nio.file.Path

import com.patson.data.AirlineSource
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
  
  def validateUpload(logoFile : Path) : Option[String] = {
    val image = ImageIO.read(logoFile.toFile)
    println("!!!!!!!!!!!!!!!!!!!!" + image.getHeight + " X " + image.getWidth)
    if (image.getHeight() != imageHeight || image.getWidth() != imageWidth) {
      Some("Image should be " + imageWidth + "px wide and " + imageHeight + "px tall") 
    } else {
      None
    }
  }
  
  def getBlankLogo() = {
    //val buffer = new ByteArrayOutputStream();
    val is = play.Environment.simple().resourceAsStream("/logo/blank.png")

    val targetArray = new Array[Byte](is.available());
    is.read(targetArray);

    targetArray
  }
}