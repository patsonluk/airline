package controllers

import java.nio.file.Path
import javax.imageio.ImageIO

object WallpaperUtil {

  //val imageHeight = 12
  val MAX_SIZE = 2 * 1024 * 1024


  def validateUpload(wallPaper : Path) : Option[String] = {
    val imageFile = wallPaper.toFile
    val image = ImageIO.read(imageFile)
    println("Wallpaper upload " + image.getHeight + " X " + image.getWidth + s" Size ${imageFile.length()}")
    if (imageFile.length() > MAX_SIZE) {
      Some(s"Image should be within $MAX_SIZE bytes")
    } else {
      None
    }
  }
}