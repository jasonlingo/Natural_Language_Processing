import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger3 {
    private static final boolean DEBUG = false;
    HashMap<String, HashSet<String>> tagDict;
    HashMap<String, Double> countItems;
    HashMap<String, Double> arcProbs;
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers;
    int wordIdx;


    public ViterbiTagger3() {
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
                        String tagTag = prevElements[1] + "/" + elements[1];
                        if (countItems.containsKey(tagTag)) {
                            countItems.replace(tagTag, countItems.get(tagTag) + 1);
                        }
                        else {
                            countItems.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize Bigrams: tag to word
                    String wordTag = elements[0] + "/" + elements[1];
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
        mus.put("###/0", 1.0);

        for (int i = 1; i < words.size(); i++) {
            HashSet<String> candidateTag = tagDict.get(words.get(i));
            HashSet<String> prevCandidateTag = tagDict.get(words.get(i - 1));
            String currI = String.valueOf(i);
            String preI  = String.valueOf(i - 1);

            for (String tag : candidateTag) {
                for (String prevTag : prevCandidateTag) {
                    // tag to tag bigram probability
                    double p_tt = (countItems.get(prevTag + "/" + tag)) / countItems.get(prevTag);
                    // tag to word probability
                    double p_tw = (countItems.get(words.get(i) + "/" + tag)) / countItems.get(tag);

                    double currentMu = mus.get(prevTag + "/" + preI) * p_tt * p_tw;

                    if (!mus.containsKey(tag + "/" + currI) || mus.get(tag + "/" + currI) < currentMu) {
                        mus.put(tag + "/" + currI, currentMu);
                        backPointers.put(tag + "/" + currI, prevTag);
                    }
                }
            }

            if (DEBUG) {
                System.out.printf("T = %d-----\n", i);
                System.out.printf("%s -> %.13e\n", "C", mus.get("C" + "/" + currI));
                System.out.printf("%s -> %.13e\n", "H", mus.get("H" + "/" + currI));
            }

        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + "/" + i));
        }
        return tags;

    }

    /*
     Compute the tagging accuracy:
        1. overall accuracy: considers all word tokens, other than the sentence boundary markers ###
        2. known-word accuracy: considers only tokens of words (other than ###) that also appeared in training data
        3. novel-word accuracy: consider only tokens of words that did not also appear in training data.

     Compute perplexity:
        perplexity = exp( - (log p(w1, t1, ..., wn, tn | w0, t0)) / n )

     */
    public void computeAccuracy(List<String> result, List<String> ans) {
        int totCnt          = 0;
        int totCorrect      = 0;
        int totKnownCnt     = 0;
        int totKnowCorrect  = 0;
        int totNovelCnt     = 0;
        int totNovelCorrect = 0;

        for(int i = 0; i < result.size(); i++) {
            String resTag = result.get(i);
            String ansTag = ans.get(i);
            if (resTag.equals("###")) {
                // we don't count ###
                continue;
            }

            totCnt++;
            if (resTag.equals(ansTag)) {
                totCorrect++;
            }
        }

        double totAccu = (double)totCorrect / (double)totCnt * 100;
        double knownAccu = 0.0;
        double novelAccu = 0.0;
        double perp = 0.0;

        //output format
        System.out.printf("Tagging accuracy (Vierbi decoding): %.2f%% (known: %.2f%% novel: %.2f%%\n", totAccu, knownAccu, novelAccu);
        System.out.printf("Perplexity per Viterbi-tagged test word: %.3f", perp);

    }

    // Main method for testing IO
//    public static void main(String[] args) throws IOException {
//        ViterbiTagger3 vt = new ViterbiTagger3();
//        vt.readFile("data/ictrain");
//        for (Map.Entry<String, Double> entry : vt.countItems.entrySet()) {
//            System.out.println(entry.getKey() + "/" + entry.getValue());
//        }
//
//
//
//    }
}
