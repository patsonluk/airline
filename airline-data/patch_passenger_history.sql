ALTER TABLE `airline`.`passenger_history` 
ADD COLUMN `home_country` VARCHAR(2) NOT NULL DEFAULT '' AFTER `inverted`,
ADD COLUMN `home_airport` INT(11) NULL AFTER `home_country`,
ADD COLUMN `preference_type` INT(11) NULL AFTER `home_airport`;
