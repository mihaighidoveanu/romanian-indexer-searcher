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
import java.util.*;

public class Searcher {

    private QueryParser parser;
    private IndexSearcher indexSearcher;

    public static void main(String[] args) throws IOException {

        String[] queries = {"sisteme de operare", "fauna", "flori", "natură", "google"};

        String indexPath = "index";
        String queryField = "content";

        Searcher searcher = new Searcher();

        Directory index = FSDirectory.open(Paths.get(indexPath));

        // Create an index searcher
        IndexReader reader = DirectoryReader.open(index);
        searcher.indexSearcher = new IndexSearcher(reader);

        // create a query parser
        Analyzer analyzer = Indexer.getAnalyzer();
        searcher.parser = new QueryParser(queryField, analyzer);

        Date start = new Date();
        Map<Query, List<String>> results = searcher.search(queries);
        Date end = new Date();
        long duration = end.getTime() - start.getTime();
        results.forEach( (query, queryResults) -> {
            System.out.println("Results for processed query : " +  query.toString(queryField));
            for(String result : queryResults){
                System.out.println(result);
            }
            if(queryResults.size() == 0){
                System.out.println("No results found.");
            }
            System.out.println();
        });
        System.out.println("Queries total time : " + duration + "ms");
    }


    HashMap<Query, List<String>> search(String[] queries){
        HashMap<Query, List<String>> results = new HashMap<>();
        for(String queryString : queries) {
            HashMap<Query, List<String>> queryResults = this.query(queryString);
            results.putAll(queryResults);
        }
        return results;
    }

    HashMap<Query, List<String>> query(String queryString){
        HashMap<Query, List<String>> queryResult = new HashMap<>();
        try{
            Query query = this.parser.parse(queryString);

            // Execute the query and show the results
            TopDocs topDocs = this.indexSearcher.search(query, 10);
            Document document;
            // Display addresses
            ArrayList<String> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                document = this.indexSearcher.doc(scoreDoc.doc);
                String documentPath = document.get("path");
                results.add(documentPath);
            }
            queryResult.put(query,results);
        }
        catch(ParseException ex){
            System.out.println(ex);
        }
        catch (IOException ex){
            System.out.println(ex);
        }

        return queryResult;
    }

}
