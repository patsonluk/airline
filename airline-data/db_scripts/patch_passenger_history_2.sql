ALTER TABLE `airline_v2`.`passenger_history`
CHANGE COLUMN `passenger_type` `passenger_type` TINYINT NULL DEFAULT NULL ,
CHANGE COLUMN `link_class` `link_class` CHAR(1) NULL DEFAULT NULL ,
CHANGE COLUMN `inverted` `inverted` TINYINT NULL DEFAULT NULL ,
CHANGE COLUMN `home_country` `home_country` CHAR(2) NOT NULL DEFAULT '' ,
CHANGE COLUMN `preference_type` `preference_type` TINYINT NULL ,
ADD COLUMN `preferred_link_class` CHAR(1) NULL AFTER `preference_type`,
ADD COLUMN `cost` INT NULL AFTER `preferred_link_class`;
