#!/bin/bash

run_benchmark () {
    {
        date -Iseconds
        echo
        time java -Xms8G -Xmx8G -jar ../colorfill.jar -benchmark "$1" "$2"
       # time java -Xms8G -Xmx8G -cp ../bin colorfill.ui.Starter -benchmark "$1"
        echo
        date -Iseconds
        echo
        time java -jar ../colorfill.jar "$1" "$1_solution.txt"
       # time java -cp ../bin colorfill.ui.Starter "$1" "$1_solution.txt"
        echo
        date -Iseconds
    } 2>&1 | tee "$1_solution_details.txt"
}


run_benchmark "pc19 tiles.txt" "AStarFlolleStrategy"

run_benchmark "floodtest 1000.txt" "AStarFlolleStrategy"
