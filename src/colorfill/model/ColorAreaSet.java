/*  ColorFill game and solver
    Copyright (C) 2014, 2015 Michael Henke

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

package colorfill.model;

import java.util.Arrays;

/**
 * this class is a bespoke implementation of a Set of ColorArea.
 * <p>
 * there are static methods in this class, only.
 * no objects of this class can be created.
 * the actual data, an "array of long", is created by the "constructor" methods and is to be passed as a parameter to each method call.
 * <p>
 * of course, this is a bit unusual and certainly not the proper OOP-way of doing it.
 * however, the adavantages of this "static implementation with raw data" are: faster operation and lower memory usage.
 */
public final class ColorAreaSet {

    private ColorAreaSet() {
        throw new IllegalStateException("can't create any objects of this class!");
    }

    /**
     * the constructor
     */
    public static long[] constructor(final Board board) {
        return new long[(board.getSizeColorAreas8() + 7) >> 3];
    }

    /**
     * copy constructor
     */
    public static long[] constructor(final long[] casOther) {
        return casOther.clone();
    }

    /**
     * copy the contents of the other set into this set
     */
    public static void copyFrom(final long[] casThis, final long[] casOther) {
        System.arraycopy(casOther, 0, casThis, 0, casThis.length);
    }

    /**
     * remove all ColorAreas from this set
     */
    public static void clear(final long[] casThis) {
        Arrays.fill(casThis, 0);
    }

    /**
     * add the ColorArea to this set
     */
    public static void add(final long[] casThis, final ColorArea ca) {
        final int caId = ca.getId();
        final int i = caId >>> 6;       // index is always >= 0
        casThis[i] |= 1L << caId;       // implicit shift distance (caId & 0x3f)
    }

    /**
     * add the ColorArea to this set
     */
    public static void add(final long[] casThis, final int caId) {
        final int i = caId >>> 6;       // index is always >= 0
        casThis[i] |= 1L << caId;       // implicit shift distance (caId & 0x3f)
    }

    /**
     * return true if the ColorArea is in this set
     */
    public static boolean contains(final long[] casThis, final ColorArea ca) {
        final int caId = ca.getId();
        final long bit = casThis[caId >>> 6] & (1L << caId); // index is always >= 0; implicit shift distance (caId & 0x3f)
        return 0 != bit;
    }

    /**
     * return true if this set contains all ColorAreas in the other set
     */
    public static boolean containsAll(final long[] casThis, final long[] casOther) {
        for (int i = 0;  i < casThis.length;  ++i) {
            final long thisLong = casThis[i];
            final long otherLong = casOther[i];
            if ((thisLong & otherLong) != otherLong) {
                return false;
            }
        }
        return true;
    }

