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

    private final long[] array;

    /**
     * the constructor
     */
    public ColorAreaSet(final Board board) {
        this.array = new long[(board.getSizeColorAreas8() + 7) >> 3];
    }

    /**
     * copy constructor
     * @param other
     */
    public ColorAreaSet(final ColorAreaSet other) {
        this.array = other.array.clone();
    }

    /**
     * copy the contents of the other set into this set
     */
    public void copyFrom(final ColorAreaSet other) {
        System.arraycopy(other.array, 0, this.array, 0, this.array.length);
    }

    /**
     * remove all ColorAreas from this set
     */
    public void clear() {
        Arrays.fill(this.array, 0);
    }

    /**
     * get the reference of the internal array
     */
    public long[] getArray() {
        return this.array;
    }

    /**
     * add the ColorArea to this set
     */
    public void add(final ColorArea ca) {
        final int caId = ca.getId();
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] |= 1L << caId;    // implicit shift distance (caId & 0x3f)
    }

    /**
     * add the ColorArea to this set
     */
    public void add(final int caId) {
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] |= 1L << caId;    // implicit shift distance (caId & 0x3f)
    }

    /**
     * remove the ColorArea from this set
     */
    public void remove(final int caId) {
        final int i = caId >>> 6;       // index is always >= 0
        this.array[i] &= ~(1L << caId); // implicit shift distance (caId & 0x3f)
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
     * return true if this set contains at least one ColorArea in the other set
     */
    public boolean intersects(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            final long thisLong = this.array[i];
            final long otherLong = other.array[i];
            if ((thisLong & otherLong) != 0) {
                return true;
            }
        }
        return false;
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
        int size = 0;
        for (final long a : this.array) {
            size += Long.bitCount(a); // hopefully an intrinsic function using instruction POPCNT
        }
        return size;
    }

    /**
     * return true is this set is empty
     */
    public boolean isEmpty() {
        for (final long a : this.array) {
            if (0L != a) {
                return false;
            }
        }
        return true;
    }

    /**
     * add all ColorAreas in the other set to this set
     */
    public void addAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            this.array[i] |= other.array[i];
        }
    }

    /**
     * remove all ColorAreas in the other set from this set
     */
    public void removeAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            this.array[i] &= ~(other.array[i]);
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
        public void init(final ColorAreaSet caSet) {
            this.array = caSet.array;
            this.longIdxLimit = this.array.length - 1;
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
        public IteratorAnd init(final ColorAreaSet caSet1, final ColorAreaSet caSet2) {
            this.array1 = caSet1.array;
            this.array2 = caSet2.array;
            this.longIdxLimit = this.array1.length - 1;
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
