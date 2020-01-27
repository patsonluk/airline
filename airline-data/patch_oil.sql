ALTER TABLE `airline`.`others_income` 
ADD COLUMN `fuel_profit` BIGINT(20) NULL DEFAULT '0' AFTER `lounge_income`;


ALTER TABLE `airline`.`cash_flow` 
ADD COLUMN `facility_construction` BIGINT(20) NULL AFTER `create_link`,
ADD COLUMN `oil_contract` BIGINT(20) NULL AFTER `facility_construction`;
