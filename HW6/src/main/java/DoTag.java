import java.io.IOException;

/**
 * Created by Jinyi on 4/16/16.
 */
public class DoTag {

    public static void main(String[] args) throws IOException {
        ViterbiTagger tagger = new ViterbiTagger();
        tagger.readFile("data/ictrain");
        tagger.tag("data/ictest");
    }
}
