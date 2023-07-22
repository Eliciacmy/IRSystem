package team05;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap.Builder;

import org.json.JSONObject;
import org.json.JSONArray;

public class LuceneSearch {

	public static void main(String[] args) throws Exception {
		String searchTerm = "details";
        List<String> synonyms = getSynonyms(searchTerm);

        if (!synonyms.isEmpty()) {
            System.out.println("Synonyms for '" + searchTerm + "': " + synonyms);
        }
  		
		JSONArray results = searchQuery("details");
		System.out.println(results.getJSONObject(results.length() - 1));
		for (int i = 0; i < results.length(); i++) {
			JSONObject resultObject = results.getJSONObject(i);
			//String title = resultObject.getString("Synonyms");
			//String title = resultObject.getString("SuggestedWord");

		}
	}

	private static final String INDEX_DIR = "indexedFiles";
	private static final String[] SEARCH_FIELDS = { "docurl", "title", "content" };

	public static JSONArray searchQuery(String queryText) throws Exception {
		IndexSearcher searcher = createSearcher();
		
		queryText = removeSpecialCharacters(queryText);

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
		
		List<String> synonyms = getSynonyms(queryText);
		
		JSONArray result = queryJson(hits, synonyms, searcher);
		
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
	
	private static JSONArray queryJson(TopDocs hits, List<String> synonyms, IndexSearcher searcher) throws IOException {
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
		
		JSONObject jsonObject = new JSONObject();
		result.put(jsonObject.put("Synonyms", synonyms));
		
		return result;
	}
	
	private static JSONArray suggestedWordsJson(TopDocs hits, IndexSearcher searcher) throws IOException {
        JSONArray result = new JSONArray();
        HashSet<String> uniqueSuggestedWords = new HashSet<>();

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            int docId = hits.scoreDocs[i].doc;
            Document d = searcher.doc(docId);
            String output = d.get("title");

            // Capitalize the First Character of the Word
            output = output.substring(1, 2).toUpperCase() + output.substring(2).toLowerCase();

            // Split by ","
            String[] suggestedWordList = output.split(",");

            // Remove leading and trailing whitespaces
            uniqueSuggestedWords.add(suggestedWordList[0].trim());           
        }

        // Convert the uniqueSuggestedWords HashSet to a JSONArray
        for (String uniqueSuggestedWord : uniqueSuggestedWords) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("SuggestedWord", uniqueSuggestedWord);
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
	
	public static String removeSpecialCharacters(String input) {
        // Define a regular expression pattern to match special characters
        String regex = "[^a-zA-Z0-9\\s]";

        // Replace all special characters with an empty string
        String result = input.replaceAll(regex, "");

        return result;
    }
	
	private static List<String> getSynonyms(String searchTerm) {
        String filePath = "wordnet/wn_s.pl";
        
        List<String> nounSynonyms = new ArrayList<>();
        int searchTermSynsetID = -1; // Placeholder for the SynsetID of the search term

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
        return nounSynonyms;
    }
}
