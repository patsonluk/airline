ALTER TABLE `airline`.`airplane`
ADD COLUMN `purchased_cycle` INT(11) NULL DEFAULT NULL AFTER `constructed_cycle`;

UPDATE `airline`.`airplane` SET purchased_cycle = constructed_cycle