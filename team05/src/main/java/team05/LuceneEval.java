package team05;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.text.DecimalFormat;
import java.io.FileWriter;
import java.util.LinkedHashMap;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// Import JSON libraries
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LuceneEval {

	// Directory where the Lucene index is stored
	private static final String INDEX_DIR = "indexedFiles";

	// Directory containing the relevant docs JSON file
	private static final String REL_DOCS_DIR = "results";

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

			JSONArray metricsResult = new JSONArray();
			for (Object queryObject : arrayObjects) {
				if (queryObject instanceof JSONObject) {
					JSONObject query = (JSONObject) queryObject;
					String queryText = (String) query.get("query");
					JSONArray relevantDocsArray = (JSONArray) query.get("relevant_documents");

					Set<String> relevantDocs = new HashSet<>();
					for (int i = 0; i < relevantDocsArray.length(); i++) {
						relevantDocs.add(relevantDocsArray.optString(i));
					}

					JSONArray result = searchQuery(queryText, metricsResult, relevantDocs);

					// Write the 'result' JSONArray to a JSON file in the same directory
					Path resultFilePath = dir.resolve("relevancePerformance.json");
					try (FileWriter fileWriter = new FileWriter(resultFilePath.toFile())) {
						fileWriter.write(result.toString());
						System.out.println("Search results have been written to: " + resultFilePath);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// Calculate MAP
			double map = calculateMAP(metricsResult);
			System.out.println("Mean Average Precision (MAP): " + map);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Method to perform a search query and return JSON results
	public static JSONArray searchQuery(String queryText, JSONArray metricsResult, Set<String> relevantDocs)
			throws Exception {
		IndexSearcher searcher = createSearcher();
		queryText = removeSpecialCharacters(queryText);

		// Perform the main search query
		TopDocs topDocs = queryResult(queryText, searcher);

		JSONArray result = queryJson(topDocs, queryText, searcher);

		// Add evaluation metrics to the result
		return addEvaluationMetrics(queryText, result, metricsResult, relevantDocs);
	}

	// Method to add R5, R10, and R20 evaluation metrics to the JSON result
	private static JSONArray addEvaluationMetrics(String queryText, JSONArray result, JSONArray metricsResult,
			Set<String> relevantDocs) {
		// Create a LinkedHashMap to maintain the order of elements
		Map<String, Object> metricsObject = new LinkedHashMap<>();

		metricsObject.put("Query", queryText);

		int relevantDocsCountR5 = calculateRelevantDocsCountAtRank(result, relevantDocs, 5);
		int relevantDocsCountR10 = calculateRelevantDocsCountAtRank(result, relevantDocs, 10);
		int relevantDocsCountR20 = calculateRelevantDocsCountAtRank(result, relevantDocs, 20);

		metricsObject.put("Relevant Document Count R5", relevantDocsCountR5);
		metricsObject.put("Relevant Document Count R10", relevantDocsCountR10);
		metricsObject.put("Relevant Document Count R20", relevantDocsCountR20);

		double rR5 = calculateRecallAtRank(result, relevantDocs, 5);
		double rR10 = calculateRecallAtRank(result, relevantDocs, 10);
		double rR20 = calculateRecallAtRank(result, relevantDocs, 20);

		metricsObject.put("Recall R5", rR5);
		metricsObject.put("Recall R10", rR10);
		metricsObject.put("Recall R20", rR20);

		double pR5 = calculatePrecisionAtRank(result, relevantDocs, 5);
		double pR10 = calculatePrecisionAtRank(result, relevantDocs, 10);
		double pR20 = calculatePrecisionAtRank(result, relevantDocs, 20);

		metricsObject.put("Precision R5", pR5);
		metricsObject.put("Precision R10", pR10);
		metricsObject.put("Precision R20", pR20);

		double f1R5 = calculateF1Score(rR5, pR5);
		double f1R10 = calculateF1Score(rR10, pR10);
		double f1R20 = calculateF1Score(rR20, pR20);

		metricsObject.put("F1 R5", f1R5);
		metricsObject.put("F1 R10", f1R10);
		metricsObject.put("F1 R20", f1R20);

		// Convert the LinkedHashMap to a JSONObject
		JSONObject jsonObject = new JSONObject(metricsObject);

		// Add the JSONObject to the JSONArray
		metricsResult.put(jsonObject);

		return metricsResult;
	}

	// Method to calculate the relevant documents at a given rank
	private static int calculateRelevantDocsCountAtRank(JSONArray result, Set<String> relevantDocs, int rank) {
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

	// Method to calculate the recall at a given rank
	private static double calculateRecallAtRank(JSONArray result, Set<String> relevantDocs, int rank) {
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

		// Calculate recall using the formula: recall = retrieved relevant / total
		// relevant
		double recall = (double) retrievedRelevantDocs / relevantDocs.size();

		return recall;
	}

	// Method to calculate precision
	private static double calculatePrecisionAtRank(JSONArray result, Set<String> relevantDocs, int rank) {
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

		// Calculate precision using the formula: precision = retrieved relevant / total
		// retrieved
		double precision = (double) retrievedRelevantDocs / rank;

		return precision;
	}

	// Method to calculate F1 score
	private static double calculateF1Score(double recall, double precision) {
		// Calculate F1 score using the formula: F1 = 2 * (precision * recall) /
		// (precision + recall)
		double f1Score = 2 * (precision * recall) / (precision + recall);

		// Round the F1 score to two decimal places
		DecimalFormat df = new DecimalFormat("#.##");
		f1Score = Double.parseDouble(df.format(f1Score));

		return f1Score;
	}

	// Method to perform the main search query
	private static TopDocs queryResult(String queryText, IndexSearcher searcher) throws Exception {
		QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);

		queryText = queryText.replaceAll("\\b(or)\\b", "OR");
		queryText = URLDecoder.decode(queryText, "UTF-8");
		queryText = removeSpecialCharacters(queryText);
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
				boostValue = 0.7f;
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
		TopDocs hits = searcher.search(boostedQuery, 20);

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

	// Method to calculate Mean Average Precision (MAP)
	private static double calculateMAP(JSONArray metricsResult) {
		int totalQueries = metricsResult.length();
		double sumAP = 0.0;

		for (int i = 0; i < metricsResult.length(); i++) {
			JSONObject metricsObject = metricsResult.getJSONObject(i);
			int relevantDocsCountR20 = metricsObject.getInt("Relevant Document Count R20");
			double precisionR20 = metricsObject.getDouble("Precision R20");

			// Calculate Average Precision (AP) for each query
			double ap = (relevantDocsCountR20 > 0) ? precisionR20 : 0.0;
			sumAP += ap;
		}

		// Calculate Mean Average Precision (MAP)
		double map = sumAP / totalQueries;

		// Round the MAP to two decimal places
		DecimalFormat df = new DecimalFormat("#.##");
		map = Double.parseDouble(df.format(map));

		return map;
	}
}
