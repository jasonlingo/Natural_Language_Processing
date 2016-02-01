import sys
import getopt
from collections import defaultdict
import random
import pprint

class SentenceGenerator(object):
    """
    This is a sentence generator. It parses the given grammar and vocabulary list and then
    generate a sentence by search the matched grammar pattern until a terminal is reached.
    This class can output the generated sentences to a plain text or a tree structure.
    """

    def __init__(self, filename):
        """
        Initialize needed dictionaries and parse the given file.
        :param filename: (string) the address of the grammar list
        """
        self.grammar = defaultdict(list)    # store the grammar and vocabulary
        self.weight = defaultdict(float)    # store the weight for each single item
        self.weightSum = defaultdict(int)   # store the summation of the weight that belongs to the same grammar key
        self.parseFile(filename)

    def parseFile(self, filename):
        """
        Parse the grammar file and store the result to the related dictionaries.
        The file starts with grammar list and then the vocabulary list.
        :param filename: (string)
        """
        grammarFlag = True
        with open(filename, 'r') as f:
            inlines = [line.rstrip() for line in f.readlines()]
            lines = [line for line in inlines if line]  # eliminate blank line
            for line in lines:
                words = line.split()

                if "Vocabulary." in words:  # switch to vocabulary mode
                    grammarFlag = False

                if "#" in words[0]: continue  # skip comments

                key = words[1]
                rule = []
                for w in words[2:]:
                    if w == "#": break  # skip comments
                    rule.append(w)
                if grammarFlag:
                    rule = tuple(rule)
                else:
                    rule = " ".join(rule)

                self.grammar[key].append(rule)
                self.weight[(key, rule)] = float(words[0])  # record the weight for the paired grammar key and rule
                self.weightSum[key] += float(words[0])      # record the summation of the weight of the same grammar key

    def generate(self, toTree, num):
        """
        Generate random sentences and output the results.
        :param toTree: (boolean) indicates the output type
        :param num: (int) the total number of sentences to be generated
        """
        sentences = []

        while num > 0:
            words = []
            root = self.weightedRandomChoice("ROOT")
            for w in root:
                subSentence = self.findSubSentence(toTree, w)
                if toTree:
                    words.append((w, subSentence))
                else:
                    words.extend(subSentence)
            if toTree:
                sentence = ("ROOT", tuple(words))
            else:
                sentence = " ".join(words)

            if sentence not in sentences:  # prevent storing duplicated sentence
                sentences.append(sentence)
                num -= 1

        # print all the generated sentences
        if toTree:
            for sentence in sentences:
                self.printTree(sentence, 0)
                # pprint.pprint(sentence, indent=2)
        else:
            for sentence in sentences:
                print sentence

    def findSubSentence(self, toTree, keyword):  #FIXME: under the default grammar weight, this could exceed the maximal depth
        """
        Find a possible sub-sentence according to the given keyword, e.g. NP and PP, and
        return the result.
        :param keyword: grammar type
        :return: (tuple)
        """
        words = []
        subGrammar = self.grammar.get(keyword, None)

        if not subGrammar:  # cannot find sub grammar, and this means the keyword is a terminal
            if toTree:
                return keyword
            else:
                return [keyword]
        else:
            subGrammar = self.weightedRandomChoice(keyword)

        if type(subGrammar) is str:  # if the subGrammar is a single string, this means the subGrammar is a vocabulary
            if toTree:
                return subGrammar
            else:
                return [subGrammar]

        for sg in subGrammar:  # for each sub-grammar, find the possible combinations
            subSent = self.findSubSentence(toTree, sg)
            if toTree:
                words.append((sg, subSent))
            else:
                words.extend(subSent)

        return tuple(words)

    def weightedRandomChoice(self, keyword):
        """
        Randomly choose the grammar by the weighted probability.
        :param keyword: the grammar key
        :return: the chosen sub-grammar
        """
        rules = self.grammar[keyword]
        total = self.weightSum[keyword]
        randomNum = random.uniform(0, total)
        cumulate = 0
        for rule in rules:
            cumulate += self.weight.get((keyword, rule), 0)
            if cumulate >= randomNum:
                return rule
        print "Err: cannot find the rule in weightedRandomChoice()"
        return

    @classmethod
    def printTree(cls, sentence, indent):
        """
        Print the sentence as a tree structure.
        :param sentence: multi-layer list
        :param indent: (int) the space before the printed words
        """
        if not sentence:
            return

        print "(" + str(sentence[0]),

        # it is a leaf, so change the line after print the current grammar
        if type(sentence[1]) is str:
            print sentence[1] + ")"
            return

        # for every child, print the child recursively
        for i in range(len(sentence[1])):
            cls.printTree(sentence[1][i], indent + 2 + len(sentence[0]))
            if i < len(sentence[1]) - 1:
                print " " * (indent + 1 + len(sentence[0])),


def parseArgs(args):
    """
    Parse the arguments:
        opts: indicate whether the sentence generator will output the sentences as a tree structure.
              "-t": output tree structure
              None: output plain text
        filename: the address of the grammar file
        sentenceNum: total number of sentences to be generated
    :param args:
    :return: opts, filename, sentenceNum
    """
    try:
        opts, args = getopt.getopt(args, "t", [])
    except getopt.GetoptError:
        print "Option should be '-t' before other arguments"
        sys.exit(1)

    try:
        filename = args[0]
    except IndexError:
        print "Missing argument: file name of grammar!"
        sys.exit(1)

    try:
        sentenceNum = int(args[1])
    except IndexError:
        sentenceNum = 1
    except ValueError:
        print "Wrong format for an argument: sentence number! Set the argument to 1 (default)"
        sentenceNum = 1

    return opts, filename, sentenceNum


if __name__ == "__main__":
    opts, filename, sentenceNum = parseArgs(sys.argv[1:])

    optKey = [opt[0] for opt in opts]
    if "-t" in optKey:
        toTree = True
    else:
        toTree = False

    # create a SentenceGenerator object and generate sentences
    sentGen = SentenceGenerator(filename)
    sentGen.generate(toTree, sentenceNum)

