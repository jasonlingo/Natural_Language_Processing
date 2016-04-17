import java.util.*;
import java.io.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    HashMap<String, List<String>> tagDict;
    HashMap<String, Integer> countBigram;
    HashMap<String, Integer> countUnigram;
    HashMap<String, Double> arcProbs;
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers; // ??



    public ViterbiTagger() {
        this.tagDict = new HashMap<String, List<String>>();
        this.countBigram = new HashMap<String, Integer>();
        this.countUnigram = new HashMap<String, Integer>();
        this.arcProbs = new HashMap<String, Double>();
        this.mus = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();

    }

    public void readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            String line = br.readLine();
            String[] prevElements = null;

            while (line != null) {
                if (line.length() > 0) {
                    String[] elements = line.split("/");
                    String word = elements[0];
                    String tag  = elements[1];

                    if (tagDict.containsKey(word)) {
                        List<String> temp = tagDict.get(word);
                        temp.add(tag);
                        tagDict.replace(word, temp);
                    }
                    else {
                        List<String> temp = new ArrayList<String>();
                        temp.add(tag);
                        tagDict.put(word, temp);
                    }


                    // Initialize Bigrams: tag to tag
                    if (prevElements != null) {
                        String tagTag = prevElements[1] + "_" + elements[1];
                        if (countBigram.containsKey(tagTag)) {
                            countBigram.replace(tagTag, countBigram.get(tagTag) + 1);
                        }
                        else {
                            countBigram.put(tagTag, 1);
                        }
                    }
                    prevElements = elements;

                    // Initialize Bigrams: tag to word
                    String wordTag = elements[0] + "_" + elements[1];
                    if (countBigram.containsKey(wordTag)) {
                        countBigram.replace(wordTag, countBigram.get(wordTag) + 1);
                    }
                    else{
                        countBigram.put(wordTag, 1);
                    }

                    // Initialize Unigrams
                    for (String e : elements) {
                        if (countUnigram.containsKey(e)) {
                            countUnigram.replace(e, countUnigram.get(e) + 1);
                        }
                        else {
                            countUnigram.put(e, 1);
                        }
                    }
                }
                line = br.readLine();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }

    public void tag(String fileName) {

    }

    // Main method for testing IO
    public static void main(String[] args) throws IOException {
        ViterbiTagger vt = new ViterbiTagger();
        vt.readFile("data/ictrain");
        for (Map.Entry<String, Integer> entry : vt.countUnigram.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }
}
