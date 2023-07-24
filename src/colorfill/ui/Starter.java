/*  ColorFill game and solver
    Copyright (C) 2014, 2015, 2016 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package colorfill.ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import colorfill.model.Board;
import colorfill.solver.AStarFlolleStrategy;
import colorfill.solver.AStarPuchertStrategy;
import colorfill.solver.AbstractSolver;
import colorfill.solver.DfsDeepStrategy;
import colorfill.solver.DfsDeeperStrategy;
import colorfill.solver.DfsExhaustiveStrategy;
import colorfill.solver.DfsGreedyStrategy;
import colorfill.solver.DfsGreedyNextStrategy;
import colorfill.solver.Solution;
import colorfill.solver.Solver;
import colorfill.solver.Strategy;

public class Starter {
    
    public static void main(String[] args) throws Exception {
        final String progname = "ColorFill";
        final String version  = "1.3.3__DEV__ (2023-07-24)";
        final String author   = "Copyright (C) 2023 Michael Henke <smack42@gmail.com>";
        System.out.println(progname + " " + version);
        System.out.println(author);
//System.in.read();

        switch (args.length) {
        case 0:
            DfsExhaustiveStrategy.setHashNormal();
            new MainController(progname, version, author);
            break;
        case 1:
            DfsExhaustiveStrategy.setHashFast();
            runSolver(args[0]);
            break;
        case 2:
            if ("-benchmark".equals(args[0])) {
                runBenchmark(args);
            } else if ("-99problems".equals(args[0])) {
                run99Problems(args[1]);
            } else {
                runValidator(args[0], args[1]);
            }
            break;
        case 3:
            if ("-benchmark".equals(args[0])) {
                runBenchmark(args);
            }
            break;
        default:
            // print command line help?
            break;
        }

//        testCheckOne();
    }



    /**
     * test some basics
     */
    @SuppressWarnings("unused")
    private static void testCheckOne() {
//        final String b = "1162252133131612635256521232523162563651114141545542546462521536446531565521654652142612462122432145511115534353355111125242362245623255453446513311451665625534126316211645151264236333165263163254";
//        final String s = "6345215456513263145";
        final String b = "1464232256454151265361121333134355423464254633453256562522536212626562361214311523421215254461265111331145426131342543161111561256314564465566551321526616635335534461614344546336223551453241656312";
        final String s = "46465321364162543614523";

        final int startPos = 0;

        final Board board = new Board(b);
        final String solutionResult = board.checkSolution(s, startPos);

        System.out.println(board);
        System.out.println(board.toStringColorDepth(startPos));
        System.out.println(s + "_" + s.length());
        if (solutionResult.isEmpty()) {
            System.out.println("solution check OK");
        } else {
            System.out.println(solutionResult);
        }
        System.out.println();
    }



    private static void runSolver(final String fileNameTestData) throws Exception {
        final String firstLine;
        {
            final BufferedReader br = new BufferedReader(new FileReader(fileNameTestData));
            firstLine = br.readLine();
            br.close();
        }
        if (null != firstLine) {
            if (firstLine.length() == 19) {
//                runSolverCg26232exhaustive(fileNameTestData);
                runSolverCg26232puchert(fileNameTestData);
            } else {
                runSolverPc19(fileNameTestData);
            }
        }
    }


    private static String readBoard(final BufferedReader br) throws Exception {
        String result = null;
        final String firstLine = br.readLine();
        if (null == firstLine) {
            // nothing to do
        } else if (19 == firstLine.length()) {
            // Code Golf 26232
            final StringBuilder sb = new StringBuilder(19*19);
            sb.append(firstLine);
            for (;;) {
                final String line = br.readLine();
                if ((null == line) || (19 != line.length())) {
                    break;
                } else {
                    sb.append(line);
                }
            }
            if (19*19 == sb.length()) {
                result = sb.toString();
            }
        } else {
            result = firstLine;
        }
        return result;
    }

    private static Board makeBoard(final BufferedReader br) throws Exception {
        Board result = null;
        final String boardData = readBoard(br);
        if (null != boardData) {
            final int startPos;
            if (19*19 == boardData.length()) {
                // Code Golf 26232
                startPos = (19*19-1)/2;
            } else {
                startPos = 0;
            }
            result = new Board(boardData, startPos);
        }
        return result;
    }



    /**
     * test a solver implementation using the "tiles.txt" from
     * Programming Challenge 19 - Fill a Grid of Tiles
     * http://cplus.about.com/od/programmingchallenges/a/challenge19.htm
     */
    @SuppressWarnings("unchecked")
    private static void runSolverPc19(final String inputFileName) throws Exception {
        // which strategies to run
        final Class<?>[] STRATEGIES = {
            DfsGreedyStrategy.class,
            DfsGreedyNextStrategy.class,
            DfsDeepStrategy.class,
            DfsDeeperStrategy.class,
            AStarFlolleStrategy.class,
            AStarPuchertStrategy.class,
            //DfsExhaustiveStrategy.class,
        };

        final String outputFileName = "results.txt";
        System.out.println("running Programming Challenge 19");
        System.out.println("reading  input file: " + inputFileName);
        System.out.println("writing output file: " + outputFileName);

        // some counters
        int countStepsBest = 0, countSteps25Best = 0;
        final Solution[] stSolution = new Solution[STRATEGIES.length];
        final int[] stCountSteps = new int[STRATEGIES.length], stCountSteps25 = new int[STRATEGIES.length], stCountBest = new int[STRATEGIES.length];
        final int[] stCountCheckFailed = new int[STRATEGIES.length], stCountCheckOK = new int[STRATEGIES.length];
        final long[] stNanoTime = new long[STRATEGIES.length];

        // read lines from the input file
        try (   final BufferedReader brTiles = new BufferedReader(new FileReader(inputFileName));
                final PrintWriter pwResults = new PrintWriter(new FileWriter(outputFileName))
            ) {
            int count = 0;
            for (;;) {
                final Board board = makeBoard(brTiles);
                if (null == board) {
                    break; // end of input file !?
                }
                ++count;
                // run each of the strategies
                Solution bestSolution = null;
                for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                    final Solver solver = AbstractSolver.createSolver((Class<Strategy>) STRATEGIES[strategy], board);
                    final long nanoStart = System.nanoTime();
                    final int numSteps = solver.execute(board.getStartPos(), DfsExhaustiveStrategy.class.equals(STRATEGIES[strategy]) ? bestSolution : null);
                    final long nanoEnd = System.nanoTime();
                    stNanoTime[strategy] += nanoEnd - nanoStart;
                    stSolution[strategy] = solver.getSolution();
                    stCountSteps[strategy] += numSteps;
                    stCountSteps25[strategy] += (numSteps > 25 ? 25 : numSteps);
                    if ((null == bestSolution) || (numSteps < bestSolution.getNumSteps())) {
                        bestSolution = solver.getSolution();
                    }
                    final String solutionCheckResult = board.checkSolution(solver.getSolution().toString(), board.getStartPos());
                    if (solutionCheckResult.isEmpty()) {
                        stCountCheckOK[strategy] += 1;
                    } else {
                        System.out.println(board.toStringCells());
                        System.out.println(board);
                        System.out.println(STRATEGIES[strategy].getName());
                        System.out.println(solutionCheckResult);
                        stCountCheckFailed[strategy] += 1;
                    }
                }
                // which strategy was best for this board?
                int minSteps = Integer.MAX_VALUE;
                for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                    if (minSteps > stSolution[strategy].getNumSteps()) {
                        minSteps = stSolution[strategy].getNumSteps();
                    }
                }
                int minStrategy = Integer.MAX_VALUE;
                for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                    if (minSteps == stSolution[strategy].getNumSteps()) {
                        stCountBest[strategy] += 1;
                        minStrategy = (strategy < minStrategy ? strategy : minStrategy);
                    }
                }
                countStepsBest += minSteps;
                countSteps25Best += (minSteps > 25 ? 25 : minSteps);
                // print one line per board
                System.out.println(
                        padRight("" + count, 4 + 1) +
                        padRight(stSolution[minStrategy] + "____________" + minSteps, 30 + 12 + 2 + 2)  +
                        (minSteps > 25 ? "!!!!!!!  " : "         ") +
                        minStrategy + "_" + stSolution[minStrategy].getSolverName());
                pwResults.println(stSolution[minStrategy].toString());
                //if (100 == count) break; // for (lineTiles)
            }
            // print summary
            for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                System.out.println(
                        padRight(strategy + "_" + STRATEGIES[strategy].getSimpleName(), 2 + 21 + 2) +
                        padRight("steps=" + stCountSteps[strategy], 6 + 5 + 2) +
                        padRight("steps25=" + stCountSteps25[strategy], 8 + 5 + 2) +
                        padRight("best=" + stCountBest[strategy], 5 + 4 + 2) +
                        padRight("checkOK=" + stCountCheckOK[strategy], 8 + 4 + 2) +
                        padRight("checkFAIL=" + stCountCheckFailed[strategy], 10 + 4 + 2) +
                        padRight("milliSeconds=" + ((stNanoTime[strategy] + 999999L) / 1000000L), 13 + 5 + 2)
                        );
            }
            System.out.println("total steps:   " + countStepsBest);
            System.out.println("total steps25: " + countSteps25Best + (1000 == count ? "  (Programming Challenge 19 score)" : ""));
            pwResults.println("Total Moves = " + countSteps25Best);
        }
    }



    /**
     * test a solver implementation using the "floodtest" file from
     * Code Golf 26232: Create a Flood Paint AI
     * https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
     * <br>
     * this is the attempt to solve it once and for all using the "exhaustive" algorithm
     */
    @SuppressWarnings("unused")
    private static void runSolverCg26232exhaustive(final String inputFileName) throws Exception {
        // which strategies to run
        final Class<?>[] STRATEGIES = {
            DfsGreedyStrategy.class,
            DfsGreedyNextStrategy.class,
            DfsExhaustiveStrategy.class  // DfsExhaustiveStrategy must be last one!
        };
        DfsExhaustiveStrategy.setCodeGolf26232();

        System.out.println("running Code Golf 26232: Create a Flood Paint AI");
        System.out.println("EXHAUSTIVE SOLVER ALGORITHM");
        System.out.println("reading  input file: " + inputFileName);
        final String outputFileName = "steps.txt";
        System.out.println("writing output file: " + outputFileName);
        try (   final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
                final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName, true))  // append to existing output file
            ) {
            int count = 0, countSteps = 0;
            final Solution[] stSolution = new Solution[STRATEGIES.length];
            final int stCountSteps[] = new int[STRATEGIES.length], stCountBest[] = new int[STRATEGIES.length];
            final long[] stNanoTime = new long[STRATEGIES.length];

            // read existing output file and fast-forward input file accordingly
            try (final BufferedReader brSteps = new BufferedReader(new FileReader(outputFileName))) {
                for (;;) {
                    final String steps = brSteps.readLine();
                    if (null == steps) {
                        break;  // end of output file
                    }
                    final Board board = makeBoard(brBoards);
                    if (null == board) {
                        break; // end of input file
                    }
                    ++count;
                    countSteps += steps.length();
                }
                if (count > 0) {
                    System.out.println("skipped existing output file content: " + count + " solutions with " + countSteps + " steps");
                }
            }

            // read input file and solve boards and write to output file
            main_loop:
                for (;;) {
                    final Board board = makeBoard(brBoards);
                    if (null == board) {
                        break; // end of input file !?
                    }
                    ++count;
                    // run each of the strategies
                    int bestStrategy = 0;
                    Solution bestSolution = null;
                    // don't run DfsExhaustiveStrategy yet
                    for (int strategy = 0;  strategy < STRATEGIES.length - 1;  ++strategy) {
                        final Solver solver = AbstractSolver.createSolver(STRATEGIES[strategy].asSubclass(Strategy.class), board);
                        final long nanoStart = System.nanoTime();
                        solver.execute(board.getStartPos(), null);
                        final long nanoEnd = System.nanoTime();
                        stNanoTime[strategy] += nanoEnd - nanoStart;
                        final Solution solution = solver.getSolution();
                        stSolution[strategy] = solution;
                        stCountSteps[strategy] += solution.getNumSteps();
                        if ((null == bestSolution) || (solution.getNumSteps() < bestSolution.getNumSteps())) {
                            bestSolution = solution;
                            bestStrategy = strategy;
                        }
                    }
                    final Solution bestQuickSolution = bestSolution;
                    System.out.print(
                            padRight("" + count, 6 + 1) +
                            padRight("quick=" + bestQuickSolution.getNumSteps(), 6 + 2 + 2) );
                    System.out.flush();
                    OutOfMemoryError oomError = null;
                    {  // run DfsExhaustiveStrategy only
                        final int strategy = STRATEGIES.length - 1;
                        final Solver solver = AbstractSolver.createSolver(STRATEGIES[strategy].asSubclass(Strategy.class), board);
                        final long nanoStart = System.nanoTime();
                        try {
                            solver.execute(board.getStartPos(), DfsExhaustiveStrategy.class.equals(STRATEGIES[strategy]) ? bestSolution : null);
                        } catch (OutOfMemoryError oom) {
                            oomError = oom;
                        }
                        final long nanoEnd = System.nanoTime();
                        stNanoTime[strategy] += nanoEnd - nanoStart;
                        final Solution solution = solver.getSolution();
                        stSolution[strategy] = solution;
                        stCountSteps[strategy] += solution.getNumSteps();
                        if ((null == bestSolution) || (solution.getNumSteps() < bestSolution.getNumSteps())) {
                            bestSolution = solution;
                            bestStrategy = strategy;
                        }
                    }
                    countSteps += bestSolution.getNumSteps();
                    for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                        if (stSolution[strategy].getNumSteps() == bestSolution.getNumSteps()) {
                            stCountBest[strategy] += 1;
                        }
                    }
                    System.out.println(
                            padRight(bestSolution.toString() + "____________" + bestSolution.getNumSteps(), 28 + 12 + 2 + 2) +
                            padRight(bestStrategy + "_" + STRATEGIES[bestStrategy].getSimpleName(), 2 + 21 + 2) +
                            padRight("gain=" + (bestQuickSolution.getNumSteps()-bestSolution.getNumSteps()), 5 + 1 + 2) +
                            padRight("predictedTotal=" + (100000L*countSteps/count), 24) +
                            (oomError != null ? "OutOfMemoryError" : "") );
                    System.out.flush();
                    pwSteps.println(bestSolution.toString());
                    pwSteps.flush();

                    // look for user input on stdin - if "q" is entered then we quit
                    while (System.in.available() > 0) {
                        final int inp = System.in.read();
                        if ('q' == inp) {
                            break main_loop;
                        }
                    }
                    if (count >= 1000) break;  // do 1% of the input file only
                }

            // print summary
            for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                System.out.println(
                        padRight(strategy + "_" + STRATEGIES[strategy].getSimpleName(), 2 + 21 + 2) +
                        padRight("steps=" + stCountSteps[strategy], 6 + 8 + 2) +
                        padRight("best=" + stCountBest[strategy], 5 + 7 + 2) +
                        padRight("milliSeconds=" + ((stNanoTime[strategy] + 999999L) / 1000000L), 13 + 8 + 2)
                        );
            }
            System.out.println("total steps: " + countSteps + (100000 == count ? "  (Code Golf 26232: Create a Flood Paint AI)" : ""));
        }
    }


    /**
     * test a solver implementation using the "floodtest" file from
     * Code Golf 26232: Create a Flood Paint AI
     * https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
     * <br>
     * this is the attempt to solve it once and for all using the "AStar Puchert" algorithm
     */
    private static void runSolverCg26232puchert(final String inputFileName) throws Exception {
        final Class<?>[] STRATEGIES = {
                AStarPuchertStrategy.class
        };
        System.out.println("running Code Golf 26232: Create a Flood Paint AI");
        System.out.println("SOLVER ALGORITHM \"ASTAR PUCHERT\"");
        System.out.println("reading  input file: " + inputFileName);
        final String outputFileName = "steps.txt";
        System.out.println("writing output file: " + outputFileName);
        try (   final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
                final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName, true))  // append to existing output file
            ) {
            int count = 0, totalSteps = 0;

            // read existing output file and fast-forward input file accordingly
            try (final BufferedReader brSteps = new BufferedReader(new FileReader(outputFileName))) {
                for (;;) {
                    final String steps = brSteps.readLine();
                    if (null == steps) {
                        break;  // end of output file
                    }
                    final String board = readBoard(brBoards);
                    if (null == board) {
                        break; // end of input file
                    }
                    ++count;
                    totalSteps += steps.length();
                }
                if (count > 0) {
                    System.out.println("skipped existing output file content: " + count + " solutions with " + totalSteps + " steps");
                }
            }

            // read input file and solve boards and write to output file
            final List<Integer> allMilliSeconds = new ArrayList<Integer>();
            int sessionStart = count + 1;
            int sessionSteps = 0;
            main_loop:
                for (;;) {
                    final Board board = makeBoard(brBoards);
                    if (null == board) {
                        break; // end of input file !?
                    }
                    ++count;
                    final Solver solver = AbstractSolver.createSolver(STRATEGIES[0].asSubclass(Strategy.class), board);
                    final long nanoStart = System.nanoTime();
                    solver.execute(board.getStartPos(), null);
                    final long nanoEnd = System.nanoTime();
                    final Solution solution = solver.getSolution();
                    totalSteps += solution.getNumSteps();
                    sessionSteps += solution.getNumSteps();
                    final int millis = (int)((nanoEnd - nanoStart + 999999L) / 1000000L);
                    allMilliSeconds.add(Integer.valueOf(millis));
                    System.out.println(
                            padRight("" + count, 6 + 1) +
                            padRight(solution.toString() + "____________" + solution.getNumSteps(), 28 + 12 + 2 + 2) +
                            padRight("milliSeconds=" + millis, 13 + 8 + 2) +
                            "predictedTotal=" + (100000L*totalSteps/count)
                            );
                    System.out.flush();
                    pwSteps.println(solution.toString());
                    pwSteps.flush();

                    // look for user input on stdin - if "q" is entered then we quit
                    while (System.in.available() > 0) {
                        final int inp = System.in.read();
                        if ('q' == inp) {
                            break main_loop;
                        }
                    }
                    //            if (count >= 1000) break;  // do 1% of the input file only
                }

            // print summary
            int minMillis = Integer.MAX_VALUE;
            int maxMillis = Integer.MIN_VALUE;
            long avgMillis = 0;
            for (final int millis : allMilliSeconds) {
                minMillis = Math.min(minMillis, millis);
                maxMillis = Math.max(maxMillis, millis);
                avgMillis += millis;
            }
            Collections.sort(allMilliSeconds);
            int medianMillis = allMilliSeconds.isEmpty() ? 0 : allMilliSeconds.get(Math.min(allMilliSeconds.size()/2, allMilliSeconds.size()-1)).intValue();
            avgMillis = avgMillis / (allMilliSeconds.isEmpty() ? 1 : allMilliSeconds.size());
            System.out.println(
                    STRATEGIES[0].getSimpleName() + "   " +
                            "session(" + sessionStart + "," + count + ")=" + (count-sessionStart+1) + "   " +
                            "steps=" + sessionSteps + "   " +
                            "milliSeconds_min/median/average/max=" + minMillis + "/" + medianMillis + "/" + avgMillis + "/" + maxMillis
                    );
            System.out.println("total steps: " + totalSteps + (100000 == count ? "  (Code Golf 26232: Create a Flood Paint AI)" : ""));
        }
    }


    private static void runBenchmark(final String[] args) throws Exception {
        final String inputFileName = args[1];
        final Class<?> STRATEGY;
        if (args.length == 2) {
            STRATEGY = AStarPuchertStrategy.class;
        } else {
            STRATEGY = Class.forName("colorfill.solver." + args[2]);
        }
        final String solverName = AbstractSolver.getSolverName(STRATEGY.asSubclass(Strategy.class));
        System.out.println("running benchmark of solver strategy " + solverName);
        System.out.println("reading  input file: " + inputFileName);
        final String outputFileName = inputFileName + "_solution_" + solverName + ".txt";
        System.out.println("writing output file: " + outputFileName);
        try (   final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
                final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName));
            ) {
            int count = 0, totalSteps = 0;
            long totalNanos = 0;
            // read input file and solve boards and write to output file
            final List<Integer> allMilliSeconds = new ArrayList<Integer>();
            for (;;) {
                final long nanoStart = System.nanoTime();
                final Board board = makeBoard(brBoards);
                if (null == board) {
                    break; // end of input file !?
                }
                ++count;
                final Solver solver = AbstractSolver.createSolver(STRATEGY.asSubclass(Strategy.class), board);
                solver.execute(board.getStartPos(), null);
                final Solution solution = solver.getSolution();
                totalSteps += solution.getNumSteps();
                final long nanoEnd = System.nanoTime();
                totalNanos += nanoEnd - nanoStart;
                final int millis = (int)((nanoEnd - nanoStart + 999999L) / 1000000L);
                allMilliSeconds.add(Integer.valueOf(millis));
                System.out.println(
                        padRight("" + count, 6 + 1) +
                        padRight(solution.toString() + "____________" + solution.getNumSteps(), 32 + 12 + 2 + 2) +
                        "milliSeconds=" + millis
                        );
                //            System.out.flush();
                pwSteps.println(solution.toString());
                pwSteps.flush();
            }

            // print summary
            int minMillis = Integer.MAX_VALUE;
            int maxMillis = Integer.MIN_VALUE;
            long avgMillis = 0;
            for (final int millis : allMilliSeconds) {
                minMillis = Math.min(minMillis, millis);
                maxMillis = Math.max(maxMillis, millis);
                avgMillis += millis;
            }
            Collections.sort(allMilliSeconds);
            int medianMillis = allMilliSeconds.isEmpty() ? 0 : allMilliSeconds.get(Math.min(allMilliSeconds.size()/2, allMilliSeconds.size()-1)).intValue();
            long totalMillis = (int)((totalNanos + 999999L) / 1000000L);
            avgMillis = avgMillis / (allMilliSeconds.isEmpty() ? 1 : allMilliSeconds.size());
            System.out.println(solverName + "  " + count + " solutions with  " + totalSteps + " steps");
            System.out.println("milliSeconds_min/median/average/max=" + minMillis + "/" + medianMillis + "/" + avgMillis + "/" + maxMillis + "  total=" + totalMillis);
        }
    }

    /**
     * read the two files and check if the boards in the first file are solved
     * by the solutions in the second file.
     * works for both: Programming Challenge 19 and Code Golf 26232
     * 
     * @param inputFileNameBoards
     * @param inputFileNameSolutions
     * @throws Exception
     */
    private static void runValidator(final String inputFileNameBoards, final String inputFileNameSolutions) throws Exception {
        System.out.println("running solution validator");
        System.out.println("reading input file    Boards: " + inputFileNameBoards);
        System.out.println("reading input file Solutions: " + inputFileNameSolutions);
        try (   final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileNameBoards));
                final BufferedReader brSolutions= new BufferedReader(new FileReader(inputFileNameSolutions))
            ) {
            final int[] countSolutionLengths = new int[1000]; // arbitrary limit
            int countOK = 0, countFAIL = 0, totalSolutionSteps = 0;
            for (;;) {
                final Board board = makeBoard(brBoards);
                final String solutionStr = brSolutions.readLine();
                if ((null == board) || (null == solutionStr)) {
                    break;
                }
                ++countSolutionLengths[solutionStr.length()];
                totalSolutionSteps += solutionStr.length();
                final String checkResult = board.checkSolution(solutionStr, board.getStartPos());
                if (checkResult.isEmpty()) {
                    ++countOK;
                } else {
                    ++countFAIL;
                    System.out.println(countOK + countFAIL);
                    System.out.println(board.toStringCells());
                    System.out.println(board);
                    System.out.println(checkResult);
                    System.out.println();
                }
//                if (0 == (countOK + countFAIL) % 1000) {
//                    System.out.println("checked " + (countOK + countFAIL) + " ...");
//                }
            }
            System.out.println("check finished:  total=" + (countOK + countFAIL) + " checkOK=" + countOK + " checkFAIL=" + countFAIL);
            System.out.println("solution steps:  total=" + totalSolutionSteps + " average=" + ((double)totalSolutionSteps / (countOK + countFAIL)));
            int maxCountSolutionLengths = 0;
            for (final int l : countSolutionLengths) {
                maxCountSolutionLengths = Math.max(maxCountSolutionLengths, l);
            }
            final NumberFormat nf = NumberFormat.getPercentInstance();
            for (int i = 0;  i < countSolutionLengths.length;  ++i) {
                if (0 != countSolutionLengths[i]) {
                    final int BARDISPLAY = 60;
                    final StringBuilder sb = new StringBuilder(BARDISPLAY);
                    for (int j = 0;  j < (countSolutionLengths[i] * BARDISPLAY / maxCountSolutionLengths);  ++j) {
                        sb.append('*');
                    }
                    System.out.println(
                            "solution=" + padRight(""+ i, 2) +
                            " " + padLeft("" + countSolutionLengths[i],  5) +
                            " |" + padRight(sb.toString(), BARDISPLAY) + "| " +
                            padLeft("" + nf.format((double)countSolutionLengths[i] / (countOK + countFAIL)), 2+1)
                            );
                }
            }
        }
    }

    /**
     * process the "99problems" files from repo https://github.com/manteuffel723/flood-it-boards
     * <p>
     * solve each board with our optimal solver (A-Star with Puchert strategy) and compare our
     * number of moves with the one specified the file.
     * 
     * @param strPath path that contains the "99problems" files
     */
    private static void run99Problems(String strPath) throws Exception {
        System.out.println(LocalDateTime.now());
        final long nanoStartAll = System.nanoTime();
        ArrayList<Path> paths = Files.list(Paths.get(strPath)).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        Collections.sort(paths);
        int numPath = 0;
        for (Path path : paths) {
            if (path.getFileName().toString().matches("^\\d.*")) { // file names start with a digit (0-9)
                System.out.print(padLeft(++numPath + ": ", 5) + path);
                List<String> lines = Files.readAllLines(path);
                int typeLine = 0;
                int boardRows = 0, boardColumns = 0, boardColors = 0, boardSolutionMoves = 0;
                String boardCells = null;
                for (String line : lines) {
                    if (line == null || line.trim().isEmpty() || line.startsWith("#")) {
                        continue; // skip comment lines
                    }
                    switch (typeLine) {
                    case 0:
                        // first line specifies the board dimensions and number of colors
                        try (Scanner scanner = new Scanner(line)) {
                            boardRows = scanner.nextInt();
                            boardColumns = scanner.nextInt();
                            boardColors = scanner.nextInt();
                        }
                        ++typeLine;
                        break;
                    case 1:
                        // second line contains the colored cells
                        boardCells = line.replace("10","A");
                        boardCells = boardCells.replaceAll("\\s", ""); // remove whitespace
                        ++typeLine;
                        break;
                    case 2:
                        // third line contains the number of moves of an optimal solution
                        try (Scanner scanner = new Scanner(line)) {
                            boardSolutionMoves = scanner.nextInt();
                        }
                        ++typeLine;
                        break;
                    default:
                        break; // unexpected
                    }
                }
                System.out.print("  columns=" + boardColumns + " rows=" + boardRows + padRight(" colors=" + boardColors, 10) + " moves=" + boardSolutionMoves);
                System.out.flush();
                Board board = new Board(boardColumns, boardRows, boardColors, boardCells, 0); // start position is 0 = top left
                final Solver solver = AbstractSolver.createSolver(AStarPuchertStrategy.class, board);
                final long nanoStart = System.nanoTime();
                solver.execute(board.getStartPos(), null);
                final long nanoEnd = System.nanoTime();
                final int millis = (int)((nanoEnd - nanoStart + 999999L) / 1000000L);
                final Solution solution = solver.getSolution();
                String compare = "===="; // equal number of moves
                if (solution.getNumSteps() > boardSolutionMoves) compare = "++++"; // we have more moves
                if (solution.getNumSteps() < boardSolutionMoves) compare = "----"; // we have less moves
                System.out.print("  " + compare + " " + "myMoves=" + solution.getNumSteps() + "  in " + millis + " ms");
                System.out.println();
            }
        }
        final long nanoEndAll = System.nanoTime();
        final int millisAll = (int)((nanoEndAll - nanoStartAll + 999999L) / 1000000L);
        Duration duration = Duration.ofMillis(millisAll);
        System.out.println("finished!     duration: " + duration);
        System.out.println(LocalDateTime.now());
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
