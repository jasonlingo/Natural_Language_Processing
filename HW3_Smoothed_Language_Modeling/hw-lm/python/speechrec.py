
# CS465 at Johns Hopkins University.
# Module to estimate n-gram probabilities.

# Updated by Jason Baldridge <jbaldrid@mail.utexas.edu> for use in NLP
# course at UT Austin. (9/9/2008)

# Modified by Mozhi Zhang <mzhang29@jhu.edu> to add the new log linear model
# with word embeddings.  (2/17/2016)


import math
import random
import re
import sys
import copy
import numpy as np
import argparse
import collections


# TODO for TA: Currently, we use the same token for BOS and EOS as we only have
# one sentence boundary symbol in the word embedding file.  Maybe we should
# change them to two separate symbols?
BOS = 'EOS'   # special word type for context at Beginning Of Sequence
EOS = 'EOS'   # special word type for observed token at End Of Sequence
OOV = 'OOV'    # special word type for all Out-Of-Vocabulary words
OOL = 'OOL'    # special word type for all Out-Of-Lexicon words
DEFAULT_TRAINING_DIR = "/usr/local/data/cs465/hw-lm/All_Training/"
OOV_THRESHOLD = 3  # minimum number of occurrence for a word to be considered in-vocabulary


# TODO for TA: Maybe we should use inheritance instead of condition on the
# smoother (similar to the Java code).
class LanguageModel:
  def __init__(self):
    # The variables below all correspond to quantities discussed in the assignment.
    # For log-linear or Witten-Bell smoothing, you will need to define some 
    # additional global variables.
    self.smoother = None       # type of smoother we're using
    self.lambdap = None        # lambda or C parameter used by some smoothers

    # The word vector for w can be found at self.vectors[w].
    # You can check if a word is contained in the lexicon using
    #    if w in self.vectors:
    self.vectors = None    # loaded using read_vectors()

    self.vocab = None    # set of words included in the vocabulary
    self.vocab_size = None  # V: the total vocab size including OOV.

    # for Witten-Bell backoff
    self.tcount = None
    self.alphaNorm = None

    self.tokens = None      # the c(...) function
    self.types_after = None # the T(...) function

    self.progress = 0        # for the progress bar

    self.ngram = None
    self.bigrams = None
    self.trigrams = None
    
    # the two weight matrices U and V used in log linear model
    # They are initialized in train() function and represented as two
    # dimensional lists.
    self.U, self.V = None, None

    # for prob
    self.probDP = {}
    self.wbProbDP = {}

    # self.tokens[(x, y, z)] = # of times that xyz was observed during training.
    # self.tokens[(y, z)]    = # of times that yz was observed during training.
    # self.tokens[z]         = # of times that z was observed during training.
    # self.tokens[""]        = # of tokens observed during training.
    #
    # self.types_after[(x, y)]  = # of distinct word types that were
    #                             observed to follow xy during training.
    # self.types_after[y]       = # of distinct word types that were
    #                             observed to follow y during training.
    # self.types_after[""]      = # of distinct word types observed during training.

  def prob(self, x, y, z):
    """Computes a smoothed estimate of the trigram probability p(z | x,y)
    according to the language model.
    """
    bigram = unigram = False

    if self.ngram == "unigram":
      bigram = unigram = True

    elif self.ngram == "bigram":
      bigram = True

    if self.smoother == "UNIFORM":
      return float(1) / self.vocab_size

    elif self.smoother == "ADDL":
      if x not in self.vocab or bigram or unigram:
        x = OOV
      if y not in self.vocab or unigram:
        y = OOV
      if z not in self.vocab:
        z = OOV
      return ((self.tokens.get((x, y, z), 0) + self.lambdap) /
        (self.tokens.get((x, y), 0) + self.lambdap * self.vocab_size))

      # Notice that summing the numerator over all values of typeZ
      # will give the denominator.  Therefore, summing up the quotient
      # over all values of typeZ will give 1, so sum_z p(z | ...) = 1
      # as is required for any probability function.

    elif self.smoother == "BACKOFF_ADDL":

      if x == '' and y == '' and z == '':
        return 1.0 / self.vocab_size

      if x == '' and y != '':
        if ('', y, z) in self.probDP:
          return self.probDP[('', y, z)]
        result = (self.tokens.get((y, z), 0) + self.lambdap * self.vocab_size * self.prob('', '', z)) / \
                (self.tokens.get((y), 0) + self.lambdap * self.vocab_size)
        self.probDP[('', y, z)] = result
        return result

      if x == '':
        if ('', '', z) in self.probDP:
          return self.probDP[('', '', z)]
        result = (self.tokens.get((z), 0) + self.lambdap * self.vocab_size * self.prob('', '', '')) / \
                (self.tokens.get((''), 0) + self.lambdap * self.vocab_size)
        self.probDP[('', '', z)] = result
        return result

      if x not in self.vocab or bigram or unigram:
        x = OOV
      if y not in self.vocab or unigram:
        y = OOV
      if z not in self.vocab:
        z = OOV

      if (x, y, z) in self.probDP:
        return self.probDP[(x, y, z)]

      # normalization
      # nor = 0
      # for v in self.vocab:
      #   nor += (self.tokens.get((x, y, v), 0) + self.lambdap * self.vocab_size * self.prob('', y, v)) / \
      #        (self.tokens.get((x, y), 0) + self.lambdap * self.vocab_size)
      # nor += (self.tokens.get((x, y, OOV), 0) + self.lambdap * self.vocab_size * self.prob('', y, OOV)) / \
      #        (self.tokens.get((x, y), 0) + self.lambdap * self.vocab_size)

      result = ((self.tokens.get((x, y, z), 0) + self.lambdap * self.vocab_size * self.prob('', y, z)) / \
                (self.tokens.get((x, y), 0) + self.lambdap * self.vocab_size))

      self.probDP[(x, y, z)] = result
      return result

    elif self.smoother == "BACKOFF_WB":

      if x not in self.vocab:
        x = OOV
      if y not in self.vocab:
        y = OOV
      if z not in self.vocab:
        z = OOV

      if (x, y, z) in self.probDP:
        return self.probDP[(x, y, z)]

      p = self.wbProbXYZ(x, y, z)
      self.probDP[(x, y, z)] = p
      return p

    elif self.smoother == "LOGLINEAR":
      if (x, y, z) in self.probDP:
        return self.probDP[(x, y, z)]

      u = self.calculateU(x, y, z)
      bigZ = sum([self.calculateU(x, y, v) for v in self.vocab])
      p = u / bigZ
      self.probDP[(x, y, z)] = p
      return p

    else:
      sys.exit("%s has some weird value" % self.smoother)


  def computeAlphaXY(self, x, y):
    otherProb = sum([self.discProbXYZ(x, y, zp) for zp in self.vocab if self.tokens.get((x, y, zp), 0) > 0])
    if otherProb > 1:
      sys.stderr.write("computeAlphaXY err: otherProb = %f" % otherProb)
    return (1 - otherProb) / (1 - sum([self.wbProbYZ(y, zp) for zp in self.vocab if self.tokens.get((x, y, zp), 0) > 0]))

  def computeAlphaY(self, y):
    otherProb = sum([self.discProbYZ(y, zp) for zp in self.vocab if self.tokens.get((y, zp), 0) > 0])
    if otherProb > 1:
      sys.stderr.write("computeAlphaY err: otherProb = %f" % otherProb)
    return (1 - otherProb) / (1 - sum([self.wbProbZ(zp) for zp in self.vocab if self.tokens.get((y, zp), 0) > 0]))

  def computeAlpha(self):
    otherProb = sum([self.discProbZ(zp) for zp in self.vocab if self.tokens.get(zp, 0) > 0])
    if otherProb > 1:
      sys.stderr.write("computeAlpha err: otherProb = %f" % otherProb)

    # deal with 0/0 situation
    if self.vocab_size - self.types_after[''] == 0:
      self.vocab.add("#FUTUREWORD#")
      self.vocab_size += 1

    return (1 - otherProb) / (self.vocab_size - self.types_after[''])

  def discProbXYZ(self, x, y, z):
    return float(self.tokens.get((x, y, z), 0)) / (self.tokens.get((x, y), 0) + self.types_after.get((x, y), 0))

  def discProbYZ(self, y, z):
    return float(self.tokens.get((y, z), 0)) / (self.tokens.get(y, 0) + self.types_after.get(y, 0))

  def discProbZ(self, z):
    return float(self.tokens.get(z, 0)) / (self.tokens.get('', 0) + self.types_after.get('', 0))  # extract 1 for OOV in self.vocab

  def wbProbXYZ(self, x, y, z):
    if (x, y, z) in self.wbProbDP:
      return self.wbProbDP[(x, y, z)]

    if self.tokens.get((x, y, z), 0) > 0:
      self.wbProbDP[(x, y, z)] = self.discProbXYZ(x, y, z)
      return self.wbProbDP[(x, y, z)]
    else:
      if (x, y) in self.alphaNorm:
        tmpAlpha = self.alphaNorm[(x, y)]
      else:
        tmpAlpha = self.computeAlphaXY(x, y)
        self.alphaNorm[(x, y)] = tmpAlpha
      self.wbProbDP[(x, y, z)] = tmpAlpha * self.wbProbYZ(y, z)
      return self.wbProbDP[(x, y, z)]

  def wbProbYZ(self, y, z):
    if (y, z) in self.wbProbDP:
      return self.wbProbDP[(y, z)]

    if self.tokens.get((y, z), 0) > 0:
      self.wbProbDP[(y, z)] = self.discProbYZ(y, z)
      return self.wbProbDP[(y, z)]
    else:
      if y in self.alphaNorm:
        tmpAlpha = self.alphaNorm[y]
      else:
        tmpAlpha = self.computeAlphaY(y)
        self.alphaNorm[y] = tmpAlpha
      self.wbProbDP[(y, z)] = tmpAlpha * self.wbProbZ(z)
      return self.wbProbDP[(y, z)]

  def wbProbZ(self, z):
    if z in self.wbProbDP:
      return self.wbProbDP[z]

    if self.tokens.get(z, 0) > 0:
      self.wbProbDP[z] = self.discProbZ(z)
      return self.wbProbDP[z]
    else:
      if "allAlpha" in self.alphaNorm:
        tmpAlpha = self.alphaNorm["allAlpha"]
      else:
        tmpAlpha = self.computeAlpha()
        self.alphaNorm["allAlpha"] = tmpAlpha
      self.wbProbDP[z] = tmpAlpha
      return tmpAlpha

  def testWB(self):

    self.computeAlpha()
    vocabList = list(self.vocab)

    for x in vocabList:
      for y in vocabList:
        prob = 0
        for z in vocabList:
          prob += self.wbProbXYZ(x, y, z)
        print prob

    for y in vocabList:
      prob = 0
      for z in vocabList:
        prob += self.wbProbYZ(y, z)
      print prob

    prob = 0
    for z in vocabList:
      prob += self.wbProbZ(z)
    print prob

  def calculateU(self, x, y, z):
    if (x, y, z) in self.logProb:
      return self.logProb[(x, y, z)]

    vecX = self.vectors.get(x, self.vectors[OOL])
    vecY = self.vectors.get(y, self.vectors[OOL])
    vecZ = self.vectors.get(z, self.vectors[OOL])
    p = math.exp(vecX.T.dot(self.U).dot(vecZ) + vecY.T.dot(self.V).dot(vecZ))
    self.logProb[(x, y, z)] = p
    return p

  def filelogprob(self, filename):
    """Compute the log probability of the sequence of tokens in file.
    NOTE: we use natural log for our internal computation.  You will want to
    divide this number by log(2) when reporting log probabilities.
    """
    logprob = 0.0
    x, y = BOS, BOS
    corpus = self.open_corpus(filename)
    for line in corpus:
      for z in line.split():
        prob = self.prob(x, y, z)
        logprob += math.log(prob)
        x = y
        y = z
    logprob += math.log(self.prob(x, y, EOS))
    corpus.close()
    return logprob

  def speechRec(self, filename):
    wordNum, candSents = self.readSpeechFile(filename)
    bestSentScore = -sys.maxint
    bestSent = None
    for candSent in candSents:  
      fTheta = self.calculateFTheta(candSent.sent)
      if fTheta + candSent.loglinear > bestSentScore:
        bestSentScore = fTheta + candSent.loglinear
        bestSent = candSent

    print "%.3f %s" % (bestSent.errRate, filename.split("/")[-1]) # + "\\\\"
    return wordNum, wordNum * bestSent.errRate

  def readSpeechFile(self, filename):
    f = open(filename, "r")
    wordNum = int(f.readline().split()[0])
    return wordNum, map(self.parseSpeechCandSent, f.readlines())
    
  def parseSpeechCandSent(self, line):
    data = line.split()
    return SpeechCandidate(float(data[0]), float(data[1]), data[3:])

  def read_vectors(self, filename):
    """Read word vectors from an external file.  The vectors are saved as
    arrays in a dictionary self.vectors.
    """
    print filename
    with open(filename) as infile:
      header = infile.readline()
      self.dim = int(header.split()[-1])
      self.vectors = {}
      for line in infile:
        arr = line.split()
        word = arr.pop(0)
        self.vectors[word] = np.array([float(x) for x in arr]).reshape(self.dim, 1)

  def train(self, filename):
    """Read the training corpus and collect any information that will be needed
    by the prob function later on.  Tokens are whitespace-delimited.

    Note: In a real system, you wouldn't do this work every time you ran the
    testing program. You'd do it only once and save the trained model to disk
    in some format.
    """
    sys.stderr.write("Training from corpus %s\n" % filename)

    # Clear out any previous training
    self.tokens = {}
    self.types_after = {}
    self.bigrams = []
    self.trigrams = []
    self.probDP = {}    # clean before training
    self.wbProbDP = {}
    self.alphaNorm = {}

    # While training, we'll keep track of all the trigram and bigram types
    # we observe.  You'll need these lists only for Witten-Bell backoff.
    # The real work:
    # accumulate the type and token counts into the global hash tables.

    # If vocab size has not been set, build the vocabulary from training corpus
    if self.vocab_size is None:
      self.set_vocab_size(filename)

    # We save the corpus in memory to a list tokens_list.  Notice that we
    # appended two BOS at the front of the list and a EOS at the end.  You
    # will need to add more BOS tokens if you want to use a longer context than
    # trigram.
    x, y = BOS, BOS  # Previous two words.  Initialized as "beginning of sequence"
    # count the BOS context
    self.tokens[(x, y)] = 1
    self.tokens[y] = 1
    self.types_after[x] = 1
    self.types_after[''] = 1
    self.bigrams.append((x, y))

    tokens_list = [x, y]  # the corpus saved as a list
    corpus = self.open_corpus(filename)

    for line in corpus:
      for z in line.split():
        # substitute out-of-vocabulary words with OOV symbol
        if z not in self.vocab:
          z = OOV
        # substitute out-of-lexicon words with OOL symbol (only for log-linear models)
        if self.smoother == 'LOGLINEAR' and z not in self.vectors:
          z = OOL
        self.count(x, y, z)
        self.show_progress()
        x=y; y=z
        tokens_list.append(z)
    tokens_list.append(EOS)   # append a end-of-sequence symbol 
    sys.stderr.write('\n')    # done printing progress dots "...."
    self.count(x, y, EOS)     # count EOS "end of sequence" token after the final context


    corpus.close()

    self.N = len(tokens_list) - 2  # number of training instances, excluding BOS, but including EOS

    self.computeAlpha()
    if self.smoother == 'LOGLINEAR': 
      # Train the log-linear model using SGD.

      # Initialize parameters
      self.U = np.zeros((self.dim, self.dim))
      self.V = np.zeros((self.dim, self.dim))

      # Optimization parameters
      gamma0 = 0.1  # initial learning rate, used to compute actual learning rate
      epochs = 10  # number of passes

      # ******** COMMENT *********
      # In log-linear model, you will have to do some additional computation at
      # this point.  You can enumerate over all training trigrams as following.
      #
      # for i in range(2, len(tokens_list)):
      #   x, y, z = tokens_list[i - 2], tokens_list[i - 1], tokens_list[i]
      #
      # Note1: self.lambdap is the regularizer constant C
      # Note2: You can use self.show_progress() to log progress.
      #
      # **************************

      sys.stderr.write("Start optimizing.\n")

      #####################
      # TODO: Implement your SGD here
      #####################
      updateTimes = 0
      OOLVec = self.vectors[OOL]
      for epoch in range(epochs):
        for i in range(2, len(tokens_list)):   # loop over summands of (21)
          self.show_progress()

          self.probDP = {}
          self.logProb = {}

          # update gamma
          gamma = gamma0 / (1.0 + gamma0 * updateTimes * self.lambdap / self.N)

          # update self.U, self.V
          x, y, z = tokens_list[i - 2], tokens_list[i - 1], tokens_list[i]
          vecX = self.vectors.get(x, OOLVec)
          vecY = self.vectors.get(y, OOLVec)
          vecZ = self.vectors.get(z, OOLVec)

          sumZP = np.zeros((self.dim, 1))
          for zp in self.vocab:
            sumZP += self.prob(x, y, zp) * self.vectors.get(zp, OOLVec)

          partialDeU = vecX.dot(vecZ.T) - vecX.dot(sumZP.T) - (2.0 * self.lambdap * self.U) / self.N
          partialDeV = vecY.dot(vecZ.T) - vecY.dot(sumZP.T) - (2.0 * self.lambdap * self.V) / self.N

          self.U += gamma * partialDeU
          self.V += gamma * partialDeV

          updateTimes += 1
        print ""
        print "epoch %d: F=%f" % (epoch + 1, self.calculateFTheta(tokens_list))


    sys.stderr.write("Finished training on %d tokens\n" % self.tokens[""])

  def calculateFTheta(self, tokenList):
    self.probDP = {}
    self.logProb = {}
    tokenList.insert(0, BOS)
    tokenList.insert(0, BOS)
    tokenList.append(EOS)
    fTheta = 0
    for i in range(2, len(tokenList)):
      x, y, z = tokenList[i - 2], tokenList[i - 1], tokenList[i]
      fTheta += math.log(self.prob(x, y, z)) / math.log(2)
    if self.smoother == "LOGLINEAR":
      fTheta -= (np.sum(np.square(self.U)) + np.sum(np.square(self.V))) * self.lambdap
    return fTheta

  def count(self, x, y, z):
    """Count the n-grams.  In the perl version, this was an inner function.
    For now, I am just using a class variable to store the found tri-
    and bi- grams.
    """
    self.tokens[(x, y, z)] = self.tokens.get((x, y, z), 0) + 1
    if self.tokens[(x, y, z)] == 1:       # first time we've seen trigram xyz
      self.trigrams.append((x, y, z))
      self.types_after[(x, y)] = self.types_after.get((x, y), 0) + 1

    self.tokens[(y, z)] = self.tokens.get((y, z), 0) + 1
    if self.tokens[(y, z)] == 1:        # first time we've seen bigram yz
      self.bigrams.append((y, z))
      self.types_after[y] = self.types_after.get(y, 0) + 1

    self.tokens[z] = self.tokens.get(z, 0) + 1
    if self.tokens[z] == 1:         # first time we've seen unigram z
      self.types_after[''] = self.types_after.get('', 0) + 1 

    self.tokens[''] = self.tokens.get('', 0) + 1  # the zero-gram


    # self.tcount[(x, y)].add(z)
    # self.tcount[y].add(z)

    # for Witten-Bell backoff
    # if y != OOV:
    #   self.tcount[y].add((y, z))
    #   if x != OOV and z != OOV:
    #     self.tcount[(x, y)].add((x, y, z))

  def set_vocab_size(self, *files):
    """When you do text categorization, call this function on the two
    corpora in order to set the global vocab_size to the size
    of the single common vocabulary.

    NOTE: This function is not useful for the loglinear model, since we have
    a given lexicon.
     """
    count = {}  # count of each word

    for filename in files:
      corpus = self.open_corpus(filename)
      for line in corpus:
        for z in line.split():
          count[z] = count.get(z, 0) + 1
          self.show_progress()
      corpus.close()

    self.vocab = set(w for w in count if count[w] >= OOV_THRESHOLD)

    self.vocab.add(OOV)  # add OOV to vocab
    self.vocab.add(EOS)  # add EOS to vocab (but not BOS, which is never a possible outcome but only a context)

    sys.stderr.write('\n')    # done printing progress dots "...."

    if self.vocab_size is not None:
      sys.stderr.write("Warning: vocab_size already set; set_vocab_size changing it\n")
    self.vocab_size = len(self.vocab)
    sys.stderr.write("Vocabulary size is %d types including OOV and EOS\n"
                      % self.vocab_size)

  def set_smoother(self, arg):
    """Sets smoother type and lambda from a string passed in by the user on the
    command line.
    """
    # r = re.compile('^(.*?)-?([0-9.]*)$')
    r = re.compile('^(.*?)-?([0-9.]*)-?([a-zA-Z]*)$')
    m = r.match(arg)

    if not m.lastindex:
      sys.exit("Smoother regular expression failed for %s" % arg)
    else:
      smoother_name = m.group(1)
      if m.lastindex >= 2 and len(m.group(2)):
        lambda_arg = m.group(2)
        self.lambdap = float(lambda_arg)
      else:
        self.lambdap = None
      if m.lastindex >= 3 and len(m.group(3)):
        self.ngram = m.group(3)

      if self.lambdap is None:
        smoother_name += self.ngram
        self.ngram = ""

    if smoother_name.lower() == 'uniform':
      self.smoother = "UNIFORM"
    elif smoother_name.lower() == 'add':
      self.smoother = "ADDL"
    elif smoother_name.lower() == 'backoff_add':
      self.smoother = "BACKOFF_ADDL"
    elif smoother_name.lower() == 'backoff_wb':
      self.smoother = "BACKOFF_WB"
    elif smoother_name.lower() == 'loglinear':
      self.smoother = "LOGLINEAR"
    else:
      sys.exit("Don't recognize smoother name '%s'" % smoother_name)
    
    if self.lambdap is None and self.smoother.find('ADDL') != -1:
      sys.exit('You must include a non-negative lambda value in smoother name "%s"' % arg)

  def open_corpus(self, filename):
    """Associates handle CORPUS with the training corpus named by filename."""
    try:
      corpus = file(filename, "r")
    except IOError, err:
      try:
        corpus = file(DEFAULT_TRAINING_DIR + filename, "r")
      except IOError, err:
        sys.exit("Couldn't open corpus at %s or %s" % (filename,
                 DEFAULT_TRAINING_DIR + filename))
    return corpus

  def show_progress(self, freq=10000):
    """Print a dot to stderr every 5000 calls (frequency can be changed)."""
    self.progress += 1
    if self.progress % freq == 1:
      self.progress = 1
      sys.stderr.write('.')

