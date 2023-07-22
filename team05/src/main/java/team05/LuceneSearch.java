package team05;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Paths;

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

import org.json.JSONObject;
import org.json.JSONArray;

public class LuceneSearch {

	public static void main(String[] args) throws Exception {
		JSONArray results = searchQuery("doctar");
		for (int i = 0; i < results.length(); i++) {
			JSONObject resultObject = results.getJSONObject(i);
			String title = resultObject.getString("SuggestedWord");
			System.out.println(title);
		}
	}

	private static final String INDEX_DIR = "indexedFiles";
	private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

	public static JSONArray searchQuery(String queryText) throws Exception {
		IndexSearcher searcher = createSearcher();

		JSONArray result = queryResult(queryText, searcher); 
		if (result.length() == 0) {
			result = suggestWordsResult(queryText, searcher);
		}

		return result;
	}
	
	// Returns a JSONArray of the query result
	private static JSONArray queryResult(String queryText, IndexSearcher searcher) throws Exception {

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);

		queryText = queryText.replaceAll("\\b(or)\\b", "OR");

		// Convert %20 to spaces
		queryText = URLDecoder.decode(queryText, "UTF-8");

		Query query = queryParser.parse(queryText);

		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		// Boost by query terms for each field
		for (String field : SEARCH_FIELDS) {
			float boostValue = 0f;
			
			Query fieldQuery = new TermQuery(new Term(field, queryText));
			
			// Set boost value
			switch (field) {
				case "docUrl":
					boostValue = 0.5f;
					break;
				case "title":
					boostValue = 0.9f;
					break;
				case "content":
					boostValue = 0.2f;
					break;
			}

			booleanQueryBuilder.add(new BoostQuery(fieldQuery, boostValue), BooleanClause.Occur.SHOULD);
		}

		booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);

		Query boostedQuery = booleanQueryBuilder.build();

		// Search the index
		TopDocs hits = searcher.search(boostedQuery, 50);
		
		JSONArray result = queryJson(hits, searcher);
		
		return result;
	}
	
	// Returns a JSONArray of the suggest words
	private static JSONArray suggestWordsResult(String queryText, IndexSearcher searcher) throws Exception {
		TopDocs hits = null;

		String[] terms = queryText.split("\\s+");
		for (String term : terms) {
			Term termField = new Term(SEARCH_FIELDS[1], term);

			// Create a fuzzy query for each term
			FuzzyQuery query = new FuzzyQuery(termField, 2); // The second parameter is the maximum edit distance
			
			// Search the index with the fuzzy query
			hits = searcher.search(query, 5);
		}
		
		JSONArray result = suggestedWordsJson(hits, searcher);
		
		return result;
	}
	
	private static JSONArray queryJson(TopDocs hits, IndexSearcher searcher) throws IOException {
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
	
	private static JSONArray suggestedWordsJson(TopDocs hits, IndexSearcher searcher) throws IOException {
		JSONArray result = new JSONArray();

		for (int i = 0; i < hits.scoreDocs.length; i++) {
            JSONObject jsonObject = new JSONObject();
            int docId = hits.scoreDocs[i].doc;
            Document d = searcher.doc(docId);
            String suggestedWord = d.get("title");
            String output = suggestedWord.substring(1, 2).toUpperCase()
                    + suggestedWord.substring(2).toLowerCase();
            jsonObject.put("SuggestedWord", output);
            result.put(jsonObject);
        }

		return result;
	}

	private static IndexSearcher createSearcher() throws IOException {
		Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}
}
