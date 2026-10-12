-- allow an airline to operate both a flight and a HSR on the same airport pair
ALTER TABLE link DROP INDEX link_index_1, ADD UNIQUE INDEX link_index_1 (from_airport, to_airport, airline, transport_type);

-- cycle on which a HSR was last built/reconfigured, for the reconfigure cooldown
CREATE TABLE high_speed_rail (
  link INTEGER PRIMARY KEY,
  update_cycle INTEGER,
  FOREIGN KEY(link) REFERENCES link(id) ON DELETE CASCADE ON UPDATE CASCADE
);
