/*  ColorFill game and solver
    Copyright (C) 2020 Michael Henke

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
 * the idea is taken from the program "terminal-flood" by Flolle (Florian Fischer),
 * which can be found at <a>https://github.com/Flolle/terminal-flood</a>
 */
public class AStarFlolleStrategy extends AStarPuchertStrategy {

    private final ColorAreaSetIteratorAnd iterAnd;
    protected ColorAreaSet nextOne, nextTwo;
    private final int caLimit;

    public AStarFlolleStrategy(final Board board) {
        super(board);
        this.iterAnd = new ColorAreaSetIteratorAnd();
        this.caLimit = board.getColorAreasArray().length / 3; // TODO: find a good value for caLimit
        this.nextOne = new ColorAreaSet(board);
        this.nextTwo = new ColorAreaSet(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AStarStrategy#setEstimatedCost(colorfill.solver.AStarNode)
     */
    @Override
    public void setEstimatedCost(final AStarNode node) {
        // this method is basically copy&paste of method AStarPuchertStrategy.setEstimatedCost(AStarNode)
        // with important changes in the code block following the comment "Nothing found, do the color-blind pseudo-move"

        // An inadmissible heuristic for Flood-It. Using this strategy will result in optimal or close to optimal solutions.
        // If more than caLimit (half?) of the fields are taken, this strategy will just call AStarPuchertStrategy.setEstimatedCost.
        // Otherwise, it works very similar to AStarPuchertStrategy, with the difference that instead of purely color-blind
        // moves it will only take two of the colors sorted by the amount of new border fields they give access to.

        int distance = 0;
        if (!node.isSolved()) {
            // already more than <caLimit> of the color areas are flooded, so call the admissible strategy.
            // note: we're counting color areas here, unlike Flolle's "terminal-flood" which counted the individual fields (slower)
            if (node.getFloodedSize() > this.caLimit) {
                super.setEstimatedCost(node); // AStarPuchertStrategy
                return;
            }

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
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    // Flolle's "terminal-flood" InadmissibleSlowStrategy: choose the two colors that give access to the most new border fields.
                    ++distance;
                    int colors = nonCompletedColors, sizeOne = 0, colorOne = 0, sizeTwo = 0, colorTwo = 0;
                    while (true) {
                        final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                        final int color = 31 - Integer.numberOfLeadingZeros(colorBit);
                        this.next.clear();
                        this.iterAnd.init(this.current, this.casByColor[color]);
                        int caId;
                        while ((caId = this.iterAnd.nextOrNegative()) >= 0) {
                            this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                        }
                        this.next.removeAll(this.visited);
                        int size = 0;
                        this.iter.init(this.next);
                        while ((caId = this.iter.nextOrNegative()) >= 0) {
                            size += this.board.getColorArea4Id(caId).getMemberSize();
                        }
                        if (size > sizeOne) { // new best color -> move previous best color to second best
                            sizeTwo = sizeOne;
                            colorTwo = colorOne;
                            final ColorAreaSet t = this.nextTwo;
                            this.nextTwo = this.nextOne;
                            sizeOne = size;
                            colorOne = color;
                            this.nextOne = this.next;
                            this.next = t;
                        } else if (size > sizeTwo) { // new second best color
                            sizeTwo = size;
                            colorTwo = color;
                            final ColorAreaSet t = this.nextTwo;
                            this.nextTwo = this.next;
                            this.next = t;
                        }
                        if (0 == (colors ^= colorBit)) {
                            break;
                        }
                    }
                    final ColorAreaSet t = this.next;
                    this.next = this.nextOne; // always take the best color
                    this.nextOne = t;
                    this.current.removeAll(this.casByColor[colorOne]);
                    if (sizeTwo > 0) { // if available, take the second best color as well 
                        this.next.addAll(this.nextTwo);
                        this.current.removeAll(this.casByColor[colorTwo]);
                    }
                    this.next.addAll(this.current); // move other colors to next
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
