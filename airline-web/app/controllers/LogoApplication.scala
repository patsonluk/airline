package controllers

import java.awt.Color

import com.patson.util.LogoGenerator
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import scala.jdk.CollectionConverters._

class LogoApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val templates : Map[Int, Array[Byte]] = LogoGenerator.getTemplates.asScala.map { case (key, value) => (key.intValue, value) }.toMap
  

  def getTemplates() = Action {
     Ok(Json.toJson(templates.keySet.toList.sorted))
  }
  
  def getTemplate(id : Int) = Action {
     Ok(templates(id)).as("image/bmp")
  }
  
  def getPreview(templateIndex : Int, color1 : String, color2 : String) = Action {
     Ok(LogoGenerator.generateLogo(templateIndex, Color.decode(color1).getRGB, Color.decode(color2).getRGB)).as("img/png")
  }	 
}
