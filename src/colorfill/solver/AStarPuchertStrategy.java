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
    protected final StateStorage storage;
    protected final UnrolledFunctions unrolledFunctions;

    public AStarPuchertStrategy(final Board board, final StateStorage storage) {
        this.casVisited = ColorAreaSet.constructor(board);
        this.casCurrent = ColorAreaSet.constructor(board);
        this.casNext = ColorAreaSet.constructor(board);
        this.casByColorBits = board.getCasByColorBitsArray();
        this.storage = storage;
        this.unrolledFunctions = UnrolledFunctions.getInstance(board, this.casVisited);
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
                    this.unrolledFunctions.addAllAndLookupRemoveVisited(next, current, colorCas);
                    ColorAreaSet.removeAll(current, colorCas);
                    // non-completed colors
                    // move nodes to next layer
                    ColorAreaSet.addAll(next, current);
                }
            } else {
                ColorAreaSet.clear(next);
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                ++distance;
                this.unrolledFunctions.addAllLookupRemoveVisited(next, current);
            }

            // Move the next layer into the current.
            final long[] t = current;
            current = next;
            next = t;
        }
    }





    static class UnrolledFunctions {
        final long[][] casLookup;
        final long[] casVisited;
        UnrolledFunctions(final long[][] casLookup, final long[] casVisited) {
            this.casLookup = casLookup;
            this.casVisited = casVisited;
        }
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    for (int i = 0;  i < casThis.length;  ++i) {
                        casThis[i] |= casAdd[i];
                    }
                }
            }
            for (int i = 0;  i < casThis.length;  ++i) {
                casThis[i] &= ~(this.casVisited[i]);
            }
        }
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    for (int i = 0;  i < casThis.length;  ++i) {
                        casThis[i] |= casAdd[i];
                    }
                }
            }
            for (int i = 0;  i < casThis.length;  ++i) {
                casThis[i] &= ~(this.casVisited[i]);
            }
        }
        static UnrolledFunctions getInstance(final Board board, final long[] casVisited) {
            final long[][] casLookup = board.getNeighborColorAreaSet4IdArray();
            switch (board.getSizeColorAreas64()) {
            case 1:     return new UnrolledFunctions01(casLookup, casVisited);
            case 2:     return new UnrolledFunctions02(casLookup, casVisited);
            case 3:     return new UnrolledFunctions03(casLookup, casVisited);
            case 4:     return new UnrolledFunctions04(casLookup, casVisited);
            case 5:     return new UnrolledFunctions05(casLookup, casVisited);
            case 6:     return new UnrolledFunctions06(casLookup, casVisited);
            case 7:     return new UnrolledFunctions07(casLookup, casVisited);
            case 8:     return new UnrolledFunctions08(casLookup, casVisited);
            case 9:     return new UnrolledFunctions09(casLookup, casVisited);
            case 10:    return new UnrolledFunctions10(casLookup, casVisited);
            default:    return new UnrolledFunctions  (casLookup, casVisited);
            }
        }
    }

    static class UnrolledFunctions01 extends UnrolledFunctions {
        UnrolledFunctions01(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
        }
    }

    static class UnrolledFunctions02 extends UnrolledFunctions {
        UnrolledFunctions02(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
        }
    }

    static class UnrolledFunctions03 extends UnrolledFunctions {
        UnrolledFunctions03(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
        }
    }

    static class UnrolledFunctions04 extends UnrolledFunctions {
        UnrolledFunctions04(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
        }
    }

    static class UnrolledFunctions05 extends UnrolledFunctions {
        UnrolledFunctions05(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
        }
    }

    static class UnrolledFunctions06 extends UnrolledFunctions {
        UnrolledFunctions06(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
        }
    }

    static class UnrolledFunctions07 extends UnrolledFunctions {
        UnrolledFunctions07(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
        }
    }

    static class UnrolledFunctions08 extends UnrolledFunctions {
        UnrolledFunctions08(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
        }
    }

    static class UnrolledFunctions09 extends UnrolledFunctions {
        UnrolledFunctions09(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            long l8 = casThis[8];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                    l8 |= casAdd[8];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
            casThis[8] = l8 & ~(this.casVisited[8]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            long l8 = casThis[8];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                    l8 |= casAdd[8];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
            casThis[8] = l8 & ~(this.casVisited[8]);
        }
    }

    static class UnrolledFunctions10 extends UnrolledFunctions {
        UnrolledFunctions10(final long[][] casLookup, final long[] casVisited) {
            super(casLookup, casVisited);
        }
        @Override
        void addAllLookupRemoveVisited(final long[] casThis, final long[] casOther) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            long l8 = casThis[8];
            long l9 = casThis[9];
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                    l8 |= casAdd[8];
                    l9 |= casAdd[9];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
            casThis[8] = l8 & ~(this.casVisited[8]);
            casThis[9] = l9 & ~(this.casVisited[9]);
        }
        @Override
        void addAllAndLookupRemoveVisited(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            long l0 = casThis[0];
            long l1 = casThis[1];
            long l2 = casThis[2];
            long l3 = casThis[3];
            long l4 = casThis[4];
            long l5 = casThis[5];
            long l6 = casThis[6];
            long l7 = casThis[7];
            long l8 = casThis[8];
            long l9 = casThis[9];
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    l0 |= casAdd[0];
                    l1 |= casAdd[1];
                    l2 |= casAdd[2];
                    l3 |= casAdd[3];
                    l4 |= casAdd[4];
                    l5 |= casAdd[5];
                    l6 |= casAdd[6];
                    l7 |= casAdd[7];
                    l8 |= casAdd[8];
                    l9 |= casAdd[9];
                }
            }
            casThis[0] = l0 & ~(this.casVisited[0]);
            casThis[1] = l1 & ~(this.casVisited[1]);
            casThis[2] = l2 & ~(this.casVisited[2]);
            casThis[3] = l3 & ~(this.casVisited[3]);
            casThis[4] = l4 & ~(this.casVisited[4]);
            casThis[5] = l5 & ~(this.casVisited[5]);
            casThis[6] = l6 & ~(this.casVisited[6]);
            casThis[7] = l7 & ~(this.casVisited[7]);
            casThis[8] = l8 & ~(this.casVisited[8]);
            casThis[9] = l9 & ~(this.casVisited[9]);
        }
    }
}
