package controllers

import com.patson.model.{Airline, AirlineGrade}

import scala.collection.mutable
import scala.util.Random

object TileUtil {

  val webAssetBuildingBase = "/assets/images/buildings/"

  val mainBuildingFolderBase = "level-"
  val tileAssetFolder = "tile-"
  val buildingSourceByLevel = new mutable.HashMap[Int, List[String]]()
  val tileSourceByY =  new mutable.HashMap[Int, List[String]]()
  val mainBuildingDimensionByLevel =  new mutable.HashMap[Int, (Int, Int)]()


  val buildingCountPerLevel : Map[Int, Int] = Map(
    1 -> 3,
    2 -> 9,
    3 -> 7,
    4 -> 4,
    5 -> 8,
    6 -> 7
  )

  val tileCountPerY : Map[Int, Int] = Map(
    1 -> 2,
    2 -> 1,
    3 -> 2
  )




  for (i <- 1 to 6) {
    val files =
      (1 to buildingCountPerLevel(i)).map( x => s"$webAssetBuildingBase$mainBuildingFolderBase$i/$x.png").toList
    buildingSourceByLevel.put(i, files)
    mainBuildingDimensionByLevel.put(i,  if (i <= 3) (1, 1) else (2, 2))
  }

  for (i <- 1 to 3) {
    val files =
      (1 to tileCountPerY(i)).map( x => s"$webAssetBuildingBase$tileAssetFolder$i/$x.png").toList
    tileSourceByY.put(i, files)
  }


  def generateHeadquartersTiles(airline : Airline) : TileLayout = {
    val buildingLevel = Math.min(6, airline.airlineGrade.value / 3 + 1)

    val mainBuildingSource = buildingSourceByLevel(buildingLevel)(airline.id % buildingSourceByLevel(buildingLevel).length)
    val backRowSource = tileSourceByY(1)(airline.id % tileSourceByY(1).length)
    val midRowSource =  tileSourceByY(2)(airline.id % tileSourceByY(2).length)
    val frontRowSource =  tileSourceByY(3)(airline.id % tileSourceByY(3).length)
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
