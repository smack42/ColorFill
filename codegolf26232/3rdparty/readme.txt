Code Golf challenge: Create a Flood Paint AI
by Joe Z.

https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai

(question/contest originally published on 2014-04-23 and closed by accepting
the answer/winning program by tigrou on 2014-05-10)

Your task is to create a program that will take a 19-by-19 grid of colours from
1 to 6 as input, in whatever form you choose [...] and return a sequence of
colours that the center square will change to each turn, again in the format
of your choosing [...]
At the end of each sequence of moves, the squares in the 19-by-19 grid must all
be the same colour.

Your program must be entirely deterministic; pseudorandom solutions are
allowed, but the program must generate the same output for the same test case
every time.

The winning program will take the fewest total number of steps to solve all
100,000 test cases found in this file (zipped text file, 14.23 MB).
If two solutions take the same number of steps (e.g. if they both found the
optimal strategy), the shorter program will win.

results:

1.  2,075,452 - user1502040, C  (published on 2017-03-28)

1.  2,098,382 - tigrou, C#  (published on 2014-05-10)
2.  2,155,834 - CoderTao, C#
3.  2,201,995 - MrBackend, Java
4.  2,383,569 - CoderTao, C#
5.  2,384,020 - Herjan, C
6.  2,403,189 - Origineil, Java
7.  2,445,761 - Herjan, C
8.  2,475,056 - Jeremy List, Haskell
9.  2,480,714 - SteelTermite, C (2,395 bytes)
10. 2,480,714 - Herjan, Java (4,702 bytes)
11. 2,588,847 - BurntPizza, Java (2,748 bytes)
12. 2,588,847 - Gero3, node.js (4,641 bytes)
13. 2,979,145 - Teun Pronk, Delphi XE3
14. 4,780,841 - BurntPizza, Java
15. 10,800,000 - Joe Z., Python


----

the files:

01_tigrou.cs        = winning program by tigrou
steps_01_tigrou.txt = output of winning program by tigrou: 2,098,382 steps

01_user1502040.c    = new, even better program by user1502040
steps_01_user1502040.txt = output of program by user1502040: 2,075,590 steps
(note: the original program crashed and had to be fixed; this change seems to
produce a slightly different/worse result, 138 steps more than claimed)

