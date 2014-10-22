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

package colorfill.solver;

public interface Solver {

    /**
     * run the solver algorithm that is implemented in this class
     * and store the found solution(s) internally.
     * 
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @return number of steps in the solution
     */
    public int execute(final int startPos);

    /**
     * return the first (best) solution as a String.
     * 
     * @return the solution
     */
    public String getSolutionString();
}
