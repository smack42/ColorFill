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
import colorfill.model.ColorArea;
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
    protected final ColorAreaSet[] casByColor;
    protected final ColorAreaSet.Iterator iter;
    private final ColorAreaSetIteratorAnd iterAnd;
    protected final int allColors;

    public AStarPuchertStrategy(final Board board) {
        this.board = board;
        this.visited = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
        this.casByColor = new ColorAreaSet[board.getNumColors()];
        for (int color = 0;  color < this.casByColor.length;  ++color) {
            this.casByColor[color] = new ColorAreaSet(board);
        }
        for (final ColorArea ca : board.getColorAreasArray()) {
            this.casByColor[ca.getColor()].add(ca);
        }
        this.iter = new ColorAreaSet.Iterator();
        this.iterAnd = new ColorAreaSetIteratorAnd();
        this.allColors = (1 << board.getNumColors()) - 1;
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

        int distance = 0;
        if (!node.isSolved()) {
            node.copyNeighborsTo(this.current);
            node.copyFloodedTo(this.visited);
            int nonCompletedColors = this.allColors;
            for (int color = 0;  color < this.casByColor.length;  ++color) {
                if (this.casByColor[color].isEmptyDifference(this.visited)) {
                    nonCompletedColors ^= 1 << color;
                }
            }

            while (true) {
                this.visited.addAll(this.current);
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  ) {
                    final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                    final int color = 31 - Integer.numberOfLeadingZeros(colorBit);
                    colors ^= colorBit;
                    if (this.casByColor[color].isEmptyDifference(this.visited)) {
                        completedColors |= colorBit;
                        nonCompletedColors ^= colorBit;
                    }
                }
                if (0 != completedColors) {
                    // We can eliminate colors. Do just that.
                    // We also combine all these elimination moves.
                    distance += Integer.bitCount(completedColors);
                    if (0 == nonCompletedColors) {
                        break; // done
                    } else {
                        this.next.clear();
                        // completed colors
                        while (true) {
                            final int colorBit = completedColors & -completedColors;  // Integer.lowestOneBit(completedColors);
                            final int color = 31 - Integer.numberOfLeadingZeros(colorBit);
                            final ColorAreaSet colorCas = this.casByColor[color];
                            this.iterAnd.init(this.current, colorCas);
                            int caId;
                            while ((caId = this.iterAnd.nextOrNegative()) >= 0) {
                                this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                            }
                            this.current.removeAll(colorCas);
                            if (0 == (completedColors ^= colorBit)) {
                                break;
                            }
                        }
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
                    int caId;
                    while ((caId = this.iter.nextOrNegative()) >= 0) {
                        this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                    }
                    this.next.removeAll(this.visited);
                }

                // Move the next layer into the current.
                final ColorAreaSet t = this.current;
                this.current = this.next;
                this.next = t;
            }
        }
        node.setEstimatedCost(node.getSolutionSize() + distance);
    }



    /**
     * an Iterator over two ColorAreaSets combined with AND, that returns the IDs of the member ColorArea objects that are contained in both sets
     */
    private static class ColorAreaSetIteratorAnd {
        private long[] array1, array2;
        private int longIdxLimit;
        private int longIdx;
        private long buf;

        /**
         * initialize this Iterator for use with these ColorAreaSets.
         */
        private void init(final ColorAreaSet caSet1, final ColorAreaSet caSet2) {
            this.array1 = caSet1.getArray();
            this.array2 = caSet2.getArray();
            this.longIdxLimit = this.array1.length - 1;
            this.longIdx = 0;
            this.buf = this.array1[0] & this.array2[0];
        }

        /**
         * return next value (always zero or positive),
         * or a negative value when there is no next value.
         * @return
         */
        private int nextOrNegative() {
            while (0 == this.buf) {
                if (this.longIdxLimit == this.longIdx) {
                    return -1;
                } else {
                    ++this.longIdx;
                    this.buf = this.array1[this.longIdx] & this.array2[this.longIdx];
                }
            }
            final long l1b = this.buf & -this.buf;  // Long.lowestOneBit(this.buf)
            final int clz = Long.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
            final int caId = (this.longIdx << 6) + 63 - clz;
            this.buf ^= l1b;
            return caId;
        }
    }
}
