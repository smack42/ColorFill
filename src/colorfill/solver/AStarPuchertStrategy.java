/*  ColorFill game and solver
    Copyright (C) 2014, 2017 Michael Henke

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
import colorfill.model.ColorArea;

/**
 * a specific strategy for the AStar (A*) solver.
 * <p>
 * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
 * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
 */
public class AStarPuchertStrategy implements AStarStrategy {

    private final ColorAreaSet visited;
    private ColorAreaSet current, next;
    private final short[] numCaNotFilled;

    AStarPuchertStrategy(final Board board) {
        this.visited = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
        this.numCaNotFilled = new short[board.getNumColors()];
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AStarStrategy#setEstimatedCost(colorfill.solver.AStarNode)
     */
    @Override
    public void setEstimatedCost(final AStarNode node) {

        // quote from floodit.cpp: int State::computeValuation()
        // (in branch "performance")
        //
        // We compute an admissible heuristic recursively: If there are no nodes
        // left, return 0. Furthermore, if a color can be eliminated in one move
        // from the current position, that move is an optimal move and we can
        // simply use it. Otherwise, all moves fill a subset of the neighbors of
        // the filled nodes. Thus, filling that layer gets us at least one step
        // closer to the end.

        node.copyFloodedTo(this.visited);
        node.copyFloodedTo(this.current);
        node.copyNumCaNotFilledTo(this.numCaNotFilled);

        int completedColors = 0;
        int distance = 0;
        while(!this.current.isEmpty()) {
            if (0 != completedColors) {
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColors);
                final int prevCompletedColors = completedColors;
                completedColors = 0;
                for (final ColorArea thisCa : this.current) {
                    if ((prevCompletedColors & (1 << thisCa.getColor())) != 0) {
                        // completed color
                        // expandNode()
                        for (final ColorArea nextCa : thisCa.getNeighborsArray()) {
                            if (!this.visited.contains(nextCa)) {
                                this.visited.add(nextCa);
                                this.next.add(nextCa);
                                if (--this.numCaNotFilled[nextCa.getColor()] == 0) {
                                    completedColors |= 1 << nextCa.getColor();
                                }
                            }
                        }
                    } else {
                        // non-completed color
                        // move node to next layer
                        this.next.add(thisCa);
                    }
                }
            } else {
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                ++distance;
                for (final ColorArea thisCa : this.current) {
                    // expandNode()
                    for (final ColorArea nextCa : thisCa.getNeighborsArray()) {
                        if (!this.visited.contains(nextCa)) {
                            this.visited.add(nextCa);
                            this.next.add(nextCa);
                            if (--this.numCaNotFilled[nextCa.getColor()] == 0) {
                                completedColors |= 1 << nextCa.getColor();
                            }
                        }
                    }
                }
            }

            // Move the next layer into the current.
            final ColorAreaSet tmp = this.current;
            this.current = this.next;
            this.next = tmp;
            this.next.clear();
        }
        node.setEstimatedCost(node.getSolutionSize() + distance);
    }

}
