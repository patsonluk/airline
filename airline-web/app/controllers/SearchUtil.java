package controllers;

import com.patson.data.AirlineSource;
import com.patson.data.AirportSource;
import com.patson.data.AllianceSource;
import com.patson.data.CountrySource;
import com.patson.model.Airline;
import com.patson.model.Airport;
import com.patson.model.Alliance;
import com.patson.model.Country;
import com.patson.util.AirlineCache;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import scala.Option;
import scala.collection.JavaConverters;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class SearchUtil {
	static {
		checkInit();
	}

	/**
	 * Initialize the index if it's empty
	 */
	private static void checkInit() {
		try (RestHighLevelClient client = getClient()) {
			if (!isIndexExist(client, "airports")) {
				System.out.println("Initializing ES airports");
				initAirports(client);
			}
			if (!isIndexExist(client, "countries")) {
				System.out.println("Initializing ES countires");
				initCountries(client);
			}
			if (!isIndexExist(client, "zones")) {
				System.out.println("Initializing ES zones");
				initZones(client);
			}
			if (!isIndexExist(client, "airlines")) {
				System.out.println("Initializing ES airlines");
				initAirlines(client);
			}
			if (!isIndexExist(client, "alliances")) {
				System.out.println("Initializing ES alliances");
				initAlliances(client);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("ES check finished");
	}

	public static void main(String[] args) throws IOException {
		init();
//		search("new");
	}


	public static void init() throws IOException {
		try (RestHighLevelClient client = getClient()) {
			initAirports(client);
			initCountries(client);
		}
		System.out.println("ES DONE");
	}

	private static boolean isIndexExist(RestHighLevelClient client, String indexName) throws IOException {
		GetIndexRequest request = new GetIndexRequest(indexName);
		return client.indices().exists(request, RequestOptions.DEFAULT);
	}

	private static void initAirports(RestHighLevelClient client) throws IOException {
		if (isIndexExist(client, "airports")) {
			client.indices().delete(new DeleteIndexRequest("airports"), RequestOptions.DEFAULT);
		}

		List<Airport> airports = JavaConverters.asJava(AirportSource.loadAllAirports(false, false));
		System.out.println("loaded " + airports.size() + " airports");

		//RestHighLevelClient client = getClient();
		int count = 0;
		for (Airport airport : airports) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("airportId", airport.id());
			jsonMap.put("airportIata", airport.iata());
			jsonMap.put("airportCity", airport.city());
			jsonMap.put("airportPower", airport.power());
			jsonMap.put("countryCode", airport.countryCode());
			jsonMap.put("airportName", airport.name());
			IndexRequest indexRequest = new IndexRequest("airports").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);

			if ((++ count) % 100 == 0) {
				System.out.println("indexed " + count + " airports");
			}

		}

	}

	private static void initCountries(RestHighLevelClient client) throws IOException {
		if (isIndexExist(client, "countries")) {
			client.indices().delete(new DeleteIndexRequest("countries"), RequestOptions.DEFAULT);
		}

		List<Country> countries = JavaConverters.asJava(CountrySource.loadAllCountries());
		System.out.println("loaded " + countries.size() + " countries");

		for (Country country : countries) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("countryName", country.name());
			jsonMap.put("countryCode", country.countryCode());
			jsonMap.put("population", country.airportPopulation());
			IndexRequest indexRequest = new IndexRequest("countries").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		}

	}

	private static void initZones(RestHighLevelClient client) throws IOException {
		if (isIndexExist(client, "zones")) {
			client.indices().delete(new DeleteIndexRequest("zones"), RequestOptions.DEFAULT);
		}

		Map<String, String> zones = new HashMap<>();
		zones.put("AS", "Asia");
		zones.put("OC", "Oceania");
		zones.put("AF", "Africa");
		zones.put("EU", "Europe");
		zones.put("NA", "North America");
		zones.put("SA", "South America");

		for (Map.Entry<String, String> entry : zones.entrySet()) {
			String zone = entry.getKey();
			String zoneName = entry.getValue();
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("zone", zone);
			jsonMap.put("zoneName", zoneName);
			IndexRequest indexRequest = new IndexRequest("zones").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		}
	}

	private static void initAirlines(RestHighLevelClient client) throws IOException {
		if (isIndexExist(client, "airlines")) {
			client.indices().delete(new DeleteIndexRequest("airlines"), RequestOptions.DEFAULT);
		}

		List<Airline> airlines = JavaConverters.asJava(AirlineSource.loadAllAirlines(false));
		System.out.println("loaded " + airlines.size() + " airlines");


		for (Airline airline : airlines) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("airlineId", airline.id());
			jsonMap.put("airlineName", airline.name());
			jsonMap.put("airlineCode", airline.getAirlineCode());
			IndexRequest indexRequest = new IndexRequest("airlines").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		}
	}

	public static void addAirline(Airline airline) {
		try (RestHighLevelClient client = getClient()) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("airlineId", airline.id());
			jsonMap.put("airlineName", airline.name());
			jsonMap.put("airlineCode", airline.getAirlineCode());
			IndexRequest indexRequest = new IndexRequest("airlines").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Added airline " + airline + " to ES");
	}

	private static void initAlliances(RestHighLevelClient client) throws IOException {
		if (isIndexExist(client, "alliances")) {
			client.indices().delete(new DeleteIndexRequest("alliances"), RequestOptions.DEFAULT);
		}

		List<Alliance> alliances = JavaConverters.asJava(AllianceSource.loadAllAlliances(false));
		System.out.println("loaded " + alliances.size() + " alliances");


		for (Alliance alliance : alliances) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("allianceId", alliance.id());
			jsonMap.put("allianceName", alliance.name());

			IndexRequest indexRequest = new IndexRequest("alliances").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		}
	}

	public static void addAlliance(Alliance alliance) {
		try (RestHighLevelClient client = getClient()) {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("allianceId", alliance.id());
			jsonMap.put("allianceName", alliance.name());

			IndexRequest indexRequest = new IndexRequest("alliances").source(jsonMap);
			client.index(indexRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Added alliance " + alliance + " to ES");

	}

	public static void removeAlliance(int allianceId) {
		try (RestHighLevelClient client = getClient()) {
			DeleteByQueryRequest request =	new DeleteByQueryRequest("alliances");
			request.setQuery(new TermQueryBuilder("allianceId", allianceId));
			request.setRefresh(true);

			client.deleteByQuery(request, RequestOptions.DEFAULT);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		System.out.println("Removed alliance with id " + allianceId + " from ES");
	}

	private static final Pattern letterSpaceOnlyPattern = Pattern.compile("^[ A-Za-z]+$");

	public static List<AirportSearchResult> searchAirport(String input) {
		if (!letterSpaceOnlyPattern.matcher(input).matches()) {
			return Collections.emptyList();
		}

		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("airports");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("airportIata",5);
			multiMatchQueryBuilder.field("airportName",1);
			multiMatchQueryBuilder.field("airportCity",2);
			multiMatchQueryBuilder.defaultOperator(Operator.AND);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);


			searchSourceBuilder.query(multiMatchQueryBuilder).size(100);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<AirportSearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();

				Object powerObject = values.get("airportPower");
				long power = powerObject instanceof Integer ? (long) ((Integer)powerObject) : (Long) powerObject;
				AirportSearchResult searchResult = new AirportSearchResult((int) values.get("airportId"), (String) values.get("airportIata"), (String) values.get("airportName"), (String) values.get("airportCity"), (String) values.get("countryCode"), power, hit.getScore());
				result.add(searchResult);
			}

			Collections.sort(result);
			Collections.reverse(result);

			//System.out.println("done");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}

	}


	public static List<CountrySearchResult> searchCountry(String input) {
		if (!letterSpaceOnlyPattern.matcher(input).matches()) {
			return Collections.emptyList();
		}

		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("countries");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("countryCode",5);
			multiMatchQueryBuilder.field("countryName",1);
			multiMatchQueryBuilder.defaultOperator(Operator.AND);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);


			searchSourceBuilder.query(multiMatchQueryBuilder).size(100);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<CountrySearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();

				CountrySearchResult searchResult = new CountrySearchResult((String) values.get("countryName"), (String) values.get("countryCode"), (int) values.get("population"), hit.getScore());
				result.add(searchResult);
			}

			Collections.sort(result);
			Collections.reverse(result);

			//System.out.println("done");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	public static List<ZoneSearchResult> searchZone(String input) {
		if (!letterSpaceOnlyPattern.matcher(input).matches()) {
			return Collections.emptyList();
		}

		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("zones");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("zone",10);
			multiMatchQueryBuilder.field("zoneName",2);
			multiMatchQueryBuilder.defaultOperator(Operator.AND);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);


			searchSourceBuilder.query(multiMatchQueryBuilder).size(10);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<ZoneSearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();

				ZoneSearchResult searchResult = new ZoneSearchResult((String) values.get("zoneName"), (String) values.get("zone"), hit.getScore());
				result.add(searchResult);
			}

			Collections.sort(result);
			Collections.reverse(result);

			//System.out.println("done");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	public static List<AirlineSearchResult> searchAirline(String input) {
		if (!letterSpaceOnlyPattern.matcher(input).matches()) {
			return Collections.emptyList();
		}

		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("airlines");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("airlineName",5);
			multiMatchQueryBuilder.field("airlineCode",1);

			multiMatchQueryBuilder.defaultOperator(Operator.AND);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);


			searchSourceBuilder.query(multiMatchQueryBuilder).size(10);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<AirlineSearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();
				Option<Airline> airlineOption = AirlineCache.getAirline((int) values.get("airlineId"), false);

				if (airlineOption.isDefined()) {
					AirlineSearchResult searchResult = new AirlineSearchResult(airlineOption.get(), hit.getScore());
					result.add(searchResult);
				}
			}

			Collections.sort(result);
			Collections.reverse(result);

			//System.out.println("done");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}

	}

	public static List<AllianceSearchResult> searchAlliance(String input) {
		if (!letterSpaceOnlyPattern.matcher(input).matches()) {
			return Collections.emptyList();
		}

		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("alliances");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("allianceName",10);
			multiMatchQueryBuilder.defaultOperator(Operator.AND);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);


			searchSourceBuilder.query(multiMatchQueryBuilder).size(10);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<AllianceSearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();

				AllianceSearchResult searchResult = new AllianceSearchResult((int) values.get("allianceId"), (String) values.get("allianceName"), hit.getScore());
				result.add(searchResult);
			}

			Collections.sort(result);
			Collections.reverse(result);

			//System.out.println("done");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}


	private static RestHighLevelClient getClient() {
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(
						new HttpHost("localhost", 9200, "http"),
						new HttpHost("localhost", 9201, "http")));
		return client;
	}
}

