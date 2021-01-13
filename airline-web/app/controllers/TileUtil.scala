package controllers

import com.patson.model.{Airline, AirlineGrade}
import java.io.File

import play.api.Play

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

object TileUtil {

  val webAssetBuildingBase = "/assets/images/buildings/"
  val buildingAssetBase = "/public/images/buildings/"
  val mainBuildingFolderBase = "level-"
  val tileAssetFolder = "tile-1"
  val buildingSourceByLevel = new mutable.HashMap[Int, List[String]]()
  val mainBuildingDimensionByLevel =  new mutable.HashMap[Int, (Int, Int)]()

  for (i <- 1 to 6) {
    val folder = play.Environment.simple().getFile(buildingAssetBase + mainBuildingFolderBase + i)

    val files =
      if (folder.exists && folder.isDirectory) {
        folder.listFiles.filter(_.isFile).map(file => webAssetBuildingBase + mainBuildingFolderBase + i + "/" + file.getName).toList
      } else {
        List[String]()
      }
    buildingSourceByLevel.put(i, files)
    mainBuildingDimensionByLevel.put(i,  if (i <= 3) (1, 1) else (2, 2))
  }

  val tileSources = {
    val folder = play.Environment.simple().getFile(buildingAssetBase + tileAssetFolder)

    if (folder.exists && folder.isDirectory) {
      folder.listFiles.filter(_.isFile).map(file => {
        println(s"building assets: ${file.getAbsolutePath}")
        webAssetBuildingBase + tileAssetFolder + "/" + file.getName
      }).toList
    } else {
      List[String]()
    }
  }


  def generateHeadquartersTiles(airline : Airline) : TileLayout = {
    val random = new Random(airline.id) //a stable seed
    val buildingLevel = airline.airlineGrade match {
      case grade if grade.value <= AirlineGrade.CONTINENTAL.value =>
        1
      case _ =>
        6
    }

    val mainBuildingSource = buildingSourceByLevel(buildingLevel)(random.nextInt(buildingSourceByLevel(buildingLevel).length))
    val backRowSource = tileSources(random.nextInt(tileSources.length))
    val midRowSource =  tileSources(random.nextInt(tileSources.length))
    val frontRowSource =  tileSources(random.nextInt(tileSources.length))
    val (mainBuildingXSize, mainBuildingYSize) = mainBuildingDimensionByLevel(buildingLevel)
    val (mapXSize, mapYSize) = (mainBuildingXSize + 2, mainBuildingYSize + 2)

    val tiles : Array[Array[Tile]] = Array.ofDim[Tile](mapXSize, mapYSize)

    //back row
    for (i <- 0 until mapXSize) {
      tiles(i)(0) = Tile(backRowSource, 1, 1, true)
    }
    //mid row
    for (i <- 0 until mainBuildingYSize) {
      tiles(0)(i + 1) = Tile(midRowSource, 1, 1, true)
      tiles(mapXSize - 1)(i + 1) = Tile(midRowSource, 1, 1, true)
    }

    //building
    for (x <- 0 until mainBuildingXSize) {
      for (y <- 0 until mainBuildingYSize) {
        tiles(x + 1)(y + 1) = Tile(mainBuildingSource, mainBuildingXSize, mainBuildingYSize, x == 0 && y == 0)
      }
    }

    //front row
    for (i <- 0 until mapXSize) {
      tiles(i)(mapYSize - 1) = Tile(frontRowSource, 1, 1, true)
    }

    TileLayout(tiles)
  }
}

case class TileLayout(matrix : Array[Array[Tile]])

case class Tile(source : String, xSize : Int, ySize : Int, isMaster : Boolean)
