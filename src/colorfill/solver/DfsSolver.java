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

import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * a solver implementation that performs a depth-first search using recursion.
 */
public class DfsSolver extends AbstractSolver {

    private static final int MAX_SEARCH_DEPTH = 500; // arbitrary limit

    private DfsStrategy strategy;

    private byte[] solution;
    private Set<ColorArea> allFlooded;
    private ColorAreaGroup notFlooded;

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
        //this.strategy = new DeepDfsStrategy(this.board, startPos);
        this.strategy = new DeeperDfsStrategy(this.board, startPos);

        final ColorArea startCa = this.board.getColorArea(startPos);
        this.allFlooded = new ReferenceOpenHashSet<>();
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
     */
    private void doRecursion(final int depth,
            final byte thisColor,
            ColorAreaGroup neighbors,
            final boolean saveNeighbors
            ) {
        // do this step
        final Collection<ColorArea> thisFlooded = neighbors.getColor(thisColor);
        this.notFlooded.removeAllColor(thisFlooded, thisColor);
        final int colorsNotFlooded = this.notFlooded.countColorsNotEmpty();
        this.solution[depth] = thisColor;

        // finished the search?
        if (0 == colorsNotFlooded) {
            // skip element 0 because it's not a step but just the initial color at startPos
            this.addSolution(Arrays.copyOfRange(this.solution, 1, depth + 1));

        // do next step
        } else if (this.solutionSize > depth + colorsNotFlooded) { // TODO use ">=" instead of ">" to find all shortest solutions; slower!
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
            for (final byte nextColor : nextColors) {
                doRecursion(depth + 1, nextColor, neighbors,
                        (nextColors.size() > 1)); // saveNeighbors
            }
            this.allFlooded.removeAll(thisFlooded); // restore for backtracking
        }

        this.notFlooded.addAllColor(thisFlooded, thisColor); // restore for backtracking
    }
}
