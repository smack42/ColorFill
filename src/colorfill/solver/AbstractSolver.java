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

import java.util.ArrayList;
import java.util.List;

import colorfill.model.Board;

/**
 * an abstract implementation of interface Solver.
 */
public abstract class AbstractSolver implements Solver {

    protected static final int MAX_SEARCH_DEPTH = (1 << Byte.SIZE) - 1;

    protected final Board board;
    protected final List<Solution> solutions = new ArrayList<Solution>();
    protected int solutionSize = Integer.MAX_VALUE;

    /**
     * create a new solver.
     * @param strategyClass strategy to be used
     * @param board to be solved
     * @return
     */
    public static Solver createSolver(final Class<Strategy> strategyClass, final Board board) {
        final AbstractSolver solver;
        if (DfsStrategy.class.isAssignableFrom(strategyClass)) {
            solver = new DfsSolver(board);
        } else if (AStarStrategy.class.isAssignableFrom(strategyClass)) {
            solver = new AStarSolver(board);
        } else {
            throw new IllegalArgumentException("unsupported strategy class " + strategyClass.getName());
        }
        solver.setStrategy(strategyClass);
        return solver;
    }

    /**
     * get the name of the solver.
     * @param strategyClass strategy to be used
     * @return
     */
    public static String getSolverName(final Class<Strategy> strategyClass) {
        final Solver solver = createSolver(strategyClass, new Board(2, 2, 2));
        return solver.getSolverName();
    }

    /**
     * store the Board reference.
     * @param board to be solved
     */
    protected AbstractSolver(final Board board) {
        this.board = board;
    }

    /**
     * the actual solver main method, to be implemented by descendants of this class.
     * should call {@link #addSolution(List)} to collect the solution(s).
     * 
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @throws InterruptedException
     */
    protected abstract void executeInternal(int startPos) throws InterruptedException;

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#execute(int)
     */
    @Override
    public int execute(final int startPos, final Solution previousSolution) throws InterruptedException {
        this.solutions.clear();
        this.solutionSize = Integer.MAX_VALUE;
        if (null != previousSolution) {
            this.solutionSize = previousSolution.getNumSteps();
            this.solutions.add(new Solution(previousSolution.getSteps(), this.getSolverName()));
        }

        this.executeInternal(startPos);

        return this.solutionSize;
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#getSolution()
     */
    @Override
    public Solution getSolution() {
        if (this.solutions.size() > 0) {
            return this.solutions.get(0);
        } else {
            return new Solution(new byte[0], this.getSolverName());
        }
    }

    /**
     * add a copy of this solution to the list of solutions if it's shorter than
     * or same length as the current best solution(s).
     * in the list of solutions only the best (shortest) solutions
     * will be stored, longer solutions will be removed when a shorter solution
     * is added.
     * 
     * @param solution to be added
     * @return true if this solution was added
     */
    protected boolean addSolution(final byte[] solution) {
        if (this.solutionSize > solution.length) {
            this.solutionSize = solution.length;
            this.solutions.clear();
        }
        if (this.solutionSize == solution.length) {
            this.solutions.add(new Solution(solution, this.getSolverName()));
            return true;
        }
        return false;
    }
}
