/*  ColorFill game and solver
    Copyright (C) 2020, 2021 Michael Henke

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

    protected final long[] casNextOne, casNextTwo;
    private final int caLimit;
    private final int[] idsMemberSize;

    public AStarFlolleStrategy(final Board board) {
        super(board);
        this.caLimit = board.getColorAreasArray().length / 3; // TODO: find a good value for caLimit
        this.casNextOne = ColorAreaSet.constructor(board);
        this.casNextTwo = ColorAreaSet.constructor(board);
        this.idsMemberSize = board.getMemberSize4IdArray();
    }

    @Override
    public int estimateCost(final AStarNode node, int nonCompletedColors) {
        // this method is basically copy&paste of method AStarPuchertStrategy.setEstimatedCost(AStarNode)
        // with important changes in the code block following the comment "Nothing found, do the color-blind pseudo-move"

        // An inadmissible heuristic for Flood-It. Using this strategy will result in optimal or close to optimal solutions.
        // If more than caLimit (half?) of the fields are taken, this strategy will just call AStarPuchertStrategy.setEstimatedCost.
        // Otherwise, it works very similar to AStarPuchertStrategy, with the difference that instead of purely color-blind
        // moves it will only take two of the colors sorted by the amount of new border fields they give access to.

        int distance = 0;
        // already more than <caLimit> of the color areas are flooded, so call the admissible strategy.
        // note: we're counting color areas here, unlike Flolle's "terminal-flood" which counted the individual fields (slower)
        if (node.getFloodedSize() > this.caLimit) {
            return super.estimateCost(node, nonCompletedColors); // AStarPuchertStrategy
        }

        long[] next = this.casNext;
        long[] current = this.casCurrent;
        node.copyNeighborsTo(current);
        node.copyFloodedTo(this.casVisited);
        long[] nextOne = this.casNextOne;
        long[] nextTwo = this.casNextTwo;

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
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                // Flolle's "terminal-flood" InadmissibleSlowStrategy: choose the two colors that give access to the most new border fields.
                ++distance;
                int sizeOne = 0, colorBitOne = 0, sizeTwo = 0, colorBitTwo = 0;
                for (int colors = nonCompletedColors;  0 != colors;  ) {
                    final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                    colors ^= colorBit;
                    ColorAreaSet.clear(next);
                    ColorAreaSet.addAllAndLookup(next, current, this.casByColorBits[colorBit], this.idsNeighborColorAreaSets);
                    ColorAreaSet.removeAll(next, this.casVisited);
                    int size = 0;
                    this.iter.init(next);
                    for (int caId;  (caId = this.iter.nextOrNegative()) >= 0;  ) {
                        size += this.idsMemberSize[caId];
                    }
                    if (size > sizeOne) { // new best color -> move previous best color to second best
                        sizeTwo = sizeOne;
                        colorBitTwo = colorBitOne;
                        final long[] t = nextTwo;
                        nextTwo = nextOne;
                        sizeOne = size;
                        colorBitOne = colorBit;
                        nextOne = next;
                        next = t;
                    } else if (size > sizeTwo) { // new second best color
                        sizeTwo = size;
                        colorBitTwo = colorBit;
                        final long[] t = nextTwo;
                        nextTwo = next;
                        next = t;
                    }
                }
                final long[] t = next;
                next = nextOne; // always take the best color
                nextOne = t;
                if (sizeTwo > 0) { // if available, take the second best color as well 
                    ColorAreaSet.addAll(next, nextTwo);
                }
                ColorAreaSet.removeAll(current, this.casByColorBits[colorBitOne | colorBitTwo]);
                ColorAreaSet.addAll(next, current); // move other colors to next
            }

            // Move the next layer into the current.
            final long[] t = current;
            current = next;
            next = t;
        }
    }
}
