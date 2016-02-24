#!/usr/bin/python

# Sample program for hw-lm
# CS465 at Johns Hopkins University.

# Converted to python by Eric Perlman <eric@cs.jhu.edu>

# Updated by Jason Baldridge <jbaldrid@mail.utexas.edu> for use in NLP
# course at UT Austin. (9/9/2008)

# Modified by Mozhi Zhang <mzhang29@jhu.edu> to add the new log linear model
# with word embeddings.  (2/17/2016)

import math
import sys

import Probs

# Computes the log probability of the sequence of tokens in file,
# according to a trigram model.  The training source is specified by
# the currently open corpus, and the smoothing method used by
# prob() is specified by the global variable "smoother".

def main():
  course_dir = '../All_Training'
  argv = sys.argv[1:]

  if len(argv) < 4:
    sys.exit(1)

  smoother = argv.pop(0)
  lexicon = argv.pop(0)
  train_file1 = argv.pop(0)
  train_file2 = argv.pop(0)

  if not argv:
    print "warning: no input files specified"



  lm = Probs.LanguageModel()
  lm.set_smoother(smoother)
  lm.read_vectors(lexicon)
  lm.set_vocab_size(train_file1, train_file2)
  lm.train(train_file1)

  # We use natural log for our internal computations and that's
  # the kind of log-probability that fileLogProb returns.
  # But we'd like to print a value in bits: so we convert
  # log base e to log base 2 at print time, by dividing by log(2).

  result1 = []
  result2 = []
  files = []
  for testfile in argv:
    files.append(testfile)
    result1.append(lm.filelogprob(testfile) / math.log(2))

  lm.train(train_file2)
  for testfile in argv:
    result2.append(lm.filelogprob(testfile) / math.log(2))

  type1 = type2 = 0
  for i in range(0,len(result1)):
    if result1[i] > result2[i]:
      type1 += 1
      print train_file1, '\t', files[i]
    else:
      type2 += 1
      print train_file2, '\t', files[i]


  print '%d looked more like %s (%.2f%%)'%(type1, train_file1, type1*100/(type1+type2))
  print '%d looked more like %s (%.2f%%)'%(type2, train_file2, type2*100/(type1+type2))

if __name__ ==  "__main__":
  main()
