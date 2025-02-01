This directory can be used to store the "99problems" files from this repo:
https://github.com/manteuffel723/flood-it-boards

It belongs to the Bachelor Thesis by Philipp von Manteuffel:
"Exact Approaches for Flood-it: A*, IDA*, a SAT- and ILP-Solver compared"
Technische Universität Berlin, 10.07.2023
https://doc.neuro.tu-berlin.de/bachelor/2023-BA-PhilippVonManteuffel-mc.pdf


Run ColorFill in the special "99problems" mode like this:
java -Xms30G -Xmx30G -jar colorfill.jar -99problems 99problems/

ColorFill solves each board with its optimal solver (A-Star Puchert strategy)
and compares its number of moves with the one specified the file.

results summary:
- all specified optimal solutions in the files are confirmed by ColorFill
- ColorFill requires about 30 GB of RAM to solve the hardest problems
- sequential processing of all 99 files takes about half an hour

see results.txt for details.
