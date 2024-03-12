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
  lazy val AFFILIATIONS = List(
    Affiliation("EU, GB, and tourist destinations", 4, List(
      "GB", "MA", "TR", "AM", "AL", "AT", "BA", "BE", "BG", "CH", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "SI", "SK", "ES", "SE", "XK"
    )),
    Affiliation("European Common Aviation Area", 5, List(
      "AL", "AT", "BA", "BE", "BG", "CH", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IE", "IS", "IT", "LT", "LU", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "SI", "SK", "ES", "SE", "XK"
    )),
    Affiliation("France", 5, List(
      "FR", "GF", "GP", "MF", "MQ", "RE", "NC", "PF", "PM", "BL"
    )),
    Affiliation("Great Great Britian", 5, List(
      "GB", "IM", "TC", "KY", "BM", "FK", "GG", "GI", "SH", "VG", "MS", "AI"
    )),
    Affiliation("Denmark", 5, List(
      "DK", "GL", "FO"
    )),
    Affiliation("Netherlands", 5, List(
      "NL", "AW", "BQ", "CW", "SX"
    )),
    Affiliation("Central America and friends", 1, List(
      "US", "MX", "GT", "HN", "SV", "NI", "PA", "CU", "PR", "DO", "CO", "VE"
    )),
//    Affiliation("Caribbean", 2, List(
//      "US", "CA", "AG", "AW", "BS", "BB", "BZ", "BQ", "CU", "CW", "DM", "DO", "GD", "GP", "GY", "HT", "JM", "MQ", "MS", "PR", "BL", "KN", "LC", "MF", "VC", "SX", "TT", "TC", "VG", "VI"
//    )),
    Affiliation("US Anglo Caribbean", 4, List(
      "US", "CA", "PR", "AW", "AG", "AI", "BB", "BS", "GY", "JM", "KN", "KY", "LC", "MS", "TT", "TC", "VC", "VG", "VI"
    )),
    Affiliation("US", 5, List(
      "US", "PR", "VI"
    )),
    Affiliation("US Pacific & COFA", 5, List(
      "US", "GU", "AS", "MP", "MH", "PW", "FM"
    )),
    Affiliation("Australia SAM", 5, List(
      "AU", "CX"
    )),
    Affiliation("Anzac SAM", 5, List(
      "AU", "NZ", "NF", "CK", "NU", "CC"
    )),
    Affiliation("Finance", 2, List(
      "AU", "NZ", "US", "CA", "GB", "IE", "SG", "HK", "BM", "LU", "CH"
    )),
    Affiliation("Anglo", 3, List(
      "AU", "NZ", "US", "CA", "GB", "IE", "ZA"
    )),
    Affiliation("Arab Free Trade Area", 3, List(
      "SA", "EG", "BH", "QA", "AE", "KW", "JO", "LB", "OM", "SD", "IQ", "LY", "MA", "TN"
    )),
    Affiliation("EAC", 4, List(
      "KE", "UG", "SS", "RW", "BI", "TZ"
    )),
    Affiliation("French North Africa", 3, List(
      "FR", "MA", "DZ", "TN"
    )),
    Affiliation("French Africa", 2, List(
      "FR", "BJ", "CI", "GN", "ML", "MR", "NE", "SN", "TG"
    )),
    Affiliation("Alliance of Sahel States", 3, List(
      "ML", "BF", "NE"
    )),
    Affiliation("ECOWAS", 2, List(
      "BJ", "BF", "CV", "CI", "GM", "GH", "GN", "GW", "LR", "NE", "NG", "SN", "TG"
    )),
    Affiliation("ECCAS", 2, List(
      "AO", "BI", "CM", "TD", "CD", "GQ", "GA", "CG", "RW", "ST"
    )),
    Affiliation("SADC", 4, List(
      "ZA", "BW", "SZ", "LS", "NA"
    )),
    Affiliation("Andean", 4, List(
      "BO", "CO", "PE", "EC"
    )),
    Affiliation("ASEAN", 2, List(
      "BN", "KH", "ID", "LA", "MY", "PH", "SG", "TH", "VN"
    )),
    Affiliation("China", 5, List(
      "CN", "HK", "MF"
    )),
    Affiliation("CIS", 4, List(
      "RU", "BY", "KZ", "KG", "TJ", "UZ", "AZ", "AM"
    ))
  )

  lazy val FRIENDSHIPS = List(
    //pacific
    Relation("AU", Direction.FROM, 2, List(
      "ID", "TH", "FJ",
    )),
    //south-east-asia
    Relation("ID", Direction.FROM, 1, List(
      "US", "NL", "AU", "SA", "QA", "AE", "BH"
    )),
    Relation("PH", Direction.FROM, 2, List(
      "US", "ES", "NL", "AU", "KR", "SA", "QA", "AE", "BH"
    )),
    Relation("SG", Direction.BI, 4, List(
      "MY", "ID", "TH"
    )),
    Relation("TH", Direction.BI, 4, List(
      "MY", "ID", "TH"
    )),
    //east-asia
    Relation("TW", Direction.BI, 4, List(
      "HK"
    )),
    Relation("TW", Direction.BI, 2, List(
      "JP", "KR", "SG", "MY", "IT", "DE", "GB", "US"
    )),
    Relation("CN", Direction.BI, 1, List(
      "PK", "ET", "DJ"
    )),
    //south-asia
    Relation("IN", Direction.BI, 3, List(
      "GB", "MY", "NP"
    )),
    Relation("IN", Direction.BI, 2, List(
      "US", "CA", "SG", "SA", "BD"
    )),
    Relation("IN", Direction.FROM, 1, List(
      "AU", "AE", "SG", "MY"
    )),
    Relation("PK", Direction.BI, 2, List(
      "US", "GB"
    )),
    //europe
    Relation("RU", Direction.BI, 3, List(
      "IR", "SY", "KP", "TM"
    )),
    Relation("RU", Direction.FROM, 2, List(
      "AE", "AM", "GE", "TR", "CN", "MN", "LK"
    )),
    Relation("RU", Direction.BI, 1, List(
      "SA", "ZA"
    )),
    Relation("GB", Direction.TO, 4, List(
      "AU", "NZ",
    )),
    Relation("ES", Direction.TO, 2, List(
      "MX", "AR", "CO"
    )),
    Relation("PT", Direction.TO, 2, List(
      "BR", "AO", "MZ"
    )),
    Relation("IT", Direction.TO, 2, List(
      "AR", "BR"
    )),
    //middle-east
    Relation("IL", Direction.FROM, 1, List(
      "JP", "AE", "IN", "FR", "GR", "IT", "MA", "RU", "ES", "CH", "UA"
    )),
    Relation("IL", Direction.BI, 3, List(
      "CA", "US"
    )),
    //haji
    Relation("SA", Direction.TO, 2, List(
      "ID", "MY", "SG", "PK", "IN", "BD", "EG", "IR", "NG", "TR", "TN", "DZ", "MA"
    )),
    //americas
    Relation("MX", Direction.FROM, 4, List(
      "US"
    )),
    Relation("MX", Direction.FROM, 3, List(
      "GT", "DO", "JM", "PR", "ES"
    )),
    Relation("MX", Direction.TO, 2, List(
      "CU", "GT", "HN", "SV", "NI", "CU", "PR", "DO", "CO", "VE"
    )),
    Relation("MX", Direction.FROM, 2, List(
      "KR", "JP"
    )),
    Relation("US", Direction.FROM, 4, List(
      "DO", "JM", "CA", "MX"
    )),
    Relation("US", Direction.BI, 4, List(
      "GB", "IE", "NL", "AU", "NZ"
    )),
    Relation("US", Direction.BI, 3, List(
      "FR", "BE", "DE", "ES"
    )),
    Relation("CA", Direction.FROM, 4, List(
      "IE", "GB", "NL", "US", "MX"
    )),
    //africa
    Relation("ZA", Direction.BI, 2, List(
      "ZW", "ZM", "MZ", "AO"
    )),
    Relation("ZA", Direction.BI, 1, List(
      "MW", "IN", "TZ", "KE"
    )),
    Relation("ET", Direction.FROM, 2, List(
      "SA", "AE", "US", "GB"
    )),
    Relation("ET", Direction.BI, 1, List(
      "CN", "IT", "TR", "EG", "SD", "ZA", "KE"
    )),
    Relation("NG", Direction.FROM, 2, List(
      "ZA", "ET", "KE", "AE", "US", "GB"
    )),
  )
  lazy val ENMITIES = List(
    Relation("KP", Direction.BI, -3, List(
      "US", "CA", "JP", "KR", "TW", "AU", "NZ", "ME", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("KP", Direction.BI, -2, List(
      "BN", "KH", "ID", "LA", "MY", "PH", "SG", "TH", "VN", "BR", "IN", "ZA", "TR"
    )),
    Relation("SY", Direction.BI, -1, List(
      "US", "CA", "JP", "KR", "TW", "AU", "NZ", "ME", "TR", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("RU", Direction.BI, -2, List(
      "UA", "US", "CA", "JP", "KR", "TW", "AU", "NZ", "GB", "ME", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("BY", Direction.BI, -2, List(
      "UA", "US", "CA", "JP", "KR", "TW", "AU", "NZ", "GB", "ME", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("IR", Direction.BI, -1, List(
      "EG", "BH", "QA", "KW", "JO", "LB", "OM", "SD", "IQ", "LY", "MA", "TN", "CA", "JP", "KR", "AU", "NZ", "ME", "IE", "RO", "BG", "CY", "AT", "BE", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HR", "HU", "IS", "IT", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "SK", "ES", "SE", "CH"
    )),
    Relation("IR", Direction.BI, -2, List(
      "GB", "SA"
    )),
    Relation("IL", Direction.BI, -2, List(
      "IQ", "YE", "LY", "SD", "KW", "QA", "SA", "EG"
    )),
    Relation("IL", Direction.BI, -4, List(
      "IR", "SY", "LB"
    )),
    Relation("PK", Direction.BI, -1, List(
      "IN", "IR"
    )),
    Relation("US", Direction.BI, -3, List(
      "IR", "CU"
    )),
    Relation("CN", Direction.BI, 0, List(
      "JP", "KR"
    )),
    Relation("CN", Direction.BI, -1, List(
      "US", "IN", "AU"
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
    val mutualRelationshipPatchMap = getCountryMutualRelationshipPatch()

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
  case class Relation(id : String, direction : Direction.Value, relationship: Int, members : List[String])

  object Direction extends Enumeration {
    type Direction = Value
    val FROM, TO, BI = Value
  }

  case class Affiliation(id : String, relationship: Int, members : List[String])



}