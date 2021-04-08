package com.patson.init

import com.patson.data.CountrySource

import scala.collection.mutable
import scala.collection.mutable.Map

object CountryMutualRelationshipGenerator extends App {
  lazy val AFFILIATIONS = List(
    Affiliation("EU", 2, List(
      "BE", "GR", "LT", "PT", "BG", "ES", "LU", "RO", "CZ", "FR", "HU", "SI", "DK", "HR", "MT", "SK", "DE", "IT", "NL", "FI", "EE", "CY", "AT", "SE", "IE", "LV", "PL"
    ))
  )


  mainFlow()



  def mainFlow() = {
    val mutualRelationshipMap = getCountryMutualRelationship()
    val mutualRelationshipPatchMap = getCountryMutualRelationshipPatch()


    val finalMutualRelationshipMap = affiliationAdjustment(mutualRelationshipMap ++ mutualRelationshipPatchMap)

    println("Saving country mutual relationships: " + finalMutualRelationshipMap)

    CountrySource.updateCountryMutualRelationships(finalMutualRelationshipMap)

    println("DONE")
  }

  def affiliationAdjustment(existingMap : mutable.Map[(String, String), Int]) : Map[(String, String), Int] = {
    println(s"affiliations: $AFFILIATIONS")
    AFFILIATIONS.foreach {
      case Affiliation(id, relationship, members) =>
        members.foreach { memberX =>
          if (CountrySource.loadCountryByCode(memberX).isDefined) {
            members.foreach { memberY =>
              if (memberX != memberY) {
                val shouldPatch = existingMap.get((memberX, memberY)) match {
                  case Some(existingValue) => existingValue < relationship
                  case None => true
                }
                if (shouldPatch) {
                  println(s"patching $memberX vs $memberY from $id with $relationship")
                  existingMap.put((memberX, memberY), relationship)
                } else {
                  println(s"Not patching $memberX vs $memberY from $id with $relationship as existing value is greater")
                }
              }
            }
          } else {
            println(s"Country code $memberX not found")
          }
        }
    }
    existingMap
  }

  /**
   * get from country-mutual-relationship.csv
   */
  def getCountryMutualRelationship() = {
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

    nameToCode.values.foreach { countryCode =>
      mutualRelationshipMap.put((countryCode, countryCode), 5) //country with itself is 5 HomeCountry
    }

    mutualRelationshipMap
  }

  /**
   * patch from country-mutual-relationship-patch.csv
   */
  def getCountryMutualRelationshipPatch() = {
    val linesIter = scala.io.Source.fromFile("country-mutual-relationship-patch.csv").getLines()
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

  case class Affiliation(id : String, relationship: Int, members : List[String])



}