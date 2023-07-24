package team05;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LuceneIndex {

    // Directory where the indexed files will be stored
    private static final String INDEX_DIRECTORY = "indexedFiless/";

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
     * Calculate PageRank scores for the indexed documents using the Random Walk algorithm.
     */
    public void calculatePageRank(Path indexDir) throws IOException {
        // Open the index directory for reading
        FSDirectory directory = FSDirectory.open(indexDir);
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(directory));

        // Initialize the PageRank scores for each document
        Map<Integer, Double> pageRankScores = new HashMap<>();
        int numDocs = indexSearcher.getIndexReader().numDocs();
        for (int i = 0; i < numDocs; i++) {
            pageRankScores.put(i, 1.0); // Start with equal PageRank for all documents
        }

        // Number of iterations for the PageRank calculation
        int numIterations = 10;

        // Damping factor (probability of following a link vs. jumping to a random page)
        double dampingFactor = 0.85;

        // Perform PageRank calculation iteratively
        for (int iter = 0; iter < numIterations; iter++) {
            Map<Integer, Double> newPageRankScores = new HashMap<>();

            // Calculate the PageRank for each document in this iteration
            for (int docID = 0; docID < numDocs; docID++) {
                Document doc = indexSearcher.doc(docID);

                // Get the outgoing links (outgoing edges) from this document
                String[] outgoingLinks = doc.getValues("outgoing_links");

                double newPageRankScore = (1 - dampingFactor) / numDocs; // Jump probability
                for (String link : outgoingLinks) {
                    int targetDocID = Integer.parseInt(link);
                    double targetDocPageRank = pageRankScores.getOrDefault(targetDocID, 0.0);
                    int numOutgoingLinks = doc.getValues("outgoing_links").length;
                    newPageRankScore += dampingFactor * (targetDocPageRank / numOutgoingLinks);
                }

                newPageRankScores.put(docID, newPageRankScore);
            }

            // Update the PageRank scores for the next iteration
            pageRankScores = newPageRankScores;
        }

        // Display the PageRank scores for each document
        for (Map.Entry<Integer, Double> entry : pageRankScores.entrySet()) {
            int docID = entry.getKey();
            double pageRankScore = entry.getValue();
            System.out.println("Document " + docID + ": PageRank Score = " + pageRankScore);
        }

        // Close the index reader
        indexSearcher.getIndexReader().close();
        directory.close();
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
     * Main function to initiate indexing and PageRank calculation.
     */
    public static void main(String[] args) throws IOException {
        Path indexDir = Paths.get(INDEX_DIRECTORY);

        LuceneIndex indexer = new LuceneIndex();

        // Perform indexing and get the number of indexed files
        int numIndexed = indexer.index(indexDir);
        System.out.println("Total files indexed: " + numIndexed);

        // Calculate PageRank scores for the indexed documents
        indexer.calculatePageRank(indexDir);
    }
}
