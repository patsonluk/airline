-- allow an airline to operate both a flight and a HSR on the same airport pair
ALTER TABLE link DROP INDEX link_index_1, ADD UNIQUE INDEX link_index_1 (from_airport, to_airport, airline, transport_type);
