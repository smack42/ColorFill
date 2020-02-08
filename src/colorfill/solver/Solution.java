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

package colorfill.solver;

import java.util.Arrays;

import colorfill.model.Board;

/**
 * this class represents a solution that has been produced by a Solver.
 */
public class Solution {

    private final byte[] steps;
    private final String stepsString;
    private final String solverName;

    /**
     * the constructor.
     * @param steps
     * @param solverName
     */
    public Solution(Board board, byte[] steps, String solverName) {
        this.solverName = solverName;
        this.steps = Arrays.copyOf(steps, steps.length);
        this.stepsString = board.solutionToString(this.steps);
    }

    /**
     * return the number of steps in this solution.
     * @return number of steps
     */
    public int getNumSteps() {
        return this.steps.length;
    }

    /**
     * return (a copy of) the array of steps in this solution.
     * each step is a color value 0...(c-1) with c=number of colors.
     * @return
     */
    public byte[] getSteps() {
        return Arrays.copyOf(this.steps, this.steps.length); // return a copy of the byte array
    }

    /**
     * return the solver name of this solution.
     * @return solver name
     */
    public String getSolverName() {
        return this.solverName;
    }

    /**
     * return a human-readable String representation of this solution.
     * each character is a color value 1...c with c=number of colors.
     * (note the "1" added to the colors compared to getSteps)
     */
    @Override
    public String toString() {
        return this.stepsString;
    }
}
