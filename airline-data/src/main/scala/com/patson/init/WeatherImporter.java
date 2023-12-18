package com.patson.init;

import com.patson.data.AirportSource;
import com.patson.model.Airport;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsObject;
import play.api.libs.json.Json;
import scala.collection.JavaConverters;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * No need to re-run, just keeping this class for reference.
 *
 * The product file is airport-weather.csv and shouldn't need to be regenerated
 *
 *
 */
public class WeatherImporter {

    private static final String urlJulyFormat = "https://api.weatherstack.com/historical?access_key=%s&query=%s,%s&historical_date_start=2020-07-26&historical_date_end=2020-07-30";
    private static final String urlJanFormat = "https://api.weatherstack.com/historical?access_key=%s&query=%s,%s&historical_date_start=2020-01-26&historical_date_end=2020-01-30";

    public static void main(String[] args) throws IOException, InterruptedException {
        File weatherFile = new File("airport-weather.csv");
        if (!weatherFile.exists()) {
            weatherFile.createNewFile(); // if file already exists will do nothing
        }
        //read first
        Set<String> collectedIata = new HashSet<>();
        try (var reader = new BufferedReader(new FileReader(weatherFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                collectedIata.add(line.substring(0, line.indexOf(",")));
            }
        }

        System.out.println("Skipping " + collectedIata.size() + " iata");

        List<Airport> allAirports = new ArrayList<>(JavaConverters.asJava(AirportSource.loadAllAirports(false, false)));
        allAirports.sort((a1, a2) -> {
            if (a1.id() == a2.id()) {
                return 0;
            } else if (a1.power() == a2.power()) {
                return a1.id() - a2.id();
            } else if (a1.power() < a2.power()){
                return 1;
            } else {
                return -1;
            }
        });

        List<Airport> targetAirports = new ArrayList<>();
        for (Airport airport : allAirports) {
            if (!collectedIata.contains(airport.iata())) {
                targetAirports.add(airport);
            }
        }

        System.out.println("Collecting " + targetAirports.size() + " iata");
        //FileOutputStream oFile = new FileOutputStream(yourFile, false);

        queryWeather(targetAirports, args[0], weatherFile);


    }


    //{historical":{"2015-01-21":{"date":"2015-01-21","date_epoch":1421798400,"astro":{"sunrise":"07:15 AM","sunset":"05:00 PM","moonrise":"07:42 AM","moonset":"06:46 PM","moon_phase":"New Moon","moon_illumination":7},"mintemp":-2,"maxtemp":1,"avgtemp":0,"totalsnow":0.3,"sunhour":3.7,"uv_index":2},
    // "2015-01-22":{"date":"2015-01-22","date_epoch":1421884800,"astro":{"sunrise":"07:15 AM","sunset":"05:01 PM","moonrise":"08:23 AM","moonset":"07:59 PM","moon_phase":"New Moon","moon_illumination":14},"mintemp":0,"maxtemp":4,"avgtemp":2,"totalsnow":0.4,"sunhour":5.4,"uv_index":1},
    // "2015-01-23":{"date":"2015-...}
    private static void queryWeather(List<Airport> targetAirports, String apiKey, File weatherFile) throws IOException, InterruptedException {
        // create a client
        var client = HttpClient.newHttpClient();
        try (var writer = new BufferedWriter(new FileWriter(weatherFile, true))) {
            for (Airport airport : targetAirports) {
                String url = String.format(urlJulyFormat, apiKey, airport.latitude(), airport.longitude());
                // create a request
                var request = HttpRequest.newBuilder(
                        URI.create(url))
                        .header("accept", "application/json")
                        .build();
                String season = airport.latitude() > 0 ? "summer" : "winter";
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.out.println("Error response code: " + response.statusCode() + " body: " + response.body());
                    return;
                } else {
                    var allDays = ((JsObject) Json.parse(response.body())).value().get("historical").get();
                    writer.write(airport.iata() + "," + season + "," + allDays.toString());
                    writer.newLine();
                    System.out.println("Written " + airport.iata());
                }

                url = String.format(urlJanFormat, apiKey, airport.latitude(), airport.longitude());
                // create a request
                request = HttpRequest.newBuilder(
                        URI.create(url))
                        .header("accept", "application/json")
                        .build();

                season = airport.latitude() > 0 ? "winter" : "summer";
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.out.println("Error response code: " + response.statusCode() + " body: " + response.body());
                    return;
                } else {
                    var allDays = ((JsObject) Json.parse(response.body())).value().get("historical").get();
                    writer.write(airport.iata() + "," + season + "," + allDays.toString());
                    writer.newLine();
                    System.out.println("Written " + airport.iata());
                }
            }
        }



    }
}
