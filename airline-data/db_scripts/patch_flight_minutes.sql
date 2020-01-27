ALTER TABLE `airline`.`link_assignment`
ADD COLUMN `flight_minutes` INT(11) NOT NULL DEFAULT 0 AFTER `frequency`;
