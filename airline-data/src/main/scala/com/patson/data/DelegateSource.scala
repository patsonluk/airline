package com.patson.data

import java.sql.{Statement, Types}

import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.{AirlineCache, AirportCache, CountryCache}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object DelegateSource {
  private[this] val BASE_BUSY_DELEGATE_QUERY = "SELECT * FROM " + BUSY_DELEGATE_TABLE


  /**
    * Load either delegates with task in progress or on cool down
    * @param airlineId
    * @return
    */
  def loadBusyDelegatesByAirline(airlineId : Int) : List[BusyDelegate] = {
    loadBusyDelegatesByCriteria(List(("airline", "=", airlineId))).get(airlineId).getOrElse(List.empty)
  }
  

  def loadBusyDelegatesByCriteria(criteria : List[(String, String, Any)]) = {
      var queryString = BASE_BUSY_DELEGATE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + criteria.last._2 + " ?"
      }
      loadBusyDelegatesByQueryString(queryString, criteria.map(_._3))
  }

  /**
    *
    * @param queryString
    * @param parameters
    * @return key is airline Id
    */
  def loadBusyDelegatesByQueryString(queryString : String, parameters : List[Any]) : Map[Int, List[BusyDelegate]]= {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }

        
        val resultSet = preparedStatement.executeQuery()
        
        val result = mutable.Map[Int, ListBuffer[DelegateLoadInfo]]() //key: Airline id


        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val delegateId = resultSet.getInt("id")
          val taskType = DelegateTaskType(resultSet.getInt("task_type"))
          val availableCycleObject = resultSet.getObject("available_cycle")
          val availableCycle = if (availableCycleObject == null) None else Some(availableCycleObject.asInstanceOf[Int])

          result.getOrElseUpdate(airlineId, ListBuffer[DelegateLoadInfo]()).append(DelegateLoadInfo(delegateId, taskType, availableCycle))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        result.toList.map {
          case (airlineId, delegateInfoEntries: ListBuffer[DelegateLoadInfo]) => {
            val airline : Airline = AirlineCache.getAirline(airlineId).get
            val delegateTaskByDelegateId = loadDelegateTasks(delegateInfoEntries.map {
              case DelegateLoadInfo(delegateId, taskType, _) => (delegateId, taskType)
            }.toMap)

            val delegates : List[BusyDelegate] = delegateInfoEntries.toList.map {
              case (DelegateLoadInfo(delegateId, _, availableCycle)) => BusyDelegate(airline, delegateTaskByDelegateId(delegateId), availableCycle, delegateId)
            }

            (airlineId, delegates)
          }
        }.toMap
      } finally {
        connection.close()
      }
  }

  case class DelegateLoadInfo(id : Int, taskType : DelegateTaskType.Value, availableCycle : Option[Int])

  /**
    *
    * @param delegateIdAndTaskTypes
    * @return key delegate Id
    */
  def loadDelegateTasks(delegateIdAndTaskTypes : Map[Int, DelegateTaskType.Value]) : Map[Int, DelegateTask] = {
    val result = mutable.HashMap[Int, DelegateTask]()
    delegateIdAndTaskTypes.toList.groupBy(_._2).foreach {
      case(taskType, grouped) => {
        val delegateIdsOfThisTaskType = grouped.map(_._1)
        taskType match {
          case DelegateTaskType.LINK_NEGOTIATION =>
            result.addAll(loadLinkNegotiationTasks(delegateIdsOfThisTaskType))
          case DelegateTaskType.COUNTRY =>
            result.addAll(loadCountryTasks(delegateIdsOfThisTaskType))
        }
      }
    }
    result.toMap
  }

  /**
    *
    * @param delegateIds
    * @return key - delegateId
    */
  def loadCountryTasks(delegateIds : List[Int]) = {
    val connection = Meta.getConnection()
    try {
      val delegateIdPhrase = delegateIds.mkString(",")
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $COUNTRY_DELEGATE_TASK_TABLE WHERE delegate IN ($delegateIdPhrase)")
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.Map[Int, DelegateTask]() //key delegateId


      while (resultSet.next()) {
        val delegateId = resultSet.getInt("delegate")
        val country = CountryCache.getCountry(resultSet.getString("country_code")).get
        val startCycle = resultSet.getInt("start_cycle")

        result.put(delegateId, DelegateTask.country(startCycle, country))
      }

      resultSet.close()
      preparedStatement.close()

      result.toMap
    } finally {
      connection.close()
    }

  }

  /**
    *
    * @param delegateIds
    * @return key - delegateId
    */
  def loadLinkNegotiationTasks(delegateIds : List[Int]) = {
    val connection = Meta.getConnection()
    try {
      val delegateIdPhrase = delegateIds.mkString(",")
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $LINK_NEGOTIATION_TASK_TABLE WHERE delegate IN ($delegateIdPhrase)")
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.Map[Int, DelegateTask]() //key delegateId


      while (resultSet.next()) {
        val delegateId = resultSet.getInt("delegate")
        val fromAirport = AirportCache.getAirport(resultSet.getInt("from_airport")).get
        val toAirport = AirportCache.getAirport(resultSet.getInt("to_airport")).get
        val startCycle = resultSet.getInt("start_cycle")

        result.put(delegateId, DelegateTask.linkNegotiation(startCycle, fromAirport, toAirport))
      }

      resultSet.close()
      preparedStatement.close()

      result.toMap
    } finally {
      connection.close()
    }

  }


  def saveBusyDelegates(delegates : List[BusyDelegate]) : Unit = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + BUSY_DELEGATE_TABLE + "(airline, task_type, available_cycle) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)
    try {
      delegates.foreach { delegate =>
        preparedStatement.setInt(1, delegate.airline.id)
        preparedStatement.setInt(2, delegate.assignedTask.getTaskType.id)
        delegate.availableCycle match {
          case Some(availableCycle) => preparedStatement.setInt(3, availableCycle)
          case None => preparedStatement.setNull(3, Types.INTEGER)
        }

        val updateCount = preparedStatement.executeUpdate()
        if (updateCount > 0) {
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            //  println("Id is " + generatedId)
            //try to save assigned airplanes if any
            delegate.id = generatedId

          }
        }
      }
      saveDelegateTasks(delegates)
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

  def deleteBusyDelegates(delegates : List[BusyDelegate]) : Unit = {
    delegates.foreach { delegate =>
      deleteBusyDelegateByCriteria(List(("id", "=", delegate.id)))
    }
  }

  def saveDelegateTasks(delegates : List[BusyDelegate]) = {
    delegates.groupBy(_.assignedTask.getTaskType).foreach {
      case (taskType, delegatesOfThisTaskType) => {
        taskType match {
          case DelegateTaskType.COUNTRY =>
            saveCountryTasks(delegatesOfThisTaskType)
          case DelegateTaskType.LINK_NEGOTIATION =>
            saveLinkNegotiationTasks(delegatesOfThisTaskType)
        }
      }
    }
  }

  def saveCountryTasks(delegates : List[BusyDelegate]) = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + COUNTRY_DELEGATE_TASK_TABLE + "(delegate, country_code, start_cycle) VALUES(?,?,?)")
    try {
      delegates.foreach { delegate =>
        preparedStatement.setInt(1, delegate.id)
        preparedStatement.setString(2, delegate.assignedTask.asInstanceOf[CountryDelegateTask].country.countryCode)
        preparedStatement.setInt(3, delegate.assignedTask.getStartCycle)

        preparedStatement.executeUpdate()
      }
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

  def saveLinkNegotiationTasks(delegates : List[BusyDelegate]) = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_NEGOTIATION_TASK_TABLE + "(delegate, from_airport, to_airport, start_cycle) VALUES(?,?,?,?)")
    try {
      delegates.foreach { delegate =>
        preparedStatement.setInt(1, delegate.id)
        preparedStatement.setInt(2, delegate.assignedTask.asInstanceOf[LinkNegotiationDelegateTask].fromAirport.id)
        preparedStatement.setInt(3, delegate.assignedTask.asInstanceOf[LinkNegotiationDelegateTask].toAirport.id)
        preparedStatement.setInt(4, delegate.assignedTask.getStartCycle)

        preparedStatement.executeUpdate()
      }
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }
 

  
  
  def deleteBusyDelegateByCriteria(criteria : List[(String, String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + BUSY_DELEGATE_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + criteria.last._2 + " ?"
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._3)
      }
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " busy delegate records")
      deletedCount
    } finally {
      connection.close()
    }
  }



  def loadCountryDelegateByAirlineAndCountry(airlineId : Int, countryCode : String) : List[BusyDelegate] = {
    loadCountryDelegateByAirline(airlineId).get(countryCode) match {
      case Some(delegates) => delegates
      case None => List.empty
    }
  }


  /**
    *
    * @param airlineId
    * @return key - country code
    */
  def loadCountryDelegateByAirline(airlineId : Int) : Map[String, List[BusyDelegate]] = {
    val result = loadBusyDelegatesByCriteria(List(("airline", "=", airlineId), ("task_type", "=", DelegateTaskType.COUNTRY.id)))
    result.get(airlineId) match {
      case Some(allCountryDelegates) => allCountryDelegates.groupBy(_.assignedTask.asInstanceOf[CountryDelegateTask].country.countryCode)
      case None => Map.empty
    }
  }
}