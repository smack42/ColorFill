#!/bin/bash

run_benchmark () {
    {
        date -Iseconds
        echo
        time java -showversion -Xms6G -Xmx6G -jar ../colorfill.jar -benchmark "$1" "$2"
        echo
        date -Iseconds
        echo
        time java -showversion -jar ../colorfill.jar "$1" "$1_solution_$2.txt"
        echo
        date -Iseconds
    } 2>&1 | tee "$1_solution_$2_details.txt"
    {
        head -1                  "$1_solution_$2_details.txt"
        grep "Runtime" -i -m1    "$1_solution_$2_details.txt"
        grep "ColorFill" -m1     "$1_solution_$2_details.txt"
        grep "milliSeconds\_min" "$1_solution_$2_details.txt"
        echo
    }  2>&1 >>"$1_solution_$2_details.log"
}


#STRATEGY=AStarPuchertStrategy
STRATEGY=AStarFlolleStrategy

### short runtime
run_benchmark "pc19 tiles.txt"            $STRATEGY   ### 2 3
run_benchmark "dataset b10c15n1000.txt"   $STRATEGY   ### 2
run_benchmark "dataset b24c4n1000.txt"    $STRATEGY   ### 5 6

### long runtime
run_benchmark "floodtest 1000.txt"        $STRATEGY   ### 4 5
run_benchmark "dataset b12c12n1000.txt"   $STRATEGY   ### 2 3
run_benchmark "dataset b14c8n1000.txt"    $STRATEGY   ### 3
run_benchmark "dataset b18c6n1000.txt"    $STRATEGY   ### 4

### very long runtime
run_benchmark "dataset b24c6n1000.txt"    $STRATEGY
run_benchmark "floodtest.txt"             $STRATEGY

