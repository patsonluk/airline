ALTER TABLE `airport`
ADD COLUMN `income` BIGINT NULL AFTER `power`;

UPDATE `airport` as a, `country` as c  SET a.income = c.income WHERE a.country_code = c.code;

ALTER TABLE `airport`
DROP COLUMN `power`;


