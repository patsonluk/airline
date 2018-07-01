package com.patson.data

import com.patson.model.Computation
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model
import java.sql.PreparedStatement
import java.sql.Connection
import com.patson.data.Constants._
import com.mchange.v2.c3p0.ComboPooledDataSource
import scala.collection.mutable.ListBuffer

object Patchers {
  def patchHomeCountry() {
    AirlineSource.loadAllAirlines(true).foreach { airline =>
      airline.bases.find(_.headquarter).foreach { headquarter =>
        airline.setCountryCode(headquarter.countryCode)
        AirlineSource.saveAirlineInfo(airline)
      }
    }
  }

  //  ALTER TABLE `airline`.`link`
  //ADD COLUMN `flight_type` INT(2) NULL AFTER `frequency`;

  def patchFlightType() {
    val updatingLinks = LinkSource.loadAllLinks(LinkSource.FULL_LOAD).map { link =>
      val flightType = Computation.getFlightType(link.from, link.to, link.distance)
      println(flightType.id)
      link.copy(flightType = flightType)
      //LinkSource.updateLink(link)
    }

    LinkSource.updateLinks(updatingLinks)
  }

  def airplaneModelPatcher() {
    ModelSource.updateModels(Model.models)
    
    val existingModelNames = ModelSource.loadAllModels().map(_.name)
    
    val newModels = ListBuffer[Model]()
    Model.models.foreach { model =>
      if (!existingModelNames.contains(model.name)) {
        newModels.append(model)
      }
    }
    
    ModelSource.saveModels(newModels.toList)
  }

  def patchDelaySchema() = {
    Class.forName(DB_DRIVER)
    val dataSource = new ComboPooledDataSource()
    //    val properties = new Properties()
    //    properties.put("user", DATABASE_USER);
    //    properties.put("password", "admin");
    //DriverManager.getConnection(DATABASE_CONNECTION, properties);
    //mysql end

    //dataSource.setProperties(properties)
    dataSource.setUser(DATABASE_USER)
    dataSource.setPassword(DATABASE_PASSWORD)
    dataSource.setJdbcUrl(DATABASE_CONNECTION)
    dataSource.setMaxPoolSize(100)
    val connection = dataSource.getConnection()
    
    var statement: PreparedStatement = null

    statement = connection.prepareStatement("ALTER TABLE `airline`.`links_income` ADD COLUMN `delay_compensation` BIGINT(20) NULL DEFAULT 0 AFTER `inflight_cost`;")
    statement.execute()

    statement = connection.prepareStatement("ALTER TABLE `airline`.`link_consumption` ADD COLUMN `delay_compensation` INT(11) NULL DEFAULT 0 AFTER `inflight_cost`, ADD COLUMN `minor_delay_count` INT(4) NULL DEFAULT 0 AFTER `profit`, ADD COLUMN `major_delay_count` INT(4) NULL DEFAULT 0 AFTER `minor_delay_count`, ADD COLUMN `cancellation_count` INT(4) NULL DEFAULT 0 AFTER `major_delay_count`, ADD COLUMN `frequency` INT(4) NULL AFTER `distance`;")
    statement.execute()
    statement.close()
    
    connection.close
  }
  //
}

 