import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jinyi on 4/16/16.
 */
public class VTag {
    List<String> words  = new ArrayList<String>();
    List<String> tags   = new ArrayList<String>();



    public void readTestFile(String fileName) throws  IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            String line = br.readLine();

            while (line != null) {
                if (line.length() > 0) {
                    String[] entries = line.split("/");
                    words.add(entries[0]);
                    tags.add(entries[1]);
                }

                line = br.readLine();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }

    public static void main(String[] args) throws IOException {
        String train = args[0];
        String test  = args[1];

        ViterbiTagger tagger = new ViterbiTagger();
        tagger.readFile(train);

        VTag vt = new VTag();
        vt.readTestFile(test);

        List<String> vTags = tagger.viterbiTag(vt.words);
        tagger.computeAccuracy(vt.words, vTags, vt.tags, true);

        List<String> posTags = tagger.posTag(vt.words);
        tagger.computeAccuracy(vt.words, posTags, vt.tags, false);

    }
}
