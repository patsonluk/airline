package controllers

import com.patson.data.AirlineSource

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

object LiveryUtil {
  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
  val liveries : LoadingCache[Int, Option[Array[Byte]]] = CacheBuilder.newBuilder.maximumSize(100).expireAfterAccess(10, TimeUnit.MINUTES).build(new LiveryLoader())
  val blank = getBlankImage(1, 1)

  //val imageHeight = 12
  val MAX_SIZE = 1 * 1024 * 1024
  
  def getLivery(airlineId : Int) : Array[Byte] = {
    liveries.get(airlineId).getOrElse(blank)
  }

  def getBlankImage(width : Int, height : Int) = {
    import javax.imageio.ImageIO
    import java.awt.Color
    import java.awt.image.BufferedImage
    val singlePixelImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR)
    val transparent = new Color(0, 0, 0, 0)
    singlePixelImage.setRGB(0, 0, transparent.getRGB)

    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(singlePixelImage, "png", outputStream)
    outputStream.toByteArray
  }
  
  def saveLivery(airlineId : Int, livery : Array[Byte]) = {
    AirlineSource.saveLivery(airlineId, livery)
    liveries.put(airlineId, Some(livery)) //update cache
  }

  def deleteLivery(airlineId : Int) = {
    AirlineSource.deleteLivery(airlineId)
    liveries.invalidate(airlineId)
  }
  
  def validateUpload(liveryFile : Path) : Option[String] = {
    val imageFile = liveryFile.toFile
    val image = ImageIO.read(imageFile)
    println("Livery upload " + image.getHeight + " X " + image.getWidth + s" Size ${imageFile.length()}")
    if (imageFile.length() > MAX_SIZE) {
      Some(s"Image should be with $MAX_SIZE bytes")
    } else {
      None
    }
  }





  def invalidateAirlineLivery(airlineId : Int) = {
    liveries.invalidate(airlineId)
  }

  class LiveryLoader extends CacheLoader[Int, Option[Array[Byte]]] {
    override def load(airlineId: Int) = {
      AirlineSource.loadLivery(airlineId)
    }
  }
}