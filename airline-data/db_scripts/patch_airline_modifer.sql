ALTER TABLE `airline_modifier` ADD INDEX `modifier_airline` (`airline` ASC) VISIBLE;

ALTER TABLE `airline_modifier` DROP INDEX `PRIMARY`;
ALTER TABLE `airline_modifier` ADD `id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT FIRST;


REPLACE INTO `airline_modifier_property`(id, name, value) SELECT o.id, "DURATION", 52 FROM `airline_modifier` o WHERE o.modifier_name = "DELEGATE_BOOST";
REPLACE INTO `airline_modifier_property`(id, name, value) SELECT o.id, "STRENGTH", 3 FROM `airline_modifier` o WHERE o.modifier_name = "DELEGATE_BOOST";