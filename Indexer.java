
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.parser.ParsingReader;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.Date;

public class Indexer {

    private IndexWriter indexWriter;

    public static void main(String[] args) {


        String indexPath = "index";
        String docsPath = "docs";

        Indexer indexer = new Indexer();
        Date start = new Date();


        //open doc directory
        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("ERROR : Document directory '" + docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        try{
            // build stopwords file
            try{
                System.out.println("--- building stopwords file at " + Indexer.stopwordsFile);
                System.out.println("IMPORTANT : Remember to add the stopwords file in the classpath when running the searcher");
                buildStopwordsFile();
            }
            catch (IOException ex){
                System.out.println("WARNING : IOException at building stopwords file at " + Indexer.stopwordsFile + ".");
                System.out.println("WARNING : Can not build custom analyzer without stopwords file. ");
                System.out.println("--- defaulting to RomanianAnalyzer instead. Diacritics will not be treated");
            }

            // Create an index writer
            // index directory
            Directory index = FSDirectory.open(Paths.get(indexPath));
            // analyzer
            Analyzer analyzer = getAnalyzer();


            // indexer config
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            indexer.indexWriter = new IndexWriter(index, config);
            indexer.indexDocs(docDir);

            indexer.indexWriter.close();

        }
        catch (IOException ex){
            System.out.println(ex);
        }

        Date end = new Date();
        System.out.println("--- total time : " + (end.getTime() - start.getTime()) + " total milliseconds");
    }

    static String stopwordsFile = "stopwords.txt";

    static Analyzer getAnalyzer()  {
        Analyzer analyzer;
        try{
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(StandardTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    // remove diacritics before stemming
                    // if this is done afterwards, it may lead to understemming :
                    // e.g. functioneaza -> functioneaz, funcţionează -> function
                    .addTokenFilter(ASCIIFoldingFilterFactory.class)
                    .addTokenFilter(StopFilterFactory.class, "words", Indexer.stopwordsFile)
                    // stemming multiple times solves some problems, but may lead to overstemming
                    // we prefer overstemmng to understemming in this search use case
                    // we prefer retrieveing more documents including less relevant ones than missing important documents
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Romanian")
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Romanian")
                    .build();
        }
        catch (IOException ex){
            System.out.println("WARNING : IOException at building custom analyzer.");
            System.out.println("--- defaulting to RomanianAnalyzer instead. Diacritics will not be treated");
            analyzer = new RomanianAnalyzer();
        }

        return analyzer;
    }

    // build stopwords file with and without diacritics
    static void buildStopwordsFile() throws IOException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Indexer.stopwordsFile)))
        {
            String stopwordsString = RomanianAnalyzer.getDefaultStopSet().toString();
            String[] stopwords = stopwordsString.substring(1, stopwordsString.length() - 1).split(", ");
            for(String word : stopwords){
                //write stopwords without diacritics
                String normalized = Normalizer.normalize(word, Normalizer.Form.NFKD);
                String withoutDiacritics = normalized.replaceAll("[^\\p{ASCII}]", "");
                writer.write(withoutDiacritics + "\n");
            }
        }
        catch (IOException ex){
            throw new IOException(ex.getMessage());
        }
    }



     void indexDocs(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                        System.out.println("WARNING : IOException at reading file " + file + ". Skipping it.");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    private void indexDoc(Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            Document doc = new Document();

            // add path as a field that is searchable (indexed)
            // but don't tokenize it, index term frequency or positional information
            Field pathField = new StringField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);

            // use a LongPoint that is indexed ( fastly filterable with PointRangeQuery)
            doc.add(new LongPoint("modified", lastModified));

            Reader reader = new ParsingReader(stream);
            // passing a reader as a parameter tokenizes and indexes the text, but doesn't store it
            // file should be in UTF-8 encoding
            doc.add(new TextField("content", reader));

            // update the documents matching the same path
            // if it does not exist, create it
            System.out.println("--- creating or updating index " + file);
            this.indexWriter.updateDocument(new Term("path", file.toString()), doc);

        }

    }

}
