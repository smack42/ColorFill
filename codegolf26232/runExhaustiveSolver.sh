#!/bin/bash

{

    date -Iseconds
    time java -Xmx16G -jar ../colorfill.jar floodtest
    date -Iseconds
    echo
    echo

} 2>&1 | tee -a steps_.txt

