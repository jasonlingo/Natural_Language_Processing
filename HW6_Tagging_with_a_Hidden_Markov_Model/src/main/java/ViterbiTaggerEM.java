
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jason on 4/16/16.
 */
public class ViterbiTaggerEM {
    // tags tables
    protected Map<String, HashSet<String>> tagDict;
    protected HashSet<String> tagsPlusBND;
    protected HashSet<String> allTags;

    // counters.
    protected Map<String, Double> currCount;
    protected Map<String, Double> orgCount;
    protected Map<String, Double> newCount;

    // probability tables.
    protected Map<String, Double> mus;
    protected Map<String, Double> alpha;
    protected Map<String, Double> beta;

    // singleton table.
    protected Map<String, Double> countSingletons;

    // word type tables, allType is the union of train and raw dataset.
    protected HashSet<String> trainTypes;
    protected HashSet<String> rawTypes;
    protected HashSet<String> allTypes;

    // word token counters and raw word list.
    protected List<String> rawWords;
    protected int trainToken;
    protected int rawToken;

    // separators for differentiating different keys for counters.
    protected final String TAGTAG_SEP   = "[TT]";
    protected final String WORD_TAG_SEP = "/";
    protected final String TIME_SEP     = "__";
    protected final String WORD_SEP     = "[w]";
    protected final String TAG_SEP      = "[T]";
    protected final String OOV          = "OOV";
    protected final String BND          = "###"; //boundary marker
    protected final String SING         = ".|";

    // smoothing parameter.
    protected double LAMBDA             = 1.0;


    public ViterbiTaggerEM() {
        this.tagDict      = new HashMap<String, HashSet<String>>();
        this.tagsPlusBND  = new HashSet<String>();
        this.allTags      = new HashSet<String>();

        this.currCount    = new HashMap<String, Double>();
        this.orgCount     = new HashMap<String, Double>();
        this.newCount     = new HashMap<String, Double>();

        this.mus          = new HashMap<String, Double>();
        this.alpha        = new HashMap<String, Double>();
        this.beta         = new HashMap<String, Double>();

        this.countSingletons = new HashMap<String, Double>();

        this.trainTypes   = new HashSet<String>();
        this.rawTypes     = new HashSet<String>();
        this.allTypes     = new HashSet<String>();

        this.rawWords     = new ArrayList<String>();
        this.trainToken   = 0;
        this.rawToken     = 0;
    }


    /*
     Read file and parse word/tag pairs and store needed data.

     @param train: training data that has words and tags.
     @param raw: raw data that only has words.
     */
    public void readFile(String train, String raw) throws IOException {
        processTrainData(train);
        processRawData(raw);

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

    }

