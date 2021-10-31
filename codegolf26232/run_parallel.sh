#!/bin/bash


## function to execute Colorfill in codegolf26232 solver mode on a part of the input file
run_part () {
    part=$1
    rm -rf part_$part
    mkdir  part_$part
    cd     part_$part
    mv ../floodtest_$part .
    (
        date -Iseconds
        echo
        time java -Xms6G -Xmx6G -jar ../../colorfill.jar floodtest_$part
        echo
        date -Iseconds
        echo
        time java -jar ../../colorfill.jar floodtest_$part steps.txt
        echo
        date -Iseconds
        echo
        echo ----- end of part $part
        echo
    ) 1>steps_.txt 2>&1  &
    cd ..
}



## main
SECONDS=0

## split input file into 8 parts of 250000 lines = 12500 tasks each: floodtest_a ... floodtest_h
split --number=8 --suffix-length=1 floodtest floodtest_
parts="a b c d e f g h"

## run the process for each part of the input file
for i in $parts
do
    run_part $i
done

date -Iseconds
echo processes started, now waiting...
wait


## gather results
rm -f steps.txt
rm -f steps_.txt
for i in $parts
do
    cat part_$i/steps.txt  >> steps.txt
    cat part_$i/steps_.txt >> steps_.txt
done


## verify all solutions
{
    date -Iseconds
    echo
    time java -jar ../colorfill.jar floodtest steps.txt
    echo
    date -Iseconds
} 1>>steps_.txt 2>&1


duration=$SECONDS
date -Iseconds
echo done!  $(($duration / 60)) minutes and $(($duration % 60)) seconds elapsed. | tee -a steps_.txt

