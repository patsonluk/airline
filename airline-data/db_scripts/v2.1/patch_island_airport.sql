-- Add island_airport flag to airport table.
-- Run IslandAirportDetector after applying this patch to populate the values.
ALTER TABLE airport ADD COLUMN island_airport TINYINT NOT NULL DEFAULT 0;
