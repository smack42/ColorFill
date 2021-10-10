/*  ColorFill game and solver
    Copyright (C) 2017, 2020, 2021 Michael Henke

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

import colorfill.model.Board;
import colorfill.model.ColorAreaSet;
import colorfill.solver.AStarSolver.StateStorage;

/**
 * a specific strategy for the AStar (A*) solver.
 * <p>
 * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
 * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
 */
public class AStarPuchertStrategy implements AStarStrategy {

    protected final long[] casVisited, casCurrent, casNext;
    protected final long[][] casByColorBits;
    protected final long[][] idsNeighborColorAreaSets;
    protected final StateStorage storage;

    public AStarPuchertStrategy(final Board board, final StateStorage storage) {
        this.casVisited = ColorAreaSet.constructor(board);
        this.casCurrent = ColorAreaSet.constructor(board);
        this.casNext = ColorAreaSet.constructor(board);
        this.casByColorBits = board.getCasByColorBitsArray();
        this.idsNeighborColorAreaSets = board.getNeighborColorAreaSet4IdArray();
        this.storage = storage;
    }

    @Override
    public int estimateCost(final AStarNode node, int nonCompletedColors) {

        // quote from floodit.cpp: int State::computeValuation()
        // (in branch "performance")
        //
        // We compute an admissible heuristic recursively: If there are no nodes
        // left, return 0. Furthermore, if a color can be eliminated in one move
        // from the current position, that move is an optimal move and we can
        // simply use it. Otherwise, all moves fill a subset of the neighbors of
        // the filled nodes. Thus, filling that layer gets us at least one step
        // closer to the end.

        int distance = 0;
        long[] next = this.casNext;
        long[] current = this.casCurrent;
        this.storage.get(node.getNeighbors(), current);
        this.storage.get(node.getFlooded(), this.casVisited);

        while (true) {
            ColorAreaSet.addAll(this.casVisited, current);
            int completedColors = 0;
            for (int colors = nonCompletedColors;  0 != colors;  ) {
                final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                colors ^= colorBit;
                if (ColorAreaSet.containsAll(this.casVisited, this.casByColorBits[colorBit])) {
                    completedColors |= colorBit;
                }
            }
            if (0 != completedColors) {
                nonCompletedColors ^= completedColors;
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColors);
                if (0 == (nonCompletedColors & (nonCompletedColors - 1))) { // one or zero colors remaining
                    distance += (-nonCompletedColors >>> 31); // nonCompletedColors is never negative // (0 == nonCompletedColors ? 0 : 1)
                    return distance; // done
                } else {
                    ColorAreaSet.clear(next);
                    // completed colors
                    final long[] colorCas = this.casByColorBits[completedColors];
                    ColorAreaSet.addAllAndLookup(next, current, colorCas, this.idsNeighborColorAreaSets);
                    ColorAreaSet.removeAll(current, colorCas);
                    ColorAreaSet.removeAll(next, this.casVisited);
                    // non-completed colors
                    // move nodes to next layer
                    ColorAreaSet.addAll(next, current);
                }
            } else {
                ColorAreaSet.clear(next);
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                ++distance;
                ColorAreaSet.addAllLookup(next, current, this.idsNeighborColorAreaSets);
                ColorAreaSet.removeAll(next, this.casVisited);
            }

            // Move the next layer into the current.
            final long[] t = current;
            current = next;
            next = t;
        }
    }
}
