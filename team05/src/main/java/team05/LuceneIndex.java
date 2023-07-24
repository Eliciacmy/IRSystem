package team05;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LuceneIndex {

    // Directory where the indexed files will be stored
    private static final String INDEX_DIRECTORY = "indexedFiles/";

    // Directory containing the JSON files to be indexed
    private static final String DIR_TO_BE_INDEXED = "scrapedData";

    /**
     * JSON Parser to read and parse JSON files
     */
    public JSONArray parseJSONFile() throws IOException {
        Path dir = Paths.get(DIR_TO_BE_INDEXED);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            // Process each JSON file one by one
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
     * Stream indexer to index JSON objects from files
     */
    @SuppressWarnings("unchecked")
    private int index(Path indexDir) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // Initialize the IndexWriter to write the indexed data to the specified directory
        IndexWriter indexWriter = new IndexWriter(FSDirectory.open(indexDir), config);

        // Parse the JSON objects from the files
        JSONArray jsonObjects = parseJSONFile();

        // Index each JSON object
        for (JSONObject object : (Iterable<JSONObject>) jsonObjects) {
            Document doc = new Document();

            // Create the field options to store term vectors and other indexing options
            final FieldType bodyOptions = new FieldType();
            bodyOptions.setStored(true);
            bodyOptions.setStoreTermVectors(true);
            bodyOptions.setTokenized(true);
            bodyOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

            // Add each field of the JSON object to the document
            for (String field : (Iterable<String>) object.keySet()) {
                doc.add(new Field(field, (String) object.get(field).toString(), bodyOptions));
            }

            try {
                // Add the document to the index writer
                indexWriter.addDocument(doc);
            } catch (IOException ex) {
                System.err.println("Error adding documents to the index. " + ex.getMessage());
            }
        }

        // Get the number of documents indexed
        int numIndexed = indexWriter.numRamDocs();

        // Close the index writer
        finish(indexWriter);

        return numIndexed;
    }

    /**
     * Write the document to the index and close the index writer
     */
    public void finish(IndexWriter indexWriter) {
        try {
            // Commit the changes and close the index writer
            indexWriter.commit();
            indexWriter.close();
        } catch (IOException ex) {
            System.err.println("We had a problem closing the index: " + ex.getMessage());
        }
    }

    /**
     * Main function to initiate indexing
     */
    public static void main(String[] args) throws IOException {
        Path indexDir = Paths.get(INDEX_DIRECTORY);

        LuceneIndex indexer = new LuceneIndex();

        // Perform indexing and get the number of indexed files
        int numIndexed = indexer.index(indexDir);
        System.out.println("Total files indexed: " + numIndexed);
    }
}
