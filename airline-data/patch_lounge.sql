ALTER TABLE `airline`.`link_consumption` 
ADD COLUMN `lounge_cost` INT(11) NULL AFTER `maintenance_cost`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`link`, `cycle`);


ALTER TABLE `airline`.`links_income` 
ADD COLUMN `lounge_cost` BIGINT(20) NULL DEFAULT '0' AFTER `maintenance_cost`;


ALTER TABLE `airline`.`others_income` 
ADD COLUMN `lounge_upkeep` BIGINT(20) NULL DEFAULT '0' AFTER `advertisement`,
ADD COLUMN `lounge_cost` BIGINT(20) NULL DEFAULT '0' AFTER `lounge_upkeep`,
ADD COLUMN `lounge_income` BIGINT(20) NULL DEFAULT '0' AFTER `lounge_cost`;


CREATE TABLE lounge (
      airport INTEGER, 
      airline INTEGER, 
      name VARCHAR(256), 
      level INTEGER,
      status VARCHAR(16), 
      founded_cycle INTEGER,
      PRIMARY KEY (airport, airline), 
      FOREIGN KEY(airport) REFERENCES airport(id) ON DELETE CASCADE ON UPDATE CASCADE,
      FOREIGN KEY(airline) REFERENCES airline(id) ON DELETE CASCADE ON UPDATE CASCADE
		      );
			  
			  
CREATE TABLE lounge_consumption(
      airport INTEGER, 
      airline INTEGER, 
      self_visitors INTEGER,
      alliance_visitors INTEGER,
      cycle INTEGER,
      PRIMARY KEY (airport, airline), 
      FOREIGN KEY(airport) REFERENCES airport(id) ON DELETE CASCADE ON UPDATE CASCADE,
      FOREIGN KEY(airline) REFERENCES airline(id) ON DELETE CASCADE ON UPDATE CASCADE
      );
