package com.patson.init

import java.net.URLEncoder

import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import java.util.NoSuchElementException

import scala.io.Source

object WikiUtil {
  def queryProfilePicture(searchItem : String, preferredWords : List[String]) : Option[String] = {
    try {
      queryTitle(searchItem) match { 
        case Some(title) => {
          val url = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=name&titles=" + URLEncoder.encode(title, "UTF-8") + "&utf8="
          //println(url)
          val responseString = get(url)
          val pageImage = Json.parse(responseString).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("pageimage").as[String]
          
          var isMatch = false
          preferredWords.foreach{ preferredWord =>
            if (pageImage.toLowerCase().contains(preferredWord)) {
              isMatch = true
            }
          }
          if (isValidExtension(pageImage) && (preferredWords.isEmpty || isMatch)) {
            val imageUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=Image:" + pageImage + "&format=json&prop=imageinfo&iiprop=url&utf8="
            val pageImageUrl = Json.parse(get(imageUrl)).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("imageinfo").asInstanceOf[JsArray].apply(0).asInstanceOf[JsObject].value("url").as[String]
            Some(pageImageUrl)
          } else {
            None
          }
        }
        case None => None
      }
    } catch {
        case e:NoSuchElementException => {
          //e.printStackTrace()
          None 
        }
        case e:java.io.IOException => {
          e.printStackTrace()
          None
        }
    }
  }
  
  def queryOtherPicture(searchItem : String, preferredWords : List[String]) : Option[String] = {
    try {
      queryTitle(searchItem) match { 
        case Some(title) =>
        
          val url = "https://en.wikipedia.org/w/api.php?action=query&titles=" + URLEncoder.encode(title, "UTF-8") + "&prop=images&format=json&imlimit=300&utf8="
          //println(url)
          val responseString = get(url)
          val images : JsArray = Json.parse(responseString).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("images").asInstanceOf[JsArray]
          
          val imageTitles = images.\\("title").map(_.as[String]).toSeq
          
          var imageTitle = findMatchingTitle(imageTitles, preferredWords).getOrElse(return None)
          if (imageTitle.startsWith("File:")) {
            imageTitle = imageTitle.substring("File:".length())
          }
          
          imageTitle = URLEncoder.encode(imageTitle, "UTF-8")
           
          val imageUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=Image:" + imageTitle + "&format=json&prop=imageinfo&iiprop=url&utf8="
          //println(imageUrl)
          val pageImageUrl = Json.parse(get(imageUrl)).asInstanceOf[JsObject].value("query").asInstanceOf[JsObject].value("pages").asInstanceOf[JsObject].values.toSeq(0).asInstanceOf[JsObject].value("imageinfo").asInstanceOf[JsArray].apply(0).asInstanceOf[JsObject].value("url").as[String]
          return Some(pageImageUrl)
        case None => None
      }
    } catch {
        case e:NoSuchElementException => {
          //e.printStackTrace()
          None 
        }
        case e:java.io.IOException => {
          e.printStackTrace()
          None
        }
    }
  }
  
  def findMatchingTitle(titles : Seq[String], preferredWords : List[String]) : Option[String] = {
    preferredWords.foreach { preferredWord =>
      titles.foreach { title =>  
        if (title.toLowerCase().contains(preferredWord) && isValidExtension(title)) {
           return Some(title) 
        }
      }
    }
    return None
  }
  
  def isValidExtension(title : String) = {
    title.toLowerCase().endsWith(".png") || title.toLowerCase().endsWith(".jpg")
  }
  

  
  def removeQuotes(value : String) = {
    value.substring(1, value.length() - 1)
  }
  
  def queryTitle(searchItem : String) : Option[String] = {
    val responseString = get(url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" + URLEncoder.encode(searchItem, "UTF-8") + "&format=json&gsrprop=snippet&prop=info&inprop=url&utf8=")
    val response : JsObject = Json.parse(responseString).asInstanceOf[JsObject]
    val queryResponse = response.value.get("query").getOrElse(return None)
    
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
    connectTimeout: Int    = 20000,
    readTimeout:    Int    = 20000,
    requestMethod:  String = "GET") =
    {
      import java.net.{ URL, HttpURLConnection }
      val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(connectTimeout)
      connection.setReadTimeout(readTimeout)
      connection.setRequestMethod(requestMethod)
      val inputStream = connection.getInputStream
      val content = Source.fromInputStream(inputStream).mkString
      if (inputStream != null) inputStream.close
      content
    }
}