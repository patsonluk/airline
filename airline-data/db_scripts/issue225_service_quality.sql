ALTER TABLE `airline`.`airline_info`
CHANGE COLUMN `service_funding` `target_service_quality` INT(11) NULL DEFAULT NULL ;

UPDATE `airline`.`airline_info` SET target_service_quality = service_quality
