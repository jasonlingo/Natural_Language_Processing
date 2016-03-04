exec 2>/dev/null
for C in 7.8 7.9 8.1 8.2 8.3 8.4 8.5 8.6 8.7 8.8 8.9 9.1 9.2 9.3 9.4
  do
    printf "C is $C "
    test1=$(./textcat.py loglinear$C ../lexicons/chars-10.txt en.1K sp.1K ../english_spanish/dev/english/*/* | cut -d ' ' -f 1)
    correct1=$(echo $test1 | cut -d ' ' -f 1)
    incorrect1=$(echo $test1 | cut -d ' ' -f 2)
    #printf "$correct1 $incorrect1  fuck\n"
    test2=$(./textcat.py loglinear$C ../lexicons/chars-10.txt en.1K sp.1K ../english_spanish/dev/spanish/*/* | cut -d ' ' -f 1)
    correct2=$(echo $test2 | cut -d ' ' -f 2)
    incorrect2=$(echo $test2 | cut -d ' ' -f 1)
    
    correct=$(($correct1 + $correct2))
    incorrect=$(($incorrect1 + $incorrect2))
    # echo "$correct fuck $incorrect \n"
    # echo  $($correct / ($correct + $incorrect)
    total=$(($correct + $incorrect))
    echo "$total fuck"
    result=$(echo "scale=4; $incorrect/$total" | bc -l)
    printf "result is $result \n"
    printf "=======================\n"
  done

