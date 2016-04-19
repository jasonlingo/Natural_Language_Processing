
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTaggerEM {
    private static final boolean DEBUG = false;
    protected Map<String, HashSet<String>> tagDict;
    protected Map<String, Double> currCount;
    protected Map<String, Double> orgCount;
    protected Map<String, Double> newCount;
    protected Map<String, Double> arcProbs;
    protected Map<String, Double> mus;
    protected Map<String, Double> probDP;
    protected Map<String, String> backPointers;
    protected HashSet<String> allTags;
    protected Map<String, Double> alpha;
    protected Map<String, Double> beta;
    protected HashSet<String> trainTypes;
    protected HashSet<String> rawTypes;
    protected HashSet<String> allTypes;
    protected List<String> rawWords;
    protected boolean forwardTag;
    protected final String TAGTAG_SEP   = "[TT]";
    protected final String WORD_TAG_SEP = "/";
    protected final String TIME_SEP     = "__";
    protected final String WORD_SEP     = "[w]";
    protected final String TAG_SEP      = "[T]";
    protected final String OOV          = "OOV";
    protected final String BND          = "###"; //boundary marker
    protected final String VITERBI      = "Viterbi";
    protected final String POSTERIOR    = "posterior";
    protected final double LAMBDA       = 0.0;   // setting LAMBDA = 0 means no add one smoothing


    public ViterbiTaggerEM() {
        this.tagDict      = new HashMap<String, HashSet<String>>();
        this.currCount    = new HashMap<String, Double>();
        this.orgCount     = new HashMap<String, Double>();
        this.newCount     = new HashMap<String, Double>();
        this.arcProbs     = new HashMap<String, Double>();
        this.mus          = new HashMap<String, Double>();
        this.probDP       = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();
        this.allTags      = new HashSet<String>();
        this.alpha        = new HashMap<String, Double>();
        this.beta         = new HashMap<String, Double>();
        this.trainTypes   = new HashSet<String>();
        this.rawTypes     = new HashSet<String>();
        this.allTypes     = new HashSet<String>();
        this.rawWords     = new ArrayList<String>();
        this.forwardTag   = false;
    }


    /*
     When starting a new training, clean relevant instance variables.
     */
    protected void init() {  // TODO
        this.tagDict.clear();
        this.currCount.clear();
        this.orgCount.clear();
        this.newCount.clear();
        this.arcProbs.clear();
        this.mus.clear();
        this.probDP.clear();
        this.backPointers.clear();
        this.allTags.clear();
        this.alpha.clear();
        this.beta.clear();
        this.trainTypes.clear();
        this.rawTypes.clear();
        this.allTypes.clear();
        this.rawWords.clear();
        this.forwardTag = false;
    }

    /*
     Read file and parse word/tag pairs and store needed data.
     */
    public void readFile(String train, String raw) throws IOException {
        init();

        BufferedReader br    = new BufferedReader(new FileReader(train));
        BufferedReader rawBr = new BufferedReader(new FileReader(raw));

        try {
            String line = br.readLine();
            String[] prevElements = null;

            while (line != null) {
                if (line.length() > 0) {
                    String[] elements = line.split("/");
                    String word = elements[0];
                    String tag  = elements[1];

                    allTags.add(tag);
                    trainTypes.add(word);

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
                        if (currCount.containsKey(tagTag)) {
                            currCount.replace(tagTag, currCount.get(tagTag) + 1.0);
                        }
                        else {
                            currCount.put(tagTag, 1.0);
                        }
                    }
                    prevElements = elements;

                    // Initialize word to tag count
                    String wordTag = elements[0] + WORD_TAG_SEP + elements[1];
                    if (currCount.containsKey(wordTag)) {
                        currCount.replace(wordTag, currCount.get(wordTag) + 1.0);
                    }
                    else{
                        currCount.put(wordTag, 1.0);
                    }

                    // Initialize Unigram
                    // Here we will count ### twice for each ###/###, need to correct count later
                    String wordkey = word + WORD_SEP;
                    if (currCount.containsKey(wordkey)) {
                        currCount.replace(wordkey, currCount.get(wordkey) + 1.0);
                    } else {
                        currCount.put(wordkey, 1.0);
                    }

                    String tagkey = tag + TAG_SEP;
                    if (currCount.containsKey(tagkey)) {
                        currCount.replace(tagkey, currCount.get(tagkey) + 1.0);
                    } else {
                        currCount.put(tagkey, 1.0);
                    }

                }
                line = br.readLine();
            }

            // process raw data
            line = rawBr.readLine();
            while(line != null) {
                String word = line.trim(); // remove trailing space if it exists
                rawTypes.add(word);
                rawWords.add(word);
                line = rawBr.readLine();
            }

            allTypes.add(OOV);

            // minus one for last ###/###
            double bndWordCnt = currCount.get(BND + WORD_SEP) - 1;
            currCount.replace(BND + WORD_SEP, bndWordCnt);
            double bndTagCnt = currCount.get(BND + TAG_SEP) - 1;
            currCount.replace(BND + TAG_SEP, bndTagCnt);

            // reduce the number of ###/### by 1 because we need to ignore the first or last ###/### in unigram counting
            // but need them in bigram counting.
            currCount.replace(BND + WORD_TAG_SEP + BND,
                    currCount.get(BND + WORD_TAG_SEP + BND) - 1);

            // we don't try ### for novel words
            allTags.remove(BND);

            // for "add one smoothing", add an OOV
            tagDict.put(OOV, allTags);

//            orgCount = new HashMap<String, Double>(currCount);  //FIXME
            orgCount = new HashMap<String, Double>();


        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            br.close();
        }


    }


    /*
     This will tag funciton will first perform Viterbi decoding, and then perform
     forward-backward decoding.
     */
    public List<String> emTag(List<String> words, List<String> testTags, int iters) {
        alpha.clear();
        beta.clear();
        mus.clear();

        List<String> tags = new ArrayList<String>();

        for(int epoc = 0; epoc < iters; epoc++) {
            // Viterbi decoding
            newCount = new HashMap<String, Double>(orgCount);

            tags = forward(words);
//            computeAccuracy(words, tags, testTags, true);


            tags = forward(words);
            Map<String, Double> probTW = backward(rawWords);
//            System.out.println(tags.toString());

//            updateNewCount(probTW);

            System.out.printf("Iteration %d: Perplexity per untagged raw word: %.2f%%\n", epoc, 0.0);

            currCount = new HashMap<String, Double>(newCount);
        }


        return tags;
    }

    protected void updateNewCount(Map<String, Double> probTW) {
        for (String k : probTW.keySet()) {
            System.out.printf("%s = %f\n", k, Math.exp(probTW.get(k)));
        }
//        for(int i = 1; i < words.size(); i++) {
//            String word    = words.get(i);
//            String tag     = tags.get(i);
//            String preWord = words.get(i - 1);
//            String preTag  = tags.get(i - 1);
//
//            // update (preTag, tag) count
//            String ttKey = preTag + TAGTAG_SEP + tag;
//
//
//            // update (word, tag) count
//            String wtKey = word + WORD_TAG_SEP + tag;
//
//
//            // update unigram count
//            String wKey = word + WORD_SEP;
//
//
//            String tKey = tag + TAG_SEP;
//
//        }
        // minus one for last ###/###
//        double bndWordCnt = newCount.get(BND + WORD_SEP) - 1;
//        newCount.replace(BND + WORD_SEP, bndWordCnt);
//        double bndTagCnt = newCount.get(BND + TAG_SEP) - 1;
//        newCount.replace(BND + TAG_SEP, bndTagCnt);
    }


    protected List<String> forward(List<String> words) {
        probDP.clear();
        alpha.clear();
        mus.clear();
        backPointers.clear();

        List<String> tags = new ArrayList<String>();

        tags.add(BND);
        alpha.put(BND + TIME_SEP + "0", Math.log(1.0));
        mus.put(BND + TIME_SEP + "0", Math.log(1.0));

        for (int i = 1; i < words.size(); i++) {
            String curWord = words.get(i);
            String preWord = words.get(i - 1);
            String curTime = String.valueOf(i);
            String preTime = String.valueOf(i - 1);

            // check novel word
            boolean novelWord = false;
            if (!tagDict.containsKey(curWord)) {
                curWord = OOV;
                novelWord = true;
            }

            if (!tagDict.containsKey(words.get(i - 1))){
                preWord = OOV;
            }

            HashSet<String> candidateTag     = tagDict.get(curWord);
            HashSet<String> prevCandidateTag = tagDict.get(preWord);

            for (String tag : candidateTag) {
                double alpha_ti = 0.0;

                for (String prevTag : prevCandidateTag) {
                    // tag to tag probability
                    String p_tt_key = prevTag + TAGTAG_SEP + tag;
                    double p_tt;
                    if (currCount.containsKey(p_tt_key)) {
                        p_tt = currCount.get(p_tt_key);
                    } else {
                        if (currCount.containsKey(p_tt_key)) {
                            p_tt = (currCount.get(p_tt_key) + LAMBDA) /
                                    (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        } else {
                            p_tt = LAMBDA / (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        }
                        currCount.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (currCount.containsKey(p_tw_key)) {
                        p_tw = currCount.get(p_tw_key);
                    } else {
                        if (novelWord) {
                            p_tw = LAMBDA / (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            p_tw = (currCount.get(p_tw_key) + LAMBDA) /
                                    (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        }
                        currCount.put(p_tw_key, p_tw);
                    }

                    // for forward-backward
                    double preAlpha_p = alpha.get(prevTag + TIME_SEP + preTime) + Math.log(p_tt) + Math.log(p_tw);
                    if(alpha_ti == 0) {
                        alpha_ti = preAlpha_p;
                    } else {
                        alpha_ti = logadd(alpha_ti, preAlpha_p);
                    }

                    // for Viterbi decoding
                    double currentMu = mus.get(prevTag + TIME_SEP + preTime) + Math.log(p_tt) + Math.log(p_tw);

                    // update max probability and back pointers
                    if (!mus.containsKey(tag + TIME_SEP + curTime) || mus.get(tag + TIME_SEP + curTime) < currentMu) {
                        mus.put(tag + TIME_SEP + curTime, currentMu);
                        backPointers.put(tag + TIME_SEP + curTime, prevTag);
                    }

                }
                alpha.put(tag + TIME_SEP + curTime, alpha_ti);

//                System.out.printf("Day %d alpha_{%s}: %f\n", i, tag, Math.exp(-alpha_ti));
            }
        }
        for (Map.Entry<String, Double> entry : alpha.entrySet()) {
            System.out.println(entry.getKey() + "  " +  Math.exp(-entry.getValue()));
        }

        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + TIME_SEP + i));
        }

        return tags;

    }

    protected Map<String, Double> backward(List<String> words) {
        beta.clear();

        Map<String, Double> probTW = new HashMap<String, Double>();

        double s = alpha.get(BND + TIME_SEP + String.valueOf(words.size() - 1));

        List<String> tags = new ArrayList<String>();
        tags.add(BND);
        beta.put(BND + TIME_SEP + String.valueOf(words.size() - 1), Math.log(1.0));

        HashSet<String> candidateTag, prevCandidateTag;

        for (int i = words.size() - 1; i > 0; i--) {
            String curWord = words.get(i);
            String preWord = words.get(i - 1);
            String curTime = String.valueOf(i);
            String preTime = String.valueOf(i - 1);

            // find tags for current word and previous word
            if (!tagDict.containsKey(preWord)) {
                preWord = OOV;
            }

            boolean curWordNovel = false;
            if (!tagDict.containsKey(curWord)) {
                curWord = OOV;
                curWordNovel = true;
            }

            prevCandidateTag = tagDict.get(preWord);
            candidateTag     = tagDict.get(curWord);

            // perform backward algorithm
            for (String tag : candidateTag) {
                // compute p(T_i = t_i | \vec{w}) = alpha_ti(i) * beta_{t_i}(i) / s
                // all are log probabilities
                double prob_ti = alpha.get(tag + TIME_SEP + curTime) + beta.get(tag + TIME_SEP + curTime) - s;
                probTW.put(tag + TIME_SEP + curTime, prob_ti);

                for (String prevTag : prevCandidateTag) {

                    // tag to tag probability
                    String p_tt_key = prevTag + TAGTAG_SEP + tag;
                    double p_tt;
                    if (probDP.containsKey(p_tt_key)) {
                        p_tt = probDP.get(p_tt_key);
                    } else {
                        if (currCount.containsKey(p_tt_key)) {
                            p_tt = (currCount.get(p_tt_key) + LAMBDA) /
                                    (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        } else {
                            p_tt = LAMBDA / (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        if (curWordNovel) {
                            p_tw = LAMBDA / (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            p_tw = (currCount.get(p_tw_key) + LAMBDA) /
                                    (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        }
                        probDP.put(p_tw_key, p_tw);
                    }
                    double p = Math.log(p_tt) + Math.log(p_tw);
                    double p_Beta_ti = beta.get(tag + TIME_SEP + curTime) + p;

                    String betaKey = prevTag + TIME_SEP + preTime;
                    if(!beta.containsKey(betaKey)){
                        beta.put(betaKey, p_Beta_ti);
                    } else {
                        beta.put(betaKey,
                                logadd(beta.get(betaKey), p_Beta_ti));
                    }

//                    System.out.printf("beta_{%d}: %f\n", i, Math.exp(p_Beta_ti));
                    // compute p(T_{i_1} = prevTag, T_i = tag | \vec{w}) = alpha_{t_{i-1}}(i - 1) * p * beta_{t_i}(i) / s
                    double prob_ti_1 = alpha.get(prevTag + TIME_SEP + preTime) + p + beta.get(tag + TIME_SEP + curTime) - s;
                    probTW.put(prevTag + TAGTAG_SEP + tag, prob_ti_1);
                }
            }
        }

        return probTW;
    }


    /*
     Find the best tag for each word according to the forward-backward probability.
     */
    protected List<String> posteriorTag(List<String> words, Map<String, Double> probTW) {
        List<String> tags = new ArrayList<String>();
        tags.add(BND);

        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            String curTime = String.valueOf(i);

            if (!tagDict.containsKey(word)) {
                word = OOV;
            }

            HashSet<String> candidateTags = tagDict.get(word);
            tags.add(findBestTag(candidateTags, probTW, curTime));
        }

        return tags;
    }


    protected String findBestTag(Set<String> candidateTags, Map<String, Double> probTW, String curTime) {
        double bestProb = -Double.MAX_VALUE;
        String bestTag  = "";

        for(String tag : candidateTags) {
            double prob = probTW.get(tag + TIME_SEP + curTime);
            if (bestProb <= prob) {
                bestProb = prob;
                bestTag = tag;
            }
        }
        return bestTag;
    }



    /*
     Compute the tagging accuracy:
        1. overall accuracy: considers all word tokens, other than the sentence boundary markers ###
        2. known-word accuracy: considers only tokens of words (other than ###) that also appeared in training data
        3. novel-word accuracy: consider only tokens of words that did not also appear in training data.

     Compute perplexity:
        perplexity = exp( - (log p(w1, t1, ..., wn, tn | w0, t0)) / n )

     */
    public void computeAccuracy(List<String> words, List<String> tags, List<String> ans, boolean isVtag) {
        int totCnt          = 0;
        int totCorrect      = 0;
        int totKnownCnt     = 0;
        int totKnowCorrect  = 0;
        int totNovelCnt     = 0;
        int totNovelCorrect = 0;

        for(int i = 0; i < words.size(); i++) {
            String word   = words.get(i);
            String resTag = tags.get(i);
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
        double knownAccu = totKnownCnt > 0? (double)totKnowCorrect / (double)totKnownCnt * 100 : 0;
        double novelAccu = totNovelCnt > 0? (double)totNovelCorrect / (double)totNovelCnt * 100 : 0;
        System.out.printf("Tagging accuracy (%s decoding): %.2f%% (known: %.2f%% novel: %.2f%%)\n", isVtag? VITERBI:POSTERIOR, totAccu, knownAccu, novelAccu);

        // Calculate perplexity
        if(isVtag) {
            String key = tags.get(tags.size() - 1) + TIME_SEP + String.valueOf(tags.size() - 1);
            double prob = mus.get(key) / Math.log(2);
            double perplexity = Math.pow(2, -prob / (words.size() - 1));
            System.out.printf("Perplexity per Viterbi-tagged test word: %.3f\n", perplexity);
        }

    }

    /*
     perform log addition:
     logadd(x, y) = x + log(1 + exp(y - x))  if y <= x;
                  = y + log(1 + exp(x - y))  otherwise
     */
    private static double logadd(double x, double y) { //TODO: handle the case x=0 or y=0
        if(y <= x) {
            if (1 + Math.exp(y - x) == 0) {
                return x + (-Double.MAX_VALUE);
            }
            return x + Math.log(1 + Math.exp(y - x));

        } else {
            if (1 + Math.exp(x - y) == 0) {
                return y + (-Double.MAX_VALUE);
            }
            return y + Math.log(1 + Math.exp(x - y));
        }
    }

}
