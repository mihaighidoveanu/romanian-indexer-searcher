import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.IOException;
import java.util.*;


public class Parser {

    public static void main(String[] args) throws IOException {
        Analyzer analyzer = Indexer.getAnalyzer();
        String parseString = "funcţionează";
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
