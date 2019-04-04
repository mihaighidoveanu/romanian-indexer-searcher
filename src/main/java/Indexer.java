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

    public static void main(String[] args) {

        Date start = new Date();

        String indexPath = "index";
        String docsPath = "docs/real";

        //open doc directory
        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("ERROR : Document directory '" + docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        try{
            // Create an index writer
            // index directory
            Directory index = FSDirectory.open(Paths.get(indexPath));
            // analyzer
            Analyzer analyzer = getAnalyzer();

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

            // indexer config
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            IndexWriter indexWriter = new IndexWriter(index, config);
            indexDocs(indexWriter, docDir);

            indexWriter.close();

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
                    .addTokenFilter(StopFilterFactory.class, "words", Indexer.stopwordsFile)
                    // remove diacritics before stemming
                    //e.g. if this is done afterwards, functioneaza -> functioneaz, funcţionează -> function
                    .addTokenFilter(ASCIIFoldingFilterFactory.class)
                    //   sometimes, the stemmer has to be applied again to get to the root of the word
                    // e.g. mamei -> mame -> mam
                    // e.g. mama -> mam
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Romanian")
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Romanian")
                    .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Romanian")
//                    .addTokenFilter(ASCIIFoldingFilterFactory.class)
                    .build();
        }
        catch (IOException ex){
            System.out.println("WARNING : IOException at building custom analyzer.");
            System.out.println("--- defaulting to RomanianAnalyzer instead. Diacritics will not be treated");
            analyzer = new RomanianAnalyzer();
        }

        return analyzer;
    }

    static void buildStopwordsFile() throws IOException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Indexer.stopwordsFile)))
        {
            String stopwordsString = RomanianAnalyzer.getDefaultStopSet().toString();
            String[] stopwords = stopwordsString.substring(1, stopwordsString.length() - 1).split(", ");
            for(String word : stopwords){
                writer.write(word + "\n");
                String normalized = Normalizer.normalize(word, Normalizer.Form.NFKD);
                String withoutDiacritics = normalized.replaceAll("[^\\p{ASCII}]", "");
                // if the string had diacritics in the first place, write the version without diacritics
                if(withoutDiacritics.equals(word) == false){
                    writer.write(withoutDiacritics + "\n");
                }
            }
        }
        catch (IOException ex){
            throw new IOException(ex.getMessage());
        }
    }



    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                        System.out.println("WARNING : IOException at reading file " + file + ". Skipping it.");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    private static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
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

            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                // New index
                System.out.println("--- creating index " + file);
                writer.addDocument(doc);
            } else {
                // update the documents matching the same path
                System.out.println("--- creating or updating index " + file);
                writer.updateDocument(new Term("path", file.toString()), doc);
            }
        }

    }

}
