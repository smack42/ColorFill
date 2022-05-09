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
        this.unrolledFunctions = UnrolledFunctions.getInstance(board);
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
                    this.unrolledFunctions.addAllAndLookup(next, current, colorCas);
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
                this.unrolledFunctions.addAllLookup(next, current);
                ColorAreaSet.removeAll(next, this.casVisited);
            }

            // Move the next layer into the current.
            final long[] t = current;
            current = next;
            next = t;
        }
    }





    static class UnrolledFunctions {
        final long[][] casLookup;
        UnrolledFunctions(final long[][] casLookup) {
            this.casLookup = casLookup;
        }
        void addAllLookup(final long[] casThis, final long[] casOther) {
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
        }
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
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
        }
        static UnrolledFunctions getInstance(final Board board) {
            final long[][] casLookup = board.getNeighborColorAreaSet4IdArray();
            switch (board.getSizeColorAreas64()) {
            case 1:     return new UnrolledFunctions01(casLookup);
            case 2:     return new UnrolledFunctions02(casLookup);
            case 3:     return new UnrolledFunctions03(casLookup);
            case 4:     return new UnrolledFunctions04(casLookup);
            case 5:     return new UnrolledFunctions05(casLookup);
            case 6:     return new UnrolledFunctions06(casLookup);
            case 7:     return new UnrolledFunctions07(casLookup);
            case 8:     return new UnrolledFunctions08(casLookup);
            case 9:     return new UnrolledFunctions09(casLookup);
            case 10:    return new UnrolledFunctions10(casLookup);
            default:    return new UnrolledFunctions  (casLookup);
            }
        }
    }

    static class UnrolledFunctions01 extends UnrolledFunctions {
        UnrolledFunctions01(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                }
            }
        }
    }

    static class UnrolledFunctions02 extends UnrolledFunctions {
        UnrolledFunctions02(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                }
            }
        }
    }

    static class UnrolledFunctions03 extends UnrolledFunctions {
        UnrolledFunctions03(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                }
            }
        }
    }

    static class UnrolledFunctions04 extends UnrolledFunctions {
        UnrolledFunctions04(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                }
            }
        }
    }

    static class UnrolledFunctions05 extends UnrolledFunctions {
        UnrolledFunctions05(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                }
            }
        }
    }

    static class UnrolledFunctions06 extends UnrolledFunctions {
        UnrolledFunctions06(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                }
            }
        }
    }

    static class UnrolledFunctions07 extends UnrolledFunctions {
        UnrolledFunctions07(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                }
            }
        }
    }

    static class UnrolledFunctions08 extends UnrolledFunctions {
        UnrolledFunctions08(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                }
            }
        }
    }

    static class UnrolledFunctions09 extends UnrolledFunctions {
        UnrolledFunctions09(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                    casThis[8] |= casAdd[8];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                    casThis[8] |= casAdd[8];
                }
            }
        }
    }

    static class UnrolledFunctions10 extends UnrolledFunctions {
        UnrolledFunctions10(final long[][] casLookup) {
            super(casLookup);
        }
        @Override
        void addAllLookup(final long[] casThis, final long[] casOther) {
            for (int o = 0;  o < casOther.length;  ++o) {
                long buf = casOther[o];
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                    casThis[8] |= casAdd[8];
                    casThis[9] |= casAdd[9];
                }
            }
        }
        @Override
        void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo) {
            for (int o = 0;  o < casOtherOne.length;  ++o) {
                long buf = (casOtherOne[o] & casOtherTwo[o]);
                final int offset = (o << 6);
                while (buf != 0) {
                    final long[] casAdd = this.casLookup[offset + Long.numberOfTrailingZeros(buf)];
                    buf &= buf - 1; // clear the least significant bit set
                    casThis[0] |= casAdd[0];
                    casThis[1] |= casAdd[1];
                    casThis[2] |= casAdd[2];
                    casThis[3] |= casAdd[3];
                    casThis[4] |= casAdd[4];
                    casThis[5] |= casAdd[5];
                    casThis[6] |= casAdd[6];
                    casThis[7] |= casAdd[7];
                    casThis[8] |= casAdd[8];
                    casThis[9] |= casAdd[9];
                }
            }
        }
    }
}
