package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.patson.data.GoogleResourceSource;
import com.patson.model.Airport;
import com.patson.model.google.GoogleResource;
import com.patson.model.google.ResourceType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import scala.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

	private static LoadingCache<CityKey, Optional<URL>> cityCache = CacheBuilder.newBuilder().maximumSize(100000).expireAfterWrite(1, TimeUnit.DAYS).build(new ResourceCacheLoader<>(key -> loadCityImageUrl(key.cityName, key.latitude, key.longitude), ResourceType.CITY_IMAGE().id()));
	private static LoadingCache<AirportKey, Optional<URL>> airportCache = CacheBuilder.newBuilder().maximumSize(100000).expireAfterWrite(1, TimeUnit.DAYS).build(new ResourceCacheLoader<>(key -> 	loadAirportImageUrl(key.airportName, key.latitude, key.longitude), ResourceType.AIRPORT_IMAGE().id()));

	private interface LoadFunction<T, R> {
		R apply(T t) throws OverLimitException, NoLongerValidException;
	}

	private static class ResourceCacheLoader<KeyType extends Key> extends CacheLoader<KeyType, Optional<URL>> {
		private static final int DEFAULT_MAX_AGE = 24 * 60 * 60; //in sec
		private final LoadFunction<KeyType, UrlResult> loadFunction;
		private final int resourceTypeValue;

		private ResourceCacheLoader(LoadFunction<KeyType, UrlResult> loadFunction, int resourceTypeValue) {
			this.loadFunction = loadFunction;
			this.resourceTypeValue = resourceTypeValue;
		}

		public Optional<URL> load(KeyType key) {
			logger.info("Loading google resource on " + key);
			//try from db first
			Option<GoogleResource> googleResourceOption = GoogleResourceSource.loadResource(key.getId(), ResourceType.apply(resourceTypeValue));

			if (googleResourceOption.isDefined()) {
				logger.info("Found previous google resource on " + key + " resource " + googleResourceOption.get());
				GoogleResource googleResource = googleResourceOption.get();
				if (googleResource.url() == null) { //previous successful query returns no result, do not proceed
					return Optional.empty();
				}
				if (!googleResource.maxAgeDeadline().isEmpty() && System.currentTimeMillis() <= (Long) googleResource.maxAgeDeadline().get()) {
					try {
						return Optional.of(new URL(googleResource.url()));
					} catch (MalformedURLException e) {
						logger.warn("Stored URL is malformed: " + e.getMessage(), e);
					}
				} else { //max deadline expired, try and see if the url still works
					Optional<Long> newDeadline = isUrlValid(googleResource.url());
					if (newDeadline != null) {
						GoogleResourceSource.insertResource().apply(GoogleResource.apply(googleResource.resourceId(), googleResource.resourceType(), googleResource.url(), newDeadline.isPresent() ? Option.apply(newDeadline.get()) : Option.empty()));
						try {
							return Optional.of(new URL(googleResource.url()));
						} catch (MalformedURLException e) {
							logger.warn("Stored URL is malformed: " + e.getMessage(), e);
						}
					}
				}
			} else {
				logger.info("No previous google resource on " + key);
			}

			//no previous successful query done, or the result is no longer valid
			//UrlResult result = loadCityImageUrl(key.cityName, key.latitude, key.longitude);
			try {
				UrlResult result = loadFunction.apply(key);
				logger.info("loaded " + ResourceType.apply(resourceTypeValue) + " image for  " + key + " " + result);
				if (result != null) {
					Long deadline = System.currentTimeMillis() + (result.maxAge != null ? result.maxAge * 1000 : DEFAULT_MAX_AGE * 1000);
					GoogleResourceSource.insertResource().apply(GoogleResource.apply(key.getId(), ResourceType.apply(resourceTypeValue), result.url.toString(), deadline != null ? Option.apply(deadline) : Option.empty()));

					return Optional.of(result.url);
				} else { //There is no result, save to DB, as we do not want to retry this at all
					GoogleResourceSource.insertResource().apply(GoogleResource.apply(key.getId(), ResourceType.apply(resourceTypeValue), null, Option.empty()));
					return Optional.empty();
				}
			} catch (OverLimitException e) {
				//result unknown since it was over the limit, try later
				logger.info("Google resource on " + key + " failed due to overlimit");
				return Optional.empty();
			} catch (NoLongerValidException e) {
				//purge the old record since it's no longer valid
				logger.info("Google resource on " + key + " is no longer valid");
				GoogleResourceSource.deleteResource(key.getId(), ResourceType.apply(resourceTypeValue));
				return Optional.empty();
			} catch (Throwable t) {
				logger.warn("Unexpected failure for google resource loading on " + key + " : " + t.getMessage(), t);
				return Optional.empty();
			}
		}
	}

	public static void invalidate(Key key) {
		if (key instanceof CityKey) {
			cityCache.invalidate(key);
			GoogleResourceSource.deleteResource(key.getId(), ResourceType.CITY_IMAGE());
		} else if (key instanceof AirportKey) {
			airportCache.invalidate(key);
			GoogleResourceSource.deleteResource(key.getId(), ResourceType.AIRPORT_IMAGE());
		}
	}

	/**
	 *
	 * @param urlString
	 * @return	null if not valid, a new maxAge Option if valid
	 */
	private static Optional<Long> isUrlValid(String urlString) {
		URL url;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			logger.warn("URL " + urlString + " is not valid : " + e.getMessage(), e);
			return null;
		}

		HttpURLConnection conn = null;

		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() == 200) {
				Long maxAge = getMaxAge(conn);
				if (maxAge != null) {
					long newDeadline = System.currentTimeMillis() + maxAge * 1000;
					logger.debug(urlString + " is still valid, new max age deadline: " + newDeadline) ;
					return Optional.of(newDeadline);
				} else {
					logger.debug(urlString + " is still valid, no max age deadline");
					return Optional.empty();
				}
			} else {
				logger.info(urlString + " is no longer valid " + conn.getResponseCode());
				return null;
			}
		} catch (IOException e) {
			logger.warn(urlString + " failed with valid check : " + e.getMessage());
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}


	}

	private static Long getMaxAge(HttpURLConnection conn) {
		String cacheControl = conn.getHeaderField("Cache-Control");
		if (cacheControl != null) {
			for (String entry : cacheControl.split(",")) {
				entry = entry.toLowerCase().trim();
				if (entry.startsWith("max-age=")) {
					try {
						return Long.valueOf(entry.substring("max-age=".length()).trim());
					} catch (NumberFormatException e) {
						logger.warn("Invalid max-age : " + entry);
					}
				}
			}
		}
		return null;
	}

	public static class CityKey extends Key {
		private final int id;
		private String cityName;
		private double latitude;
		private double longitude;

		public CityKey(int id, String cityName, double latitude, double longitude) {
			this.id = id;
			this.cityName = cityName;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CityKey cityKey = (CityKey) o;

			if (id != cityKey.id) return false;
			if (Double.compare(cityKey.latitude, latitude) != 0) return false;
			if (Double.compare(cityKey.longitude, longitude) != 0) return false;
			return cityName != null ? cityName.equals(cityKey.cityName) : cityKey.cityName == null;

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = id;
			result = 31 * result + (cityName != null ? cityName.hashCode() : 0);
			temp = Double.doubleToLongBits(latitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "CityKey{" +
					"id=" + id +
					", cityName='" + cityName + '\'' +
					", latitude=" + latitude +
					", longitude=" + longitude +
					'}';
		}

		@Override
		public int getId() {
			return id;
		}
	}

	public static class AirportKey extends Key{
		private final int id;
		private String airportName;
		private double latitude;
		private double longitude;

		public AirportKey(int id, String airportName, double latitude, double longitude) {
			this.id = id;
			this.airportName = airportName;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			AirportKey that = (AirportKey) o;

			if (id != that.id) return false;
			if (Double.compare(that.latitude, latitude) != 0) return false;
			if (Double.compare(that.longitude, longitude) != 0) return false;
			return airportName != null ? airportName.equals(that.airportName) : that.airportName == null;

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = id;
			result = 31 * result + (airportName != null ? airportName.hashCode() : 0);
			temp = Double.doubleToLongBits(latitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "AirportKey{" +
					"id=" + id +
					", airportName='" + airportName + '\'' +
					", latitude=" + latitude +
					", longitude=" + longitude +
					'}';
		}

		@Override
		public int getId() {
			return id;
		}
	}

	public static abstract class Key {
		abstract int getId();
	}

	public static URL getCityImageUrl(Airport airport) {
		return getCityImageUrl(airport.id(), airport.city(), airport.latitude(), airport.longitude());
	}

	static URL getCityImageUrl(int airportId, String cityName, Double latitude, Double longitude) {
		try {
			Optional<URL> result = cityCache.get(new CityKey(airportId, cityName, latitude, longitude));
			return result.orElse(null);
		} catch (Exception e) {
			if (!(e.getCause() instanceof OverLimitException)) {
				e.printStackTrace();
			}
			return null;
		}
	}

	static URL getAirportImageUrl(Airport airport) {
		return getAirportImageUrl(airport.id(), airport.name(), airport.latitude(), airport.longitude());
	}
	static URL getAirportImageUrl(int airportId, String airportName, Double latitude, Double longitude) {
		try {
			Optional<URL> result = airportCache.get(new AirportKey(airportId, airportName, latitude, longitude));
			return result.orElse(null);
		} catch (Exception e) {
			if (!(e.getCause() instanceof OverLimitException)) {
				e.printStackTrace();
			}
			return null;
		}
	}


	public static UrlResult loadCityImageUrl(String cityName, Double latitude, Double longitude) throws OverLimitException, NoLongerValidException {
		if (cityName == null) {
			return null;
		}
		return loadImageUrl(Collections.singletonList(cityName), latitude, longitude, "(regions)");
	}

	public static UrlResult loadAirportImageUrl(String airportName, Double latitude, Double longitude) throws OverLimitException, NoLongerValidException {
		if (airportName == null) {
			return null;
		}
		return loadImageUrl(Collections.singletonList(airportName), latitude, longitude, null);
	}



	/**
	 * Executes actual google query
	 * @param phrases
	 * @param latitude
	 * @param longitude
	 * @param types
	 * @return
	 * @throws OverLimitException
	 */
	public static UrlResult loadImageUrl(List<String> phrases, Double latitude, Double longitude, String types) throws OverLimitException, NoLongerValidException {
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
			conn.setInstanceFollowRedirects(false);
			conn.connect();
			if (conn.getResponseCode() == 403 || conn.getResponseCode() == 404) { //forbidden/not found
				throw new NoLongerValidException();
			}
			String location = conn.getHeaderField("Location");

			if (location == null) {
				String result = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).lines()
						.collect(Collectors.joining("\n"));
				logger.warn("Failed to get location redirect from " + imageUrl + " response code " + conn.getResponseCode() + " text " + result + " trying later...");

				throw new RedirectUnavailableException(imageUrl, conn.getResponseCode(), result);
			}
			return new UrlResult(new URL(location), getMaxAge(conn));


			//System.out.println("==>" + imageUrl);
			//return imageUrl;
		} catch (IOException e) {
			logger.warn("Failed to load google place API redirect : " + e.getMessage(), e);
			return null;
		}
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
	private static class UrlResult {
		private URL url;
		private Long maxAge;

		private UrlResult(URL url, Long maxAge) {
			this.url = url;
			this.maxAge = maxAge;
		}

		@Override
		public String toString() {
			return "UrlResult{" +
					"url=" + url +
					", maxAge=" + maxAge +
					'}';
		}
	}
	
	
	public static void main(String[] args) {
//		System.out.println(loadImageUrl(List.of("Vancouver"), 45.633331, -122.599998));
		//System.out.println(loadImageUrl(List.of("Vancouver"), 49.193901062, -123.183998108));
		//System.out.println(loadImageUrl(List.of("Hong Kong"), 22.3089008331,  113.915000916));
		System.out.println(getCityImageUrl(0, "Hong Kong", 22.3089008331,  113.915000916));

		System.out.println("==============");
		System.out.println(getAirportImageUrl(0, "Hong Kong International Airport", 22.3089008331,  113.915000916));
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

	private static class OverLimitException extends Exception {
    }

    private static class NoLongerValidException extends Exception{

	}
}