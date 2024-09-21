CREATE TABLE airport_champion_bonus (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  airport INTEGER,
  airline INTEGER,
  description VARCHAR(256),
  FOREIGN KEY (airport, airline) REFERENCES airport_champion(airport, airline) ON DELETE CASCADE ON UPDATE CASCADE
)