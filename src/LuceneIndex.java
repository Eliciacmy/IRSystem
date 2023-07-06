import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LuceneIndex extends Analyzer {

	private static final String indexDirectory = "indexedFiles/";
	private static final String dirToBeIndexed = "scrapedData";

	private CharArraySet stopWords;
	private static HashMap<String, Integer> wordFrequencies;

	public LuceneIndex() {
		this.stopWords = EnglishAnalyzer.getDefaultStopSet();
		LuceneIndex.wordFrequencies = new HashMap<>();
	}

	/**
	 * Split words using Standard tokenizer (split text based on Unicode character
	 * and apply normalization)
	 * Filter words with less than 2 characters
	 * Remove stopwords
	 * (https://github.com/apache/lucene/blob/main/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java)
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		TokenFilter lengthFilter = new LengthFilter(source, 3, Integer.MAX_VALUE);
		TokenFilter lowercaseFilter = new LowerCaseFilter(lengthFilter);
		TokenFilter stopwordFilter = new StopFilter(lowercaseFilter, stopWords);
		TokenFilter stemFilter = new PorterStemFilter(stopwordFilter);
		return new TokenStreamComponents(source, stemFilter);
	}

	/**
	 * JSON Parser
	 */
	public JSONArray parseJSONFile() throws IOException {
		  
		  Path dir = Paths.get(dirToBeIndexed);
		     
		  try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
		      for (Path p : stream) {
		          InputStream jsonFile = new FileInputStream(p.toString());
		          Reader readerJson = new InputStreamReader(jsonFile);
		    
		    // Parse the json file using simple-json library
		    Object fileObjects = JSONValue.parse(readerJson);
		    JSONArray arrayObjects = (JSONArray) fileObjects;

		    return arrayObjects;
		      }
		  }
		  return null;
		 }

	/**
	 * Stream indexer
	 */
	@SuppressWarnings("unchecked")
	private int index(Path indexDir) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(indexDir), config);

		JSONArray jsonObjects = parseJSONFile();

		for (JSONObject object : (List<JSONObject>) jsonObjects) {
			Document doc = new Document();
			final FieldType bodyOptions = new FieldType();
            bodyOptions.setStored(true);
            bodyOptions.setStoreTermVectors(true);
            bodyOptions.setTokenized(true);
            bodyOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			
			for (String field : (Set<String>) object.keySet()) {
				doc.add(new Field(field, (String) object.get(field).toString(), bodyOptions));
			}
			
			try {
				System.out.println(doc);
				indexWriter.addDocument(doc);
			} catch (IOException ex) {
				System.err.println("Error adding documents to the index. " + ex.getMessage());
			}
			
		}
		
		int numIndexed = indexWriter.numRamDocs();
		
		finish(indexWriter);
		
		return numIndexed;
	}

	/**
	 * Write the document to the index and close it
	 */
	public void tokenization(String field, Analyzer analyzer, Document doc, JSONObject object) throws IOException {
		if (field == "content") {
			TokenStream stream = analyzer.tokenStream("field", new StringReader((String) object.get(field)));
			CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

			try {
				stream.reset();
				while (stream.incrementToken()) {
					String term = termAtt.toString();
					int frequency = wordFrequencies.getOrDefault(term, 0);
					wordFrequencies.put(term, frequency + 1);
				}
				stream.end();
			} finally {
				stream.close();
			}
			
			// Sort bag of words based on frequency
			List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(wordFrequencies.entrySet());
			Collections.sort(sortedEntries, Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()));

			for (Map.Entry<String, Integer> entry : sortedEntries) {
				String term = entry.getKey();
				int frequency = entry.getValue();
				System.out.println(term + ": " + frequency);

				doc.add(new TextField("term", term, Field.Store.NO));
				doc.add(new TextField("frequency", Integer.toString(frequency), Field.Store.NO));
			}
		}
	}

	/**
	 * Write the document to the index and close it
	 */
	public void finish(IndexWriter indexWriter) {
		try {
			indexWriter.commit();
			indexWriter.close();
		} catch (IOException ex) {
			System.err.println("We had a problem closing the index: " + ex.getMessage());
		}
	}
	
	/**
	 * Main function
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException {
		Path indexDir = Paths.get(indexDirectory);

		LuceneIndex indexer = new LuceneIndex();

		int numIndexed = indexer.index(indexDir);
		System.out.println("Total files indexed " + numIndexed);
		System.out.println(numIndexed);
	}
}
