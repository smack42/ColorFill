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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import colorfill.model.Board;
import colorfill.solver.AStarPuchertStrategy;
import colorfill.solver.AStarTigrouStrategy;
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
        final String progname = "ColorFill __DEV__";
        final String version  = "1.1 (2018-01-28)";
        final String author   = "Copyright (C) 2018 Michael Henke <smack42@gmail.com>";
        System.out.println(progname + " " + version);
        System.out.println(author);

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
            runValidator(args[0], args[1]);
            break;
        default:
            // print command line help?
            break;
        }

//      testCheckOne();
    }



    /**
     * test some basics
     */
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
//                runSolverCg26232(fileNameTestData);
//                runSolverCg26232exhaustive(fileNameTestData);
                runSolverCg26232puchert(fileNameTestData);
            } else {
                runSolverPc19(fileNameTestData);
            }
        }
    }



    private static Board makeBoard(final BufferedReader br) throws Exception {
        Board result = null;
        final String firstLine = br.readLine();
        if (null == firstLine) {
            // nothing to do
        } else if (14*14 == firstLine.length()) {
            // Programming Challenge 19
            final int startPos = 0;
            result = new Board(firstLine, startPos);
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
                final int startPos = (19*19-1)/2;
                result = new Board(sb.toString(), startPos);
            }
        } else {
            // nothing to do
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
            AStarTigrouStrategy.class,
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
        final BufferedReader brTiles = new BufferedReader(new FileReader(inputFileName));
        final PrintWriter pwResults = new PrintWriter(new FileWriter(outputFileName));

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
        pwResults.close();
        brTiles.close();
    }



    /**
     * test a solver implementation using the "floodtest" file from
     * Code Golf 26232: Create a Flood Paint AI
     * https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
     */
    private static void runSolverCg26232(final String inputFileName) throws Exception {
        // which strategies to run
        final Class[] STRATEGIES = {
            DfsGreedyStrategy.class,
            DfsGreedyNextStrategy.class,
//            DeepDfsStrategy.class,
//            DeeperDfsStrategy.class,
//            ExhaustiveDfsStrategy.class
            AStarTigrouStrategy.class
        };
        final int BOARDS_PER_LOOP = 500;

        System.out.println("running Code Golf 26232: Create a Flood Paint AI");
        System.out.println("reading  input file: " + inputFileName);
        final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
        final String outputFileName = "steps.txt";
        System.out.println("writing output file: " + outputFileName);
        final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName));

        final int stCountSteps[] = new int[STRATEGIES.length], stCountBest[] = new int[STRATEGIES.length];
        int count = 0, countSteps = 0;
        final List<Future<Solution>> futureSolutions = new ArrayList<Future<Solution>>(BOARDS_PER_LOOP * STRATEGIES.length);
        final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        for (;;) {
            // read boards and start solver threads using all the strategies
            for (int loop = 0;  loop < BOARDS_PER_LOOP;  ++loop) {
                final Board board = makeBoard(brBoards);
                if (null == board) {
                    break; // end of input file
                }
                for (final Class strategy : STRATEGIES) {
                    final Solver solver = AbstractSolver.createSolver(strategy, board);
                    futureSolutions.add(exec.submit(new Callable<Solution>() {
                        public Solution call() throws Exception {
                            solver.execute(board.getStartPos(), null);  // TODO execute previousSolution
                            return solver.getSolution();
                        }
                    }));
                }
            }
            // are we finished yet?
            if (futureSolutions.isEmpty()) {
                break; // end of input file
            }
            // collect the solutions from the solver threads
            int strategyIndex = 0;
            final String strategySolutions[] = new String[STRATEGIES.length];
            for (final Future<Solution> futureSolution : futureSolutions) {
                Solution solution = null;
                try {
                    solution = futureSolution.get();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                if (null != solution) {
                    strategySolutions[strategyIndex++] = solution.toString();
                }
                if (STRATEGIES.length == strategyIndex) {
                    ++count;
                    strategyIndex = 0;
                    // which strategies are best for this board?
                    int minSteps = Integer.MAX_VALUE;
                    for (int i = 0;  i < STRATEGIES.length;  ++i) {
                        final int steps = strategySolutions[i].length();
                        stCountSteps[i] += steps;
                        if (minSteps > steps) {
                            minSteps = steps;
                        }
                    }
                    int minStrategy = Integer.MAX_VALUE;
                    for (int i = 0;  i < STRATEGIES.length;  ++i) {
                        if (minSteps == strategySolutions[i].length()) {
                            stCountBest[i] += 1;
                            minStrategy = (i < minStrategy ? i : minStrategy);
                        }
                    }
                    countSteps += minSteps;
                    System.out.println(
                            padRight("" + count, 7 + 1) +
                            padRight(strategySolutions[minStrategy] + "____________" + minSteps, 40 + 12 + 2 + 2)  +
                            minStrategy + "_" + STRATEGIES[minStrategy].getSimpleName());
                    pwSteps.println(strategySolutions[minStrategy]);
                }
            }
            futureSolutions.clear();
//            if (count >= 10000) break;
        }
        // print summary
        for (int strategy = 0;  strategy < STRATEGIES.length;  ++strategy) {
            System.out.println(
                    padRight(strategy + "_" + STRATEGIES[strategy].getSimpleName(), 2 + 21 + 2) +
                    padRight("steps=" + stCountSteps[strategy], 6 + 8 + 2) +
                    padRight("best=" + stCountBest[strategy], 5 + 7 + 2)
                    );
        }
        System.out.println("total steps: " + countSteps + (100000 == count ? "  (Code Golf 26232: Create a Flood Paint AI)" : ""));
        exec.shutdown();
        pwSteps.close();
        brBoards.close();
    }

    /**
     * test a solver implementation using the "floodtest" file from
     * Code Golf 26232: Create a Flood Paint AI
     * https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
     * <br>
     * this is the attempt to solve it once and for all using the "exhaustive" algorithm
     */
    private static void runSolverCg26232exhaustive(final String inputFileName) throws Exception {
        // which strategies to run
        final Class[] STRATEGIES = {
            DfsGreedyStrategy.class,
            DfsGreedyNextStrategy.class,
            AStarTigrouStrategy.class,
            DfsExhaustiveStrategy.class  // DfsExhaustiveStrategy must be last one!
        };
        DfsExhaustiveStrategy.setCodeGolf26232();

        System.out.println("running Code Golf 26232: Create a Flood Paint AI");
        System.out.println("EXHAUSTIVE SOLVER ALGORITHM");
        System.out.println("reading  input file: " + inputFileName);
        final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
        final String outputFileName = "steps.txt";
        System.out.println("writing output file: " + outputFileName);
        final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName, true));  // append to existing output file

        int count = 0, countSteps = 0;
        final Solution[] stSolution = new Solution[STRATEGIES.length];
        final int stCountSteps[] = new int[STRATEGIES.length], stCountBest[] = new int[STRATEGIES.length];
        final long[] stNanoTime = new long[STRATEGIES.length];

        // read existing output file and fast-forward input file accordingly
        final BufferedReader brSteps = new BufferedReader(new FileReader(outputFileName));
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
        brSteps.close();

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
                final Solver solver = AbstractSolver.createSolver((Class<Strategy>) STRATEGIES[strategy], board);
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
                final Solver solver = AbstractSolver.createSolver((Class<Strategy>) STRATEGIES[strategy], board);
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
        pwSteps.close();
        brBoards.close();
    }


    /**
     * test a solver implementation using the "floodtest" file from
     * Code Golf 26232: Create a Flood Paint AI
     * https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
     * <br>
     * this is the attempt to solve it once and for all using the "AStar Puchert" algorithm
     */
    private static void runSolverCg26232puchert(final String inputFileName) throws Exception {
        final Class[] STRATEGIES = {
                AStarPuchertStrategy.class
        };
        System.out.println("running Code Golf 26232: Create a Flood Paint AI");
        System.out.println("SOLVER ALGORITHM \"ASTAR PUCHERT\"");
        System.out.println("reading  input file: " + inputFileName);
        final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileName));
        final String outputFileName = "steps.txt";
        System.out.println("writing output file: " + outputFileName);
        final PrintWriter pwSteps = new PrintWriter(new FileWriter(outputFileName, true));  // append to existing output file

        int count = 0, totalSteps = 0;

        // read existing output file and fast-forward input file accordingly
        final BufferedReader brSteps = new BufferedReader(new FileReader(outputFileName));
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
            totalSteps += steps.length();
        }
        if (count > 0) {
            System.out.println("skipped existing output file content: " + count + " solutions with " + totalSteps + " steps");
        }
        brSteps.close();

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
            final Solver solver = AbstractSolver.createSolver(STRATEGIES[0], board);
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
            if (count >= 1000) break;  // do 1% of the input file only
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
        pwSteps.close();
        brBoards.close();
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
        final BufferedReader brBoards = new BufferedReader(new FileReader(inputFileNameBoards));
        final BufferedReader brSolutions= new BufferedReader(new FileReader(inputFileNameSolutions));
        int countOK = 0, countFAIL = 0;
        for (;;) {
            final Board board = makeBoard(brBoards);
            final String solutionStr = brSolutions.readLine();
            if ((null == board) || (null == solutionStr)) {
                break;
            }
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
        }
        System.out.println("check finished:  total=" + (countOK + countFAIL) + " checkOK=" + countOK + " checkFAIL=" + countFAIL);
        brBoards.close();
        brSolutions.close();
    }



    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}