    protected void processTrainData(String train) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(train));

        try {
            String line = br.readLine();
            String[] prevElements = null;

            while (line != null) {
                if (line.length() > 0) {
                    String[] elements = line.split("/");
                    String word = elements[0];
                    String tag  = elements[1];

                    trainToken++;
                    allTags.add(tag);
                    trainTypes.add(word);
                    allTypes.add(word);

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
                        addBigramTag(prevElements[1], tag);
                    }
                    prevElements = elements;

                    // Initialize word to tag count
                    addWordToTag(word, tag);

                    // Initialize Unigram
                    // Here we will count ### twice for each ###/###, need to correct count later
                    addUnigram(word, tag);
                }
                line = br.readLine();
            }
            trainToken--;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
        }
    }


    protected void processRawData(String raw) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(raw));
        try {
            // process raw data
            String line = br.readLine();
            while (line != null) {
                String word = line.trim(); // remove trailing space if it exists
                rawTypes.add(word);
                allTypes.add(word);
                rawWords.add(word);
                rawToken++;
                line = br.readLine();
            }
            rawToken--;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
        }
    }

    protected void addBigramTag(String preTag, String curTag) {
        String tagTag = preTag + TAGTAG_SEP + curTag;
        String tagTagSingleton = SING + TAGTAG_SEP + preTag;

        if (currCount.containsKey(tagTag)) {
            double newItemCount = currCount.get(tagTag) + 1.0;
            currCount.replace(tagTag, newItemCount);

            // Check if the count of current tag-tag bigram reaches 2
            // if so, deduce the count of tag-tag singletons of preTag by 1
            if (Double.compare(newItemCount, 2.0) == 0) {
                if (countSingletons.containsKey(tagTagSingleton)) {
                    double newSingletonCount = countSingletons.get(tagTagSingleton) - 1.0;

                    if (Double.compare(newSingletonCount, 0.0) == 0) {
                        countSingletons.remove(tagTagSingleton);
                    } else {
                        countSingletons.replace(tagTagSingleton, newSingletonCount);
                    }
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
                countSingletons.replace(SING + TAGTAG_SEP + preTag, newCount2);
            }
        }
    }

    protected void addWordToTag(String word, String tag) {
        String wordTag = word + WORD_TAG_SEP + tag;
        String wordTagSingleton = SING + WORD_TAG_SEP + tag;

        if (currCount.containsKey(wordTag)) {
            double newItemCount = currCount.get(wordTag) + 1.0;
            currCount.replace(wordTag, newItemCount);

            // Update/initialize tag-word singletons
            // Same idea for tag-tag singletons, but here we check current tag instead of previous one
            if (Double.compare(newItemCount, 2.0) == 0) {
                double newSingletonCount = countSingletons.get(wordTagSingleton) - 1.0;

                if (Double.compare(newSingletonCount, 0.0) == 0) {
                    countSingletons.remove(wordTagSingleton);
                } else {
                    countSingletons.replace(wordTagSingleton, newSingletonCount);
                }
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
    }

    protected void addUnigram(String word, String tag) {
        // add word unigram
        String wordkey = word + WORD_SEP;
        if (currCount.containsKey(wordkey)) {
            currCount.replace(wordkey, currCount.get(wordkey) + 1.0);
        } else {
            currCount.put(wordkey, 1.0);
        }

        // add tag unigram
        String tagkey = tag + TAG_SEP;
        if (currCount.containsKey(tagkey)) {
            currCount.replace(tagkey, currCount.get(tagkey) + 1.0);
        } else {
            currCount.put(tagkey, 1.0);
        }
    }




    /*******************************************************************************
     This will tag funciton will first perform Viterbi decoding, and then perform
     forward-backward decoding for many iteration. After each iteration, we also
     reestimate new parameters (counters) for the tagging model.

     @param words: a list of test words to be decoded.
     @param testTags: the true tags for the input words.
     @param iters: the total number of iteration for running the EM algorithm.
     */
    public void emTag(List<String> words, List<String> testTags, int iters) {
        alpha.clear();
        beta.clear();
        mus.clear();
        orgCount = new HashMap<String, Double>(currCount);

        for(int epoc = 0; epoc < iters; epoc++) {
            newCount = new HashMap<String, Double>(orgCount);

            //****************************************************
            // perform Viterbi decoding (supervised) on test data
            //****************************************************
            double token = (double)trainToken;
            double v = (double)trainTypes.size() + 1.0; // OOV
            List<String> tags = forward(words, false, token, v);
            calAccuracy(words, tags, testTags);

            //****************************************************
            // perform forward-backward (unsupervised) on raw data
            // update counters
            //****************************************************
            token = (double)(trainToken + rawToken);
            v = (double)allTypes.size();
            forward(rawWords, true, token, v);
            backward(rawWords, token, v);
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
    protected List<String> forward(List<String> words, boolean isFB, double token, double v) {
        alpha.clear();
        mus.clear();

        Map<String, Double> probDP = new HashMap<String, Double>();
        Map<String, String> backPointers = new HashMap<String, String>();
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

            // get the candidate tags for current and previous words.
            HashSet<String> candidateTag;
            if (tagDict.containsKey(curWord)) {
                candidateTag = tagDict.get(curWord);
            } else {
                candidateTag = tagDict.get(OOV);
            }
            HashSet<String> prevCandidateTag;
            if (tagDict.containsKey(preWord)) {
                prevCandidateTag = tagDict.get(preWord);
            } else {
                prevCandidateTag = tagDict.get(OOV);
            }

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
                        double p_tt_backoff = currCount.get(tag + TAG_SEP) / token;

                        // Update the lambda value
                        String singletonTTKey = SING + TAGTAG_SEP + prevTag;
                        LAMBDA = 1.0 + countSingletons.getOrDefault(singletonTTKey, 0.0);

                        p_tt = (currCount.getOrDefault(p_tt_key, 0.0) + LAMBDA * p_tt_backoff) /
                                (currCount.get(prevTag + TAG_SEP) + LAMBDA);
                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Update LAMBDA value
                        String singletonTKKey = SING + WORD_TAG_SEP + tag;
                        LAMBDA = 1.0 + countSingletons.getOrDefault(singletonTKKey, 0.0);

                        if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (currCount.getOrDefault(curWord + WORD_SEP, 0.0) + 1.0) / (token + v);
                            p_tw = (currCount.getOrDefault(p_tw_key, 0.0) + LAMBDA * p_tw_backoff) /
                                    ( currCount.get(tag + TAG_SEP) + LAMBDA);
                        }
                        probDP.put(p_tw_key, p_tw);
                    }


                    if (isFB) {
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
                if (isFB) {
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
    protected Map<String, Double> backward(List<String> words, double token, double v) {
        beta.clear();

        Map<String, Double> probDP = new HashMap<String, Double>();
        Map<String, Double> probTW = new HashMap<String, Double>();

        double s = alpha.get(BND + TIME_SEP + String.valueOf(words.size() - 1));
        beta.put(BND + TIME_SEP + String.valueOf(words.size() - 1), Math.log(1.0));

        for (int i = words.size() - 1; i > 0; i--) {
            String curWord = words.get(i);
            String preWord = words.get(i - 1);
            String curTime = String.valueOf(i);
            String preTime = String.valueOf(i - 1);

            // find tags for current word and previous word
            HashSet<String> candidateTag;
            if (tagDict.containsKey(curWord)) {
                candidateTag = tagDict.get(curWord);
            } else {
                candidateTag = tagDict.get(OOV);
            }
            HashSet<String> prevCandidateTag;
            if (tagDict.containsKey(preWord)) {
                prevCandidateTag = tagDict.get(preWord);
            } else {
                prevCandidateTag = tagDict.get(OOV);
            }

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
                        String singletonTTKey = SING + TAGTAG_SEP + prevTag;
                        LAMBDA = 1.0 + countSingletons.getOrDefault(singletonTTKey, 0.0);
                        double p_tt_backoff = currCount.get(tag + TAG_SEP) / token;

                        p_tt = (currCount.getOrDefault(p_tt_key, 0.0) + LAMBDA * p_tt_backoff) /
                                (currCount.get(prevTag + TAG_SEP) + LAMBDA);

                        probDP.put(p_tt_key, p_tt);
                    }

                    // tag to word probability
                    String p_tw_key = curWord + WORD_TAG_SEP + tag;
                    double p_tw;
                    if (probDP.containsKey(p_tw_key)) {
                        p_tw = probDP.get(p_tw_key);
                    } else {
                        // Again, update LAMBDA
                        String singletonTWKey = SING + WORD_TAG_SEP + tag;
                        LAMBDA = 1.0 + countSingletons.getOrDefault(singletonTWKey, 0.0);

                        if (curWord.equals(BND) && tag.equals(BND)) {
                            p_tw = 1.0;
                        } else {
                            double p_tw_backoff = (currCount.getOrDefault(curWord + WORD_SEP, 0.0) + 1.0) / (token + v);
                            p_tw = (currCount.getOrDefault(p_tw_key, 0.0) + LAMBDA * p_tw_backoff ) /
                                    (currCount.get(tag + TAG_SEP) + LAMBDA);
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
