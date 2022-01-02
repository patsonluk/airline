ALTER TABLE `others_income`
DROP COLUMN `shuttle_cost`,
DROP COLUMN `maintenance_investment`,
ADD COLUMN `asset_expense` MEDIUMTEXT NULL DEFAULT NULL AFTER `lounge_income`,
ADD COLUMN `asset_revenue` MEDIUMTEXT NULL DEFAULT NULL AFTER `asset_expense`;


ALTER TABLE `cash_flow`
ADD COLUMN `asset_transactions` BIGINT NULL DEFAULT NULL AFTER `oil_contract`;
