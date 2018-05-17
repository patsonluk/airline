package com.patson.init

import com.patson.data.CountrySource
import scala.collection.mutable.Map

object CountryMutualRelationshipGenerator extends App {
  mainFlow()
  
  def mainFlow() = {
    val mutualRelationshipMap = getCountryMutalRelationship()
    val mutualRelationshipPatchMap = getCountryMutalRelationshipPatch()
    
    val finalMutalRelationshipMap = mutualRelationshipMap ++ mutualRelationshipPatchMap
    
    println("Saving country mutual relationships: " + finalMutalRelationshipMap)
    
    CountrySource.updateCountryMutualRelationships(finalMutalRelationshipMap)
  }
  
  /**
   * get from country-mutual-relationship.csv
   */
  def getCountryMutalRelationship() = {
    val nameToCode = CountrySource.loadAllCountries().map( country => (country.name, country.countryCode)).toMap
    val linesIter = scala.io.Source.fromFile("country-mutual-relationship.csv").getLines()
    val headerLine = linesIter.next()
    
    val countryHeader = headerLine.split(',').filter(!_.isEmpty())
    
    val mutualRelationshipMap = Map[(String, String), Int]()
    
    while (linesIter.hasNext) {
      val tokens = linesIter.next().split(',').filter(!_.isEmpty())
      //first token is the country name itself
      val fromCountry = tokens(0)
      for (i <- 1 until tokens.size) {
        val relationship = tokens(i)
        val strength = relationship.count( _ == '1') //just count the number of ones should be sufficient
        val toCountry = countryHeader(i - 1)
        //println(fromCountry + " " + toCountry + " " + strength)
        if (strength > 0) {
          if (nameToCode.contains(fromCountry) && nameToCode.contains(toCountry)) { 
            mutualRelationshipMap.put((nameToCode(fromCountry), nameToCode(toCountry)), strength)
          }
        }
      }
    }
    mutualRelationshipMap
  }

  /**
   * patch from country-mutual-relationship-patch.csv
   */
  def getCountryMutalRelationshipPatch() = {
    val linesIter = scala.io.Source.fromFile("country-mutual-relationship-patch.csv").getLines()
    val headerLine = linesIter.next()
    
    val mutualRelationshipMap = Map[(String, String), Int]()
    
    while (linesIter.hasNext) {
      val tokens = linesIter.next().split(',')
      //first token is the country name itself
      val fromCountry = tokens(0)
      val toCountry = tokens(1)
      val strength = Integer.valueOf(tokens(2))
      mutualRelationshipMap.put((fromCountry, toCountry), strength)
      mutualRelationshipMap.put((toCountry, fromCountry), strength)
    }
    mutualRelationshipMap
  }
  
  
}