package com.patson

import scala.util.Random
import scala.annotation.tailrec


object Util {
  def calculateDistance(lat1InDegree : Double, lon1InDegree: Double, lat2InDegree : Double, lon2InDegree : Double) = {
    val lat1 = Math.toRadians(lat1InDegree)
    val lat2 = Math.toRadians(lat2InDegree)
    val lon1 = Math.toRadians(lon1InDegree)
    val lon2 = Math.toRadians(lon2InDegree)
//    
    Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)) * 6371 //=ACOS(SIN(Lat1)*SIN(Lat2)+COS(Lat1)*COS(Lat2)*COS(Lon2-Lon1))*6371
//    val dlon = lon2 - lon1 
//    val dlat = lat2 - lat1 
//    val a = Math.pow(Math.sin(dlat/2), 2)  + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2) 
//    val c = 2 * Math.atan2( Math.sqrt(a), Math.sqrt(1-a) ) 
//    6371 * c
  }
  
  /**
   * Generate a random value based on Gaussian distribution, with mean at center and boundary at (center - 0.5) and (center + 0.5) cut off at n*standard deviation  
   */
  @tailrec
  def getBellRandom(center : Double = 0, boundaryFromCenter : Double = 0.5) : Double = {
    val cutoff = 3 // 3 standard deviation (3 * 1), how much distribution is OK, not to confused with boundaryFromCenter
    //boundaries
//    val min = center - 0.5
//    val max = center + 0.5
    
    //squeeze it to cutoff as boundary : from (-cutoff,cutoff) to (-0.5, 0.5), then shift center from 0.0 to center, such that boundary is (cutoff - 0.5, cutoff + 0.5)
    //val value = Random.nextGaussian() / (2 * cutoff)  + center
    val randomGaussian = Random.nextGaussian();
    if (randomGaussian < -1 * cutoff || randomGaussian > cutoff) { //regen
      getBellRandom(center, boundaryFromCenter)
    } else { //within cutoff, now scale and shift to desired value
      randomGaussian / cutoff * boundaryFromCenter + center //divide by cutoff : scale boundary to (-1.0, 1.0), then times boundaryFrom center : scale boundary to (-boundaryFromCenter, boundaryFromCenter), lastly shift the center
    }
  }
}