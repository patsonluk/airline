ALTER TABLE `airline`.`passenger_history`
ADD COLUMN `destination_airport` INT(11) NULL DEFAULT NULL AFTER `home_airport`;
