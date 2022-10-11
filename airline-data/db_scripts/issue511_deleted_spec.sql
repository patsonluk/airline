SELECT * FROM airport_airline_appeal_bonus WHERE bonus_type = 7 AND (airline, airport) NOT IN (SELECT airline, airport FROM airline_base);

DELETE FROM airport_airline_appeal_bonus WHERE bonus_type = 7 AND (airline, airport) NOT IN (SELECT airline, airport FROM airline_base);

SELECT * FROM airline_base_specialization WHERE (airline, airport) NOT IN (SELECT airline, airport FROM airline_base);

DELETE FROM airline_base_specialization WHERE (airline, airport) NOT IN (SELECT airline, airport FROM airline_base);

