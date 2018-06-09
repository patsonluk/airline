package com.patson.init

import java.net.URLEncoder
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import java.util.NoSuchElementException

object WikiUtil {
  def queryProfilePicture(searchItem : String) : Option[String] = {
    try {
      queryTitle(searchItem).map { title =>
          val url = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=name&titles=" + URLEncoder.encode(title, "UTF-8") + "&utf8="
          //println(url)
          val responseString = get(url)
          val pageImage = Json.parse(responseString).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("pageimage").as[String]
          //println(pageImage)
          val imageUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=Image:" + pageImage + "&format=json&prop=imageinfo&iiprop=url&utf8="
          val pageImageUrl = Json.parse(get(imageUrl)).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("imageinfo").asInstanceOf[JsArray].apply(0).get.asInstanceOf[JsObject].value("url").as[String]
          pageImageUrl
      }
    } catch {
        case e:NoSuchElementException => {
          //e.printStackTrace()
          None 
        }
    }
  }
  

  
  def removeQuotes(value : String) = {
    value.substring(1, value.length() - 1)
  }
  
  def queryTitle(searchItem : String) : Option[String] = {
    val responseString = get(url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" + URLEncoder.encode(searchItem, "UTF-8") + "&format=json&gsrprop=snippet&prop=info&inprop=url&utf8=")
    val response : JsObject = Json.parse(responseString).asInstanceOf[JsObject]
    val queryResponse = response.value.get("query").getOrElse(return None)
    
    //println(queryResponse)
    
    val pagesResponse = queryResponse.asInstanceOf[JsObject].value.get("pages").getOrElse(return None)
    
    //println(pagesResponse)
    
    val topPage = pagesResponse.asInstanceOf[JsObject].values.find{ pageEntry =>
      pageEntry.asInstanceOf[JsObject].value("index").as[Int] == 1
    }.getOrElse(return None)
    
    topPage.asInstanceOf[JsObject].value.get("title").map(_.as[String])
  }
  
  @throws(classOf[java.io.IOException])
  @throws(classOf[java.net.SocketTimeoutException])
  def get(
    url:            String,
    connectTimeout: Int    = 5000,
    readTimeout:    Int    = 5000,
    requestMethod:  String = "GET") =
    {
      import java.net.{ URL, HttpURLConnection }
      val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(connectTimeout)
      connection.setReadTimeout(readTimeout)
      connection.setRequestMethod(requestMethod)
      val inputStream = connection.getInputStream
      val content = io.Source.fromInputStream(inputStream).mkString
      if (inputStream != null) inputStream.close
      content
    }
}