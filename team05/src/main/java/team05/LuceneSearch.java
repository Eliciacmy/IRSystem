package team05;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;

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
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.util.BytesRef;

import org.json.JSONObject;
import org.json.JSONArray;

public class LuceneSearch {

	public static void main(String[] args) throws Exception {
		JSONArray results = searchQuery("doctar");
		for (int i = 0; i < results.length(); i++) {
			JSONObject resultObject = results.getJSONObject(i);
			String title = resultObject.getString("Title");
			System.out.println(title);
		}
	}

	private static final String INDEX_DIR = "indexedFiles";
	private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

	public static JSONArray searchQuery(String queryText) throws Exception {
		IndexSearcher searcher = createSearcher();

		TopDocs topDocs = searchInContent(queryText, searcher).topHits;
		JSONArray result = searchIndex(topDocs, searcher);

		return result;
	}

	private static SearchResult searchInContent(String queryText, IndexSearcher searcher) throws Exception {

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		List<String> suggestedWords = new ArrayList<>();

		queryText = queryText.replaceAll("\\b(or)\\b", "OR");

		// Convert %20 to spaces
		queryText = URLDecoder.decode(queryText, "UTF-8");

		Query query = queryParser.parse(queryText);

		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		// Boost by query terms for each field
		for (String field : SEARCH_FIELDS) {
			FuzzyQuery fieldQuery = new FuzzyQuery(new Term(field, queryText), 1);
			float boostValue = 1.0f;
			booleanQueryBuilder.add(new BoostQuery(fieldQuery, boostValue), BooleanClause.Occur.SHOULD);
		}

		booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);

		Query boostedQuery = booleanQueryBuilder.build();

		// Search the index
		TopDocs hits = searcher.search(boostedQuery, 50);

		if (hits.scoreDocs.length == 0) {
			String[] terms = queryText.split("\\s+");
			for (String term : terms) {
				Term termField = new Term(SEARCH_FIELDS[1], term);

				// Create a fuzzy query for each term
				FuzzyQuery fuzzyQuery = new FuzzyQuery(termField, 2); // The second parameter is the maximum edit
																		// distance
				// Search the index with the fuzzy query
				TopDocs fuzzyHits = searcher.search(fuzzyQuery, 5);

				// Collect the suggested words from the fuzzy query results
				for (int i = 0; i < fuzzyHits.scoreDocs.length; i++) {
					String suggestedWord = searcher.doc(fuzzyHits.scoreDocs[i].doc).get(SEARCH_FIELDS[1]);
					String output = suggestedWord.substring(0, 1).toUpperCase()
							+ suggestedWord.substring(1).toLowerCase();
					suggestedWords.add(suggestedWord);
				}
			}
		}

		return new SearchResult(suggestedWords, hits);
	}

	private static JSONArray searchIndex(TopDocs hits, IndexSearcher searcher) throws IOException {
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

	private static IndexSearcher createSearcher() throws IOException {
		Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}


	public static class SearchResult {
		private final List<String> suggestedWords;
		private final TopDocs topHits;

		public SearchResult(List<String> suggestedWords, TopDocs topHits) {
			this.suggestedWords = suggestedWords;
			this.topHits = topHits;
		}

		public List<String> getSuggestedWords() {
			return suggestedWords;
		}

		public TopDocs getTopHits() {
			return topHits;
		}
	}
}
