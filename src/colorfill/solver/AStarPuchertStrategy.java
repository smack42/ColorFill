/*  ColorFill game and solver
    Copyright (C) 2017, 2020, 2021, 2022 Michael Henke

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

    public static AStarPuchertStrategy getInstance(final Board board, final StateStorage storage) {
        switch (board.getSizeColorAreas64()) {
        case 1:  return new AStarPuchertStrategy_1(board, storage);
        case 2:  return new AStarPuchertStrategy_2(board, storage);
        case 3:  return new AStarPuchertStrategy_3(board, storage);
        case 4:  return new AStarPuchertStrategy_4(board, storage);
        case 5:  return new AStarPuchertStrategy_5(board, storage);
        case 6:  return new AStarPuchertStrategy_6(board, storage);
        default: return new AStarPuchertStrategy  (board, storage);
        }
    }

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
            for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                final int colorBit = Integer.lowestOneBit(colors);
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




    // below are the performance-optimized versions of this class
    // (manually inlined functions and unrolled loops)


    static class AStarPuchertStrategy_1 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_1(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-1 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            while (true) {
                visited0 |= current0;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                        }
                        current0 |= l0 & ~visited0;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                    }
                    current0 = l0 & ~visited0;
                }
            }
        }
    }


    static class AStarPuchertStrategy_2 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_2(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-2 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            while (true) {
                visited0 |= current0;
                visited1 |= current1;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0, l1 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                        }
                        buf = (current1 & colorCas[1]);
                        current1 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                        }
                        current0 |= l0 & ~visited0;
                        current1 |= l1 & ~visited1;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0, l1 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                    }
                    while (current1 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                        current1 &= current1 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                    }
                    current0 = l0 & ~visited0;
                    current1 = l1 & ~visited1;
                }
            }
        }
    }


    static class AStarPuchertStrategy_3 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_3(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-3 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long current2 = this.storage.get(node.getNeighbors(), 2);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            long visited2 = this.storage.get(node.getFlooded(), 2);
            while (true) {
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0, l1 = 0, l2 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                        }
                        buf = (current1 & colorCas[1]);
                        current1 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                        }
                        buf = (current2 & colorCas[2]);
                        current2 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                        }
                        current0 |= l0 & ~visited0;
                        current1 |= l1 & ~visited1;
                        current2 |= l2 & ~visited2;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0, l1 = 0, l2 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                    }
                    while (current1 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                        current1 &= current1 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                    }
                    while (current2 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                        current2 &= current2 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                    }
                    current0 = l0 & ~visited0;
                    current1 = l1 & ~visited1;
                    current2 = l2 & ~visited2;
                }
            }
        }
    }


    static class AStarPuchertStrategy_4 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_4(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-4 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long current2 = this.storage.get(node.getNeighbors(), 2);
            long current3 = this.storage.get(node.getNeighbors(), 3);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            long visited2 = this.storage.get(node.getFlooded(), 2);
            long visited3 = this.storage.get(node.getFlooded(), 3);
            while (true) {
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0, l1 = 0, l2 = 0, l3 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                        }
                        buf = (current1 & colorCas[1]);
                        current1 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                        }
                        buf = (current2 & colorCas[2]);
                        current2 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                        }
                        buf = (current3 & colorCas[3]);
                        current3 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                        }
                        current0 |= l0 & ~visited0;
                        current1 |= l1 & ~visited1;
                        current2 |= l2 & ~visited2;
                        current3 |= l3 & ~visited3;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0, l1 = 0, l2 = 0, l3 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                    }
                    while (current1 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                        current1 &= current1 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                    }
                    while (current2 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                        current2 &= current2 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                    }
                    while (current3 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                        current3 &= current3 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                    }
                    current0 = l0 & ~visited0;
                    current1 = l1 & ~visited1;
                    current2 = l2 & ~visited2;
                    current3 = l3 & ~visited3;
                }
            }
        }
    }


    static class AStarPuchertStrategy_5 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_5(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-5 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long current2 = this.storage.get(node.getNeighbors(), 2);
            long current3 = this.storage.get(node.getNeighbors(), 3);
            long current4 = this.storage.get(node.getNeighbors(), 4);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            long visited2 = this.storage.get(node.getFlooded(), 2);
            long visited3 = this.storage.get(node.getFlooded(), 3);
            long visited4 = this.storage.get(node.getFlooded(), 4);
            while (true) {
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                visited4 |= current4;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3]) &&
                        ((visited4 & casColor[4]) == casColor[4])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0, l1 = 0, l2 = 0, l3 = 0, l4 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                        }
                        buf = (current1 & colorCas[1]);
                        current1 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                        }
                        buf = (current2 & colorCas[2]);
                        current2 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                        }
                        buf = (current3 & colorCas[3]);
                        current3 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                        }
                        buf = (current4 & colorCas[4]);
                        current4 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                        }
                        current0 |= l0 & ~visited0;
                        current1 |= l1 & ~visited1;
                        current2 |= l2 & ~visited2;
                        current3 |= l3 & ~visited3;
                        current4 |= l4 & ~visited4;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0, l1 = 0, l2 = 0, l3 = 0, l4 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                    }
                    while (current1 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                        current1 &= current1 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                    }
                    while (current2 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                        current2 &= current2 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                    }
                    while (current3 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                        current3 &= current3 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                    }
                    while (current4 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(current4)];
                        current4 &= current4 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                    }
                    current0 = l0 & ~visited0;
                    current1 = l1 & ~visited1;
                    current2 = l2 & ~visited2;
                    current3 = l3 & ~visited3;
                    current4 = l4 & ~visited4;
                }
            }
        }
    }


    static class AStarPuchertStrategy_6 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_6(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-6 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColors) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long current2 = this.storage.get(node.getNeighbors(), 2);
            long current3 = this.storage.get(node.getNeighbors(), 3);
            long current4 = this.storage.get(node.getNeighbors(), 4);
            long current5 = this.storage.get(node.getNeighbors(), 5);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            long visited2 = this.storage.get(node.getFlooded(), 2);
            long visited3 = this.storage.get(node.getFlooded(), 3);
            long visited4 = this.storage.get(node.getFlooded(), 4);
            long visited5 = this.storage.get(node.getFlooded(), 5);
            while (true) {
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                visited4 |= current4;
                visited5 |= current5;
                int completedColors = 0;
                for (int colors = nonCompletedColors;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3]) &&
                        ((visited4 & casColor[4]) == casColor[4]) &&
                        ((visited5 & casColor[5]) == casColor[5])) {
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
                        // completed colors
                        final long[] colorCas = this.casByColorBits[completedColors];
                        // non-completed colors
                        // move nodes to next layer
                        long l0 = 0, l1 = 0, l2 = 0, l3 = 0, l4 = 0, l5 = 0;
                        long buf = (current0 & colorCas[0]);
                        current0 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        buf = (current1 & colorCas[1]);
                        current1 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        buf = (current2 & colorCas[2]);
                        current2 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        buf = (current3 & colorCas[3]);
                        current3 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        buf = (current4 & colorCas[4]);
                        current4 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        buf = (current5 & colorCas[5]);
                        current5 ^= buf;
                        while (buf != 0) {
                            final long[] casAdd = this.idsNeighborColorAreaSets[64 * 5 + Long.numberOfTrailingZeros(buf)];
                            buf &= buf - 1; // clear the least significant bit set
                            l0 |= casAdd[0];
                            l1 |= casAdd[1];
                            l2 |= casAdd[2];
                            l3 |= casAdd[3];
                            l4 |= casAdd[4];
                            l5 |= casAdd[5];
                        }
                        current0 |= l0 & ~visited0;
                        current1 |= l1 & ~visited1;
                        current2 |= l2 & ~visited2;
                        current3 |= l3 & ~visited3;
                        current4 |= l4 & ~visited4;
                        current5 |= l5 & ~visited5;
                    }
                } else {
                    // Nothing found, do the color-blind pseudo-move
                    // Expand current layer of nodes.
                    ++distance;
                    long l0 = 0, l1 = 0, l2 = 0, l3 = 0, l4 = 0, l5 = 0;
                    while (current0 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                        current0 &= current0 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    while (current1 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                        current1 &= current1 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    while (current2 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                        current2 &= current2 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    while (current3 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                        current3 &= current3 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    while (current4 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(current4)];
                        current4 &= current4 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    while (current5 != 0) {
                        final long[] casAdd = this.idsNeighborColorAreaSets[64 * 5 + Long.numberOfTrailingZeros(current5)];
                        current5 &= current5 - 1; // clear the least significant bit set
                        l0 |= casAdd[0];
                        l1 |= casAdd[1];
                        l2 |= casAdd[2];
                        l3 |= casAdd[3];
                        l4 |= casAdd[4];
                        l5 |= casAdd[5];
                    }
                    current0 = l0 & ~visited0;
                    current1 = l1 & ~visited1;
                    current2 = l2 & ~visited2;
                    current3 = l3 & ~visited3;
                    current4 = l4 & ~visited4;
                    current5 = l5 & ~visited5;
                }
            }
        }
    }


}
