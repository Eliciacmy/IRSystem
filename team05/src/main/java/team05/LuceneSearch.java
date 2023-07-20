package team05;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.json.JSONObject;
import org.json.JSONArray;

public class LuceneSearch {

	private static final String INDEX_DIR = "../../team05/indexedFiles";
	private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

	public static JSONArray searchQuery(String queryText) throws Exception {
		IndexSearcher searcher = createSearcher();
		
		TopDocs topDocs = searchInContent(queryText, searcher);
		JSONArray result = searchIndex(topDocs, searcher);
		
		return result;
	}
	
	private static TopDocs searchInContent(String queryText, IndexSearcher searcher) throws Exception {
		// Create search query parser
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);

		// Parse the user query
		Query query = queryParser.parse(queryText);

		// Search the index
		TopDocs hits = searcher.search(query, 50);
		return hits;
	}

	public static JSONArray searchIndex(TopDocs hits, IndexSearcher searcher) throws IOException {
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
}
