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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * a solver implementation that performs a depth-first search using recursion.
 */
public class DfsSolver extends AbstractSolver {

    private static final int MAX_SEARCH_DEPTH = 500; // arbitrary limit

    private DfsStrategy strategy;
    private List<Integer> solution;

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    public DfsSolver(final Board board) {
        super(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AbstractSolver#executeInternal(int)
     */
    // TODO make strategy a runtime parameter
    @Override
    protected void executeInternal(final int startPos) {
        //this.strategy = new GreedyDfsStrategy();
        this.strategy = new DeepDfsStrategy(this.board, startPos);

        final ColorArea startCa = this.board.getColorArea(startPos);
        final HashSet<ColorArea> flooded = new HashSet<>();
        final ColorAreaGroup notFlooded = new ColorAreaGroup(this.board);
        notFlooded.addAll(this.board.getColorAreas(), flooded);
        final ColorAreaGroup neighbors = new ColorAreaGroup(this.board);
        neighbors.addAll(Collections.singleton(startCa), flooded);
        this.solution = new ArrayList<>();
        for (int i = 0;  i < MAX_SEARCH_DEPTH;  ++i) {
            this.solution.add(Integer.valueOf(0));
        }

        this.doRecursion(0, startCa.getColor(), flooded, notFlooded, neighbors);
    }

    /**
     * the recursion used in this depth-first search.
     * @param depth
     * @param thisColor
     * @param solution
     * @param flooded
     * @param notFlooded
     * @param neighbors
     */
    @SuppressWarnings("unchecked")
    private void doRecursion(final int depth,
            final Integer thisColor,
            final HashSet<ColorArea> flooded,
            final ColorAreaGroup notFlooded,
            final ColorAreaGroup neighbors
            ) {
        // do this step
        final Collection<ColorArea> thisFlooded = neighbors.removeColor(thisColor);
        flooded.addAll(thisFlooded);
        notFlooded.removeAll(thisFlooded);
        final int colorsNotFlooded = notFlooded.countColorsNotEmpty();
        this.solution.set(depth, thisColor);

        // finished the search?
        if (0 == colorsNotFlooded) {
            // skip element 0 because it's not a step but just the initial color at startPos
            this.addSolution(this.solution.subList(1, depth + 1));

        // do next step
        } else if (this.solutionSize > depth + colorsNotFlooded) { // TODO use ">=" instead of ">" to find all shortest solutions; slower!
            // add new neighbors
            for (final ColorArea ca : thisFlooded) {
                neighbors.addAll(ca.getNeighbors(), flooded);
            }
            // pick the "best" neighbor colors to go on
            final List<Integer> nextColors = this.strategy.selectColors(depth, thisColor, this.solution, flooded, notFlooded, neighbors);
            // go to next recursion level
            if (1 == nextColors.size()) {
                // no need to clone the data structures
                doRecursion(depth + 1, nextColors.get(0), flooded, notFlooded, neighbors);
            } else {
                for (final Integer nextColor : nextColors) {
                    // clone the data structures for backtracking
                    doRecursion(depth + 1, nextColor,
                            (HashSet<ColorArea>) flooded.clone(), // use clone() because HashSet doesn't have a real copy constructor
                            new ColorAreaGroup(notFlooded), // use copy constructor
                            new ColorAreaGroup(neighbors)); // use copy constructor
                }
            }
        }
    }
}
