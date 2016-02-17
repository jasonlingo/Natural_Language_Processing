import numpy as np
import argparse
import math
import sys


class FindSim(object):

    def __init__(self, wordFile):
        """
        :param wordFile: the file of words' lexicons
        """
        self.lexiVector = {}  # keep all the vector value for each word
        self.loadFile(wordFile)

    def loadFile(self, filename):
        """
        Read lexicon data from filename and convert the vector value into a np.array and then store the
        result in the self.vector dictionary.
        :param filename: lexicon file
        """
        f = open(filename, 'r')
        dataset = f.readlines()
        for data in dataset[1:]:
            d = data.split()
            self.lexiVector[d[0]] = np.array(d[1:], dtype=float)

    def findsim(self, word, wordNum=5):
        order = [(None, -sys.maxint) for _ in range(wordNum)]
        targetVec = self.lexiVector[word]

        for w in self.lexiVector:
            if w != word:
                sim = self.cosineSim(targetVec, self.lexiVector[w])
                order = self.updateOrder(order, sim, w)
        print "-----------------------------"
        print "Top", wordNum, "similar words to '" + word + "'"
        for word, sim in order:
            print word, sim

    def updateOrder(self, order, newSim, newWord):
        ordNum = len(order)
        insertOrd = -1
        for i in range(ordNum):
            if order[i][1] < newSim:
                insertOrd = i
                break
        if insertOrd >= 0:
            order = order[:insertOrd] + [(newWord, newSim)] + order[insertOrd:ordNum-1]
        return order


    def cosineSim(self, vec1, vec2):
        """
        Compute the cosine similarity, which is the cosine of the angle theta between the two vectors.
        :param vec1: vector of word 1
        :param vec2: vector of word 2
        :return: cosine similarity [-1, 1]
        """
        return np.dot(vec1, vec2) / (math.sqrt(np.dot(vec1, vec1)) * math.sqrt(np.dot(vec2, vec2)))




if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('file', default=None, help='lexicons file')
    parser.add_argument('word', default=None, help='input word')

    args = parser.parse_args()
    lexiconFile = args.file
    word = args.word

    fs = FindSim(lexiconFile)
    fs.findsim(word)