package com.patson.init

import com.patson.data.Meta

/**
 * The main flow to initialize everything
 */
object MainInit extends App {
  Meta.createSchema()
  GeoDataGenerator.mainFlow()
  AirplaneModelInitializer.mainFlow()
  //AirlineGenerator.mainFlow()
 // AirportProfilePicturePatcher.patchProfilePictures()
}