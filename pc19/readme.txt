Programming Challenge 19 - Fill a Grid of Tiles
By David Bolton

http://cplus.about.com/od/programmingchallenges/a/challenge19.htm


the files:

tiles.txt         - the input file: 1000 puzzles to be solved
results_1.txt     - output of entry 1 by Joshua Warner: 20195
results_5_1.txt   - output of entry 5/sol1 by Tyler Mitchell: 24490
results_5_2_7.txt - output of entry 5/sol2/07 by Tyler Mitchell: 21033
results_6.txt     - output of entry 6 by Antonio Cortes: 22243


results_ColorFill_exhaustive.txt
results_ColorFill_exhaustive_details.txt

These two files contain the output of this program, ColorFill.

Its ExhaustiveDfsStrategy scores 20086, which is the same score as the
winning program by Aliaksei Sanko.

20086 has to be the optimal (lowest possible) result, because it was found by
an exhaustive search. That run took 6 CPU hours. (see details file)

The other DFS strategies of ColorFill are weaker but much faster, achieving
a score of 20708 using only 1 CPU minute.
