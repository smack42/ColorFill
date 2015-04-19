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

package colorfill.solver;

import java.util.Iterator;
import java.util.NoSuchElementException;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * this class is a bespoke implementation of a Set of ColorArea. 
 */
public class ColorAreaSet implements Iterable<ColorArea> {

    private static final int SIZE_UNKNOWN = -1;

    private final Board board;
    private final byte[] array;
    private int size;

    /**
     * the constructor
     */
    public ColorAreaSet(final Board board) {
        this.board = board;
        this.array = new byte[board.getSizeColorAreas8()];
        this.size = 0;
    }

    /**
     * the copy constructor
     */
    public ColorAreaSet(final ColorAreaSet other) {
        this.board = other.board;
        this.array = other.array.clone();
        this.size = other.size;
    }

    /**
     * get the reference to the internal byte array
     */
    public byte[] getArray() {
        return this.array;
    }

    /**
     * add the ColorArea to this set
     */
    public void add(final ColorArea ca) {
        final int id = ca.getId();
        this.array[id >> 3] |= 1 << (id & 7);
        this.size = SIZE_UNKNOWN;
    }

    /**
     * return true if the ColorArea is in this set
     */
    public boolean contains(final ColorArea ca) {
        final int id = ca.getId();
        final int bit = this.array[id >> 3] & (1 << (id & 7));
        return 0 != bit;
    }

    /**
     * return true if this set contains all ColorAreas in the other set
     */
    public boolean containsAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            final byte thisByte = this.array[i];
            final byte otherByte = other.array[i];
            if ((thisByte & otherByte) != otherByte) {
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
            this.size = 0;
            for (final byte b : this.array) {
                this.size += Integer.bitCount(0xff & b);
            }
        }
        return this.size;
    }

    /**
     * return true is this set is empty
     */
    public boolean isEmpty() {
        if (SIZE_UNKNOWN == this.size) {
            for (final byte b : this.array) {
                if (0 != b) {
                    return false;
                }
            }
            this.size = 0;
        }
        return 0 == this.size;
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
     * remove all ColorAreas in the other set from this set
     */
    public void removeAll(final ColorAreaSet other) {
        for (int i = 0;  i < this.array.length;  ++i) {
            this.array[i] &= ~(other.array[i]);
        }
        this.size = SIZE_UNKNOWN;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<ColorArea> iterator() {
        return new ColorAreaSetIterator();
    }

    private class ColorAreaSetIterator implements Iterator<ColorArea> {
        private int count = 0;
        private final int countLimit = ColorAreaSet.this.size();
        private int byteIdx = -1, bitIdx = 0;
        private int buf = 0;

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return this.count < this.countLimit;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public ColorArea next() {
            if (false == this.hasNext()) {
                throw new NoSuchElementException();
            }
            while (0 == this.buf) {
                this.buf = 0xff & ColorAreaSet.this.array[++this.byteIdx];
                this.bitIdx = 0;
            }
            while (0 == (1 & this.buf)) {
                this.bitIdx += 1;
                this.buf >>= 1;
            }
            final int caId = (this.byteIdx << 3) + this.bitIdx;
            this.count += 1;
            this.bitIdx += 1;
            this.buf >>= 1;
            return ColorAreaSet.this.board.getColorArea4Id(caId);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