class AirportSearchResult implements Comparable {
	private final long power;
	private int id;
	private String iata, name, city, countryCode;
	private double score;

	public AirportSearchResult(int id, String iata, String name, String city, String countryCode, long power, double score) {
		this.id = id;
		this.iata = iata;
		this.name = name;
		this.city = city;
		this.countryCode = countryCode;
		this.power = power;
		this.score = score;
	}

	public int getId() {
		return id;
	}

	public String getIata() {
		return iata;
	}

	public String getName() {
		return name;
	}

	public String getCity() {
		return city;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public double getScore() {
		return score;
	}

	public long getPower() {
		return power;
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof AirportSearchResult)) {
			throw new IllegalArgumentException(o + " is not a " + AirportSearchResult.class.getSimpleName());
		}

		AirportSearchResult that = (AirportSearchResult) o;

		if (this.score != that.score) {
			return this.score < that.score ? -1 : 1;
		} else if (this.power != that.power){
			return this.power < that.power ? -1 : 1;
		} else {
			return this.iata.compareTo(that.iata);
		}
	}
}

class CountrySearchResult implements Comparable {
	private final String name, countryCode;
	private final double score;
	private final int population;

	public CountrySearchResult(String name, String countryCode, int population, double score) {
		this.name = name;
		this.countryCode = countryCode;
		this.population = population;
		this.score = score;
	}

