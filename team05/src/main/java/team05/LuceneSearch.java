package team05;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
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

// Import JSON libraries
import org.json.JSONObject;
import org.json.JSONArray;

public class LuceneSearch {

    // Directory where the Lucene index is stored
    private static final String INDEX_DIR = "../../team05/indexedFiles";
    
    // Fields to be used for searching
    private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };
    
    // Path to the Prolog file containing WordNet data
    private static final String filePath = "../../team05/wordnet/wn_s.pl";

    // Analyzer to use for tokenizing text
    private static final Analyzer analyzer = new StandardAnalyzer();

    // Method to perform a search query and return JSON results
    public static JSONArray searchQuery(String queryText) throws Exception {
        IndexSearcher searcher = createSearcher();
        queryText = removeSpecialCharacters(queryText);

        // Perform the main search query
        JSONArray result = queryResult(queryText, searcher);

        // If no results found, suggest alternative words based on WordNet data
        if (result.length() == 0) {
            result = suggestWordsResult(queryText, searcher);
        }

        return result;
    }

    // Method to perform the main search query
    private static JSONArray queryResult(String queryText, IndexSearcher searcher) throws Exception {
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
                    boostValue = 0.7f;
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
        TopDocs hits = searcher.search(boostedQuery, 50);

        // Convert search results to JSON
        JSONArray result = queryJson(hits, queryText, searcher);

        return result;
    }

    // Method to suggest alternative words based on WordNet data
    private static JSONArray suggestWordsResult(String queryText, IndexSearcher searcher) throws Exception {
        TopDocs hits = null;

        String[] terms = queryText.split("\\s+");
        Set<String> uniqueSuggestedWords = new HashSet<>();

        for (String term : terms) {
            Term termField = new Term(SEARCH_FIELDS[1], term);
            FuzzyQuery query = new FuzzyQuery(termField, 2); // The second parameter is the maximum edit distance
            hits = searcher.search(query, 5);

            for (int i = 0; i < hits.scoreDocs.length; i++) {
                int docId = hits.scoreDocs[i].doc;
                Document d = searcher.doc(docId);
                String output = d.get("title");
                output = output.substring(1, 2).toUpperCase() + output.substring(2).toLowerCase();
                String[] suggestedWordList = output.split(",");
                uniqueSuggestedWords.add(suggestedWordList[0].trim());
            }
        }

        // Convert suggested words to JSON
        JSONArray result = suggestedWordsJson(uniqueSuggestedWords);

        return result;
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

        List<String> synonyms = getSynonyms(queryText);
        if (!synonyms.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Synonyms", synonyms);
            result.put(jsonObject);
        }

        return result;
    }

    // Method to convert suggested words to JSON format
    private static JSONArray suggestedWordsJson(Set<String> uniqueSuggestedWords) {
        JSONArray result = new JSONArray();

        for (String uniqueSuggestedWord : uniqueSuggestedWords) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("SuggestedWord", uniqueSuggestedWord);
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

    // Method to get synonyms of the given search term from WordNet
    private static List<String> getSynonyms(String searchTerm) {
        List<String> nounSynonyms = new ArrayList<>();
        int searchTermSynsetID = -1; // Placeholder for the SynsetID of the search term

        // Read WordNet data from the Prolog file
        try {
            Map<Integer, List<String>> synsetIDMap = new HashMap<>();

            String line;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    int synsetID = Integer.parseInt(parts[0].replaceAll("\\D", ""));
                    String word = parts[2].replaceAll("'", "").trim();
                    String POS = parts[3].replaceAll("'", "").trim();

                    List<String> words = synsetIDMap.getOrDefault(synsetID, new ArrayList<>());
                    words.add(word);
                    synsetIDMap.put(synsetID, words);

                    // Check if the word is an exact match for the search term and has POS "n" (nouns)
                    if (word.equalsIgnoreCase(searchTerm) && POS.equalsIgnoreCase("n")) {
                        searchTermSynsetID = synsetID;
                    }
                }
            }
            bufferedReader.close();

            // Collect noun synonyms associated with the search term's SynsetID
            if (searchTermSynsetID != -1) {
                List<String> wordsWithSameSynsetID = synsetIDMap.get(searchTermSynsetID);
                for (String word : wordsWithSameSynsetID) {
                    if (!word.equalsIgnoreCase(searchTerm)) {
                        nounSynonyms.add(word);
                    }
                }
            } else {
                System.out.println("Word '" + searchTerm + "' not found in the Prolog file.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return nounSynonyms.subList(0, Math.min(5, nounSynonyms.size()));
    }
}
