ALTER TABLE `airline`.`link_consumption`
ADD COLUMN `duration` SMALLINT NULL AFTER `frequency`,
ADD COLUMN `flight_type` TINYINT NULL AFTER `duration`,
ADD COLUMN `flight_number` SMALLINT NULL AFTER `flight_type`,
ADD COLUMN `airplane_model` SMALLINT NULL AFTER `flight_number`,
ADD COLUMN `raw_quality` SMALLINT NULL AFTER `airplane_model`;


ALTER TABLE `airline`.`link_consumption`
CHANGE COLUMN `quality` `quality` SMALLINT(4) NULL DEFAULT NULL ,
CHANGE COLUMN `minor_delay_count` `minor_delay_count` SMALLINT(4) NULL DEFAULT '0' ,
CHANGE COLUMN `major_delay_count` `major_delay_count` SMALLINT(4) NULL DEFAULT '0' ,
CHANGE COLUMN `cancellation_count` `cancellation_count` SMALLINT(4) NULL DEFAULT '0' ,
CHANGE COLUMN `frequency` `frequency` SMALLINT(4) NULL DEFAULT NULL ;

ALTER TABLE `airline`.`passenger_history`
CHANGE COLUMN `passenger_type` `passenger_type` TINYINT NULL DEFAULT NULL ,
CHANGE COLUMN `inverted` `inverted` TINYINT NULL DEFAULT NULL ,
CHANGE COLUMN `preference_type` `preference_type` TINYINT NULL DEFAULT NULL ;

