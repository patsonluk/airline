DELETE FROM link WHERE transport_type <> 0;

ALTER TABLE `airline_v2`.`link`
DROP FOREIGN KEY `link_ibfk_3`;
