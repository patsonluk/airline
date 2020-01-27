ALTER TABLE `airline`.`link_assignment`
ADD COLUMN `frequency` INT(11) NOT NULL DEFAULT 0 AFTER `airplane`;

ALTER TABLE `airline`.`airplane`
ADD COLUMN `available_flight_minutes` INT(11) NOT NULL DEFAULT 0 AFTER `dealer_ratio`;

ALTER TABLE `airline`.`airplane`
ADD COLUMN `home` INT(11) NULL AFTER `available_flight_minutes`;

ALTER TABLE `airline`.`others_income`
ADD COLUMN `overtime_compensation` BIGINT(20) NULL DEFAULT 0 AFTER `depreciation`;
