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
 */
public class ColorAreaSet {

    private static final short SIZE_UNKNOWN = -1;

    private final long[] array;
    private short size;

    /**
     * the constructor
     */
    public ColorAreaSet(final Board board) {
        this.array = new long[(board.getSizeColorAreas8() + 7) >> 3];
        this.size = 0;
    }

    /**
     * copy constructor
     * @param other
     */
    public ColorAreaSet(final ColorAreaSet other) {
        this.array = other.array.clone();
        this.size = other.size;
    }

    /**
     * copy the contents of the other set into this set
     */
    public void copyFrom(final ColorAreaSet other) {
        System.arraycopy(other.array, 0, this.array, 0, this.array.length);
        this.size = other.size;
    }

    /**
     * remove all ColorAreas from this set
     */
    public void clear() {
        Arrays.fill(this.array, 0);
        this.size = 0;
    }

//FIXME the move from int[] to long[] array here (for a performance improvement) broke DfsExhaustiveStrategy
//    /**
//     * get the reference of the internal array
//     */
//    public int[] getArray() {
//        return this.array;
//    }

    /**
     * add the ColorArea to this set
     */
    public void add(final ColorArea ca) {
        final int caId = ca.getId();
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] |= 1L << caId;    // implicit shift distance (caId & 0x3f)
        this.size = SIZE_UNKNOWN;
    }

    /**
     * add the ColorArea to this set
     */
    public void add(final int caId) {
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] |= 1L << caId;    // implicit shift distance (caId & 0x3f)
        this.size = SIZE_UNKNOWN;
    }

    /**
     * remove the ColorArea from this set
     */
    public void remove(final int caId) {
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] &= ~(1L << caId); // implicit shift distance (caId & 0x3f)
        this.size = SIZE_UNKNOWN;
    }

    /**
     * return true if the ColorArea is in this set
     */
    public boolean contains(final ColorArea ca) {
        final int caId = ca.getId();
        final long bit = this.array[caId >>> 6] & (1L << caId); // index is always >= 0; implicit shift distance (caId & 0x3f)
        return 0 != bit;
    }

    /**
     * return true if the ColorArea is in this set
     */
    public boolean contains(final int caId) {
        final long bit = this.array[caId >>> 6] & (1L << caId); // index is always >= 0; implicit shift distance (caId & 0x3f)
        return 0 != bit;
    }

    /**
     * return true if this set contains all ColorAreas in the other set
     */
    public boolean containsAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            final long thisLong = this.array[i];
            final long otherLong = other.array[i];
            if ((thisLong & otherLong) != otherLong) {
                return false;
            }
        }
        return true;
    }

    /**
     * return true if this set contains all ColorAreas in the array
     */
    public boolean containsAll(final ColorArea[] others) {
        for (final ColorArea other : others) {
            if (false == this.contains(other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * return true if this set contains none of the ColorAreas in the array
     */
    public boolean containsNone(final ColorArea[] others) {
        for (final ColorArea other : others) {
            if (true == this.contains(other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * return the number of ColorAreas in this set
     */
    public int size() {
        if (SIZE_UNKNOWN == this.size) {
            int sz = 0;
            for (final long a : this.array) {
                sz += Long.bitCount(a); // hopefully an intrinsic function using instruction POPCNT
            }
            this.size = (short)sz;
        }
        return this.size;
    }

    /**
     * return true is this set is empty
     */
    public boolean isEmpty() {
        return (short)0 == this.size;
    }

    /**
     * add all ColorAreas in the other set to this set
     */
    public void addAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            this.array[i] |= other.array[i];
        }
        this.size = SIZE_UNKNOWN;
    }

    /**
     * add all ColorAreas in the and-combined other sets to this set
     */
    public void addAllAnd(final ColorAreaSet other1, final ColorAreaSet other2) {
        for (int i = 0;  i < this.array.length;  ++i) {
            this.array[i] |= (other1.array[i] & other2.array[i]);
        }
        this.size = SIZE_UNKNOWN;
    }

    /**
     * remove all ColorAreas in the other set from this set
     */
    public int removeAll(final ColorAreaSet other) {
        int sz = 0;
        for (int i = 0;  i < this.array.length;  ++i) {
            final long a = (this.array[i] &= ~(other.array[i]));
            sz += Long.bitCount(a); // hopefully an intrinsic function using instruction POPCNT
        }
        this.size = (short)sz;
        return sz;
    }

    /**
     * return true if the set difference (this - other) would be an empty set.
     * note: both sets remain unchanged.
     */
    public boolean isEmptyDifference(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            if (0L != (this.array[i] & ~(other.array[i]))) {
                return false;
            }
        }
        return true;
    }

    /**
     * create an Iterator over this set that returns the IDs of the member ColorArea objects
     * @return
     */
    public FastIteratorColorAreaId fastIteratorColorAreaId() {
        return new FastIteratorColorAreaId(this);
    }

    public static class FastIteratorColorAreaId {
        private long[] array;
        private int longIdxLimit;
        private int longIdx;
        private long buf;

        /**
         * create an Iterator for use with this ColorAreaSet.
         * @param caSet
         */
        private FastIteratorColorAreaId(final ColorAreaSet caSet) {
            this.init(caSet);
        }

        /**
         * initialize this Iterator for use with this ColorAreaSet.
         * @param caSet
         */
        public void init(final ColorAreaSet caSet) {
            this.array = caSet.array;
            this.longIdxLimit = ((short)0 == caSet.size ? 0 : this.array.length - 1);
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
}
