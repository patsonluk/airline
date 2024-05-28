package com.patson.init

import com.patson.data.CountrySource

import scala.collection.mutable
import scala.collection.mutable.Map

object CountryMutualRelationshipGenerator extends App {
  /**
   * - Affliation is mutal between all members. Affliations only "upgrade" relations, never decrease
   * - A "5" relation creates a "home market"
   *
   * Some relations set in the computation function!
   */
  lazy val OECDish = List("CA","US","FR","DE","AT","CH","IT","GB","ES","NL","BE","PL","DK","SE","IE","JP","KR","AU","SG")

  lazy val AFFILIATIONS = List(

    Affiliation("OECD Allies", 3, OECDish),

    Affiliation("EU", 4, List(
      "BE", "GR", "LT", "PT", "BG", "ES", "LU", "RO", "CZ", "FR", "HU", "SI", "DK", "HR", "MT", "SK", "DE", "IT", "NL", "FI", "EE", "CY", "AT", "SE", "IE", "LV", "PL"
    )),
    Affiliation("Scandinavia / NATO", 4, List("DK","NO","SE","FI","IS") ++ List(
      "IE", "GB", "NL", "BE", "DE", "PL", "ES", "PT", "GR", "IT", "US", "CA"
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
    Affiliation("COFTA", 3, List(
      "US", "GT", "HN", "SV", "NI", "PR", "DO"
    )),
    Affiliation("ANZAC common market", 5, List(
      "AU", "NZ", "CX", "CK", "NU", "CC"
    )),
    Affiliation("Arab Free Trade Area", 3, List(
      "SA", "EG", "BH", "QA", "AE", "KW", "JO", "LB", "OM", "SD", "IQ", "LY", "MA", "TN"
    )),
    Affiliation("GCC+", 4, List(
      "SA", "EG", "BH", "QA", "AE", "KW", "JO", "OM"
    )),
    Affiliation("EAC", 4, List(
      "KE", "UG", "SS", "RW", "BI", "TZ"
    )),
    Affiliation("Comunidad Andina", 4, List(
      "BO", "EC", "PE", "CO"
    )),
//    Affiliation("ECOWAS", 2, List(
//      "BJ", "BF", "CV", "CI", "GM", "GH", "GN", "GW", "LR", "NE", "NG", "SN", "TG"
//    )),
//    Affiliation("ECCAS", 2, List(
//      "AO", "BI", "CM", "TD", "CD", "GQ", "GA", "CG", "RW", "ST"
//    )),
    Affiliation("SADC+", 3, List(
      "ZA", "BW", "SZ", "LS", "NA", "ZM", "ZW"
    )),
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
      "SG","ID","JP","HK"
    )),
    Relation("NZ", Direction.BI, 4, List(
      "AU","GB","DE","US","CA","JP","KR","MY","TH"
    )),
    //e-asia
    Relation("CN", Direction.BI, 4, List(
      "KH", "PK", "RU"
    )),
    Relation("CN", Direction.BI, 2, List(
      "KP", "ET", "DJ", "VN", "IR", "SG", "MY", "MM"
    )),
    Relation("JP", Direction.BI, 2, List(
      "PE", "BR", "IN", "VN", "TH", "FJ", "PG", "SB"
    )),
    Relation("KR", Direction.BI, 4, List(
      "SG", "JP", "TW", "US"
    )),
    Relation("KR", Direction.BI, 2, List(
      "MY", "TH", "VN"
    )),
    Relation("TW", Direction.BI, 4, List(
      "NL","CA","US","JP","HK"
    )),
    Relation("TW", Direction.BI, 3, List(
      "DE","GB","JP","KR","SG","AU"
    )),
    //se-asia
    Relation("ID", Direction.BI, 3, List(
      "AU", "NZ", "JP"
    )),
    Relation("ID", Direction.BI, 2, List(
      "IN", "CN", "KR", "AE", "SA", "NL"
    )),
    Relation("PH", Direction.BI, 3, List(
      "US", "JP"
    )),
    Relation("PH", Direction.BI, 2, List(
      "KR", "AU", "AE", "SA"
    )),
    Relation("VN", Direction.BI, 2, List(
      "TW", "JP", "DE", "SA", "GB"
    )),
    Relation("TH", Direction.BI, 3, List(
      "CN", "JP", "KR", "US"
    )),
    Relation("TH", Direction.TO, 2, List(
      "RU","IL","FR","DE","GB","NL","BE","DK","SE","NO","FI","AU","NZ","US"
    )),
    Relation("MY", Direction.TO, 2, List(
      "RU", "IL", "FR", "DE", "GB", "NL", "BE", "DK", "SE", "NO", "FI", "AU", "NZ", "US", "JP", "TW", "KR"
    )),
    //south-asia
    Relation("IN", Direction.BI, 4, List(
      "BT"
    )),
    Relation("IN", Direction.BI, 3, List(
      "NP", "LK", "BD", "AE"
    )),
    Relation("IN", Direction.BI, 2, List(
      "GB", "FR", "MY", "MM", "US", "CA", "SG", "SA", "KW", "OM", "QA", "PH", "ID", "JP", "ZA", "KR"
    )),
    Relation("IN", Direction.BI, 1, List(
      "KE", "IL", "AF", "RU"
    )),
    Relation("BD", Direction.BI, 2, List(
      "SG", "MY", "AE", "SA", "GB", "US", "CA", "CN", "IT"
    )),
    Relation("PK", Direction.BI, 2, List(
      "AE", "SA", "GB", "US"
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
    Relation("TR", Direction.BI, 4, List(
      "AZ"
    )),
    Relation("TR", Direction.BI, 3, List(
      "QA", "KZ", "UZ"
    )),
    Relation("TR", Direction.BI, 2, List(
      "RU", "DE", "UA", "BG", "GB", "GE", "FR", "IQ", "IR", "AZ", "US", "CA", "PK"
    )),
    Relation("GR", Direction.TO, 3, List(
      "DE", "BG", "GB", "IT", "FR"
    )),
    Relation("RS", Direction.BI, 2, List(
      "DE", "FR", "RU", "TR", "RO", "CY"
    )),
    Relation("BA", Direction.BI, 2, List(
      "DE", "FR", "IT", "HU"
    )),
    Relation("FR", Direction.BI, 4, List(
      "GB", "US", "CA"
    )),
    Relation("FR", Direction.BI, 2, List(
      "TN", "DZ", "MA", "SN", "CI"
    )),
    Relation("IT", Direction.BI, 2, List(
      "TN", "MA", "DZ", "IL", "TR",
    )),
    Relation("CH", Direction.BI, 4, List(
      "FR","DE","AT","IT","ES","NL","BE","DK","SE"
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
      "CA", "US", "BG", "SG"
    )),
    Relation("SA", Direction.BI, 1, List(
      "SG", "CN", "IN", "BD", "NG"
    )),
    Relation("SA", Direction.BI, 2, List(
      "BN", "DZ", "LY", "TR", "TN"
    )),
    Relation("SA", Direction.BI, 3, List(
      "MY", "PK"
    )),
    Relation("AE", Direction.BI, 3, List(
      "MY", "SG", "IN", "PK", "LY"
    )),
    Relation("QA", Direction.BI, 3, List(
      "MY", "SG", "IN", "PK", "LY"
    )),
    Relation("EG", Direction.BI, 2, List(
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
      "ZW", "MZ", "GB", "DE", "US", "AU", "MW", "IN", "TZ", "KE", "CN", "TZ", "ET"
    )),
    Relation("ET", Direction.BI, 2, List(
      "EG", "CN", "KE", "AE"
    )),
    Relation("ET", Direction.BI, 1, List(
      "IT", "SD", "US"
    )),
    //americas
    Relation("US", Direction.BI, 4, List(
      "JP", "KR", "TW", "AU", "NZ", "BM", "BS"
    )),
    Relation("US", Direction.BI, 4, List( //US overseas / COFA
      "US", "PR", "VI", "GU", "AS", "MP", "MH", "PW", "FM"
    )),
    Relation("CO", Direction.BI, 3, List(
      "US", "PE", "EC", "PA", "CL"
    )),
    Relation("CO", Direction.BI, 2, List(
      "MX", "BR", "BO"
    )),
    Relation("PE", Direction.BI, 2, List(
      "CL", "BO", "EC", "CO", "MX", "US", "CN", "JP"
    )),
    Relation("BR", Direction.BI, 3, List(
      "AR", "BO", "PY", "UY", "PE", "CL"
    )),
    Relation("BR", Direction.BI, 2, List(
      "BO", "CO", "MX", "US", "CN", "ZA", "AO", "PT", "JP", "DE"
    )),
    Relation("BR", Direction.BI, 1, List(
      "IN", "FR"
    )),
    Relation("CL", Direction.BI, 3, List(
      "PE","AR","US","CA","PA" //chile has FTAs with everyone
    )),
    Relation("CL", Direction.BI, 2, List(
      "MX", "JP", "KR", "CN", "HK", "AU", "NZ"
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
      "UA", "MD", "EE", "LT", "LV", "BG"
    )),
    Relation("IR", Direction.BI, -1, OECDish ++ List(
      "SA", "EG", "PK"
    )),
    Relation("IL", Direction.BI, -2, List(
      "IQ", "YE", "LY", "SD", "KW", "SA", "EG"
    )),
    Relation("IL", Direction.BI, -4, List(
      "IR", "SY", "LB"
    )),
    Relation("PK", Direction.BI, -2, List(
      "IN", "IR"
    )),
    Relation("TR", Direction.BI, -1, List(
      "GR", "AM"
    )),
    Relation("US", Direction.BI, -3, List(
      "IR", "CU"
    )),
    Relation("CN", Direction.BI, 0, List(
      "KR"
    )),
    Relation("CN", Direction.BI, -1, List(
      "US", "IN", "AU", "JP"
    )),
    Relation("AM", Direction.BI, -3, List(
      "AZ"
    )),
    Relation("VE", Direction.TO, -1, List(
      "US", "CO", "BR", "GF", "GY", "SR", "AR"
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