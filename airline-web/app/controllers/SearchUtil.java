package controllers;

import com.patson.data.AirportSource;
import com.patson.data.CountrySource;
import com.patson.model.Airport;
import com.patson.model.Country;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("ES check finished");
	}

	public static void main(String[] args) throws IOException {

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

		List<Airport> airports = JavaConverters.asJava(AirportSource.loadAllAirports(false));
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

