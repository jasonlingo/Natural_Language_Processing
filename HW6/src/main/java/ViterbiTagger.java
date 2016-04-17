import java.util.*;
import java.io.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    HashMap<String, HashSet<String>> tagDict;
    HashMap<String, Integer> countItems;
    HashMap<String, Double> arcProbs; // Unused yet
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers;


    public ViterbiTagger() {
        this.tagDict = new HashMap<String, HashSet<String>>();
        this.countItems = new HashMap<String, Integer>();
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

                    // Initialize tagDict
                    if (tagDict.containsKey(word)) {
                        HashSet<String> temp = tagDict.get(word);
                        if (!temp.contains(tag)) { // could be time-consuming here
                            temp.add(tag);
                            tagDict.replace(word, temp);
                        }
                    }
                    else {
                        HashSet<String> temp = new HashSet<String>();
                        temp.add(tag);
                        tagDict.put(word, temp);
                    }


                    // Initialize Bigrams: tag to tag
                    if (prevElements != null) {
                        String tagTag = prevElements[1] + "/" + tag;
                        if (countItems.containsKey(tagTag)) {
                            countItems.replace(tagTag, countItems.get(tagTag) + 1);
                        }
                        else {
                            countItems.put(tagTag, 1);
                        }
                    }
                    prevElements = elements;

                    // Initialize Bigrams: tag to word
                    String wordTag = elements[0] + "/" + elements[1];
                    if (countItems.containsKey(wordTag)) {
                        countItems.replace(wordTag, countItems.get(wordTag) + 1);
                    }
                    else{
                        countItems.put(wordTag, 1);
                    }

                    // Initialize Unigrams
                    for (String e : elements) {
                        if (countItems.containsKey(e)) {
                            countItems.replace(e, countItems.get(e) + 1);
                        }
                        else {
                            countItems.put(e, 1);
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

        // Reduce the count of "###" by 1 (the last one) and half the count
        // Looks pretty much like hard code here
        countItems.replace("###", (countItems.get("###") - 1 )/ 2);
    }



    public List<String> tag(List<String> words) {
//        List<String> test = readTestFile("data/ictest");
        List<String> tags = new ArrayList<String>();
        tags.add("###");
        mus.put("###/0", 1.0);

        for (int i = 1; i < words.size(); i++) {
            Set<String> candidateTag = tagDict.get(words.get(i));
            Set<String> prevCandidateTag = tagDict.get(words.get(i - 1));

            for (String tag : candidateTag) {
                for (String prevTag : prevCandidateTag) {
                    double p_tt = ((double)countItems.get(prevTag + "/" + tag)) / (double) countItems.get(prevTag);
                    double p_tw = ((double)countItems.get(words.get(i) + "/" + tag)) / (double) countItems.get(tag);
                    double currentMu = mus.get(prevTag + "/" + (i - 1)) * p_tt * p_tw;

                    if (!mus.containsKey(tag + "/" + i) || mus.get(tag + "/" + i) < currentMu) {
                        mus.put(tag + "/" + i, currentMu);
                        backPointers.put(tag + "/" + i, prevTag);
                    }

                }
            }
        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + "/" + i));
        }
        return tags;

    }

    // Main method for testing IO
    public static void main(String[] args) throws IOException {
        ViterbiTagger vt = new ViterbiTagger();
        vt.readFile("data/ictrain");
        for (Map.Entry<String, Integer> entry : vt.countItems.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }


    }
}