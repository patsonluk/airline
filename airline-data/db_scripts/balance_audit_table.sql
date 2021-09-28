CREATE TABLE `balance_audit` (
  `airline` int(11) NOT NULL,
  `cycle` int(11) NOT NULL,
  `balance` mediumtext,
  UNIQUE KEY `balance_audit_UN` (`airline`,`cycle`),
  CONSTRAINT `balance_audit_FK` FOREIGN KEY (`airline`) REFERENCES `airline` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)