ALTER TABLE `airline`.`airplane_model`
ADD COLUMN `family` VARCHAR(256) NULL DEFAULT NULL AFTER `name`;
