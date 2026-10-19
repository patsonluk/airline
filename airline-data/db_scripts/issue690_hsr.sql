-- allow an airline to operate both a flight and a HSR on the same airport pair
ALTER TABLE link DROP INDEX link_index_1, ADD UNIQUE INDEX link_index_1 (from_airport, to_airport, airline, transport_type);

-- cycle on which a HSR was last built/reconfigured, for the reconfigure cooldown
CREATE TABLE high_speed_rail (
  link INTEGER PRIMARY KEY,
  update_cycle INTEGER,
  FOREIGN KEY(link) REFERENCES link(id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- cycle on which a HSR was funded, it takes HighSpeedRail.CONSTRUCTION_DURATION cycles to complete. Existing HSR (funded_cycle 0) are treated as completed
ALTER TABLE high_speed_rail ADD COLUMN funded_cycle INTEGER NOT NULL DEFAULT 0;

-- extra description for tracking notices that carry their own message (for example HSR completion)
ALTER TABLE tracking_notice ADD COLUMN description VARCHAR(1024);

-- details of the HIGH_SPEED_RAIL_COMPLETION event, shown in link event history
CREATE TABLE high_speed_rail_completion_event (
  event INTEGER PRIMARY KEY,
  from_airport INTEGER,
  to_airport INTEGER,
  airline INTEGER,
  FOREIGN KEY(event) REFERENCES event(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY(from_airport) REFERENCES airport(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY(to_airport) REFERENCES airport(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY(airline) REFERENCES airline(id) ON DELETE CASCADE ON UPDATE CASCADE
);