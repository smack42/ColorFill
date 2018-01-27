#!/bin/bash

{

    date -Iseconds
    time java -Xms14G -Xmx14G -jar ../colorfill.jar floodtest
    date -Iseconds
    echo
    echo

} 2>&1 | tee -a steps_.txt

