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

public class LuceneSearch {
	
	private static final String INDEX_DIR = "indexedFiles";
    private static final String[] SEARCH_FIELDS = {"docurl", "title", "content"}; // Add your desired search fields here

    public static void main(String[] args) throws Exception {
        // Create Lucene searcher. It searches over a single IndexReader.
        IndexSearcher searcher = createSearcher();

        // Accept user input for the search query
        String searchQuery = getUserInput();

        // Normalize the search query
        String normalizedQuery = normalizeQuery(searchQuery);

        // Search indexed contents using the normalized query
        TopDocs foundDocs = searchInContent(normalizedQuery, searcher);

        // Total found documents
        System.out.println("Total Results :: " + foundDocs.totalHits);

        // Print out the path of files which have the searched term
        for (ScoreDoc sd : foundDocs.scoreDocs) {
            Document d = searcher.doc(sd.doc);
            System.out.println("URL: " + d.get("docurl") + ", Title: " + d.get("title") + ", Score: " + sd.score);
        }
    }

    private static String getUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter search query: ");
        String searchQuery = scanner.nextLine();
        scanner.close();
        return searchQuery;
    }

    private static String normalizeQuery(String searchQuery) {
        // Escape special characters in the search query
        searchQuery = QueryParser.escape(searchQuery);

        // Remove unnecessary characters or perform other text processing techniques if required
        return searchQuery;
    }

    private static TopDocs searchInContent(String textToFind, IndexSearcher searcher) throws Exception {
        // Create search query parser
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new MultiFieldQueryParser(SEARCH_FIELDS, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        // Parse the user query
        Query query = queryParser.parse(textToFind);

        // Search the index
        TopDocs hits = searcher.search(query, 10);
        return hits;
    }

    private static IndexSearcher createSearcher() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }
}
