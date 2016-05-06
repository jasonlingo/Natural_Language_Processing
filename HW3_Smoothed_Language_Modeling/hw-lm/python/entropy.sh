#./python/textcat.py backoff_add2.7 lexicons/words-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/test/english/*/*
#./python/fileprob.py backoff_add2.7 lexicons/words-10.txt All_Training/en.1K All_Training/sp.1K english_spanish/test/spanish/*/*

#./fileprob.py loglinear1 ../lexicons/chars-10.txt ../All_Training/sp.1K
# for problem #4
#./textcat.py add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/test/english/*/*
#./textcat.py add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/test/spanish/*/*

# for problem #5
#./textcat.py backoff_add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/english/*/*
#./textcat.py backoff_add2.7 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/spanish/*/*
#printf "============================"
#./textcat.py backoff_add10000000000000 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/english/*/*
#./textcat.py backoff_add10000000000000 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/dev/spanish/*/*

printf "============================"
#./textcat.py backoff_add0.0001 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/test/english/*/*
#./textcat.py backoff_add0.0001 ../lexicons/words-10.txt en.1K sp.1K ../english_spanish/test/spanish/*/*

printf "problem 6\n"
for C in 0.05 0.1 0.5 1 2
  do
    printf "C is $C "
    #for len in 10 20 50 100 200 500
      #do
       # printf "the lenght is $len \n"
        ./fileprob.py loglinear$C ../lexicons/chars-10.txt en.1K ../english_spanish/dev/english/length-10/en.10.00 ../english_spanish/dev/english/length-20/en.20.00 ../english_spanish/dev/english/length-50/en.50.00 ../english_spanish/dev/english/length-100/en.100.00 ../english_spanish/dev/english/length-200/en.200.00 ../english_spanish/dev/english/length-500/en.500.00
     # done 
    printf "\n"
    
  done
#./textcat.py loglinear0.1 ../lexicons/chars-10.txt en.1K sp.1K ../english_spanish/test/english/*/*
#./textcat.py loglinear0.1 ../lexicons/chars-10.txt en.1K sp.1K ../english_spanish/test/spanish/*/*
