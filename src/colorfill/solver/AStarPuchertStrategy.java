/*  ColorFill game and solver
    Copyright (C) 2017 - 2025 Michael Henke

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
    public int estimateCost(final AStarNode node, int nonCompletedColorBits) {

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
            int completedColorBits = 0;
            for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                final int colorBit = Integer.lowestOneBit(colors);
                if (ColorAreaSet.containsAll(this.casVisited, this.casByColorBits[colorBit])) {
                    completedColorBits |= colorBit;
                }
            }
            if (0 != completedColorBits) {
                nonCompletedColorBits ^= completedColorBits;
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColorBits);
                if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                    distance += (-nonCompletedColorBits >>> 31); // nonCompletedColorBits is never negative // (0 == nonCompletedColorBits ? 0 : 1)
                    return distance; // done
                } else {
                    ColorAreaSet.clear(next);
                    // completed colors
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    ColorAreaSet.addAllAndLookup(next, current, casColors, this.idsNeighborColorAreaSets);
                    ColorAreaSet.removeAll(current, casColors);
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
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            while (true) {
                ++distance;
                visited0 |= current0;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                }
                current0 = next0 & ~visited0;
            }
        }
    }


    static class AStarPuchertStrategy_2 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_2(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-2 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            while (true) {
                ++distance;
                visited0 |= current0;
                visited1 |= current1;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0, next1 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                    next1 = (current1 & ~casColors[1]);   current1 ^= next1;   visited1 ^= next1;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                }
                while (current1 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                    current1 &= current1 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                }
                current0 = next0 & ~visited0;
                current1 = next1 & ~visited1;
            }
        }
    }


    static class AStarPuchertStrategy_3 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_3(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-3 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
            int distance = 0;
            long current0 = this.storage.get(node.getNeighbors(), 0);
            long current1 = this.storage.get(node.getNeighbors(), 1);
            long current2 = this.storage.get(node.getNeighbors(), 2);
            long visited0 = this.storage.get(node.getFlooded(), 0);
            long visited1 = this.storage.get(node.getFlooded(), 1);
            long visited2 = this.storage.get(node.getFlooded(), 2);
            while (true) {
                ++distance;
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0, next1 = 0, next2 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                    next1 = (current1 & ~casColors[1]);   current1 ^= next1;   visited1 ^= next1;
                    next2 = (current2 & ~casColors[2]);   current2 ^= next2;   visited2 ^= next2;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                }
                while (current1 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                    current1 &= current1 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                }
                while (current2 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                    current2 &= current2 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                }
                current0 = next0 & ~visited0;
                current1 = next1 & ~visited1;
                current2 = next2 & ~visited2;
            }
        }
    }


    static class AStarPuchertStrategy_4 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_4(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-4 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
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
                ++distance;
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0, next1 = 0, next2 = 0, next3 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                    next1 = (current1 & ~casColors[1]);   current1 ^= next1;   visited1 ^= next1;
                    next2 = (current2 & ~casColors[2]);   current2 ^= next2;   visited2 ^= next2;
                    next3 = (current3 & ~casColors[3]);   current3 ^= next3;   visited3 ^= next3;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                }
                while (current1 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                    current1 &= current1 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                }
                while (current2 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                    current2 &= current2 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                }
                while (current3 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                    current3 &= current3 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                }
                current0 = next0 & ~visited0;
                current1 = next1 & ~visited1;
                current2 = next2 & ~visited2;
                current3 = next3 & ~visited3;
            }
        }
    }


    static class AStarPuchertStrategy_5 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_5(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-5 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
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
                ++distance;
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                visited4 |= current4;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3]) &&
                        ((visited4 & casColor[4]) == casColor[4])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0, next1 = 0, next2 = 0, next3 = 0, next4 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                    next1 = (current1 & ~casColors[1]);   current1 ^= next1;   visited1 ^= next1;
                    next2 = (current2 & ~casColors[2]);   current2 ^= next2;   visited2 ^= next2;
                    next3 = (current3 & ~casColors[3]);   current3 ^= next3;   visited3 ^= next3;
                    next4 = (current4 & ~casColors[4]);   current4 ^= next4;   visited4 ^= next4;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                }
                while (current1 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                    current1 &= current1 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                }
                while (current2 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                    current2 &= current2 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                }
                while (current3 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                    current3 &= current3 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                }
                while (current4 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(current4)];
                    current4 &= current4 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                }
                current0 = next0 & ~visited0;
                current1 = next1 & ~visited1;
                current2 = next2 & ~visited2;
                current3 = next3 & ~visited3;
                current4 = next4 & ~visited4;
            }
        }
    }


    static class AStarPuchertStrategy_6 extends AStarPuchertStrategy {
        public AStarPuchertStrategy_6(final Board board, final StateStorage storage) {
            super(board, storage);  //System.out.println("-64-6 !!");
        }
        @Override
        public int estimateCost(final AStarNode node, int nonCompletedColorBits) {
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
                ++distance;
                visited0 |= current0;
                visited1 |= current1;
                visited2 |= current2;
                visited3 |= current3;
                visited4 |= current4;
                visited5 |= current5;
                int completedColorBits = 0;
                for (int colors = nonCompletedColorBits;  0 != colors;  colors &= colors - 1) {
                    final int colorBit = Integer.lowestOneBit(colors);
                    final long[] casColor = this.casByColorBits[colorBit];
                    if (((visited0 & casColor[0]) == casColor[0]) &&
                        ((visited1 & casColor[1]) == casColor[1]) &&
                        ((visited2 & casColor[2]) == casColor[2]) &&
                        ((visited3 & casColor[3]) == casColor[3]) &&
                        ((visited4 & casColor[4]) == casColor[4]) &&
                        ((visited5 & casColor[5]) == casColor[5])) {
                        completedColorBits |= colorBit;
                    }
                }
                long next0 = 0, next1 = 0, next2 = 0, next3 = 0, next4 = 0, next5 = 0;
                if (0 != completedColorBits) {
                    nonCompletedColorBits ^= completedColorBits;
                    distance += Integer.bitCount(completedColorBits) - 1;
                    if (0 == (nonCompletedColorBits & (nonCompletedColorBits - 1))) { // one or zero colors remaining
                        return distance + (-nonCompletedColorBits >>> 31); // done
                    }
                    final long[] casColors = this.casByColorBits[completedColorBits];
                    next0 = (current0 & ~casColors[0]);   current0 ^= next0;   visited0 ^= next0;
                    next1 = (current1 & ~casColors[1]);   current1 ^= next1;   visited1 ^= next1;
                    next2 = (current2 & ~casColors[2]);   current2 ^= next2;   visited2 ^= next2;
                    next3 = (current3 & ~casColors[3]);   current3 ^= next3;   visited3 ^= next3;
                    next4 = (current4 & ~casColors[4]);   current4 ^= next4;   visited4 ^= next4;
                    next5 = (current5 & ~casColors[5]);   current5 ^= next5;   visited5 ^= next5;
                }
                while (current0 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[Long.numberOfTrailingZeros(current0)];
                    current0 &= current0 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                while (current1 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 1 + Long.numberOfTrailingZeros(current1)];
                    current1 &= current1 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                while (current2 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 2 + Long.numberOfTrailingZeros(current2)];
                    current2 &= current2 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                while (current3 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 3 + Long.numberOfTrailingZeros(current3)];
                    current3 &= current3 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                while (current4 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 4 + Long.numberOfTrailingZeros(current4)];
                    current4 &= current4 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                while (current5 != 0) {
                    final long[] casAdd = this.idsNeighborColorAreaSets[64 * 5 + Long.numberOfTrailingZeros(current5)];
                    current5 &= current5 - 1; // clear the least significant bit set
                    next0 |= casAdd[0];
                    next1 |= casAdd[1];
                    next2 |= casAdd[2];
                    next3 |= casAdd[3];
                    next4 |= casAdd[4];
                    next5 |= casAdd[5];
                }
                current0 = next0 & ~visited0;
                current1 = next1 & ~visited1;
                current2 = next2 & ~visited2;
                current3 = next3 & ~visited3;
                current4 = next4 & ~visited4;
                current5 = next5 & ~visited5;
            }
        }
    }


}
