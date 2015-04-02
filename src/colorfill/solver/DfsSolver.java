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

import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * a solver implementation that performs a depth-first search using recursion.
 */
public class DfsSolver extends AbstractSolver {

    private static final int MAX_SEARCH_DEPTH = 500; // arbitrary limit

    @SuppressWarnings("rawtypes")
    private static final Class[] SUPPORTED_STRATEGIES = {
        GreedyDfsStrategy.class,
        GreedyNextDfsStrategy.class,
        DeepDfsStrategy.class,
        DeeperDfsStrategy.class,
        ExhaustiveDfsStrategy.class
    };

    private Class<? extends DfsStrategy> strategyClass;
    private DfsStrategy strategy;

    private byte[] solution;
    private ReferenceSet<ColorArea> allFlooded;
    private ColorAreaGroup notFlooded;

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    public DfsSolver(final Board board) {
        super(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#setStrategy(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setStrategy(final Class<Strategy> strategyClass) {
        if (false == DfsStrategy.class.isAssignableFrom(strategyClass)) {
            throw new IllegalArgumentException(
                    "unsupported strategy class " + strategyClass.getName()
                    + "  " + this.getClass().getSimpleName() + " supports " + DfsStrategy.class.getSimpleName() + " only.");
        }
        this.strategyClass = (Class<? extends DfsStrategy>) strategyClass;
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#getSupportedStrategies()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<Strategy>[] getSupportedStrategies() {
        return Arrays.copyOf(SUPPORTED_STRATEGIES, SUPPORTED_STRATEGIES.length);
    }

    @SuppressWarnings("unchecked")
    private DfsStrategy makeStrategy(final int startPos) {
        final DfsStrategy result;
        if (null == this.strategyClass) {
            this.strategyClass = SUPPORTED_STRATEGIES[0];
        }
        if (GreedyDfsStrategy.class.equals(this.strategyClass)) {
            result = new GreedyDfsStrategy();
        } else if (GreedyNextDfsStrategy.class.equals(this.strategyClass)) {
            result = new GreedyNextDfsStrategy();
        } else if (DeepDfsStrategy.class.equals(this.strategyClass)) {
            result = new DeepDfsStrategy(this.board, startPos);
        } else if (DeeperDfsStrategy.class.equals(this.strategyClass)) {
            result = new DeeperDfsStrategy(this.board, startPos);
        } else if (ExhaustiveDfsStrategy.class.equals(this.strategyClass)) {
            result = new ExhaustiveDfsStrategy(this.board);
        } else {
            throw new IllegalArgumentException(
                    "DfsSolver.makeStrategy() - unsupported strategy class " + this.strategyClass.getName());
        }
        return result;
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#getSolverName()
     */
    @Override
    public String getSolverName() {
        return this.strategy.getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AbstractSolver#executeInternal(int)
     */
    @Override
    protected void executeInternal(final int startPos) throws InterruptedException {
        this.strategy = this.makeStrategy(startPos);

        final ColorArea startCa = this.board.getColorArea(startPos);
        this.allFlooded = new ReferenceOpenHashSet<ColorArea>();
        this.notFlooded = new ColorAreaGroup(this.board);
        notFlooded.addAll(this.board.getColorAreas(), this.allFlooded);
        final ColorAreaGroup neighbors = new ColorAreaGroup(this.board);
        neighbors.addAll(Collections.singleton(startCa), this.allFlooded);
        this.solution = new byte[MAX_SEARCH_DEPTH];

        this.doRecursion(0, startCa.getColor(), neighbors, true);
    }

    /**
     * the recursion used in this depth-first search.
     * @param depth
     * @param thisColor
     * @param neighbors
     * @throws InterruptedException
     */
    private void doRecursion(final int depth,
            final byte thisColor,
            ColorAreaGroup neighbors,
            final boolean saveNeighbors
            ) throws InterruptedException {

        final Collection<ColorArea> thisFlooded = neighbors.getColor(thisColor);
        int colorsNotFlooded = this.notFlooded.countColorsNotEmpty();
        if (thisFlooded.size() == this.notFlooded.getColor(thisColor).size()) {
            --colorsNotFlooded;
        }

        // finished the search?
        if (0 == colorsNotFlooded) {
            this.solution[depth] = thisColor;
            // skip element 0 because it's not a step but just the initial color at startPos
            this.addSolution(Arrays.copyOfRange(this.solution, 1, depth + 1));

        // do next step
        } else if (this.solutionSize > depth + colorsNotFlooded) { // TODO use ">=" instead of ">" to find all shortest solutions; slower!

            if (Thread.interrupted()) { throw new InterruptedException(); }

            this.solution[depth] = thisColor;
            this.notFlooded.removeAllColor(thisFlooded, thisColor);
            this.allFlooded.addAll(thisFlooded);
            if (saveNeighbors) {
                neighbors = new ColorAreaGroup(neighbors); // clone for backtracking
            }
            neighbors.removeColor(thisColor);
            // add new neighbors
            for (final ColorArea ca : thisFlooded) {
                neighbors.addAll(ca.getNeighbors(), this.allFlooded);
            }
            // pick the "best" neighbor colors to go on
            final ByteList nextColors = this.strategy.selectColors(depth, thisColor, this.solution, this.allFlooded, this.notFlooded, neighbors);
            // go to next recursion level
            final boolean nextSaveNeighbors = (nextColors.size() > 1);
            for (final byte nextColor : nextColors) {
                doRecursion(depth + 1, nextColor, neighbors, nextSaveNeighbors);
            }
            this.allFlooded.removeAll(thisFlooded); // restore for backtracking
            this.notFlooded.addAllColor(thisFlooded, thisColor); // restore for backtracking
        }
    }
}
