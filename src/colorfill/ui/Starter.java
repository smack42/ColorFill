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
import java.io.IOException;

import colorfill.model.Board;
import colorfill.solver.Solver;
import colorfill.solver.DfsSolver;


public class Starter {

    public static void main(String[] args) throws IOException {
        new MainController("ColorFill __DEV__2014-10-25__");
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
     */
    private static void testSolverPc19() throws IOException {
        final long nanoStart = System.nanoTime();
        final int startPos = 0;
        final BufferedReader brTiles = new BufferedReader(new FileReader("pc19/tiles.txt"));
        int countSolutions = 0, countSteps = 0, countSteps25 = 0, countCheckFailed = 0, countCheckOK = 0;
        for (String lineTiles = brTiles.readLine();  lineTiles != null;  lineTiles = brTiles.readLine()) {
            final Board board = new Board(lineTiles);
            final Solver solver = new DfsSolver(board);
            final int solutionSteps = solver.execute(startPos);
            ++countSolutions;
            countSteps += solutionSteps;
            countSteps25 += (solutionSteps > 25 ? 25 : solutionSteps);
            final String solutionString = solver.getSolutionString();
            final String solutionCheckResult = board.checkSolution(solutionString, startPos);
            System.out.println(countSolutions + " " + solutionString + "_______________" + solutionSteps + (solutionSteps > 25 ? "  !!!!!!!!!!" : ""));
            if (solutionCheckResult.isEmpty()) {
                ++countCheckOK;
            } else {
                System.out.println(lineTiles);
                System.out.println(board);
                System.out.println(solutionCheckResult);
                ++countCheckFailed;
            }
        }
        final long nanoEnd = System.nanoTime();
        System.out.println("total steps:  " + countSteps + "     with limit 25: " + countSteps25);
        System.out.println("check OK:     " + countCheckOK);
        System.out.println("check failed: " + countCheckFailed);
        System.out.println("wall time:    " + ((nanoEnd - nanoStart + 999999999L) / 1000000000L) + " seconds");
        System.out.println();
        brTiles.close();
    }
}
