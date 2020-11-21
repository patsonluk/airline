package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GoogleImageUtil {
	private static final String API_KEY = loadApiKey();

	private static String loadApiKey() {
		Config configFactory = ConfigFactory.load();
		return configFactory.hasPath("google.apiKey") ? configFactory.getString("google.apiKey") : null;
	}

	private final static Logger logger = LoggerFactory.getLogger(GoogleImageUtil.class);
	private final static int MAX_PHOTO_WIDTH = 1000;
	private final static int SEARCH_RADIUS = 100000; //100km

	private static LoadingCache<CityKey, Optional<URL>> cityCache = CacheBuilder.newBuilder().maximumSize(100000).build(new CacheLoader<CityKey, Optional<URL>>() {
		public Optional<URL> load(CityKey key) {
			URL result = loadCityImageUrl(key.cityName, key.latitude, key.longitude);
			logger.info("loaded city image for  " + key + " " + result);
			return result != null ? Optional.of(result) : Optional.empty();
		}
	});

	private static LoadingCache<AirportKey, Optional<URL>> airportCache = CacheBuilder.newBuilder().maximumSize(100000).build(new CacheLoader<AirportKey, Optional<URL>>() {
		public Optional<URL> load(AirportKey key) {
			URL result = loadAirportImageUrl(key.airportName, key.latitude, key.longitude);
			logger.info("loaded airport image for  " + key + " " + result);
			return result != null ? Optional.of(result) : Optional.empty();
		}
	});

	private static class CityKey {
		private String cityName;
		private double latitude;
		private double longitude;

		public CityKey(String cityName, double latitude, double longitude) {
			this.cityName = cityName;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CityKey cityKey = (CityKey) o;

			if (Double.compare(cityKey.latitude, latitude) != 0) return false;
			if (Double.compare(cityKey.longitude, longitude) != 0) return false;
			return cityName.equals(cityKey.cityName);

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = cityName.hashCode();
			temp = Double.doubleToLongBits(latitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "CityKey{" +
					"cityName='" + cityName + '\'' +
					", latitude=" + latitude +
					", longitude=" + longitude +
					'}';
		}
	}

	private static class AirportKey {
		private String airportName;
		private double latitude;
		private double longitude;

		public AirportKey(String airportName, double latitude, double longitude) {
			this.airportName = airportName;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			AirportKey that = (AirportKey) o;

			if (Double.compare(that.latitude, latitude) != 0) return false;
			if (Double.compare(that.longitude, longitude) != 0) return false;
			return airportName.equals(that.airportName);

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = airportName.hashCode();
			temp = Double.doubleToLongBits(latitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "AirportKey{" +
					"airportName='" + airportName + '\'' +
					", latitude=" + latitude +
					", longitude=" + longitude +
					'}';
		}
	}


	public static URL getCityImageUrl(String cityName, Double latitude, Double longitude) {
		try {
			Optional<URL> result = cityCache.get(new CityKey(cityName, latitude, longitude));
			return result.orElse(null);
		} catch (Exception e) {
			if (!(e.getCause() instanceof OverLimitException)) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public static URL getAirportImageUrl(String airportName, Double latitude, Double longitude) {
		try {
			Optional<URL> result = airportCache.get(new AirportKey(airportName, latitude, longitude));
			return result.orElse(null);
		} catch (Exception e) {
			if (!(e.getCause() instanceof OverLimitException)) {
				e.printStackTrace();
			}
			return null;
		}
	}


	public static URL loadCityImageUrl(String cityName, Double latitude, Double longitude) throws OverLimitException {
		if (cityName == null) {
			return null;
		}
		return getImageUrl(Collections.singletonList(cityName), latitude, longitude, "(regions)");
	}

	public static URL loadAirportImageUrl(String airportName, Double latitude, Double longitude) throws OverLimitException {
		if (airportName == null) {
			return null;
		}
		return getImageUrl(Collections.singletonList(airportName), latitude, longitude, null);
	}

	public static URL getImageUrl(List<String> phrases, Double latitude, Double longitude, String types) throws OverLimitException {
		if (phrases.isEmpty()) {
			return null;
		}

//		StringBuilder placeQuery = new StringBuilder("https://maps.googleapis.com/maps/api/place/findplacefromtext/json?inputtype=textquery&fields=photos,types,geometry&key=" + API_KEY + "&input=");

		StringBuilder autoCompleteQuery = new StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json?fields=photos,types,geometry&key=" + API_KEY + "&input=");

//		https://maps.googleapis.com/maps/api/place/autocomplete/xml?input=Amoeba&types=establishment&location=37.76999,-122.44696&radius=500&strictbounds&key=YOUR_API_KEY

		for (String phrase : phrases) {
			try {
				autoCompleteQuery.append(URLEncoder.encode(phrase, StandardCharsets.UTF_8.toString()));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (latitude != null && longitude != null) {
			//placeQuery.append("&locationbias=circle:" + SEARCH_RADIUS + "@" + latitude + "," + longitude);
			//placeQuery.append("&locationbias=point:" + latitude + "," + longitude);
			autoCompleteQuery.append("&location=" + latitude + "," + longitude + "&radius=" + SEARCH_RADIUS + "&strictbounds");
		}

		if (types != null) {
			autoCompleteQuery.append("&types=" + types);
		}



		//{"predictions":[{"description":"Norco Medical, Vancouver, Northeast Andresen Road, Vancouver, WA, USA","matched_substrings":[{"length":9,"offset":15}],"place_id":"ChIJ_UOGeICllVQR3yPG5fG5dl0","reference":"ChIJ_UOGeICllVQR3yPG5fG5dl0","structured_formatting":{"main_text":"Norco Medical, Vancouver","main_text_matched_substrings":[{"length":9,"offset":15}],"secondary_text":"Northeast Andresen Road, Vancouver, WA, USA"},"terms":[{"offset":0,"value":"Norco Medical, Vancouver"},{"offset":26,"value":"Northeast Andresen Road"},{"offset":51,"value":"Vancouver"},{"offset":62,"value":"WA"},{"offset":66,"value":"USA"}],"types":["health","point_of_interest","store","establishment"]}],"status":"OK"}

		URL url = null;
		try {
			url = new URL(autoCompleteQuery.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}


		String placeId;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				logger.info("Failed to find image for " + phrases + " response code " + conn.getResponseCode());
				return null;
			}

			//System.out.println("URL => " + url);

			JsonNode result = Json.parse(conn.getInputStream());

			//System.out.println("Result => " + result);
            if ("OVER_QUERY_LIMIT".equals(result.get("status").asText())) {
                throw new OverLimitException();
            }

			if (result.get("predictions") == null || result.get("predictions").size() == 0) {
				logger.info("Failed to find image for " + phrases + " no candidates. Response: " + result);
				return null;
			}

			JsonNode predictionNode = result.get("predictions").get(0);
			placeId = predictionNode.get("place_id").asText();

		} catch (IOException e) {
			logger.warn("Failed to use google place API : " + e.getMessage(), e);
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		StringBuilder placeDetailQuery = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?key=" + API_KEY + "&fields=photos,types&place_id=" + placeId);
		try {
			url = new URL(placeDetailQuery.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		String photoRef = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				logger.info("Failed to find image for " + phrases + " response code " + conn.getResponseCode());
				return null;
			}

			//System.out.println(url);

			JsonNode result = Json.parse(conn.getInputStream());

			//System.out.println(result);
            if ("OVER_QUERY_LIMIT".equals(result.get("status").asText())) {
                throw new OverLimitException();
            }

			JsonNode resultNode = result.get("result");
			if (resultNode == null || resultNode.get("photos") == null || resultNode.get("photos").size() == 0) {
				logger.info("Failed to find image for " + phrases + " no photos response: " + result);
				return null;
			}

			photoRef = resultNode.get("photos").get(0).get("photo_reference").asText();

		} catch (IOException e) {
			logger.warn("Failed to use google place API : " + e.getMessage(), e);
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			URL imageUrl = new URL("https://maps.googleapis.com/maps/api/place/photo?maxwidth=" + MAX_PHOTO_WIDTH + "&key=" + API_KEY + "&photoreference=" + photoRef);
			conn = (HttpURLConnection) imageUrl.openConnection();
			conn.setInstanceFollowRedirects( false );
			conn.connect();
			String location = conn.getHeaderField( "Location" );

			if (location == null) {
				String result = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).lines()
						.collect(Collectors.joining("\n"));
				logger.warn("Failed to get location redirect from " + imageUrl + " response code " + conn.getResponseCode() + " text " + result + " trying later...");

				throw new RedirectUnavailableException(imageUrl, conn.getResponseCode(), result);
			}
			return new URL(location);


			//System.out.println("==>" + imageUrl);
			//return imageUrl;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

//		try {
//			url = new URL(photoQuery.toString());
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		try {
//			conn = (HttpURLConnection) url.openConnection();
//			conn.connect();
//			return conn.getURL();
//		} catch (Exception e) {
//			logger.warn("Failed to use google place API : " + e.getMessage(), e);
//			return null;
//		} finally {
//			if (conn != null) {
//				conn.disconnect();
//			}
//		}
	}
	
	
	public static void main(String[] args) {
//		System.out.println(getImageUrl(List.of("Vancouver"), 45.633331, -122.599998));
		//System.out.println(getImageUrl(List.of("Vancouver"), 49.193901062, -123.183998108));
		//System.out.println(getImageUrl(List.of("Hong Kong"), 22.3089008331,  113.915000916));
		System.out.println(getCityImageUrl("Los Angeles", null, null));

		System.out.println("==============");
		System.out.println(getAirportImageUrl("Los Angeles International Airport", null, null));
	}


	private static class RedirectUnavailableException extends RuntimeException {
		private final URL imageUrl;
		private final int responseCode;
		private final String result;

		public RedirectUnavailableException(URL imageUrl, int responseCode, String result) {
			this.imageUrl = imageUrl;
			this.responseCode = responseCode;
			this.result = result;
		}

		@Override
		public String getMessage() {
			return "Redirect for " + imageUrl + " failed with response code " + responseCode + " result " + result;
		}
	}

	private static class OverLimitException extends RuntimeException {
    }
}