# python python/textcat.py add1 lexicons/chars-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/dev/english/*/*
# python python/textcat.py loglinear1 lexicons/chars-10.txt All_Training/sp.1K 

# python python/textcat.py loglinear1 lexicons/chars-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/dev/english/*/*
# python python/textcat.py loglinear1 lexicons/chars-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/dev/spanish/*/*

#./textcat.py add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/spanish/*/*
#./textcat.py add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/english/*/*


# python python/fileprob.py loglinear1 lexicons/chars-10.txt All_Training/sp.1K


# Qustion 8
# python python/speechrec.py add0.01unigram lexicons/words-10.txt All_Training/switchboard speech/test/unrestricted/*


# Question 9
# # python python/speechrec.py backoff_wb lexicons/words-10.txt All_Training/switchboard speech/test/easy/*


# Question 10
# python python/textcat2.py backoff_wb lexicons/chars-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/test/english/*/*
# python python/textcat2.py backoff_wb lexicons/chars-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/test/spanish/*/*


python python/textcat2.py backoff_wb /usr/local/data/cs465/hw-lm/lexicons/chars-10.txt /usr/local/data/cs465/hw-lm/All_Training/en.1K /usr/local/data/cs465/hw-lm/All_Training/sp.1K /usr/local/data/cs465/hw-lm/english_spanish/test/english/*/*