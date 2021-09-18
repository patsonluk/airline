ALTER TABLE `airline_v2`.`user_ip`
ADD COLUMN `occurrence` INT DEFAULT 0 AFTER `ip`,
ADD COLUMN `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER `occurrence`;
