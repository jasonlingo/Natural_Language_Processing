
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTaggerEM {
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
    protected Map<String, Double> countSingletons;
    protected HashSet<String> trainTypes;
    protected HashSet<String> rawTypes;
    protected HashSet<String> allTypes;
    protected HashSet<String> tagsPlusBND;
    protected List<String> rawWords;
    protected boolean forwardTag;
    protected final String TAGTAG_SEP   = "[TT]";
    protected final String WORD_TAG_SEP = "/";
    protected final String TIME_SEP     = "__";
    protected final String WORD_SEP     = "[w]";
    protected final String TAG_SEP      = "[T]";
    protected final String OOV          = "OOV";
    protected final String BND          = "###"; //boundary marker
    protected double LAMBDA       = 0.0;   // setting LAMBDA = 0 means no add one smoothing
    protected double tokenCount     = 0.0;  // Token count (number of training entries


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
        this.tagsPlusBND  = new HashSet<String>();
        this.rawWords     = new ArrayList<String>();
        this.countSingletons = new HashMap<String, Double>();
        this.forwardTag   = false;
    }


    /*
     When starting a new training, clean relevant instance variables.
     */
    protected void init() {
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
        this.tagsPlusBND.clear();
        this.countSingletons.clear();
        this.forwardTag = false;
    }

    /*
     Read file and parse word/tag pairs and store needed data.

     @param train: training data that has words and tags.
     @param raw: raw data that only has words.
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
                    tokenCount += 1.0;

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
                        String tagTagSingleton = ".|" + TAGTAG_SEP + prevElements[1];

                        if (currCount.containsKey(tagTag)) {
                            double newItemCount = currCount.get(tagTag) + 1.0;
                            currCount.replace(tagTag, newItemCount);

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
                            currCount.put(tagTag, 1.0);

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

                    // Initialize word to tag count
                    String wordTag = elements[0] + WORD_TAG_SEP + elements[1];
                    String wordTagSingleton = ".|" + WORD_TAG_SEP + elements[1];

                    if (currCount.containsKey(wordTag)) {
                        double newItemCount = currCount.get(wordTag) + 1.0;
                        currCount.replace(wordTag, newItemCount);

                        // Update/initialize tag-word singletons
                        // Same idea for tag-tag singletons, but here we check current tag instead of previous one
                        if (Double.compare(newItemCount, 2.0) == 0) {
                            double newSingletonCount = countSingletons.get(wordTagSingleton) - 1.0;
                            countSingletons.replace(wordTagSingleton, newSingletonCount);
                        }
                    }
                    else{
                        currCount.put(wordTag, 1.0);
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
            tagsPlusBND = new HashSet<String>(allTags);
            allTags.remove(BND);

            // for "add one smoothing", add an OOV
            tagDict.put(OOV, allTags);

            orgCount = new HashMap<String, Double>(currCount);
//            orgCount = new HashMap<String, Double>();

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            rawBr.close();
            br.close();
        }
    }


    /*
     This will tag funciton will first perform Viterbi decoding, and then perform
     forward-backward decoding.

     @param words: a list of test words to be decoded.
     @param testTags: the true tags for the input words.
     @param iters: the total number of iteration for running the EM algorithm.
     */
    public void emTag(List<String> words, List<String> testTags, int iters) {
        alpha.clear();
        beta.clear();
        mus.clear();

        List<String> tags = new ArrayList<String>();

        for(int epoc = 0; epoc < iters; epoc++) {
            newCount = new HashMap<String, Double>(orgCount);

            // perform Viterbi decoding on test data
            tags = forward(words, false);
            calAccuracy(words, tags, testTags);

            // perform forward-backward on raw data
            tags = forward(rawWords, true);
            Map<String, Double> probTW = backward(rawWords);
            calPerplexity(epoc);

            currCount = new HashMap<String, Double>(newCount);
        }

    }


    /*
     Viterbi decoding (calculate mu probability) and forward process (calculate alpha probability).

     @param: words: the words list to be decoded
     @param: isFB: indicate forward(true) or Viterbi(false) mode
     @return: (Viterbi mode) a list of tags for input words;
              (FB mode) no use
     */
    protected List<String> forward(List<String> words, boolean isFB) {
        probDP.clear();
        alpha.clear();
        mus.clear();
        backPointers.clear();

        List<String> tags = new ArrayList<String>();

        if(isFB) {
            alpha.put(BND + TIME_SEP + "0", Math.log(1.0));
        } else {
            mus.put(BND + TIME_SEP + "0", Math.log(1.0));
            tags.add(BND);
        }

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

            // get the candidate tags for current and previous words.
            HashSet<String> candidateTag     = tagDict.get(curWord);
            HashSet<String> prevCandidateTag = tagDict.get(preWord);

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
                        double p_tt_backoff = currCount.get(tag + TAG_SEP) / (tokenCount - 1.0);

                        // Update the lambda value
                        LAMBDA = 1.0;
                        String singletonTTKey = ".|" + TAGTAG_SEP + prevTag;
                        if (countSingletons.containsKey(singletonTTKey)) {
                            LAMBDA += countSingletons.get(singletonTTKey);
                        }

                        if (currCount.containsKey(p_tt_key)) {
//                            p_tt = (currCount.get(p_tt_key) + LAMBDA) /
//                                    (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);

                            p_tt = (currCount.get(p_tt_key) + LAMBDA * p_tt_backoff) /
                                    (currCount.get(prevTag + TAG_SEP) + LAMBDA);
                        } else {
                            p_tt = LAMBDA * p_tt_backoff /  (currCount.get(prevTag + TAG_SEP) + LAMBDA);
//                            p_tt = LAMBDA / (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);

                        }
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Update LAMBDA value
                        LAMBDA = 1.0;
                        String singletonTKKey = ".|" + WORD_TAG_SEP + tag;

                        if (countSingletons.containsKey(singletonTKKey)) {
                            LAMBDA += countSingletons.get(singletonTKKey);
                        }

                        if (novelWord) {
                            double p_tw_backoff = 1.0 / (tokenCount + tagDict.size() - 1.0);
                            p_tw = LAMBDA * p_tw_backoff / (currCount.get(tag + TAG_SEP) + LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (currCount.get(curWord + WORD_SEP) + 1.0) / (tokenCount + tagDict.size() - 1.0);
                            p_tw = (currCount.get(p_tw_key) + LAMBDA * p_tw_backoff) / ( currCount.get(tag + TAG_SEP) + LAMBDA);

                        }
                        probDP.put(p_tw_key, p_tw);
                    }


                    if(isFB) {
                        // for forward-backward
                        double preAlpha_p = alpha.get(prevTag + TIME_SEP + preTime) + Math.log(p_tt) + Math.log(p_tw);
                        if (alpha_ti == 0) {
                            alpha_ti = preAlpha_p;
                        } else {
                            alpha_ti = logadd(alpha_ti, preAlpha_p);
                        }
                    } else {
                        // for Viterbi decoding
                        double currentMu = mus.get(prevTag + TIME_SEP + preTime) + Math.log(p_tt) + Math.log(p_tw);

                        // update max probability and back pointers
                        if (!mus.containsKey(tag + TIME_SEP + curTime) || mus.get(tag + TIME_SEP + curTime) < currentMu) {
                            mus.put(tag + TIME_SEP + curTime, currentMu);
                            backPointers.put(tag + TIME_SEP + curTime, prevTag);
                        }
                    }

                }
                if(isFB) {
                    alpha.put(tag + TIME_SEP + curTime, alpha_ti);
                }
            }
        }

        if(!isFB) {
            for (int i = words.size() - 1; i > 0; i--) {
                tags.add(0, backPointers.get(tags.get(0) + TIME_SEP + i));
            }
        }

        return tags;

    }


    /*
     Backward process, calculate beta probability.

     @param words: list of words (from raw data) to be decoded.
     @return: a HashMap containing the p_tw and p_tt probabilities.
     */
    protected Map<String, Double> backward(List<String> words) {
        beta.clear();

        Map<String, Double> probTW = new HashMap<String, Double>();
        HashSet<String> candidateTag, prevCandidateTag;

        double s = alpha.get(BND + TIME_SEP + String.valueOf(words.size() - 1));
        beta.put(BND + TIME_SEP + String.valueOf(words.size() - 1), Math.log(1.0));

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

                // update c(tag)
                newCount.put(tag + TAG_SEP, newCount.getOrDefault(tag + TAG_SEP, 0.0) + Math.exp(prob_ti));

                // update c(word/tag)
                newCount.put(curWord + WORD_TAG_SEP + tag,
                             newCount.getOrDefault(curWord + WORD_TAG_SEP + tag, 0.0) + Math.exp(prob_ti));

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
                        double p_tt_backoff = currCount.get(tag + TAG_SEP) / (tokenCount - 1.0);

                        if (currCount.containsKey(p_tt_key)) {
                            p_tt = (currCount.get(p_tt_key) + LAMBDA * p_tt_backoff) /
                                    (currCount.get(prevTag + TAG_SEP) + LAMBDA);
//                            p_tt = (currCount.get(p_tt_key) + LAMBDA) /
//                                    (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                        } else {
//                            p_tt = LAMBDA / (currCount.get(prevTag + TAG_SEP) + (allTags.size() + 1.0) * LAMBDA);
                            p_tt = LAMBDA * p_tt_backoff / ( currCount.get(prevTag + TAG_SEP) + LAMBDA);
                        }

                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Again, update LAMBDA
                        LAMBDA = 1.0;
                        String singletonTWKey = ".|" + WORD_TAG_SEP + tag;
                        if (countSingletons.containsKey(singletonTWKey)) {
                            LAMBDA += countSingletons.get(singletonTWKey);
                        }

                        if (curWordNovel) {
                            double p_tw_backoff = 1.0 / (tokenCount + tagDict.size() - 1.0);
                            p_tw = LAMBDA * p_tw_backoff / ( currCount.get(tag + TAG_SEP) + LAMBDA);
//                            p_tw = LAMBDA / (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
                        } else if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (currCount.get(curWord + WORD_SEP) + 1.0) / ( tokenCount + tagDict.size() - 1.0 );
                            p_tw = (currCount.get(p_tw_key) + LAMBDA * p_tw_backoff ) / (currCount.get(tag + TAG_SEP) + LAMBDA);
//
//                            p_tw = (currCount.get(p_tw_key) + LAMBDA) /
//                                    (currCount.get(tag + TAG_SEP) + tagDict.size() * LAMBDA);
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

                    // update c(tag to tag)
                    String key = prevTag + TAGTAG_SEP + tag;
                    newCount.put(key,
                                 newCount.getOrDefault(key, 0.0) + Math.exp(prob_ti_1));
                    probTW.put(prevTag + TAGTAG_SEP + tag + TIME_SEP + curTime, prob_ti_1);

                }
            }
        }

        return probTW;
    }


    /*
     Find the best tag for each word according to the forward-backward probability.

     @param words: a list of words to be tagged.
     @param probTW: the probability for each possible tag for each word in the input words list.
     @return: a list of tags.
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


    /*
     Find the best tag with the highest probability for a word.

     @param candidateTags: a list of candidate tags for a word.
     @param probTW: the probability for each possible tag for each word.
     @param curTime: indicate the word position.
     @return: the best tag.
     */
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
        1. overall accuracy: considers all word tokens, other than the sentence boundary markers ###.
        2. known-word accuracy: considers only tokens of words (other than ###) that also appeared in training data.
        3. novel-word accuracy: consider only tokens of words that did not also appear in training data.

     Compute perplexity:
        perplexity = exp( - (log p(w1, t1, ..., wn, tn | w0, t0)) / n )

     @param words: a list of words from test data.
     @param tags: predicted tags for the input words.
     @param ans: correct tags for the input words.
     */
    public void calAccuracy(List<String> words, List<String> tags, List<String> ans) {
        int totCnt          = 0;
        int totCorrect      = 0;
        int totKnownCnt     = 0;
        int totKnowCorrect  = 0;
        int totSeenCnt      = 0;
        int totSeenCorrect  = 0;
        int totNovelCnt     = 0;
        int totNovelCorrect = 0;

        for(int i = 0; i < words.size(); i++) {
            String word = words.get(i);
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
                // count known word that appeared in train data
                totKnownCnt++;
                if (correct) {
                    totKnowCorrect++;
                }
            } else if (rawTypes.contains(word)) {
                // count seen word that did not appear in train data but did appear in raw data
                totSeenCnt++;
                if (correct) {
                    totSeenCorrect++;
                }
            } else {
                // count novel word correct data (in neither train data nor raw data)
                totNovelCnt++;
                if (correct) {
                    totNovelCorrect++;
                }

            }
        }

        // calculate accuracy
        double totAccu   = (double)totCorrect / (double)totCnt * 100;
        double knownAccu = totKnownCnt > 0? (double)totKnowCorrect / (double)totKnownCnt * 100 : 0;
        double seenAccu  = totSeenCnt  > 0? (double)totSeenCorrect / (double)totSeenCnt * 100 : 0;
        double novelAccu = totNovelCnt > 0? (double)totNovelCorrect / (double)totNovelCnt * 100 : 0;
        System.out.printf("Tagging accuracy (Viterbi decoding): %.2f%% (known: %.2f%% seen: %.2f%% novel: %.2f%%)\n",
                           totAccu, knownAccu, seenAccu, novelAccu);

        // calculate perplexity
        String key = tags.get(tags.size() - 1) + TIME_SEP + String.valueOf(tags.size() - 1);
        double prob = mus.get(key) / Math.log(2);
        double perplexity = Math.pow(2, -prob / (words.size() - 1));
        System.out.printf("Perplexity per Viterbi-tagged test word: %f\n", perplexity);
    }

    /*
     Calculate perplexity of forward-backward decoding and print it.

     @param epoc: iteration number.
     */
    protected void calPerplexity(int epoc) {
        double prob = 0.0;

        if (rawWords.size() > 1) {
            String word = rawWords.get(1);
            if (!tagDict.containsKey(word)) {
                word = OOV;
            }

            HashSet<String> tags = tagDict.get(word);

            for (String tag : tags) {
                String key = tag + TIME_SEP + "1";
                double abProb = alpha.getOrDefault(key, 0.0) + beta.getOrDefault(key, 0.0);
                if (prob == 0) {
                    prob = abProb;
                } else {
                    prob = logadd(prob, abProb);
                }
            }
        }

        prob = prob / Math.log(2);
        double perpl = Math.pow(2, -prob / (rawWords.size() - 1));

        System.out.printf("Iteration %d: Perplexity per untagged raw word: %f\n\n", epoc, perpl);
    }

    /*
     perform log addition:
     logadd(x, y) = x + log(1 + exp(y - x))  if y <= x;
                  = y + log(1 + exp(x - y))  otherwise
     @param x, y: log probabilities.
     */
     protected static double logadd(double x, double y) {
        if(y <= x) {
            if (1 + Math.exp(y - x) == 0) { //handle the case log(0)
                return x + (-Double.MAX_VALUE);
            }
            return x + Math.log(1 + Math.exp(y - x));

        } else {
            if (1 + Math.exp(x - y) == 0) { //handle the case log(0)
                return y + (-Double.MAX_VALUE);
            }
            return y + Math.log(1 + Math.exp(x - y));
        }
    }

}