    /**
     * return true if this set contains at least one ColorArea in the other set
     */
    public static boolean intersects(final long[] casThis, final long[] casOther) {
        for (int i = 0;  i < casThis.length;  ++i) {
            final long thisLong = casThis[i];
            final long otherLong = casOther[i];
            if ((thisLong & otherLong) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if this set contains all ColorAreas in the array
     */
    public static boolean containsAll(final long[] casThis, final ColorArea[] others) {
        for (final ColorArea other : others) {
            if (false == contains(casThis, other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * return true if this set contains none of the ColorAreas in the array
     */
    public static boolean containsNone(final long[] casThis, final ColorArea[] others) {
        for (final ColorArea other : others) {
            if (true == contains(casThis, other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * return the number of ColorAreas in this set
     */
    public static int size(final long[] casThis) {
        int size = 0;
        for (final long a : casThis) {
            size += Long.bitCount(a); // hopefully an intrinsic function using instruction POPCNT
        }
        return size;
    }

    /**
     * return true is this set is empty
     */
    public static boolean isEmpty(final long[] casThis) {
        for (final long a : casThis) {
            if (0L != a) {
                return false;
            }
        }
        return true;
    }

    /**
     * add all ColorAreas in the other set to this set
     */
    public static void addAll(final long[] casThis, final long[] casOther) {
        for (int i = 0;  i < casThis.length;  ++i) {
            casThis[i] |= casOther[i];
        }
    }

    /**
     * remove all ColorAreas in the other set from this set
     */
    public static void removeAll(final long[] casThis, final long[] casOther) {
        for (int i = 0;  i < casThis.length;  ++i) {
            casThis[i] &= ~(casOther[i]);
        }
    }

    /**
     * add all ColorAreaSets indexed by the other set in the lookup-array, to this set.
     * (this combines an iteration over casOther, with a lookup and an addAll-call inside the loop)
     */
    public static void addAllLookup(final long[] casThis, final long[] casOther, final long[][] casLookup) {
        for (int o = 0;  o < casOther.length;  ++o) {
            long buf = casOther[o];
            final int offset = (o << 6) + 63;
            while (buf != 0) {
                final long l1b = buf & -buf; // Long.lowestOneBit
                final int caId = offset - Long.numberOfLeadingZeros(l1b);
                buf ^= l1b;
                final long[] casAdd = casLookup[caId];
                for (int i = 0;  i < casThis.length;  ++i) {
                    casThis[i] |= casAdd[i];
                }
            }
        }
    }

    /**
     * add all ColorAreaSets indexed by the AND-combined other two sets in the lookup-array, to this set.
     * (this combines an iteration over casOtherOne and casOtherTwo, with a lookup and an addAll-call inside the loop)
     */
    public static void addAllAndLookup(final long[] casThis, final long[] casOtherOne, final long[] casOtherTwo, final long[][] casLookup) {
        for (int o = 0;  o < casOtherOne.length;  ++o) {
            long buf = (casOtherOne[o] & casOtherTwo[o]);
            final int offset = (o << 6) + 63;
            while (buf != 0) {
                final long l1b = buf & -buf; // Long.lowestOneBit
                final int caId = offset - Long.numberOfLeadingZeros(l1b);
                buf ^= l1b;
                final long[] casAdd = casLookup[caId];
                for (int i = 0;  i < casThis.length;  ++i) {
                    casThis[i] |= casAdd[i];
                }
            }
        }
    }

    /**
     * an Iterator over one ColorAreaSet that returns the IDs of the member ColorArea objects
     */
    public static class Iterator {
        private long[] array;
        private int longIdxLimit;
        private int longIdx;
        private long buf;

        /**
         * initialize this Iterator for use with this ColorAreaSet.
         * @param caSet
         */
        public void init(final long[] caSet) {
            this.array = caSet;
            this.longIdxLimit = caSet.length - 1;
            this.longIdx = 0;
            this.buf = this.array[0];
        }

        /**
         * return next value (always zero or positive),
         * or a negative value when there is no next value.
         * @return
         */
        public int nextOrNegative() {
            while (0 == this.buf) {
                if (this.longIdxLimit == this.longIdx) {
                    return -1;
                } else {
                    this.buf = this.array[++this.longIdx];
                }
            }
            final long l1b = this.buf & -this.buf;  // Long.lowestOneBit(this.buf)
            final int clz = Long.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
            final int caId = (this.longIdx << 6) + 63 - clz;
            this.buf ^= l1b;
            return caId;
        }
    }

    /**
     * an Iterator over two ColorAreaSets combined with AND, that returns the IDs of the member ColorArea objects that are contained in both sets
     */
    public static class IteratorAnd {
        private long[] array1, array2;
        private int longIdxLimit;
        private int longIdx;
        private long buf;

        /**
         * initialize this Iterator for use with these ColorAreaSets.
         */
        public IteratorAnd init(final long[] caSet1, final long[] caSet2) {
            this.array1 = caSet1;
            this.array2 = caSet2;
            this.longIdxLimit = caSet1.length - 1;
            this.longIdx = 0;
            this.buf = this.array1[0] & this.array2[0];
            return this;
        }

        /**
         * re-initialize this Iterator for use with the ColorAreaSets set by the previous init().
         */
        public IteratorAnd restart() {
            this.longIdx = 0;
            this.buf = this.array1[0] & this.array2[0];
            return this;
        }

        /**
         * return next value (always zero or positive),
         * or a negative value when there is no next value.
         * @return
         */
        public int nextOrNegative() {
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
