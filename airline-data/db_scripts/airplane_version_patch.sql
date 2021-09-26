ALTER TABLE `airline_v2`.`airplane`
ADD COLUMN `version` INT(11) NOT NULL DEFAULT 0 AFTER `purchase_rate`;
