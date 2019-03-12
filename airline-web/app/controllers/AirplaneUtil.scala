package controllers

import com.patson.model.airplane.Model
import com.patson.model.Airline
import com.patson.data.CycleSource
import scala.collection.mutable.ListBuffer
import com.patson.model.airplane.Airplane
import com.patson.data.AirlineSource
import com.patson.data.CountrySource
import com.patson.data.AirplaneSource
import com.patson.model.AirlineCashFlowItem
import com.patson.model.CashFlowType

object AirplaneUtil {
  def buyNewAirplanes(airline : Airline, model : Model, quantity : Int) : Either[String, List[Airplane]] = {
      val currentCycle = CycleSource.loadCycle()
      val airlineId = airline.id
      val constructedCycle = currentCycle + model.constructionTime
      
      
      val rejectionOption = getRejection(model, airline)
      if (rejectionOption.isDefined) {
        Left(rejectionOption.get)   
      } else {
        
        val amount = -1 * model.price * quantity
        AirlineSource.adjustAirlineBalance(airlineId, amount)
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, amount))
        
        val airplanes = ListBuffer[Airplane]()
        for (i <- 0 until quantity) {
          airplanes.append(Airplane(model, airline, constructedCycle = constructedCycle , Airplane.MAX_CONDITION, depreciationRate = 0, value = model.price))
        }
        
        val updateCount = AirplaneSource.saveAirplanes(airplanes.toList)
        if (updateCount > 0) {
            Right(airplanes.toList)
        } else {
            Left("Cannot save airplane")
        }
      }
  }
  
  
  
  def getRejection(model: Model, airline : Airline) : Option[String] = {
    val countryRelation = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationship(homeCountry, model.countryCode)
      case None => 0
    }
    
    val ownedModels = AirplaneSource.loadAirplanesByOwner(airline.id).map(_.model).toSet
    getRejection(model, countryRelation, ownedModels, airline)
  }
  
  def getRejection(model: Model, countryRelationship : Int, ownedModels : Set[Model], airline : Airline) : Option[String]= {
    if (!model.purchasable(countryRelationship)) {
      return Some("The company refuses to sell " + model.name + " to your airline due to bad country relationship")
    }
    
    if (!ownedModels.contains(model) && ownedModels.size >= airline.airlineGrade.getModelsLimit) {
      return Some("Can only own up to " + airline.airlineGrade.getModelsLimit + " different airplane model(s) at current airline grade")
    }
    
    if (model.price > airline.getBalance()) {
      return Some("Not enough cash to purchase this airplane model")
    }
    
    return None
  }
  
  def getRejections(models : List[Model], airline : Airline) : Map[Model, Option[String]] = {
     
    val countryRelations : Map[String, Int] = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationships(homeCountry)
      case None => Map.empty
    }    
    
    val ownedModels = AirplaneSource.loadAirplanesByOwner(airline.id).map(_.model).toSet
    models.map { model =>
      (model, getRejection(model, countryRelations.getOrElse(model.countryCode, 0), ownedModels, airline))
    }.toMap
    
  }
}