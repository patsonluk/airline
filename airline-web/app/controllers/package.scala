import com.patson.data._
import com.patson.data.airplane._
import com.patson.model._
import com.patson.model.airplane._
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsValue
import com.patson.model.Computation
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import com.patson.Util
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json



package object controllers {
  implicit object AirplaneModelWrites extends Writes[Model] {
    def writes(airplaneModel: Model): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplaneModel.id),
      "name" -> JsString(airplaneModel.name),
      "capacity" -> JsNumber(airplaneModel.capacity),
      "fuelBurn" -> JsNumber(airplaneModel.fuelBurn),
      "speed" -> JsNumber(airplaneModel.speed),
      "range" -> JsNumber(airplaneModel.range),
      "price" -> JsNumber(airplaneModel.price)))
      
    }
  }
  
  implicit object AirplaneWrites extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplane.id),
      "ownerId" -> JsNumber(airplane.owner.id), 
      "name" -> JsString(airplane.model.name),
      "modelId" -> JsNumber(airplane.model.id),
      "capacity" -> JsNumber(airplane.model.capacity),
      "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
      "speed" -> JsNumber(airplane.model.speed),
      "range" -> JsNumber(airplane.model.range),
      "price" -> JsNumber(airplane.model.price),
      "condition" -> JsNumber(airplane.condition),
      "age" -> JsNumber(Computation.calculateAge(airplane.constructedCycle)),
      "value" -> JsNumber(Computation.calculateAirplaneValue(airplane))))
    }
  }
  
  implicit object LinkFormat extends Format[Link] {
    def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      //val capacity = json.\("capacity").as[Int]
      val price = json.\("price").as[Int]
      val fromAirport = AirportSource.loadAirportById(fromAirportId).get
      val toAirport = AirportSource.loadAirportById(toAirportId).get
      val airline = AirlineSource.loadAirlineById(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
      val airplaneIds = json.\("airplanes").as[List[Int]]
      val frequency = json.\("frequency").as[Int]
      val modelId = json.\("model").as[Int]
      val capacity = frequency * ModelSource.loadModelById(modelId).fold(0)(_.capacity) 
      val duration = ModelSource.loadModelById(modelId).fold(Integer.MAX_VALUE)(Computation.calculateDuration(_, distance))
       
      val airplanes = airplaneIds.foldRight(List[Airplane]()) { (airplaneId, foldList) =>
        AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId) match { 
          case Some((airplane, Some(link))) if (link.from.id != fromAirport.id || link.to.id != toAirport.id) =>
            throw new IllegalStateException("Airplane with id " + airplaneId + " is assigned to other link")
          case Some((airplane, _)) =>
            airplane :: foldList
          case None => 
            throw new IllegalStateException("Airplane with id " + airplaneId + " does not exist")
        }
      }
      var rawQuality =  json.\("rawQuality").as[Int]
      if (rawQuality > Link.MAX_RAW_QUALITY) {
        rawQuality = Link.MAX_RAW_QUALITY
      } else if (rawQuality < 0) {
        rawQuality = 0
      }
         
      val link = Link(fromAirport, toAirport, airline, price, distance, capacity, rawQuality, duration, frequency)
      link.setAssignedAirplanes(airplanes)
      (json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
    
    def writes(link: Link): JsValue = JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportId" -> JsNumber(link.from.id),
      "toAirportId" -> JsNumber(link.to.id),
      "fromAirportCode" -> JsString(link.from.iata),
      "toAirportCode" -> JsString(link.to.iata),
      "fromAirportName" -> JsString(link.from.name),
      "toAirportName" -> JsString(link.to.name),
      "fromAirportCity" -> JsString(link.from.city),
      "toAirportCity" -> JsString(link.to.city),
      "airlineId" -> JsNumber(link.airline.id),
      "price" -> JsNumber(link.price),
      "distance" -> JsNumber(link.distance),
      "capacity" -> JsNumber(link.capacity),
      "rawQuality" -> JsNumber(link.rawQuality),
      "computedQuality" -> JsNumber(link.computedQuality),
      "duration" -> JsNumber(link.duration),
      "frequency" -> JsNumber(link.frequency),
      "availableSeat" -> JsNumber(link.availableSeats),
      "fromLatitude" -> JsNumber(link.from.latitude),
      "fromLongitude" -> JsNumber(link.from.longitude),
      "toLatitude" -> JsNumber(link.to.latitude),
      "toLongitude" -> JsNumber(link.to.longitude),
      "assignedAirplanes" -> Json.toJson(link.getAssignedAirplanes())))
  }
}