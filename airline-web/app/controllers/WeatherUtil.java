package controllers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import play.libs.Json;

public class WeatherUtil {
	private static final String APP_ID = "a73aa7c1bf75d7e3cb83d45a40d50c78";
	private static LoadingCache<Coordinates, Weather> cache = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<Coordinates, Weather>() {
				public Weather load(Coordinates coordinates) {
					Weather result = loadWeather(coordinates);
					System.out.println("loaded weather for  " + coordinates + " " + result);
					return result;
				}
			});

	public static class Coordinates {
		@Override
		public String toString() {
			return "Coordinates [longitude=" + longitude + ", latitude=" + latitude + "]";
		}

		private double longitude;
		private double latitude;

		public Coordinates(double latitude, double longitude) {
			super();
			this.longitude = longitude;
			this.latitude = latitude;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(latitude);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coordinates other = (Coordinates) obj;
			if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
				return false;
			if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
				return false;
			return true;
		}
		
		
	}

	public static class Weather {
		private int weatherId;
		private String description;
		private String icon;
		private double temperature;
		private double windSpeed;
		public Weather(int weatherId, String description, String icon, double temperature, double windSpeed) {
			super();
			this.weatherId = weatherId;
			this.description = description;
			this.icon = icon;
			this.temperature = temperature;
			this.windSpeed = windSpeed;
		}
		
		@Override
		public String toString() {
			return "Weather [weatherId=" + weatherId + ", description=" + description + ", icon=" + icon
					+ ", temperature=" + temperature + ", windSpeed=" + windSpeed + "]";
		}

		public int getWeatherId() {
			return weatherId;
		}

		public String getDescription() {
			return description;
		}

		public String getIcon() {
			return icon;
		}

		public double getTemperature() {
			return temperature;
		}

		public double getWindSpeed() {
			return windSpeed;
		}
		
		
	}
	
	public static Weather getWeather(Coordinates coordinates) {
		try {
			return cache.get(coordinates);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static void main(String[] args) {
		System.out.println(loadWeather(new Coordinates(100, 35)));
	}

	private static Weather loadWeather(Coordinates coordinates) {
		try {

			URL url = new URL("https://api.openweathermap.org/data/2.5/weather?lat=" + coordinates.latitude + "&lon=" + coordinates.longitude + "&units=metric&appid=" + APP_ID);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			JsonNode result = Json.parse(conn.getInputStream());

			JsonNode weatherNode = result.get("weather").get(0);
			int weatherId = weatherNode.get("id").asInt();
			String description = weatherNode.get("description").asText();
			String icon = weatherNode.get("icon").asText();
			double temperature = result.get("main").get("temp").asDouble();
			double windSpeed = result.get("wind").get("speed").asDouble();
			
			conn.disconnect();

			return new Weather(weatherId, description, icon, temperature, windSpeed);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
}