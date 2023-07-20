package team05;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LuceneIndex {

	private static final String indexDirectory = "indexedFiles/";
	private static final String dirToBeIndexed = "scrapedData";

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
				switch (field) {
				case "docUrl":
					Field urlField = new Field(field, (String) object.get(field).toString(), bodyOptions);
					doc.add(urlField);
					doc.add(new NumericDocValuesField("boost", Double.doubleToRawLongBits(1.0)));
					break;
				case "title":
					Field titleField = new Field(field, (String) object.get(field).toString(), bodyOptions);
					doc.add(titleField);
					doc.add(new NumericDocValuesField("boost", Double.doubleToRawLongBits(3.0)));
					break;
				case "content":
					Field contentField = new Field(field, (String) object.get(field).toString(), bodyOptions);
					doc.add(contentField);
					doc.add(new NumericDocValuesField("boost", Double.doubleToRawLongBits(2.0)));
					break;
				}
				// doc.add(new Field(field, (String) object.get(field).toString(),
				// bodyOptions));

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
