
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTagger {
    private static final boolean DEBUG = false;
    protected Map<String, HashSet<String>> tagDict;
    protected Map<String, Double> countItems;
    protected Map<String, Double> arcProbs;
    protected Map<String, Double> mus;
    protected Map<String, Double> probDP;
    protected Map<String, String> backPointers;
    protected HashSet<String> allTags;
    protected Map<String, Double> alpha;
    protected Map<String, Double> beta;
    protected Map<String, Double> countSingletons;
    protected boolean forwardTag;
    protected final String TAGTAG_SEP   = "[TT]";
    protected final String TAG_WORD_SEP = "/";
    protected final String TIME_SEP     = "__";
    protected final String WORD_SEP     = "[w]";
    protected final String TAG_SEP      = "[T]";
    protected final String OOV          = "OOV";
    protected final String BND          = "###"; //boundary marker
    protected final String VITERBI      = "Viterbi";
    protected final String POSTERIOR    = "posterior";
    protected double LAMBDA       = 0.0;   // setting LAMBDA = 0 means no add one smoothing
    protected double tokenCount     = 0.0;  // Token count (number of training entries


    public ViterbiTagger() {
        this.tagDict      = new HashMap<String, HashSet<String>>();
        this.countItems   = new HashMap<String, Double>();
        this.arcProbs     = new HashMap<String, Double>();
        this.mus          = new HashMap<String, Double>();
        this.probDP       = new HashMap<String, Double>();
        this.backPointers = new HashMap<String, String>();
        this.allTags      = new HashSet<String>();
        this.alpha        = new HashMap<String, Double>();
        this.beta         = new HashMap<String, Double>();
        this.countSingletons = new HashMap<String, Double>();
        this.forwardTag   = false;
    }


    /*
     When starting a new training, clean relevant instance variables.
     */
    protected void init() {  // TODO
        countItems.clear();
        allTags.clear();
        forwardTag = false;
    }

    /*
     Read file and parse word/tag pairs and store needed data.
     */
    public void readFile(String fileName) throws IOException {
        init();

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            String line = br.readLine();
            String[] prevElements = null;

            while (line != null) {
                if (line.length() > 0) {
                    String[] elements = line.split("/");
                    String word = elements[0];
                    String tag  = elements[1];
                    tokenCount += 1.0;

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
                        String tagTagSingleton = ".|" + TAGTAG_SEP + prevElements[1];

                        if (countItems.containsKey(tagTag)) {
                            double newItemCount = countItems.get(tagTag) + 1.0;
                            countItems.replace(tagTag, newItemCount);

                            // Check if the count of current tag-tag bigram reaches 2
                            // if so, deduce the count of tag-tag singletons of preTag by 1
                            if (Double.compare(newItemCount, 2.0) == 0) {
                                if (countSingletons.containsKey(tagTagSingleton)) {
                                    double newSingletonCount = countSingletons.get(tagTagSingleton) - 1.0;
                                    countSingletons.replace(tagTagSingleton, newSingletonCount);
                                }
                            }
                            else if (Double.compare(newItemCount, 1.0) == 0) {
                                if (countSingletons.containsKey(tagTagSingleton)) {
                                    double newSingletonCount = countSingletons.get(tagTagSingleton) + 1.0;
                                    countSingletons.replace(tagTagSingleton, newSingletonCount);
                                }
                            }
                        }
                        else {
                            countItems.put(tagTag, 1.0);

                            // In this case there must be increment on number of tag-tag singletons,
                            // Check whether create a new entry or increment on an existing one
                            if ( !countSingletons.containsKey(tagTagSingleton)) {
                                countSingletons.put(tagTagSingleton, 1.0);
                            }
                            else {
                                double newCount2 = countSingletons.get(tagTagSingleton) + 1.0;
                                countSingletons.replace(".|" + TAGTAG_SEP + prevElements[1], newCount2);
                            }
                        }
                    }
                    prevElements = elements;

                    // Initialize tag to word
                    String wordTag = elements[0] + TAG_WORD_SEP + elements[1];
                    String wordTagSingleton = ".|" + TAG_WORD_SEP + elements[1];

                    if (countItems.containsKey(wordTag)) {
                        double newItemCount = countItems.get(wordTag) + 1.0;
                        countItems.replace(wordTag, newItemCount);

                        // Update/initialize tag-word singletons
                        // Same idea for tag-tag singletons, but here we check current tag instead of previous one
                        if (Double.compare(newItemCount, 2.0) == 0) {
                            double newSingletonCount = countSingletons.get(wordTagSingleton) - 1.0;
                            countSingletons.replace(wordTagSingleton, newSingletonCount);
                        }
                    }
                    else{
                        countItems.put(wordTag, 1.0);

                        // Do increment or initialize a new tag-word singleton entry
                        if ( !countSingletons.containsKey(wordTagSingleton)) {
                            countSingletons.put(wordTagSingleton, 1.0);
                        }
                        else {
                            double newSingletonCount = countSingletons.get(wordTagSingleton) + 1.0;
                            countSingletons.replace(wordTagSingleton, newSingletonCount);
                        }

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
            countItems.replace(BND + TAG_WORD_SEP + BND,
                               countItems.get(BND + TAG_WORD_SEP + BND) - 1);

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


    /*
     This will tag funciton will first perform Viterbi decoding, and then perform
     forward-backward decoding.
     */
    public List<String> viterbiTag(List<String> words) {
        alpha.clear();
        beta.clear();
        mus.clear();

        List<String> tags = forward(words);
        return tags;
    }

    public List<String> posTag(List<String> words) {
        if (!forwardTag) {
            forward(words);
        }
        Map<String, Double> probTW = backward(words);
        List<String> tags = posteriorTag(words, probTW);
        return tags;
    }

    protected List<String> forward(List<String> words) {
        forwardTag = true;

        List<String> tags = new ArrayList<String>();

        tags.add(BND);
        alpha.put(BND + TIME_SEP + "0", Math.log(1.0));
        mus.put(BND + TIME_SEP + "0", Math.log(1.0));

        HashSet<String> candidateTag;
        HashSet<String> prevCandidateTag = tagDict.get(words.get(0));

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

            candidateTag     = tagDict.get(curWord);
            prevCandidateTag = tagDict.get(preWord);

            for (String tag : candidateTag) {
                double alpha_ti = 0.0;

                for (String prevTag : prevCandidateTag) {
                    // tag to tag probability
                    String p_tt_key = prevTag + TAGTAG_SEP + tag;
                    double p_tt;
                    if (probDP.containsKey(p_tt_key)) {
                        p_tt = probDP.get(p_tt_key);
                    } else {
                        // tag-tag backoff probability
                        double p_tt_backoff = countItems.get(tag + TAG_SEP) / (tokenCount - 1.0);

                        // Update the lambda value
                        LAMBDA = 1.0;
                        String singletonTTKey = ".|" + TAGTAG_SEP + prevTag;
                        if (countSingletons.containsKey(singletonTTKey)) {
                            LAMBDA += countSingletons.get(singletonTTKey);
                        }

                        if (countItems.containsKey(p_tt_key)) {
                            p_tt = (countItems.get(p_tt_key) + LAMBDA * p_tt_backoff) /
                                    (countItems.get(prevTag + TAG_SEP) + LAMBDA);
                        } else {
                            p_tt = LAMBDA * p_tt_backoff /  (countItems.get(prevTag + TAG_SEP) + LAMBDA);
                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + TAG_WORD_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Update LAMBDA value
                        LAMBDA = 1.0;
                        String singletonTKKey = ".|" + TAG_WORD_SEP + tag;
                        if (countSingletons.containsKey(singletonTKKey)) {
                            LAMBDA += countSingletons.get(singletonTKKey);
                        }

                        // compute tag-word backoff first
                        if (novelWord) {
                            double p_tw_backoff = 1.0 / (tokenCount + tagDict.size() - 1.0);
                            p_tw = LAMBDA * p_tw_backoff / (countItems.get(tag + TAG_SEP) + LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (countItems.get(curWord + WORD_SEP) + 1.0) / (tokenCount + tagDict.size() - 1.0);
                            p_tw = (countItems.get(p_tw_key) + LAMBDA * p_tw_backoff) / ( countItems.get(tag + TAG_SEP) + LAMBDA);

                        }
                        probDP.put(p_tw_key, p_tw);
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
            }
        }
        for (int i = words.size() - 1; i > 0; i--) {
            tags.add(0, backPointers.get(tags.get(0) + TIME_SEP + i));
        }

        return tags;

    }

    protected Map<String, Double> backward(List<String> words) {
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
                        // Compute backoff probabilities and update LAMBDA
                        LAMBDA = 1.0;
                        String singletonTTKey = ".|" + TAGTAG_SEP + prevTag;
                        if (countSingletons.containsKey(singletonTTKey)) {
                            LAMBDA += countSingletons.get(singletonTTKey);
                        }
                        double p_tt_backoff = countItems.get(tag + TAG_SEP) / (tokenCount - 1.0);
                        if (countItems.containsKey(p_tt_key)) {
                            p_tt = (countItems.get(p_tt_key) + LAMBDA * p_tt_backoff) /
                                    (countItems.get(prevTag + TAG_SEP) + LAMBDA);
                        } else {
//                            p_tt = LAMBDA / (countItems.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                            p_tt = LAMBDA * p_tt_backoff / ( countItems.get(prevTag + TAG_SEP) + LAMBDA);
                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + TAG_WORD_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Again, update LAMBDA
                        LAMBDA = 1.0;
                        String singletonTWKey = ".|" + TAG_WORD_SEP + tag;
                        if (countSingletons.containsKey(singletonTWKey)) {
                            LAMBDA += countSingletons.get(singletonTWKey);
                        }

                        // Compute backoff probabilities first
                        if (curWordNovel) {
                            double p_tw_backoff = 1.0 / (tokenCount + tagDict.size() - 1.0);
                            p_tw = LAMBDA * p_tw_backoff / ( countItems.get(tag + TAG_SEP) + LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (countItems.get(curWord + WORD_SEP) + 1.0) / ( tokenCount + tagDict.size() - 1.0 );
                            p_tw = (countItems.get(p_tw_key) + LAMBDA * p_tw_backoff ) / (countItems.get(tag + TAG_SEP) + LAMBDA);
//                            p_tw = (countItems.get(p_tw_key) + LAMBDA) /
//                                    (countItems.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
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
