import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jinyi on 4/16/16.
 */
public class DoTag {
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
        ViterbiTagger tagger = new ViterbiTagger();
        tagger.readFile("data/entrain");

        DoTag dt = new DoTag();
        dt.readTestFile("data/entest");

        List<String> vTags = tagger.viterbiTag(dt.words);
        tagger.computeAccuracy(dt.words, vTags, dt.tags, true);

        List<String> posTags = tagger.posTag(dt.words);
        tagger.computeAccuracy(dt.words, posTags, dt.tags, false);

    }
}
