ALTER TABLE `airline`.`passenger_history`
ADD COLUMN `destination_airport` INT(11) NULL DEFAULT NULL AFTER `home_airport`;
ALTER TABLE `airline`.`passenger_history`
ADD INDEX `passenger_history_index_3` (`home_airport` ASC, `destination_airport` ASC);
