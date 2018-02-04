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

    private final Board board;
    private final ColorAreaSet visited;
    private ColorAreaSet current, next;
    private final short[] numCaNotFilledInitial;
    private final short[] numCaNotFilled;

    public AStarPuchertStrategy(final Board board) {
        this.board = board;
        this.visited = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
        this.numCaNotFilledInitial = new short[board.getNumColors()];
        for (final ColorArea ca : board.getColorAreasArray()) {
            ++this.numCaNotFilledInitial[ca.getColor()];
        }
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
        System.arraycopy(this.numCaNotFilledInitial, 0, this.numCaNotFilled, 0, this.numCaNotFilledInitial.length);
        {
            final ColorAreaSet.IteratorColorAreaId iter = this.visited.iteratorColorAreaId();
            while (iter.hasNext()) {
                --this.numCaNotFilled[this.board.getColor4Id(iter.next())];
            }
        }

        // visit the first layer of neighbors, which is never empty, i.e. the puzzle is not solved yet
        node.copyNeighborsTo(this.current);
        this.visited.addAll(this.current);
        int completedColors = 0;
        {
            final ColorAreaSet.IteratorColorAreaId iter = this.current.iteratorColorAreaId();
            while (iter.hasNext()) {
                final byte nextColor = this.board.getColor4Id(iter.next());
                if (--this.numCaNotFilled[nextColor] == 0) {
                    completedColors |= 1 << nextColor;
                }
            }
        }
        int distance = 1;

        while(!this.current.isEmpty()) {
            this.next.clear();
            final ColorAreaSet.IteratorColorAreaId iter = this.current.iteratorColorAreaId();
            if (0 != completedColors) {
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColors);
                final int prevCompletedColors = completedColors;
                completedColors = 0;
                while (iter.hasNext()) {
                    final ColorArea thisCa = this.board.getColorArea4Id(iter.next());
                    if ((prevCompletedColors & (1 << thisCa.getColor())) != 0) {
                        // completed color
                        // expandNode()
                        for (final ColorArea nextCa : thisCa.getNeighborsArray()) {
                            final int nextCaId = nextCa.getId();
                            if (!this.visited.contains(nextCaId)) {
                                this.visited.add(nextCaId);
                                this.next.add(nextCaId);
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
                while (iter.hasNext()) {
                    final ColorArea thisCa = this.board.getColorArea4Id(iter.next());
                    // expandNode()
                    for (final ColorArea nextCa : thisCa.getNeighborsArray()) {
                        final int nextCaId = nextCa.getId();
                        if (!this.visited.contains(nextCaId)) {
                            this.visited.add(nextCaId);
                            this.next.add(nextCaId);
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
        }
        node.setEstimatedCost(node.getSolutionSize() + distance);
    }

}
