import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Parser {




    public static void main(String[] args) throws IOException {


        Analyzer analyzer = Indexer.getAnalyzer();
        String parseString = "şi ce a mai zis";
        List<String> result = new ArrayList<String>();
        TokenStream stream  = analyzer.tokenStream(null, parseString);
        CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while(stream.incrementToken()) {
            result.add(attr.toString());
        }

        for(String token : result){
            System.out.print(token + " ");
        }
        System.out.println();

    }
}
