package controllers

import com.patson.util.AirlineCache
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._


class TileApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object TileLayoutWrites extends Writes[TileLayout] {
    def writes(layout: TileLayout): JsValue = {
      var result = Json.obj("xDimension" -> layout.matrix.length, "yDimension" -> layout.matrix(0).length)

      var tilesArray = Json.arr()


      for (x <- 0 until layout.matrix.length) {
        val xTiles = layout.matrix(x)
        for (y <- 0 until xTiles.length) {
          val tile = xTiles(y)
          val tileJson = Json.obj("x" -> x, "y" -> y, "source" -> tile.source, "xSize" -> tile.xSize, "ySize" -> tile.ySize, "master" -> tile.isMaster)
          tilesArray = tilesArray.append(tileJson)
        }
      }
      result = result + ("tiles" -> tilesArray)
      result
    }
  }


  
  
  val LOG_RANGE = 100 //load 100 weeks worth of alerts
  
  
  def getHeadquartersTiles(airlineId : Int) = Action { request =>
    AirlineCache.getAirline(airlineId) match {
      case Some(airline) => Ok(Json.toJson(TileUtil.generateHeadquartersTiles(airline)))
      case None => NotFound(s"Airline with $airlineId is not found")
    }

  }
  
  

  
}
