#!/usr/bin/python
import argparse
import math
import numpy as np

class FindSim(object):
  # Get the vector of a word from the file
  def get_vector(self, file, word):
    f = open(file, 'r')
    for line in f:
      split = line.split('\t')
      if word == split[0]:
        vec = []
        for i in range(1, len(split)):
          vec.append(float(split[i]))
        return vec
    return None
     
  def cos_sim(self, vec1, vec2):
    if not len(vec1) == len(vec2):
      raise ValueError("Wrong vector input")
    numerator = 0
    v1_square_sum = v2_square_sum = 0
    for i in range(0, len(vec1)):
      numerator += vec1[i]*vec2[i]
      v1_square_sum += (vec1[i] * vec1[i])
      v2_square_sum += (vec2[i] * vec2[i])
    denominator = math.sqrt(v1_square_sum)* math.sqrt(v2_square_sum) 
    return numerator/denominator

  def findsim(self, file, word_vec, words):
    f = open(file,'r')
    result = []
    # word_vec = self.get_vector(file, word)
    # print word_vec
    for line in f:
      vector = []
      split = line.split('\t')
      if len(split) < 2 or split[0] in words:
        continue

      # Build the vector of current line
      for i in range(1,len(split)):
        vector.append(float(split[i]))

      # Fill in the result list if not full
      if len(result) < 5:
        result.append((split[0], self.cos_sim(word_vec, vector)))
        continue

      # Calculate the cosine similarity with the target word
      sim = self.cos_sim(word_vec, vector)

      # Find the tuple with the minimum cosine similarity in the result list
      min_sim = result[0][1]
      min_index = 0
      for i in range(0, len(result)):
        if result[i][1] < min_sim:
          min_sim = result[i][1]
          min_index = i

      # Replace the tuple with minimum cosine similarity
      if sim > min_sim: 
        del result[min_index]
        result.append((split[0],sim)) 

    return sorted(result, key=lambda x:x[1], reverse=True)


def main(): 
  #Get the parameteres from the command line
  parser = argparse.ArgumentParser()
  parser.add_argument('file', help='lexicon file')
  parser.add_argument('word1', help='choose a word')
  parser.add_argument('word2',nargs='?', default=None)
  parser.add_argument('word3',nargs='?', default=None)


  args = parser.parse_args()
  lexicon_file = args.file

  words = []
  words.append(args.word1)
  if args.word2:
    words.append(args.word2)
  if args.word3:
    words.append(args.word3)

  fs = FindSim()

  if len(words) > 2:
    # Use numpy to make vector calculation
    vector = np.array(fs.get_vector(lexicon_file, words[0])) - \
             np.array(fs.get_vector(lexicon_file,words[1])) + np.array(fs.get_vector(lexicon_file, words[2]))
    results =  fs.findsim(lexicon_file, vector, words)
  else:
    results =  fs.findsim(lexicon_file, fs.get_vector(lexicon_file, words[0]), words)

  for i in range(0, len(results)):
    print results[i][0]
 
if __name__ == "__main__":
  main()
  
