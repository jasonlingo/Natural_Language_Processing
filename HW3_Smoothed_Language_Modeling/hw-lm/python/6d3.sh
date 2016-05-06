for C in 8
  do
    for len in 10 20 40
      do
        printf "lexicon length: $len\n"
          for train_len in 1K 2K 5K 20K
            do
              printf "train length is $train_len"
              ./textcat.py loglinear$C ../lexicons/chars-$len.txt en.$train_len sp.$train_len ../english_spanish/test/english/*/*
              ./textcat.py loglinear$C ../lexicons/chars-$len.txt en.$train_len sp.$train_len ../english_spanish/test/spanish/*/*
            done
        printf "============================\n"
      done
  done

