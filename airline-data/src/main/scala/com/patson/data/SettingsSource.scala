package com.patson.data

import com.patson.data.Constants._
import com.patson.model.notice._

import java.io.ByteArrayInputStream
import scala.collection.mutable.ListBuffer


object SettingsSource {
  def saveWallpaper(userId : Int, wallpaper : Array[Byte]) = {
    val connection = Meta.getConnection()
    val stream = new ByteArrayInputStream(wallpaper)
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + USER_WALLPAPER_TABLE + " VALUES(?, ?)")
      preparedStatement.setInt(1, userId)
      preparedStatement.setBlob(2, stream)
      preparedStatement.executeUpdate()

      preparedStatement.close()

    } finally {
      connection.close()
      stream.close()
    }
  }
  def hasWallpaper(userId : Int) : Boolean = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT user FROM $USER_WALLPAPER_TABLE WHERE user = ?")
      preparedStatement.setInt(1, userId)
      val resultSet = preparedStatement.executeQuery()
      resultSet.next()
    } finally {
      connection.close()
    }
  }

  def deleteWallpaper(userId : Int) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $USER_WALLPAPER_TABLE WHERE user = ?")
      preparedStatement.setInt(1, userId)
      preparedStatement.executeUpdate()

      preparedStatement.close()

    } finally {
      connection.close()
    }
  }

  def loadWallpaper(userId : Int) : Option[Array[Byte]] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $USER_WALLPAPER_TABLE WHERE user = ?")
      preparedStatement.setInt(1, userId)
      val resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        val blob = resultSet.getBlob("wallpaper")
        val result = Some(blob.getBytes(1, blob.length.toInt))
        blob.free()
        result
      } else {
        None
      }
    } finally {
      connection.close()
    }
  }
}