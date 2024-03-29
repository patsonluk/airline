package com.patson.init

import com.patson.data.CountrySource

import scala.collection.mutable
import scala.collection.mutable.Map

object CountryMutualRelationshipGenerator extends App {
  /**
   * - Affliation is mutal between all members. Affliations only "upgrade" relations, never decrease
   * - A "5" relation creates a "home market"
   * - Relation is between just the ID country and list countries, and can be from, to, or bidirectional
   * - Relation sets the relationship, regardless of what was there before.
   */
  lazy val OECDish = List("CA","US","FR","DE","AT","CH","IT","GB","ES","NL","BE","PL","DK","SE","IE","JP","KR","AU")

  lazy val AFFILIATIONS = List(

    Affiliation("EU", 3, List(
      "BE", "GR", "LT", "PT", "BG", "ES", "LU", "RO", "CZ", "FR", "HU", "SI", "DK", "HR", "MT", "SK", "DE", "IT", "NL", "FI", "EE", "CY", "AT", "SE", "IE", "LV", "PL"
    )),
    Affiliation("OECD Allies", 3, OECDish),
    Affiliation("Scandinavia and Friends", 3, List("DK","NO","SE","FI","IE") ++ List(
      "GB", "NL", "BE", "DE", "ES", "PT", "GR", "IT", "US", "CA"
    )),
    Affiliation("France", 5, List(
      "FR", "GF", "GP", "MF", "MQ", "RE", "NC", "PF", "PM", "BL"
    )),
    //other UK territories are one-one relationships
    Affiliation("UK Caribbean", 5, List(
      "GB", "TC", "KY", "BM", "VG", "MS", "AI"
    )),
    Affiliation("Denmark", 5, List(
      "DK", "GL", "FO"
    )),
    Affiliation("Netherlands", 5, List(
      "NL", "AW", "BQ", "CW", "SX"
    )),
    Affiliation("US Anglo Caribbean", 2, List(
      "US", "CA", "PR", "AW", "AG", "AI", "BB", "BS", "GY", "JM", "KY", "MS", "TC", "VG", "VI"
    )),
    Affiliation("NAFTA", 4, List(
      "US", "CA", "MX", "PR"
    )),
    Affiliation("COFTA", 4, List(
      "US", "GT", "HN", "SV", "NI", "PR", "DO"
    )),
    Affiliation("US", 5, List(
      "US", "PR", "VI"
    )),
    Affiliation("US Pacific & COFA", 5, List(
      "US", "GU", "AS", "MP", "MH", "PW", "FM"
    )),
    Affiliation("Arab Free Trade Area", 3, List(
      "SA", "EG", "BH", "QA", "AE", "KW", "JO", "LB", "OM", "SD", "IQ", "LY", "MA", "TN"
    )),
    Affiliation("EAC", 4, List(
      "KE", "UG", "SS", "RW", "BI", "TZ"
    )),
//    Affiliation("ECOWAS", 2, List(
//      "BJ", "BF", "CV", "CI", "GM", "GH", "GN", "GW", "LR", "NE", "NG", "SN", "TG"
//    )),
//    Affiliation("ECCAS", 2, List(
//      "AO", "BI", "CM", "TD", "CD", "GQ", "GA", "CG", "RW", "ST"
//    )),
    Affiliation("SADC", 4, List(
      "ZA", "BW", "SZ", "LS", "NA"
    )),
    Affiliation("ASEAN", 3, List(
      "BN", "KH", "ID", "LA", "MY", "PH", "SG", "TH", "VN"
    )),
//    Affiliation("CPTPP", 3, List(
//      "AU", "BN", "CA", "CL", "JP", "MY", "MX", "NZ", "PE", "SG", "VN", "GB"
//    )),
    Affiliation("China", 5, List(
      "CN", "HK", "MO"
    )),
    Affiliation("CIS", 3, List(
      "RU", "BY", "KZ", "KG", "TJ", "UZ", "AZ", "AM"
    ))
  )
  lazy val FRIENDSHIPS = List(
    //pacific
    Relation("AU", Direction.BI, 3, List(
      "SG","TH","ID","PH"
    )),
    Relation("NZ", Direction.BI, 3, List(
      "SG","ID"
    )),
    Relation("NZ", Direction.BI, 4, List(
      "AU","GB","DE","US","CA","JP","KR","HK","MY","TH"
    )),
    //e-asia
    Relation("CN", Direction.BI, 2, List(
      "PK", "ET", "DJ"
    )),
    Relation("JP", Direction.BI, 3, List(
      "PE","BR"
    )),
    Relation("TW", Direction.BI, 4, List(
      "NL","CA","US"
    )),
    Relation("TW", Direction.BI, 3, List(
      "DE","GB","HK","JP","KR","SG","AU"
    )),
    //se-asia
    Relation("ID", Direction.BI, 3, List(
      "AU", "NZ", "CN"
    )),
    Relation("TH", Direction.TO, 3, List(
      "CN","KR","JP","RU","IL","FR","DE","GB","NL","BE","DK","SE","NO","FI","AU","NZ","US"
    )),
    //south-asia
    Relation("IN", Direction.BI, 2, List(
      "GB", "MY", "NP", "LK", "FR", "MY", "AE", "US", "CA", "SG", "SA", "BD"
    )),
    Relation("IN", Direction.BI, 1, List(
      "KE", "ZA", "IL"
    )),
    Relation("PK", Direction.BI, 1, List(
      "US", "GB"
    )),
    //w-asia
    Relation("GE", Direction.BI, 3, List(
      "IL", "TR", "UA", "AE", "AZ", "DE"
    )),
    //europe
    Relation("RU", Direction.BI, 3, List(
      "SY", "EG", "KP", "TM", "IR"
    )),
    Relation("RU", Direction.BI, 2, List(
      "SA", "ZA"
    )),
    Relation("TR", Direction.BI, 3, List(
      "RU" ,"DE", "UA", "BG", "GB", "GE", "FR", "IQ", "IR", "AZ", "KZ"
    )),
    Relation("GR", Direction.TO, 3, List(
      "DE", "BG", "GB", "IT", "FR"
    )),
    //UK territories
    Relation("GB", Direction.BI, 5, List(
      "IM", "BM", "GI", "FK", "GG", "SH"
    )),
    Relation("GB", Direction.BI, 4, List(
      "AU", "NZ"
    )),
    //mena
    Relation("IL", Direction.BI, 1, List(
      "IN", "RO", "PL", "GB", "RU"
    )),
    Relation("IL", Direction.BI, 3, List(
      "CA", "US", "BG"
    )),
    Relation("SA", Direction.BI, 1, List(
      "ID", "SG", "PK", "IN", "BD", "IR", "NG", "TR", "TN", "LY", "DZ"
    )),
    Relation("SA", Direction.BI, 3, List(
      "MY", "PK"
    )),
    Relation("AE", Direction.BI, 3, List(
      "MY", "SG", "ID", "IN", "PK", "LY"
    )),
    Relation("QA", Direction.BI, 3, List(
      "MY", "SG", "ID", "IN", "PK", "LY"
    )),
    Relation("EG", Direction.BI, 3, List(
      "DE", "FR", "UA", "IT", "GB", "LY"
    )),
    Relation("TN", Direction.BI, 3, List(
      "FR", "LY", "AE", "SA"
    )),
    Relation("MA", Direction.BI, 3, List(
      "FR", "GB", "DE", "US"
    )),
    //africa, sub
    Relation("ZA", Direction.BI, 2, List(
      "ZW", "MZ", "GB", "DE", "US", "AU"
    )),
    Relation("ZA", Direction.BI, 1, List(
      "MW", "IN", "TZ", "KE"
    )),
    Relation("ET", Direction.BI, 1, List(
      "CN", "IT", "TR", "EG", "SD", "ZA", "KE", "AE", "US"
    )),
    //americas
    Relation("US", Direction.BI, 3, List(
      "CH", "PT", "GR"
    )),
    Relation("CA", Direction.BI, 3, List(
      "CH", "PT", "GR", "TR"
    )),
    Relation("CL", Direction.BI, 3, List(
      "US","CA","MX","PA","HK","CN","KR","JP","TH","AU","NZ" //chile has FTAs with everyone
    )),
  )
  lazy val ENMITIES = List(
    Relation("KP", Direction.BI, -3, OECDish),
    Relation("KP", Direction.BI, -2, List(
      "BN", "KH", "ID", "LA", "MY", "PH", "SG", "TH", "VN", "BR", "IN", "ZA", "TR"
    )),
    Relation("SY", Direction.BI, -2, OECDish),
    Relation("SY", Direction.BI, -1, List(
      "TR", "GR"
    )),
    Relation("RU", Direction.BI, -3, List(
      "UA", "US", "CA", "JP", "KR", "TW", "AU", "NZ", "GB", "ME", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("BY", Direction.BI, -2, OECDish ++ List(
      "UA", "MD", "CH", "DK", "EE", "FI", "LT", "LV", "GR", "BG"
    )),
    Relation("IR", Direction.BI, -1, OECDish ++ List(
      "SA", "EG", "PK"
    )),
    Relation("IL", Direction.BI, -2, List(
      "IQ", "YE", "LY", "SD", "KW", "QA", "SA", "EG"
    )),
    Relation("IL", Direction.BI, -4, List(
      "IR", "SY", "LB"
    )),
    Relation("PK", Direction.BI, -2, List(
      "IN", "IR"
    )),
    Relation("US", Direction.BI, -3, List(
      "IR", "CU"
    )),
    Relation("CN", Direction.BI, 0, List(
      "JP", "KR"
    )),
    Relation("CN", Direction.BI, -1, List(
      "US", "IN", "AU", "JP"
    )),
    Relation("AM", Direction.BI, -3, List(
      "AZ", "TR"
    )),
    Relation("VE", Direction.TO, -1, List(
      "US", "CO", "BR", "GF", "GY", "SR", "CO", "AR"
    ))
  )


  mainFlow()



  def mainFlow() = {
    var mutualRelationshipMap = getCountryMutualRelationship()
//    val mutualRelationshipPatchMap = getCountryMutualRelationshipPatch()

    mutualRelationshipMap = affiliationAdjustment(mutualRelationshipMap)
    mutualRelationshipMap = relationAdjustment(mutualRelationshipMap, FRIENDSHIPS)
    mutualRelationshipMap = relationAdjustment(mutualRelationshipMap, ENMITIES)

    println("Saving country mutual relationships: " + mutualRelationshipMap)

    CountrySource.updateCountryMutualRelationships(mutualRelationshipMap)

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

  def relationAdjustment(existingMap: mutable.Map[(String, String), Int], adjustmentMap: List[Relation] ): Map[(String, String), Int] = {
    import Direction._
    adjustmentMap.foreach {
      case Relation(id, direction, relationship, members) =>
        members.foreach { member =>
          if (CountrySource.loadCountryByCode(member).isDefined && member != id) {
            if(direction == Direction.TO){
              existingMap.put((member, id), relationship)
              println(s"$member -> $id with $relationship")
            } else if (direction == Direction.FROM) {
              existingMap.put((id, member), relationship)
              println(s"$id -> $member with $relationship")
            } else {
              existingMap.put((id, member), relationship)
              existingMap.put((member, id), relationship)
              println(s"$id <-> $member with $relationship")
            }
          } else {
            println(s"Country code $member not found | duplicate entry")
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
//    val linesIter = scala.io.Source.fromFile("country-mutual-relationship.csv").getLines()
//    val headerLine = linesIter.next()
//
//    val countryHeader = headerLine.split(',').filter(!_.isEmpty())
//
    val mutualRelationshipMap = Map[(String, String), Int]()
//
//    while (linesIter.hasNext) {
//      val tokens = linesIter.next().split(',').filter(!_.isEmpty())
//      //first token is the country name itself
//      val fromCountry = tokens(0)
//      for (i <- 1 until tokens.size) {
//        val relationship = tokens(i)
//        val strength = relationship.count( _ == '1') //just count the number of ones should be sufficient
//        val toCountry = countryHeader(i - 1)
//        //println(fromCountry + " " + toCountry + " " + strength)
//        if (strength > 0) {
//          if (nameToCode.contains(fromCountry) && nameToCode.contains(toCountry)) {
//            mutualRelationshipMap.put((nameToCode(fromCountry), nameToCode(toCountry)), strength)
//          }
//        }
//      }
//    }

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
  case class Relation(id : String, direction : Direction.Value, relationship: Int, members : List[String])

  object Direction extends Enumeration {
    type Direction = Value
    val FROM, TO, BI = Value
  }

  case class Affiliation(id : String, relationship: Int, members : List[String])



}