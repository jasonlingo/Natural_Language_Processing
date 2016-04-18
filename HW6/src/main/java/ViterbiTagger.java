import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    private static final boolean DEBUG = false;
    HashMap<String, HashSet<String>> tagDict;
    HashMap<String, Double> countItems;
    HashMap<String, Double> arcProbs;
    HashMap<String, Double> mus;
    HashMap<String, String> backPointers;
    HashSet<String> allTags;
    String tagtagSep  = "[TT]";
    String tagWordSep = "/";
    String timeSep    = "__";
    String wordSep    = "[w]";
    String tagSep     = "[T]";


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
                        String tagTag = prevElements[1] + tagtagSep + elements[1];
                        if (countItems.containsKey(tagTag)) {
                            countItems.replace(tagTag, countItems.get(tagTag) + 1.0);
                        }
                        else {
                            countItems.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize tag to word
                    String wordTag = elements[0] + tagWordSep + elements[1];
                    if (countItems.containsKey(wordTag)) {
                        countItems.replace(wordTag, countItems.get(wordTag) + 1.0);
                    }
                    else{
                        countItems.put(wordTag, 1.0);
                    }

                    // Initialize Unigram
                    // Here we will count ### twice for each ###/###, need to correct count later
                    String wordkey = word + wordSep;
                    if (countItems.containsKey(wordkey)) {
                        countItems.replace(wordkey, countItems.get(wordkey) + 1.0);
                    } else {
                        countItems.put(wordkey, 1.0);
                    }

                    String tagkey = tag + tagSep;
                    if (countItems.containsKey(tagkey)) {
                        countItems.replace(tagkey, countItems.get(tagkey) + 1.0);
                    } else {
                        countItems.put(tagkey, 1.0);
                    }

                }
                line = br.readLine();
            }

            // minus one for last ###/###
            double bndWordCnt = countItems.get("###" + wordSep) - 1;
            countItems.replace("###" + wordSep, bndWordCnt);
            double bndTagCnt = countItems.get("###" + tagSep) - 1;
            countItems.replace("###" + tagSep, bndTagCnt);

            // reduce the number of ###/### by 1 because we need to ignore the first or last ###/### in unigram counting
            // but need them in bigram counting.
            countItems.replace("###" + tagWordSep + "###", countItems.get("###" + tagWordSep + "###") - 1);

            // we don't try ### for novel words
            allTags.remove("###");

            // for "add one smoothing", add an OOV
            tagDict.put("OOV", allTags);
            
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }

    public List<String> tag(List<String> words) {
//        Set<String> probs = new HashSet<String>();
//        String line = "%s prob: %s | %f"; //mode, key, prob

        List<String> tags = new ArrayList<String>();
        tags.add("###");
        mus.put("###" + timeSep + "0", Math.log(1.0));

        HashSet<String> candidateTag;
        HashSet<String> prevCandidateTag = tagDict.get(words.get(0));

        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            // check novel word
            boolean novelWord = false;
            if (!tagDict.containsKey(word)) {
                word = "OOV";
                novelWord = true;
            }

            candidateTag = tagDict.get(word);

            String currI = String.valueOf(i);
            String preI  = String.valueOf(i - 1);

            // find the tag with max probability
            for (String tag : candidateTag) {
                for (String prevTag : prevCandidateTag) {

                    // tag to tag probability
                    double p_tt;
                    if(countItems.containsKey(prevTag + tagtagSep + tag)){
                        p_tt = (countItems.get(prevTag + tagtagSep + tag) + 1.0) / (countItems.get(prevTag + tagSep) + allTags.size() + 1.0);
                    } else {
                        p_tt = 1.0 / (countItems.get(prevTag + tagSep) + allTags.size() + 1.0);
                    }

                    // tag to word probability
                    double p_tw;
                    if (novelWord) {
                        p_tw = 1.0 / (countItems.get(tag + tagSep) + tagDict.size());
                    } else if (word.equals("###") && tag.equals("###")) {
                        p_tw = 1.0;
                    } else {
                        p_tw = (countItems.get(word + tagWordSep + tag) + 1.0) / (countItems.get(tag + tagSep) + tagDict.size());
                    }

                    double currentMu = mus.get(prevTag + timeSep + preI) + Math.log(p_tt) + Math.log(p_tw);

                    // update max probability and back pointers
                    if (!mus.containsKey(tag + timeSep + currI) || mus.get(tag + timeSep + currI) < currentMu) {
                        if(DEBUG) {
                            System.out.printf("update %s = %f\n", tag + timeSep + currI, currentMu);
                        }
                        mus.put(tag + timeSep + currI, currentMu);
                        backPointers.put(tag + timeSep + currI, prevTag);
                    }
                }
            }

            prevCandidateTag = candidateTag;

            if (DEBUG) {
                System.out.printf("T = %d-----\n", i);
                if (mus.containsKey("C" + timeSep + currI)) {
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + timeSep + currI)));
                }
                if (mus.containsKey("H" + timeSep + currI)) {
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + timeSep + currI)));
                }
                if (mus.containsKey("C" + timeSep + currI))
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + timeSep + currI)));
                if (mus.containsKey(("H" + timeSep + currI)))
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + timeSep + currI)));
                if (mus.containsKey(("###" + timeSep + currI)))
                    System.out.printf("%s -> %.13e\n", "###", Math.exp(mus.get("###" + timeSep + currI)));
            }

        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + timeSep + i));
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
    public void computeAccuracy(List<String> words, List<String> result, List<String> ans) {
        int totCnt          = 0;
        int totCorrect      = 0;
        int totKnownCnt     = 0;
        int totKnowCorrect  = 0;
        int totNovelCnt     = 0;
        int totNovelCorrect = 0;

        for(int i = 0; i < result.size(); i++) {
            String word   = words.get(i);
            String resTag = result.get(i);
            String ansTag = ans.get(i);

            if (resTag.equals("###")) {
                // we don't count ###
                continue;
            }

            boolean correct = false;
            // count overall correct data
            totCnt++;
            if (resTag.equals(ansTag)) {
                totCorrect++;
                correct = true;
            }

            if (tagDict.containsKey(word)) {
                // count known word correct data
                totKnownCnt++;
                if (correct) {
                    totKnowCorrect++;
                }
            } else {
                // count novel word correct data
                totNovelCnt++;
                if (correct) {
                    totNovelCorrect++;
                }

            }
        }

        // calcuate accuracy
        double totAccu   = (double)totCorrect / (double)totCnt * 100;
        double knownAccu = (double)totKnowCorrect / (double)totKnownCnt * 100;
        double novelAccu = totNovelCnt > 0? (double)totNovelCorrect / (double)totNovelCnt * 100 : 0;

        // Calculate perplexity
        String key = result.get(result.size() - 1) + timeSep + String.valueOf(result.size() - 1);
        double prob = mus.get(key) / Math.log(2);
        double perplexity = Math.pow(2, -prob / (words.size() - 1) );

        //output format
        System.out.printf("Tagging accuracy (Vierbi decoding): %.2f%% (known: %.2f%% novel: %.2f%%)\n", totAccu, knownAccu, novelAccu);
        System.out.printf("Perplexity per Viterbi-tagged test word: %.3f\n", perplexity);

    }

}
