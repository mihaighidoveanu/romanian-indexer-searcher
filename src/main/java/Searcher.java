import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class Searcher {

    public static void main(String[] args) throws IOException {

        String queryString = "maşinuţă";
        String queryField = "content";
        String indexPath = "index";

        Directory index = FSDirectory.open(Paths.get(indexPath));

        // Create an index searcher
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        // create a query parser
        Analyzer analyzer = Indexer.getAnalyzer();
        QueryParser parser = new QueryParser(queryField, analyzer);

        // create a query based on the parsed interrogation
        try{
            Query query = parser.parse(queryString);
            System.out.println("Original query: " + queryString);
            System.out.println("Searching for: " + query.toString(queryField));

            // Execute que query and show the results
            TopDocs topDocs = searcher.search(query, 10);

            Document document;
            // Display addresses
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                document = searcher.doc(scoreDoc.doc);
                System.out.println(document.get("path"));
            }
        }
        catch (ParseException ex){
            System.out.println(ex);
        }




    }
}
