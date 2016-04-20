import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason on 4/19/16.
 */
public class VTagEM {
    List<String> words  = new ArrayList<String>();
    List<String> tags   = new ArrayList<String>();

    public void readTestFile(String fileName) throws  IOException {
        words.clear();
        tags.clear();

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
        String raw   = args[2];

        ViterbiTaggerEM tagger = new ViterbiTaggerEM();
        tagger.readFile(train, raw);

        VTagEM dt = new VTagEM();
        dt.readTestFile(test);

        tagger.emTag(dt.words, dt.tags, 100);

    }
}
