import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    private static final boolean DEBUG = true;
    HashMap<String, HashSet<String>> tagDict;
    HashMap<String, Double> countItems;
    HashMap<String, Double> arcProbs;
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers;
    HashSet<String> allTags;

    public ViterbiTagger() {
        this.tagDict = new HashMap<String, HashSet<String>>();
        this.countItems = new HashMap<String, Double>();
        this.arcProbs = new HashMap<String, Double>();
        this.mus = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();
        this.allTags = new HashSet<String>();
    }

    /*
     Read file and parse word/tag pairs and store needed data.
     */
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

                    allTags.add(tag);

                    // build word-tag HashMap
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
                            countItems.replace(tagTag, countItems.get(tagTag) + 1.0);
                        }
                        else {
                            countItems.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize tag to word
                    String wordTag = elements[0] + "/" + elements[1];
                    if (countItems.containsKey(wordTag)) {
                        countItems.replace(wordTag, countItems.get(wordTag) + 1.0);
                    }
                    else{
                        countItems.put(wordTag, 1.0);
                    }

                    // Initialize Unigram
                    // Here we will count ### twice for each ###/###, need to correct count later
                    for (String e : elements) {
                        if (countItems.containsKey(e)) {
                            countItems.replace(e, countItems.get(e) + 1.0);
                        }
                        else {
                            countItems.put(e, 1.0);
                        }
                    }
                }
                line = br.readLine();
            }
            // divide the count of ### by 2 and minus the last one
            double bndTagCnt = countItems.get("###");
            countItems.replace("###", bndTagCnt / 2 - 1);
            countItems.replace("###/###", countItems.get("###/###") - 1);

            // we don't try ### for novel words
            allTags.remove("###");


        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }

    public List<String> tag(List<String> words) {
        List<String> tags = new ArrayList<String>();
        tags.add("###");
        mus.put("###/0", Math.log(1.0));

        HashSet<String> candidateTag;
        HashSet<String> prevCandidateTag = tagDict.get(words.get(0));

        for (int i = 1; i < words.size(); i++) {
            candidateTag = tagDict.get(words.get(i));

            String currI = String.valueOf(i);
            String preI  = String.valueOf(i - 1);

            // deal with novel word, try all tags
            boolean novelWord = false;
            if (candidateTag == null) {
                candidateTag = allTags;
                novelWord = true;
            }

            // find the tag with max probability
            for (String tag : candidateTag) {
                for (String prevTag : prevCandidateTag) {
                    // tag to tag bigram probability

                    // tag to tag probability
                    double tagtag;
                    if(countItems.containsKey(prevTag + "/" + tag)){
                        tagtag = countItems.get(prevTag + "/" + tag);
                    } else {
                        tagtag = 0.0;
                    }

                    double p_tt = tagtag / countItems.get(prevTag);

                    // tag to word probability
                    double p_tw;
                    if (novelWord) {
                        p_tw = 1.0;
                    } else {
                        String key = words.get(i) + "/" + tag;
                        p_tw = (countItems.get(words.get(i) + "/" + tag)) / countItems.get(tag);
                    }

                    double currentMu = mus.get(prevTag + "/" + preI) + Math.log(p_tt) + Math.log(p_tw);

                    if (!mus.containsKey(tag + "/" + currI) || mus.get(tag + "/" + currI) < currentMu) {
                        if(DEBUG) {
                            System.out.printf("update %s = %f\n", tag + "/" + currI, currentMu);
                        }
                        mus.put(tag + "/" + currI, currentMu);
                        backPointers.put(tag + "/" + currI, prevTag);
                    }
                }
            }

            prevCandidateTag = candidateTag;

            if (DEBUG) {
                System.out.printf("T = %d-----\n", i);
                if (mus.containsKey("C" + "/" + currI)) {
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + "/" + currI)));
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + "/" + currI)));
                }
                if (mus.containsKey("C" + "/" + currI))
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + "/" + currI)));
                if (mus.containsKey(("H" + "/" + currI)))
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + "/" + currI)));
                if (mus.containsKey(("###" + "/" + currI)))
                    System.out.printf("%s -> %.13e\n", "###", Math.exp(mus.get("###" + "/" + currI)));
            }

        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + "/" + i));
        }

        if(DEBUG) {
            System.out.println(tags.toString());
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
        String key = result.get(result.size() - 1) + "/" + String.valueOf(result.size() - 1);
        double prob = mus.get(key) / Math.log(2);

        double perplexity = Math.pow(2, -prob/);


        //output format
        System.out.printf("Tagging accuracy (Vierbi decoding): %.2f%% (known: %.2f%% novel: %.2f%%\n", totAccu, knownAccu, novelAccu);
        System.out.printf("Perplexity per Viterbi-tagged test word: %.3f\n", perplexity);

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
