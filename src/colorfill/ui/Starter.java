/*  ColorFill game and solver
    Copyright (C) 2014, 2015 Michael Henke

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
import colorfill.solver.GreedyNextDfsStrategy;
import colorfill.solver.Solution;
import colorfill.solver.Solver;
import colorfill.solver.Strategy;


public class Starter {

    public static void main(String[] args) throws Exception {
        final String progname = "ColorFill __DEVELOPMENT__";
        final String version  = "0.1.8 (2015-03-14)";
        final String author   = "Copyright (C) 2015 Michael Henke <smack42@gmail.com>";
        if (0 == args.length) {
            System.out.println(progname + " " + version);
            System.out.println(author);
            new MainController(progname, version, author);
        } else {
            runSolverPc19(progname + " " + version, author, args[0]);
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
            GreedyNextDfsStrategy.class,
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
        brTiles.close();
    }



    private static void runSolverPc19(final String title, final String author, final String inputFileName) throws Exception {
        final int startPos = 0;
        final String outputFileName = "results.txt";
        System.out.println(title);
        System.out.println(author);
        System.out.println("running Programming Challenge 19");
        System.out.println("reading  input file: " + inputFileName);
        System.out.println("writing output file: " + outputFileName);
        final BufferedReader brTiles = new BufferedReader(new FileReader(inputFileName));
        final PrintWriter pwResults = new PrintWriter(new FileWriter(outputFileName));

        final List<Board> boards = new ArrayList<Board>();
        String inputLine;
        while ((inputLine = brTiles.readLine()) != null) {
            boards.add(new Board(inputLine, startPos));
        }
        brTiles.close();
        System.out.println("input lines read:    " + boards.size());

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        final List<Future<List<Solution>>> futureSolutions = new ArrayList<Future<List<Solution>>>();
        for (final Class<Strategy> strategy : new DfsSolver(boards.get(0)).getSupportedStrategies()) {
            futureSolutions.add(executor.submit(new Callable<List<Solution>>() {
                public List<Solution> call() throws Exception {
                    final List<Solution> result = new ArrayList<Solution>();
                    for (final Board board : boards) {
                        final Solver solver = new DfsSolver(board);
                        solver.setStrategy(strategy);
                        solver.execute(startPos);
                        result.add(solver.getSolution());
                    }
                    return result;
                }
            }));
        }
        executor.shutdown();

        final Solution[] bestSolutions = new Solution[boards.size()];
        for (final Future<List<Solution>> future : futureSolutions) {
            final List<Solution> solutions = future.get();
            int moves = 0;
            for (int i = 0;  i < solutions.size();  ++i) {
                final Solution solution = solutions.get(i);
                if ((null == bestSolutions[i]) || (solution.getNumSteps() < bestSolutions[i].getNumSteps())) {
                    bestSolutions[i] = solution;
                }
                moves += solution.getNumSteps();
            }
            System.out.println("finished " + padRight(solutions.get(0).getSolverName(), 21 + 1) + moves);
        }

        int totalMoves = 0;
        for (final Solution solution : bestSolutions) {
            pwResults.println(solution.toString());
            totalMoves += solution.getNumSteps();
        }
        System.out.println("total moves = " + totalMoves);
        pwResults.println("Total Moves = " + totalMoves);
        pwResults.close();
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
