package controllers;

import com.patson.data.AirportSource;
import com.patson.model.Airport;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import scala.collection.JavaConverters;

import java.io.IOException;
import java.util.*;

public class SearchUtil {
	public static void main(String[] args) throws IOException {
//		init();
		search("new");
	}


	public static void init() throws IOException {
		try (RestHighLevelClient client = getClient()) {
			CountRequest countRequest = new CountRequest();
			CountRequest query = countRequest.query(QueryBuilders.matchAllQuery());
			System.out.println(client.count(query, RequestOptions.DEFAULT).getCount());

			client.indices().delete(new DeleteIndexRequest("airports"), RequestOptions.DEFAULT);

			List<Airport> airports = JavaConverters.asJava(AirportSource.loadAllAirports(false));
			System.out.println("loaded " + airports.size() + " airports");

			//RestHighLevelClient client = getClient();

			for (Airport airport : airports) {
				Map<String, Object> jsonMap = new HashMap<>();
				jsonMap.put("airportId", airport.id());
				jsonMap.put("airportIata", airport.iata());
				jsonMap.put("airportCity", airport.city());
				jsonMap.put("countryCode", airport.countryCode());
				jsonMap.put("airportName", airport.name());
				IndexRequest indexRequest = new IndexRequest("airports").source(jsonMap);
				client.index(indexRequest, RequestOptions.DEFAULT);

				System.out.println("indexed " + airport);
			}
		}
		System.out.println("ES DONE");

	}

	public static List<AirportSearchResult> search(String input) {
		//TODO sanitize input?
		try (RestHighLevelClient client = getClient()) {
			SearchRequest searchRequest = new SearchRequest("airports");
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


			QueryStringQueryBuilder multiMatchQueryBuilder = QueryBuilders.queryStringQuery(input + "*");
			multiMatchQueryBuilder.field("airportIata",50);
			multiMatchQueryBuilder.field("airportName",0);
			multiMatchQueryBuilder.field("airportCity",10);
//			multiMatchQueryBuilder.fuzziness(Fuzziness.TWO);
//			multiMatchQueryBuilder.maxExpansions(100);
//			multiMatchQueryBuilder.prefixLength(10);
//			multiMatchQueryBuilder.tieBreaker(20);

			multiMatchQueryBuilder.type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
			multiMatchQueryBuilder.boost(20);
			searchSourceBuilder.query(multiMatchQueryBuilder);

			searchRequest.source(searchSourceBuilder);
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

			List<AirportSearchResult> result = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> values = hit.getSourceAsMap();
				AirportSearchResult searchResult = new AirportSearchResult((int) values.get("airportId"), (String) values.get("airportIata"), (String) values.get("airportName"), (String) values.get("airportCity"), (String) values.get("countryCode"), hit.getScore());
				result.add(searchResult);
			}
			System.out.println("done");
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

class AirportSearchResult {
	private int id;
	private String iata, name, city, countryCode;
	private double score;

	public AirportSearchResult(int id, String iata, String name, String city, String countryCode, double score) {
		this.id = id;
		this.iata = iata;
		this.name = name;
		this.city = city;
		this.countryCode = countryCode;
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
}