class SpeechCandidate(object):

  def __init__(self, errRate, loglinear, sent):
      self.errRate = errRate
      self.loglinear = loglinear
      self.sent = sent

  
if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('smoother', type=str, help='smoother')
    parser.add_argument('lexicon', type=str, help='training lexicon')
    parser.add_argument('corpus', type=str, help='training corpus')
    parser.add_argument('testing', nargs='*', type=str, default=[], help="testing data")

    args = parser.parse_args()
    smoother = args.smoother
    lexicon = args.lexicon
    corpus = args.corpus
    testingFiles = args.testing

    lm = LanguageModel()
    lm.set_smoother(smoother)
    lm.read_vectors(lexicon)
    lm.set_vocab_size(corpus)
    lm.train(corpus)
    #
    result = map(lm.speechRec, testingFiles)
    errData = reduce(lambda x, y: (x[0] + y[0], x[1] + y[1]), result)

    print "%.3f %s" % (float(errData[1]) / errData[0], "OVERALL")

    # lm.testWB()   # test for sum to 1

    # preScore = sys.maxint
    # tryTime = 100
    # bestLambda = None
    # while lm.lambdap > 0.0001:
    #   result = map(lm.speechRec, testingFiles)
    #   errData = reduce(lambda x, y: (x[0] + y[0], x[1] + y[1]), result)
    #   print "%.3f %s, %f" % (errData[1] / errData[0], "OVERALL", lm.lambdap)
    #   if errData[1] / errData[0] < preScore:
    #     preScore = errData[1] / errData[0]
    #     bestLambda = lm.lambdap
    #   lm.lambdap *= 0.99
    # print "best lambda:", lm.lambdap