package models

import com.patson.model.Loan
import com.patson.model.Airport
import com.patson.model.airplane.Airplane

case class Profile(name : String, description : String, cash : Int, airport : Airport, awareness : Int = 0, reputation : Int = 0, airplanes : List[Airplane] = List.empty, loan : Option[Loan] = None)
