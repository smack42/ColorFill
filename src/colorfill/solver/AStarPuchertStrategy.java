/*  ColorFill game and solver
    Copyright (C) 2017, 2020 Michael Henke

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

/**
 * a specific strategy for the AStar (A*) solver.
 * <p>
 * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
 * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
 */
public class AStarPuchertStrategy implements AStarStrategy {

    protected final Board board;
    protected final ColorAreaSet visited;
    protected ColorAreaSet current, next;
    protected final ColorAreaSet[] casByColorBits;
    protected final ColorAreaSet.Iterator iter;
    protected final ColorAreaSet.IteratorAnd iterAnd;

    public AStarPuchertStrategy(final Board board) {
        this.board = board;
        this.visited = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
        this.casByColorBits = board.getCasByColorBitsArray();
        this.iter = new ColorAreaSet.Iterator();
        this.iterAnd = new ColorAreaSet.IteratorAnd();
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AStarStrategy#setEstimatedCost(colorfill.solver.AStarNode)
     */
    @Override
    public void setEstimatedCost(final AStarNode node, int nonCompletedColors) {

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
        node.copyNeighborsTo(this.current);
        node.copyFloodedTo(this.visited);

        while (true) {
            this.visited.addAll(this.current);
            int completedColors = 0;
            for (int colors = nonCompletedColors;  0 != colors;  ) {
                final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                colors ^= colorBit;
                if (this.visited.containsAll(this.casByColorBits[colorBit])) {
                    completedColors |= colorBit;
                    nonCompletedColors ^= colorBit;
                }
            }
            if (0 != completedColors) {
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColors);
                if (0 == (nonCompletedColors & (nonCompletedColors - 1))) { // one or zero colors remaining
                    distance += (0 == nonCompletedColors ? 0 : 1);
                    break; // done
                } else {
                    this.next.clear();
                    // completed colors
                    final ColorAreaSet colorCas = this.casByColorBits[completedColors];
                    this.iterAnd.init(this.current, colorCas);
                    for (int caId;  (caId = this.iterAnd.nextOrNegative()) >= 0;  ) {
                        this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                    }
                    this.current.removeAll(colorCas);
                    this.next.removeAll(this.visited);
                    // non-completed colors
                    // move nodes to next layer
                    this.next.addAll(this.current);
                }
            } else {
                this.next.clear();
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                ++distance;
                this.iter.init(this.current);
                for (int caId;  (caId = this.iter.nextOrNegative()) >= 0;  ) {
                    this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                }
                this.next.removeAll(this.visited);
            }

            // Move the next layer into the current.
            final ColorAreaSet t = this.current;
            this.current = this.next;
            this.next = t;
        }
        node.setEstimatedCost(node.getSolutionSize() + distance);
    }
}
