package models

import com.patson.model.Airline
import com.patson.model.airplane.Model
import com.patson.model.airplane.Airplane
import com.patson.data.BankSource
import com.patson.model.Loan
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.data.CycleSource

abstract class StartupProfile(val title : String, val description : String, val outlines : List[String], val id : Int) {
  def initializeAirline(airline : Airline) : Unit
}

class RevivalProfile extends StartupProfile(title = "Revival of past glory",
      description = "An airline that has previously over-expanded by mismanagement of now retired CEO. It is left with 2 aging Boeing 737-700C and heavy debt. Can you turn this airline around?", 
      List("Own 2 X 50% condition Boeing 737-700C",
					  "20,000,000 cash",
					  "100,000,000 debt (10 years term)",
					  "25 airline reputation"),
		  1) {
	
		override def initializeAirline(airline : Airline) : Unit = {
		  //condition
		  airline.setBalance(20000000)
		  airline.setReputation(25)
		  AirlineSource.saveAirlineInfo(airline)
		  val cycle = CycleSource.loadCycle - 15 * 52 //15 years old
		  
		  val model = ModelSource.loadModelsByCriteria(List(("name", "Boeing 737-700C")))(0)
		  val airplanes = List(
		      Airplane(model, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION / 2, depreciationRate = 0, value = model.price / 2),
		      Airplane(model, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION / 2, depreciationRate = 0, value = model.price / 2))
		  AirplaneSource.saveAirplanes(airplanes)
		  val loan = Loan(airline.id, borrowedAmount = 100000000, interest = 0, remainingAmount = 100000000, creationCycle = 0, loanTerm = 10 * 52)
		  BankSource.saveLoan(loan)
		}
}

class CommuterProfile extends StartupProfile(title = "A humble beginning",
      description = "A newly acquired airline with a modest aircraft fleet of young age. Grow this humble airline into the most powerful and respected brand in the aviation world!", 
      List("Own 8 X 80% condition Cessna 421",
					  "Own 4 X 80% condition Embraer ERJ 140",
					  "30,000,000 cash",
					  "50,000,000 debt (10 years term)",
					  "15 airline reputation"),
		  2) {
	
		override def initializeAirline(airline : Airline) : Unit = {
		  //condition
		  airline.setBalance(30000000)
		  airline.setReputation(15)
		  AirlineSource.saveAirlineInfo(airline)
		  val cycle = CycleSource.loadCycle - 4 * 52 // 4 years old
		  
		  val cessnaModel = ModelSource.loadModelsByCriteria(List(("name", "Cessna 421")))(0)
		  val erjModel = ModelSource.loadModelsByCriteria(List(("name", "Embraer ERJ140")))(0)
		  val airplanes = List(
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(cessnaModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (cessnaModel.price * 0.8).toInt),
		      Airplane(erjModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (erjModel.price * 0.8).toInt),
		      Airplane(erjModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (erjModel.price * 0.8).toInt),
		      Airplane(erjModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (erjModel.price * 0.8).toInt),
		      Airplane(erjModel, owner = airline, constructedCycle = cycle, purchasedCycle = cycle, condition = Airplane.MAX_CONDITION * 0.8, depreciationRate = 0, value = (erjModel.price * 0.8).toInt))
		      
		  AirplaneSource.saveAirplanes(airplanes)
		  val loan = Loan(airline.id, borrowedAmount = 50000000, interest = 0, remainingAmount = 50000000, creationCycle = 0, loanTerm = 10 * 52)
		  BankSource.saveLoan(loan)
		}
}


class EntrepreneurProfile extends StartupProfile(title = "Entrepreneurial spirit",
      description = "You have sold all your assets to create this new airline of your dream! Plan carefully but make bold moves to thrive in this brave new world.", 
      List("50,000,000 cash",
					  "0 airline reputation"),
			3) {
	
 		override def initializeAirline(airline : Airline) : Unit = {
		  //condition
		  airline.setBalance(EntrepreneurProfile.INITIAL_BALANCE)
		  airline.setReputation(0)
		  AirlineSource.saveAirlineInfo(airline)
		}
}
object EntrepreneurProfile {
  	val INITIAL_BALANCE = 50000000  
}

object StartupProfile {
  val profiles = List(new RevivalProfile(), new CommuterProfile(), new EntrepreneurProfile())
  val profilesById = profiles.map(profile => (profile.id, profile)).toMap
}