	public String getName() {
		return name;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public double getScore() {
		return score;
	}

	public int getPopulation() {
		return population;
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof CountrySearchResult)) {
			throw new IllegalArgumentException(o + " is not a " + CountrySearchResult.class.getSimpleName());
		}

		CountrySearchResult that = (CountrySearchResult) o;

		if (this.score != that.score) {
			return this.score < that.score ? -1 : 1;
		} else {
			return this.population - that.population;
		}
	}
}


class ZoneSearchResult implements Comparable {
	private final String name, zone;
	private final double score;

	public ZoneSearchResult(String name, String zone,  double score) {
		this.name = name;
		this.zone = zone;
		this.score = score;
	}

	public String getName() {
		return name;
	}

	public String getZone() {
		return zone;
	}

	public double getScore() {
		return score;
	}


	@Override
	public int compareTo(Object o) {
		if (!(o instanceof ZoneSearchResult)) {
			throw new IllegalArgumentException(o + " is not a " + ZoneSearchResult.class.getSimpleName());
		}

		ZoneSearchResult that = (ZoneSearchResult) o;

		if (this.score != that.score) {
			return this.score < that.score ? -1 : 1;
		} else {
			return this.name.compareTo(that.name);
		}
	}
}


class AirlineSearchResult implements Comparable {
	private final Airline airline;
	private final double score;
	//private final int status;

	public AirlineSearchResult(Airline airline, double score) {
		this.airline = airline;
		this.score = score;
		//this.status = status;
	}

	public Airline getAirline() {
		return airline;
	}

	public double getScore() {
		return score;
	}

//	public int getStatus() {
//		return status;
//	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof AirlineSearchResult)) {
			throw new IllegalArgumentException(o + " is not a " + AirlineSearchResult.class.getSimpleName());
		}

		AirlineSearchResult that = (AirlineSearchResult) o;

		if (this.score != that.score) {
			return this.score < that.score ? -1 : 1;
		} else {
			return that.airline.id() - this.airline.id();
		}
	}
}

class AllianceSearchResult implements Comparable {
	private final String allianceName;
	private final double score;
	private final int allianceId;

	public AllianceSearchResult(int id, String name, double score) {
		this.allianceId = id;
		this.allianceName = name;
		this.score = score;
	}

	public String getAllianceName() {
		return allianceName;
	}

	public double getScore() {
		return score;
	}

	public int getAllianceId() {
		return allianceId;
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof AllianceSearchResult)) {
			throw new IllegalArgumentException(o + " is not a " + AllianceSearchResult.class.getSimpleName());
		}

		AllianceSearchResult that = (AllianceSearchResult) o;

		if (this.score != that.score) {
			return this.score < that.score ? -1 : 1;
		} else {
			return that.allianceId - this.allianceId;
		}
	}
}
