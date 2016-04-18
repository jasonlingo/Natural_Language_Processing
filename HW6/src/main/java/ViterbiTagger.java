import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    private static final boolean DEBUG = false;
    Map<String, HashSet<String>> tagDict;
    Map<String, Double> countItems;
    Map<String, Double> arcProbs;
    Map<String, Double> mus;
    Map<String, Double> probDP;
    Map<String, String> backPointers;
    HashSet<String> allTags;
    final String TAGTAG_SEP   = "[TT]";
    final String TAG_WORD_SEP = "/";
    final String TIME_SEP     = "__";
    final String WORD_SEP     = "[w]";
    final String TAG_SEP      = "[T]";
    final String OOV          = "OOV";
    final String BND          = "###"; //boundary marker
    final double LAMBDA       = 1.0;   // setting LAMBDA = 0 means no add one smoothing


    public ViterbiTagger() {
        this.tagDict      = new HashMap<String, HashSet<String>>();
        this.countItems   = new HashMap<String, Double>();
        this.arcProbs     = new HashMap<String, Double>();
        this.mus          = new HashMap<String, Double>();
        this.probDP       = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();
        this.allTags      = new HashSet<String>();
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
                        String tagTag = prevElements[1] + TAGTAG_SEP + elements[1];
                        if (countItems.containsKey(tagTag)) {
                            countItems.replace(tagTag, countItems.get(tagTag) + 1.0);
                        }
                        else {
                            countItems.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize tag to word
                    String wordTag = elements[0] + TAG_WORD_SEP + elements[1];
                    if (countItems.containsKey(wordTag)) {
                        countItems.replace(wordTag, countItems.get(wordTag) + 1.0);
                    }
                    else{
                        countItems.put(wordTag, 1.0);
                    }

                    // Initialize Unigram
                    // Here we will count ### twice for each ###/###, need to correct count later
                    String wordkey = word + WORD_SEP;
                    if (countItems.containsKey(wordkey)) {
                        countItems.replace(wordkey, countItems.get(wordkey) + 1.0);
                    } else {
                        countItems.put(wordkey, 1.0);
                    }

                    String tagkey = tag + TAG_SEP;
                    if (countItems.containsKey(tagkey)) {
                        countItems.replace(tagkey, countItems.get(tagkey) + 1.0);
                    } else {
                        countItems.put(tagkey, 1.0);
                    }

                }
                line = br.readLine();
            }

            // minus one for last ###/###
            double bndWordCnt = countItems.get(BND + WORD_SEP) - 1;
            countItems.replace(BND + WORD_SEP, bndWordCnt);
            double bndTagCnt = countItems.get(BND + TAG_SEP) - 1;
            countItems.replace(BND + TAG_SEP, bndTagCnt);

            // reduce the number of ###/### by 1 because we need to ignore the first or last ###/### in unigram counting
            // but need them in bigram counting.
            countItems.replace(BND + TAG_WORD_SEP + BND, countItems.get(BND + TAG_WORD_SEP + BND) - 1);

            // we don't try ### for novel words
            allTags.remove(BND);

            // for "add one smoothing", add an OOV
            tagDict.put(OOV, allTags);
            
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }
    }


    public void tag(List<String> words) {
        Map<String, Double> alpha = forward(words);
        backward(words, alpha);
    }

    private Map<String, Double> forward(List<String> words) {
        Map<String, Double> alpha = new HashMap<String, Double>();
        List<String> tags = new ArrayList<String>();

        tags.add(BND);
        alpha.put(BND + TIME_SEP + "0", Math.log(1.0));

        HashSet<String> candidateTag;
        HashSet<String> prevCandidateTag = tagDict.get(words.get(0));
        String curTime;
        String preTime = String.valueOf(0);
        double p_tt;
        double p_tw;
        double preAlpha;
        double alpha_ti;

        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            curTime = String.valueOf(i);

            // check novel word
            boolean novelWord = false;
            if (!tagDict.containsKey(word)) {
                word = OOV;
                novelWord = true;
            }

            candidateTag = tagDict.get(word);

            // find the tag with max probability
            for (String tag : candidateTag) {
                alpha_ti = 0.0;

                for (String prevTag : prevCandidateTag) {
                    // tag to tag probability
                    String p_tt_key = prevTag + TAGTAG_SEP + tag;
                    if (probDP.containsKey(p_tt_key)) {
                        p_tt = probDP.get(p_tt_key);
                    } else {
                        if (countItems.containsKey(p_tt_key)) {
                            p_tt = (countItems.get(p_tt_key) + LAMBDA) / (countItems.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        } else {
                            p_tt = LAMBDA / (countItems.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = word + TAG_WORD_SEP + tag;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        if (novelWord) {
                            p_tw = LAMBDA / (countItems.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        } else if (word.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            p_tw = (countItems.get(p_tw_key) + LAMBDA) / (countItems.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        }
                        probDP.put(p_tw_key, p_tw);
                    }

                    preAlpha = alpha.get(prevTag + TIME_SEP + preTime) + Math.log(p_tt) + Math.log(p_tw);
                    alpha_ti = logadd(alpha_ti, preAlpha);
                }
                alpha.put(tag + TIME_SEP + curTime, alpha_ti);
            }

            preTime = curTime;
            prevCandidateTag = candidateTag;

        }

        return alpha;

    }

    private List<String> backward(List<String> words, Map<String, Double> alpha) {
        long start = System.nanoTime();

        double s = alpha.get(BND + TIME_SEP + String.valueOf(words.size() - 1));

        List<String> tags = new ArrayList<String>();
        tags.add(BND);
        mus.put(BND + TIME_SEP + "0", Math.log(1.0));

        HashSet<String> candidateTag;
        HashSet<String> prevCandidateTag = tagDict.get(words.get(0));

        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            // check novel word
            boolean novelWord = false;
            if (!tagDict.containsKey(word)) {
                word = OOV;
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
                    String p_tt_key = prevTag + TAGTAG_SEP + tag;
                    if (probDP.containsKey(p_tt_key)) {
                        p_tt = probDP.get(p_tt_key);
                    } else {
                        if (countItems.containsKey(p_tt_key)) {
                            p_tt = (countItems.get(p_tt_key) + LAMBDA) / (countItems.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        } else {
                            p_tt = LAMBDA / (countItems.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    double p_tw;
                    String p_tw_key = word + TAG_WORD_SEP + tag;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        if (novelWord) {
                            p_tw = LAMBDA / (countItems.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        } else if (word.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            p_tw = (countItems.get(p_tw_key) + LAMBDA) / (countItems.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        }
                        probDP.put(p_tw_key, p_tw);
                    }

                    double currentMu = mus.get(prevTag + TIME_SEP + preI) + Math.log(p_tt) + Math.log(p_tw);

                    // update max probability and back pointers
                    if (!mus.containsKey(tag + TIME_SEP + currI) || mus.get(tag + TIME_SEP + currI) < currentMu) {
                        if(DEBUG) {
                            System.out.printf("update %s = %f\n", tag + TIME_SEP + currI, currentMu);
                        }
                        mus.put(tag + TIME_SEP + currI, currentMu);
                        backPointers.put(tag + TIME_SEP + currI, prevTag);
                    }
                }
            }

            prevCandidateTag = candidateTag;

            if (DEBUG) {
                System.out.printf("T = %d-----\n", i);
                if (mus.containsKey("C" + TIME_SEP + currI)) {
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + TIME_SEP + currI)));
                }
                if (mus.containsKey("H" + TIME_SEP + currI)) {
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + TIME_SEP + currI)));
                }
                if (mus.containsKey("C" + TIME_SEP + currI))
                    System.out.printf("%s -> %.13e\n", "C", Math.exp(mus.get("C" + TIME_SEP + currI)));
                if (mus.containsKey(("H" + TIME_SEP + currI)))
                    System.out.printf("%s -> %.13e\n", "H", Math.exp(mus.get("H" + TIME_SEP + currI)));
                if (mus.containsKey((BND + TIME_SEP + currI)))
                    System.out.printf("%s -> %.13e\n", BND, Math.exp(mus.get(BND + TIME_SEP + currI)));
            }

        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + TIME_SEP + i));
        }

        if(DEBUG) {
            System.out.println(tags.toString());
        }

        System.out.println((System.nanoTime() - start) / 1000);
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

            if (resTag.equals(BND)) {
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
        String key = result.get(result.size() - 1) + TIME_SEP + String.valueOf(result.size() - 1);
        double prob = mus.get(key) / Math.log(2);
        double perplexity = Math.pow(2, -prob / (words.size() - 1) );

        //output format
        System.out.printf("Tagging accuracy (Vierbi decoding): %.2f%% (known: %.2f%% novel: %.2f%%)\n", totAccu, knownAccu, novelAccu);
        System.out.printf("Perplexity per Viterbi-tagged test word: %.3f\n", perplexity);

    }

    /*
     perform log addition:
     logadd(x, y) = x + log(1 + exp(y - x))  if y <= x;
                  = y + log(1 + exp(x - y))  otherwise
     */
    private static double logadd(double x, double y) { //TODO: handle the case x=0 or y=0
        if(y <= x) {
            return x + Math.log(1 + Math.exp(y - x));
        } else {
            return y + Math.log(1 + Math.exp(x - y));
        }
    }

}
