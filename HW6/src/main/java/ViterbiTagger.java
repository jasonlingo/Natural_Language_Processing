import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    HashMap<String, HashSet<String>> tagDict;
    HashMap<String, Double> countItems;
    HashMap<String, Double> arcProbs;
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers; // ??
    int wordIdx;



    public ViterbiTagger() {
        this.tagDict = new HashMap<String, HashSet<String>>();
        this.countItems = new HashMap<String, Double>();
        this.arcProbs = new HashMap<String, Double>();
        this.mus = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();
        this.wordIdx = 0;

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
                        tagDict.get(word).add(tag);
                    }
                    else {
                        HashSet<String> temp = new HashSet<String>();
                        temp.add(tag);
                        tagDict.put(word, temp);
                    }


                    // Initialize Bigrams: tag to tag
                    if (prevElements != null) {
                        String tagTag = prevElements[1] + "_" + elements[1];
                        if (countItems.containsKey(tagTag)) {
                            countItems.replace(tagTag, countItems.get(tagTag) + 1);
                        }
                        else {
                            countItems.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize Bigrams: tag to word
                    String wordTag = elements[0] + "_" + elements[1];
                    if (countItems.containsKey(wordTag)) {
                        countItems.replace(wordTag, countItems.get(wordTag) + 1);
                    }
                    else{
                        countItems.put(wordTag, 1.0);
                    }

                    // Initialize Unigrams
                    // Here we will count ### twice for each ###/###
                    for (String e : elements) {
                        if (countItems.containsKey(e)) {
                            countItems.replace(e, countItems.get(e) + 1);
                        }
                        else {
                            countItems.put(e, 1.0);
                        }
                    }
                }
                line = br.readLine();
            }
            // divide the count of ### by 2 and minus the last one
            double startTag = countItems.get("###");
            countItems.replace("###", startTag / 2 - 1);

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }



    public List<String> tag(List<String> words) {
        List<String> tags = new ArrayList<String>();
        tags.add("###");
        mus.put("###_0", 1.0);

        for (int i = 1; i < words.size(); i++) {
            HashSet<String> candidateTag = tagDict.get(words.get(i));
            HashSet<String> prevCandidateTag = tagDict.get(words.get(i - 1));
            String currI = String.valueOf(i);
            String preI  = String.valueOf(i - 1);

            for (String tag : candidateTag) {
                for (String prevTag : prevCandidateTag) {
                    // tag to tag bigram probability
                    double p_tt = (countItems.get(prevTag + "_" + tag)) / countItems.get(prevTag);
                    // tag to word probability
                    double p_tw = (countItems.get(words.get(i) + "_" + tag)) / countItems.get(tag);

                    double currentMu = mus.get(prevTag + "_" + preI) * p_tt * p_tw;

                    if (!mus.containsKey(tag + "_" + currI) || mus.get(tag + "_" + currI) < currentMu) {
                        mus.put(tag + "_" + currI, currentMu);
                        backPointers.put(tag + "_" + currI, prevTag);
                    }
                }
            }
            // for testing
            System.out.printf("T = %d-----\n", i);
            System.out.printf("%s -> %.13e\n", "C", mus.get("C" + "_" + currI));
            System.out.printf("%s -> %.13e\n", "H", mus.get("H" + "_" + currI));

        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + "_" + i));
        }
        return tags;

    }

    // Main method for testing IO
    public static void main(String[] args) throws IOException {
        ViterbiTagger vt = new ViterbiTagger();
        vt.readFile("data/ictrain");
        for (Map.Entry<String, Double> entry : vt.countItems.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

    }
}
