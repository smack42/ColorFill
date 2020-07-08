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

public interface Solver {

    /**
     * run the solver algorithm that is implemented in this class
     * and store the found solution(s) internally.
     * 
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @param previousSolution the best solution found previously (by another solver?) for this board;
     *        used to speed up DfsExhaustiveStrategy; if not known then just give a null value.
     * @return number of steps in the solution
     * @throws InterruptedException
     */
    public int execute(final int startPos, final Solution previousSolution) throws InterruptedException;

    /**
     * return the first (best) solution.
     * 
     * @return the solution
     */
    public Solution getSolution();

    /**
     * set the strategy to be used by this solver.
     * @param strategyClass the class of the strategy
     */
    public void setStrategy(Class<? extends Strategy> strategyClass);

    /**
     * get the name of the solver and / or the strategy.
     * @return the name
     */
    public String getSolverName();

    /**
     * get some info from the solver, like memory usage after execute() has finished.
     * @return some info, may be null or empty if the particular solver has nothing to say.
     */
    public String getSolverInfo();
}
