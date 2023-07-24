package team05;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

// Import Lucene libraries
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

// Import JSON libraries
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LuceneEval {

	// Directory where the Lucene index is stored
	private static final String INDEX_DIR = "indexedFiles";

	// Directory containing the relevant docs JSON file
	private static final String REL_DOCS_DIR = "placeholder";

	// Fields to be used for searching
	private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

	// Analyzer to use for tokenizing text
	private static final Analyzer analyzer = new StandardAnalyzer();

	public static void main(String[] args) {
	    Path dir = Paths.get(REL_DOCS_DIR);
	    Path jsonFilePath = dir.resolve("relevantDoc.json");
	    try {
	        byte[] jsonData = Files.readAllBytes(jsonFilePath);
	        String jsonString = new String(jsonData);

	        JSONTokener tokener = new JSONTokener(jsonString);
	        JSONArray arrayObjects = new JSONArray(tokener);

	        for (Object queryObject : arrayObjects) {
	            if (queryObject instanceof JSONObject) {
	                JSONObject query = (JSONObject) queryObject;
	                String queryText = (String) query.get("query");
	                JSONArray relevantDocsArray = (JSONArray) query.get("relevant_documents");

	                Set<String> relevantDocs = new HashSet<>();
	                for (int i = 0; i < relevantDocsArray.length(); i++) {
	                    relevantDocs.add(relevantDocsArray.optString(i));
	                }

	                JSONArray result = searchQuery(queryText, relevantDocs);

	                // Display the results and evaluation metrics
	                System.out.println("Query (" + queryText + ") Results:");
	                System.out.println(result.toString(2));
	                System.out.println();
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	// Method to perform a search query and return JSON results
	public static JSONArray searchQuery(String queryText, Set<String> relevantDocs) throws Exception {
		IndexSearcher searcher = createSearcher();
		queryText = removeSpecialCharacters(queryText);

		// Perform the main search query
		TopDocs topDocs = queryResult(queryText, searcher);

		JSONArray result = queryJson(topDocs, queryText, searcher);

		// Add evaluation metrics to the result
		return addEvaluationMetrics(result, relevantDocs);
	}

	// Method to add R5, R10, and R20 evaluation metrics to the JSON result
	private static JSONArray addEvaluationMetrics(JSONArray result, Set<String> relevantDocs) {
		int relevantDocsCount = relevantDocs.size();
		int r5 = calculateRecallAtRank(result, relevantDocs, 5);
		int r10 = calculateRecallAtRank(result, relevantDocs, 10);
		int r20 = calculateRecallAtRank(result, relevantDocs, 20);

		JSONObject metricsObject = new JSONObject();
		metricsObject.put("TotalRelevantDocuments", relevantDocsCount);
		metricsObject.put("R5", r5);
		metricsObject.put("R10", r10);
		metricsObject.put("R20", r20);

		result.put(metricsObject);

		return result;
	}

	// Method to calculate the recall at a given rank
	private static int calculateRecallAtRank(JSONArray result, Set<String> relevantDocs, int rank) {
		int retrievedRelevantDocs = 0;

		for (int i = 0; i < rank; i++) {
			JSONObject jsonObject = result.optJSONObject(i);
			if (jsonObject != null) {
				String url = jsonObject.optString("Url");
				if (relevantDocs.contains(url)) {
					retrievedRelevantDocs++;
				}
			}
		}

		return retrievedRelevantDocs;
	}

	// Method to perform the main search query
	private static TopDocs queryResult(String queryText, IndexSearcher searcher) throws Exception {
		QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		queryText = queryText.replaceAll("\\b(or)\\b", "OR");
		queryText = URLDecoder.decode(queryText, "UTF-8");
		Query query = queryParser.parse(queryText);

		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		// Boost by query terms for each field
		for (String field : SEARCH_FIELDS) {
			float boostValue = 0f;

			Query fieldQuery = new TermQuery(new Term(field, queryText));

			// Set boost value based on the field
			switch (field) {
			case "docurl":
				boostValue = 0.9f;
				break;
			case "title":
				boostValue = 0.5f;
				break;
			case "content":
				boostValue = 0.2f;
				break;
			}

			// Add the field query with boost to the boolean query builder
			booleanQueryBuilder.add(new BoostQuery(fieldQuery, boostValue), BooleanClause.Occur.SHOULD);
		}

		// Add the main query with boolean MUST to the boolean query builder
		booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);

		Query boostedQuery = booleanQueryBuilder.build();

		// Search the index
		TopDocs hits = searcher.search(boostedQuery, 5);

		return hits;
	}

	// Method to convert search results to JSON format
	private static JSONArray queryJson(TopDocs hits, String queryText, IndexSearcher searcher) throws IOException {
		JSONArray result = new JSONArray();

		for (int i = 0; i < hits.scoreDocs.length; i++) {
			JSONObject jsonObject = new JSONObject();
			int docId = hits.scoreDocs[i].doc;
			Document d = searcher.doc(docId);
			jsonObject.put("Title", d.get("title"));
			jsonObject.put("Url", d.get("docurl"));
			jsonObject.put("Content", d.get("content"));

			result.put(jsonObject);
		}

		return result;
	}

	// Method to create the Lucene searcher
	private static IndexSearcher createSearcher() throws IOException {
		Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
		IndexReader reader = DirectoryReader.open(dir);
		return new IndexSearcher(reader);
	}

	// Method to remove special characters from the input query
	public static String removeSpecialCharacters(String input) {
		// Define a regular expression pattern to match special characters
		String regex = "[^a-zA-Z0-9\\s]";

		// Replace all special characters with an empty string using a StringBuilder
		StringBuilder resultBuilder = new StringBuilder();
		for (char c : input.toCharArray()) {
			if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
				resultBuilder.append(c);
			}
		}

		return resultBuilder.toString();
	}
}
