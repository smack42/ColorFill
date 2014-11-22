/*  ColorFill game and solver
    Copyright (C) 2014 Michael Henke

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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import colorfill.model.Board;
import colorfill.solver.DeepDfsStrategy;
import colorfill.solver.DeeperDfsStrategy;
import colorfill.solver.DfsSolver;
import colorfill.solver.GreedyDfsStrategy;
import colorfill.solver.Solution;
import colorfill.solver.Solver;
import colorfill.solver.Strategy;


public class Starter {

    public static void main(String[] args) throws Exception {
        if (0 == args.length) {
            new MainController("ColorFill 0.1.3 __DEVELOPMENT__ (2014-11-22)");
        } else {
            runSolverPc19(args[0]);
        }
//        testCheckOne();
//        testCheckPc19();
//        testSolverPc19();
    }


    /**
     * test some basics
     */
    private static void testCheckOne() {
//        final String b = "1162252133131612635256521232523162563651114141545542546462521536446531565521654652142612462122432145511115534353355111125242362245623255453446513311451665625534126316211645151264236333165263163254";
//        final String s = "6345215456513263145";
        final String b = "1464232256454151265361121333134355423464254633453256562522536212626562361214311523421215254461265111331145426131342543161111561256314564465566551321526616635335534461614344546336223551453241656312";
        final String s = "46465321364162543614523";

        final Board board = new Board(b);
        final String solutionResult = board.checkSolution(s, 0); // startPos=0

        System.out.println(board);
        System.out.println(board.toStringColorDepth(0)); // startPos=0
        System.out.println(s + "_" + s.length());
        if (solutionResult.isEmpty()) {
            System.out.println("solution check OK");
        } else {
            System.out.println(solutionResult);
        }
        System.out.println();
    }


    /**
     * test class Board using some results of
     * Programming Challenge 19 - Fill a Grid of Tiles
     * http://cplus.about.com/od/programmingchallenges/a/challenge19.htm
     * 
     * @throws IOException
     */
    private static void testCheckPc19() throws IOException {
        final BufferedReader brTiles = new BufferedReader(new FileReader("pc19/tiles.txt"));
        final String resultsFileName = "results_1.txt"; // results_1.txt  results_5_1.txt  results_5_2_7.txt  results_6.txt
        final BufferedReader brResults = new BufferedReader(new FileReader("pc19/" + resultsFileName));
        int numTotal = 0, numFailed = 0, numFailed25 = 0, numOK = 0;
        System.out.println(resultsFileName);
        for (String lineTiles = brTiles.readLine();  lineTiles != null;  lineTiles = brTiles.readLine()) {
            ++numTotal;
            final String lineResults = brResults.readLine().replaceAll("\\s", ""); // remove whitespace;
            final Board board = new Board(lineTiles);
            final String solutionResult = board.checkSolution(lineResults, 0); // startPos=0
            if (solutionResult.isEmpty()) {
//                System.out.println(numTotal + " solution check OK");
                ++numOK;
            } else {
                System.out.println(numTotal + " " + solutionResult);
                ++numFailed;
                if (25 > lineResults.length()) {
                    ++numFailed25;
                }
            }
        }
        System.out.println("check OK:     " + numOK);
        System.out.println("check failed: " + numFailed + "     at less than 25 moves: " + numFailed25);
        System.out.println();
        brTiles.close();
        brResults.close();
    }


    /**
     * test a solver implementation using the "tiles.txt" from
     * Programming Challenge 19 - Fill a Grid of Tiles
     * http://cplus.about.com/od/programmingchallenges/a/challenge19.htm
     * 
     * @throws IOException 
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    private static void testSolverPc19() throws IOException, InterruptedException {
        // which strategies to run
        final Class<?>[] STRATEGIES = {
            GreedyDfsStrategy.class,
            DeepDfsStrategy.class,
            DeeperDfsStrategy.class
        };
        final int startPos = 0;

        // some counters
        int countStepsBest = 0, countSteps25Best = 0;
        final Solution[] stSolution = new Solution[STRATEGIES.length];
        final int[] stCountSteps = new int[STRATEGIES.length], stCountSteps25 = new int[STRATEGIES.length], stCountBest = new int[STRATEGIES.length];
        final int[] stCountCheckFailed = new int[STRATEGIES.length], stCountCheckOK = new int[STRATEGIES.length];
        final long[] stNanoTime = new long[STRATEGIES.length];

        // read lines from the input file
        final BufferedReader brTiles = new BufferedReader(new FileReader("pc19/tiles.txt"));
        int count = 0;
        for (String lineTiles = brTiles.readLine();  lineTiles != null;  lineTiles = brTiles.readLine()) {
            ++count;
            final Board board = new Board(lineTiles);
            final Solver solver = new DfsSolver(board);
            // run each of the strategies
            for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
                solver.setStrategy((Class<Strategy>) STRATEGIES[strategy]);
                final long nanoStart = System.nanoTime();
                final int numSteps = solver.execute(startPos);
                final long nanoEnd = System.nanoTime();
                stNanoTime[strategy] += nanoEnd - nanoStart;
                stSolution[strategy] = solver.getSolution();
                stCountSteps[strategy] += numSteps;
                stCountSteps25[strategy] += (numSteps > 25 ? 25 : numSteps);
                final String solutionCheckResult = board.checkSolution(solver.getSolution().toString(), startPos);
                if (solutionCheckResult.isEmpty()) {
                    stCountCheckOK[strategy] += 1;
                } else {
                    System.out.println(lineTiles);
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
            //if (100 == count) break; // for (lineTiles)
        }
        // print summary
        for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
            System.out.println(
                    padRight(strategy + "_" + STRATEGIES[strategy].getSimpleName(), 2 + 17 + 2) +
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
        brTiles.close();
    }



    private static void runSolverPc19(final String inputFileName) throws Exception {
        final int startPos = 0;
        final String outputFileName = "results.txt";
        System.out.println("ColorFill by Michael Henke, running Programming Challenge 19");
        System.out.println("reading  input file: " + inputFileName);
        System.out.println("writing output file: " + outputFileName);

        final BufferedReader brTiles = new BufferedReader(new FileReader(inputFileName));
        final PrintWriter pwResults = new PrintWriter(new FileWriter(outputFileName));

        final ExecutorService executor = Executors.newCachedThreadPool(); // newFixedThreadPool(3)
        int countLines = 0, countSolutionMoves = 0;

        // pre-load 1st board
        Board board = null;
        String lineTiles = brTiles.readLine();
        if (null != lineTiles) { board = new Board(lineTiles, startPos); }

        while (null != lineTiles) {
            ++countLines;
            // run the solvers for the current board
            final List<Future<Solution>> futureSolutions = new ArrayList<Future<Solution>>();
            for (final Class<Strategy> strategy : new DfsSolver(board).getSupportedStrategies()) {
                final Solver solver = new DfsSolver(board);
                solver.setStrategy(strategy);
                futureSolutions.add(executor.submit(new Callable<Solution>() {
                    public Solution call() throws Exception {
                        solver.execute(0); // startPos = 0
                        return solver.getSolution();
                    }
                }));
            }
            // pre-load next board, use the time while the solvers are busy
            lineTiles = brTiles.readLine();
            if (null != lineTiles) { board = new Board(lineTiles, startPos); }
            // wait for the solvers to complete and take the best solution
            Solution bestSolution = null;
            for (final Future<Solution> futureSolution : futureSolutions) {
                final Solution solution = futureSolution.get();
                if ((null == bestSolution) || (bestSolution.getNumSteps() > solution.getNumSteps())) {
                    bestSolution = solution;
                }
            }
            // print result and progress indicator
            pwResults.println(bestSolution.toString());
            countSolutionMoves += bestSolution.getNumSteps();
            if (0 == countLines % 10) {
                System.out.print("\b\b\b\b" + countLines); // stay on the same line - not good for Eclipse console
                //System.out.println(countLines);
                System.out.flush();
            }
        }

        System.out.println();
        System.out.println("total moves = " + countSolutionMoves);
        pwResults.println("Total Moves = " + countSolutionMoves);
        pwResults.close();
        brTiles.close();
        executor.shutdown();
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
