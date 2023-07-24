package team05;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;

public class LuceneEval {

    // Directory where the Lucene index is stored
    private static final String INDEX_DIR = "indexedFiles";
    
    // Fields to be used for searching
    private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

    // Analyzer to use for tokenizing text
    private static final Analyzer analyzer = new StandardAnalyzer();
    
    public static void main(String[] args) {
    	
    	String url = "https://www.google.com/search?q=computer+site%3Aencyclopedia.com"; // Replace with the URL of the web page you want to extract links from

        // Set to store the extracted links
        Set<String> relevantDocsQuery1 = new HashSet<>();

        try {
            org.jsoup.nodes.Document document = Jsoup.connect(url).get();
            Elements links = (Elements) document.select("a[href]");

            for (Element link : links) {
                String linkUrl = link.attr("abs:href"); // Get the absolute URL of the link
                relevantDocsQuery1.add(linkUrl);
            }
            
            System.out.println(relevantDocsQuery1);
        } catch (IOException e) {
            System.err.println("Error fetching content from the URL: " + e.getMessage());
        }
    	
    	
    	
    	
    	
    	try {
//    		Path indexPath = Paths.get("team05/placeholder");
//    		Directory index = new MMapDirectory(indexPath);

            // Perform search queries
            String query1 = "Computer";
//            String query2 = "Glasgow";
//            String query3 = "United";
//            String query4 = "Kingdom";
//            String query5 = "Library";
//            String query6 = "Fog";
//            String query7 = "Empires";
//            String query8 = "Doctor";
//            String query9 = "Hospital";
//            String query10 = "Bachelor";
//            String query11 = "Degree";
//            String query12 = "Internet";
//            String query13 = "Things";
//            String query14 = "Information";
//            String query15 = "Info";
//            String query16 = "Retrieval";
//            String query17 = "Retrieve";
//            String query18 = "Universe";
//            String query19 = "University";

            // Relevant documents for evaluation (Assuming you have these URLs from the ground truth)
            //Set<String> relevantDocsQuery1 = new HashSet<>();
            //relevantDocsQuery1.add("https://www.google.com/search?q=computer+site%3Aencyclopedia.com");

//            Set<String> relevantDocsQuery2 = new HashSet<>();
//            relevantDocsQuery2.add("https://www.google.com/search?q=glasgow+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery3 = new HashSet<>();
//            relevantDocsQuery3.add("https://www.google.com/search?q=united+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery4 = new HashSet<>();
//            relevantDocsQuery4.add("https://www.google.com/search?q=kingdom+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery5 = new HashSet<>();
//            relevantDocsQuery5.add("https://www.google.com/search?q=library+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery6 = new HashSet<>();
//            relevantDocsQuery6.add("https://www.google.com/search?q=fog+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery7 = new HashSet<>();
//            relevantDocsQuery7.add("https://www.google.com/search?q=empires+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery8 = new HashSet<>();
//            relevantDocsQuery8.add("https://www.google.com/search?q=doctor+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery9 = new HashSet<>();
//            relevantDocsQuery9.add("https://www.google.com/search?q=hospital+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery10 = new HashSet<>();
//            relevantDocsQuery10.add("https://www.google.com/search?q=bachelor+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery11 = new HashSet<>();
//            relevantDocsQuery11.add("https://www.google.com/search?q=degree+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery12 = new HashSet<>();
//            relevantDocsQuery12.add("https://www.google.com/search?q=internet+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery13 = new HashSet<>();
//            relevantDocsQuery13.add("https://www.google.com/search?q=things+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery14 = new HashSet<>();
//            relevantDocsQuery14.add("https://www.google.com/search?q=informations+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery15 = new HashSet<>();
//            relevantDocsQuery15.add("https://www.google.com/search?q=info+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery16 = new HashSet<>();
//            relevantDocsQuery16.add("https://www.google.com/search?q=retrieval+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery17 = new HashSet<>();
//            relevantDocsQuery17.add("https://www.google.com/search?q=retrieve+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery18 = new HashSet<>();
//            relevantDocsQuery18.add("https://www.google.com/search?q=universe+site%3Aencyclopedia.com");
//
//            Set<String> relevantDocsQuery19 = new HashSet<>();
//            relevantDocsQuery19.add("https://www.google.com/search?q=university+site%3Aencyclopedia.com");

            // Perform search queries and get the results with evaluation metrics
            JSONArray result1 = searchQuery(query1, relevantDocsQuery1);
//            JSONArray result2 = searchQuery(query2, relevantDocsQuery2);
//            JSONArray result3 = searchQuery(query3, relevantDocsQuery3);
//            JSONArray result4 = searchQuery(query4, relevantDocsQuery4);
//            JSONArray result5 = searchQuery(query5, relevantDocsQuery5);
//            JSONArray result6 = searchQuery(query6, relevantDocsQuery6);
//            JSONArray result7 = searchQuery(query7, relevantDocsQuery7);
//            JSONArray result8 = searchQuery(query8, relevantDocsQuery8);
//            JSONArray result9 = searchQuery(query9, relevantDocsQuery9);
//            JSONArray result10 = searchQuery(query10, relevantDocsQuery10);
//            JSONArray result11 = searchQuery(query11, relevantDocsQuery11);
//            JSONArray result12 = searchQuery(query12, relevantDocsQuery12);
//            JSONArray result13 = searchQuery(query13, relevantDocsQuery13);
//            JSONArray result14 = searchQuery(query14, relevantDocsQuery14);
//            JSONArray result15 = searchQuery(query15, relevantDocsQuery15);
//            JSONArray result16 = searchQuery(query16, relevantDocsQuery16);
//            JSONArray result17 = searchQuery(query17, relevantDocsQuery17);
//            JSONArray result18 = searchQuery(query18, relevantDocsQuery18);
//            JSONArray result19 = searchQuery(query19, relevantDocsQuery19);

            // Display the results and evaluation metrics
//            System.out.println("Query 1 Results:");
//            System.out.println(result1.toString(2));
//            System.out.println();

//            System.out.println("Query 2 Results:");
//            System.out.println(result2.toString(2));
//            System.out.println();
//
//            System.out.println("Query 3 Results:");
//            System.out.println(result3.toString(2));
//            System.out.println();
//
//            System.out.println("Query 4 Results:");
//            System.out.println(result4.toString(2));
//            System.out.println();
//
//            System.out.println("Query 5 Results:");
//            System.out.println(result5.toString(2));
//            System.out.println();
//
//            System.out.println("Query 6 Results:");
//            System.out.println(result6.toString(2));
//            System.out.println();
//
//            System.out.println("Query 7 Results:");
//            System.out.println(result7.toString(2));
//            System.out.println();
//
//            System.out.println("Query 8 Results:");
//            System.out.println(result8.toString(2));
//            System.out.println();
//
//            System.out.println("Query 9 Results:");
//            System.out.println(result9.toString(2));
//            System.out.println();
//
//            System.out.println("Query 10 Results:");
//            System.out.println(result10.toString(2));
//            System.out.println();
//
//            System.out.println("Query 11 Results:");
//            System.out.println(result11.toString(2));
//            System.out.println();
//
//            System.out.println("Query 12 Results:");
//            System.out.println(result12.toString(2));
//            System.out.println();
//
//            System.out.println("Query 13 Results:");
//            System.out.println(result13.toString(2));
//            System.out.println();
//
//            System.out.println("Query 14 Results:");
//            System.out.println(result14.toString(2));
//            System.out.println();
//
//            System.out.println("Query 15 Results:");
//            System.out.println(result15.toString(2));
//            System.out.println();
//
//            System.out.println("Query 16 Results:");
//            System.out.println(result16.toString(2));
//            System.out.println();
//
//            System.out.println("Query 17 Results:");
//            System.out.println(result17.toString(2));
//            System.out.println();
//
//            System.out.println("Query 18 Results:");
//            System.out.println(result18.toString(2));
//            System.out.println();
//
//            System.out.println("Query 19 Results:");
//            System.out.println(result19.toString(2));
//            System.out.println();

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
                //System.out.println(url);
                if (relevantDocs.contains(url)) {
                    retrievedRelevantDocs++;
                }
            }
        }

        return retrievedRelevantDocs;
    }
    
 // Function to calculate Precision at K
    private static double calculatePrecisionAtK(Set<String> relevantDocs, Set<String> searchResults, int k) {
        int relevantCount = 0;
        int retrievedCount = Math.min(k, searchResults.size());

        for (String url : searchResults) {
            if (relevantDocs.contains(url)) {
                relevantCount++;
            }
            if (relevantCount == k) {
                break;
            }
        }

        return (double) relevantCount / retrievedCount;
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
