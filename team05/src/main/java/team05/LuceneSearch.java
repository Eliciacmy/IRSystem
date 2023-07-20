package team05;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DoubleValuesSource;
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

     // Define the boosting queries and their boost values
        Query boostQuery1 = new TermQuery(new Term("title", queryText));
        float boostValue1 = 0.7f;

        Query boostQuery2 = new TermQuery(new Term("content", queryText));
        float boostValue2 = 0.2f;
        
        Query boostQuery3 = new TermQuery(new Term("docurl", queryText));
        float boostValue3 = 0.7f;

        // Create FunctionScoreQuery with multiple boostByQuery
        FunctionScoreQuery q = new FunctionScoreQuery(query, DoubleValuesSource.SCORES);
        q = FunctionScoreQuery.boostByQuery(q, boostQuery1, boostValue1);
        q = FunctionScoreQuery.boostByQuery(q, boostQuery2, boostValue2);
        q = FunctionScoreQuery.boostByQuery(q, boostQuery3, boostValue3);
        
//        // Compile an expression
//        //Expression expr = JavascriptCompiler.compile("sqrt(_score) + ln(boost)");
//        Expression expr = JavascriptCompiler.compile("_score * boost");
//
//        // Bindings
//        SimpleBindings bindings = new SimpleBindings();
//        bindings.add("_score", DoubleValuesSource.SCORES);
//        bindings.add("boost", DoubleValuesSource.fromFloatField("boost"));
//
//        FunctionScoreQuery q = new FunctionScoreQuery(query, expr.getDoubleValuesSource(bindings));

        // Search the index
        TopDocs hits = searcher.search(q, 50);
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
