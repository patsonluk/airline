package com.patson

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
}