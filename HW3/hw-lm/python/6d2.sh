#exec 2>/dev/null
for C in 8
  do
    for len in 10 20 40
    do
    printf "current lexicon length is $len \n"
      for tran_len in 1K 2K 5K 10K 20K
        do
          printf "$tran_len \n" 
          test1=$(./textcat.py loglinear$C ../lexicons/chars-$len.txt en.$tran_len sp.$tran_len ../english_spanish/test/english/*/* | cut -d ' ' -f 1)
          correct1=$(echo $test1 | cut -d ' ' -f 1)
          incorrect1=$(echo $test1 | cut -d ' ' -f 2)
          printf "$correct1  fuck $incorrect1 \n"
          test2=$(./textcat.py loglinear$C ../lexicons/chars-$len.txt en.$tran_len sp.$tran_len ../english_spanish/test/spanish/*/* | cut -d ' ' -f 1)
          correct2=$(echo $test2 | cut -d ' ' -f 2)
          incorrect2=$(echo $test2 | cut -d ' ' -f 1)
          printf "$correct2 fuck2 $incorrect2 \n"
   
          correct=$(($correct1 + $correct2))
          incorrect=$(($incorrect1 + $incorrect2))
          total=$(($correct + $incorrect))
          result=$(echo "scale=4; $incorrect/$total" | bc -l)
          printf "result is $result \n"
          printf "=======================\n"
        done
    done
  done

