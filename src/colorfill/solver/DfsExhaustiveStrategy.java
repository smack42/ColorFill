/*  ColorFill game and solver
    Copyright (C) 2015 Michael Henke

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

import java.util.Arrays;

import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;

import it.unimi.dsi.fastutil.HashCommon;

import colorfill.model.Board;
import static colorfill.solver.ColorAreaGroup.NO_COLOR;

/**
 * this strategy results in a complete search.
 * it chooses the colors in two steps:
 * <p>
 * 1) colors that can be completely flooded in the next step.
 * (these are always optimal moves!?)
 * <p>
 * 2) all colors that are possible in the next step.
 * (hence the name "exhaustive")
 */
public class DfsExhaustiveStrategy implements DfsStrategy {

    public DfsExhaustiveStrategy(final Board board) {
        final int stateSize = board.getSizeColorAreas8();
        this.stateSize = stateSize;
        this.stateSize4 = (stateSize + 3) & ~3; // next multiple of four
        this.constructorInt2ByteOpenCustomHashMapPutIfLess(100000000);
        this.f = 0.5f; // Hash.FAST_LOAD_FACTOR
    }

    @Override
    public String getInfo() {
        final long mbHashK = (this.key.length * 4) >> 20;
        final long mbHashV = (this.value.length) >> 20;
        long bData = 0;
        for (final byte[] b : this.memoryBlocks) {
            if (null != b) {
                bData += b.length;
            }
        }
        final long mbData = bData >> 20;
        return (mbHashK + mbHashV + mbData) + " MB memory used (hashMap "
                + (mbHashK + mbHashV) + " MB, data " + mbData + " MB)"
                + " stateSize=" + this.stateSize + " numStates=" + (bData / this.stateSize);
    }

    @Override
    public byte[] selectColors(final int depth,
            final byte thisColor,
            final byte[] solution,
            final ColorAreaSet flooded,
            final ColorAreaGroup notFlooded,
            final ColorAreaGroup neighbors) {
        byte[] result = neighbors.getColorsCompleted(notFlooded);
        if (null == result) {
            final byte[] tmpColors = neighbors.getColorsNotEmpty();
            result = new byte[tmpColors.length];
            int idx = 0;

            // filter the result:
            // only include colors which do not result in already known states (at this or lower depth)
            for (final byte nextColor : tmpColors) {
                if (NO_COLOR == nextColor) break;
                if (true == this.put(flooded, neighbors.getColor(nextColor), depth + 1)) {
                    result[idx++] = nextColor;
                }
            }
            result[idx] = NO_COLOR;
        }
        return result;
    }


    /** this class is a minimal HashMap implementation that is used here
     * to store the known states and the depths they were found at */
//    private static class StateMap {

        private final int stateSize, stateSize4;

        private static final int MEMORY_BLOCK_SHIFT = 20; // 20 == 1 MiB
        private static final int MEMORY_BLOCK_SIZE = 1 << MEMORY_BLOCK_SHIFT;
        private static final int MEMORY_BLOCK_MASK = MEMORY_BLOCK_SIZE - 1;
        private byte[][] memoryBlocks = new byte[1][MEMORY_BLOCK_SIZE];
        private byte[] nextStateMemory = memoryBlocks[0];
        private int numMemoryBlocks = 1, nextState = 1, nextStateOffset = 1, nextMemoryBlock = MEMORY_BLOCK_SIZE;

        /** add "state" to this map, assign depth to it and return true
         *  if the "state" is not present yet
         *  or if it's present and had a larger depth assigned to it.
         * @param set1 colors part 1, combined with set2 it will be stored as "state"
         * @param set2 colors part 2, combined with set1 it will be stored as "state"
         * @param depth to be assigned (as value) to "state"
         * @return true if the state/depth pair was added.
         */
        private boolean put(final ColorAreaSet set1, final ColorAreaSet set2, final int depth) {
            // copy state into memory at nextState
            final int[] arr1 = set1.getArray();
            final int[] arr2 = set2.getArray();
            for (int b = this.nextStateOffset - 1, i = 0;  i < arr1.length;  ++i) {
                final int v = arr1[i] | arr2[i];
                this.nextStateMemory[++b] = (byte)(v);
                this.nextStateMemory[++b] = (byte)(v >> 8);
                this.nextStateMemory[++b] = (byte)(v >> 16);
                this.nextStateMemory[++b] = (byte)(v >> 24);
            }
            // add to the map, increment nextState only if we want to accept the new state/depth pair
            if (this.putIfLess(this.nextState, (byte)depth)) {
                this.nextState += this.stateSize;
                this.nextStateOffset += this.stateSize;
                // ensure that nextState points to next available memory position
                if (this.nextMemoryBlock - this.nextState < this.stateSize4) { // this.nextState + this.stateSize4 > this.nextMemoryBlock
                    if (this.memoryBlocks.length <= this.numMemoryBlocks) {
                        this.memoryBlocks = Arrays.copyOf(this.memoryBlocks, this.memoryBlocks.length << 1);
                    }
                    this.nextStateMemory = new byte[MEMORY_BLOCK_SIZE];
                    this.memoryBlocks[this.numMemoryBlocks++] = this.nextStateMemory;
                    this.nextState = this.nextMemoryBlock;
                    this.nextStateOffset = 0;
                    this.nextMemoryBlock += MEMORY_BLOCK_SIZE;
                    if (0 == this.nextMemoryBlock) { // works only if MEMORY_BLOCK_SIZE is a power of 2
                        throw new IllegalStateException("Integer overflow! (4 GB data storage exceeded)");
                    }
                }
                return true; // added
            } else {
                return false; // not added
            }
        }

        /** this hash strategy accesses the data in the StateMap memory arrays */
//        private class HashStrategy {
            private final XXHash32 xxhash32 = XXHashFactory.fastestJavaInstance().hash32();

            public boolean hashStrategyEquals(final int arg0, final int arg1) {
                final byte[] memory0 = this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                int offset0 = (arg0 & MEMORY_BLOCK_MASK) - 1;
                final byte[] memory1 = this.memoryBlocks[arg1 >>> MEMORY_BLOCK_SHIFT];
                int offset1 = (arg1 & MEMORY_BLOCK_MASK) - 1;
                int count = this.stateSize;
                do {
                    if (memory0[++offset0] != memory1[++offset1]) {
                        return false; // not equal
                    }
                } while (--count > 0);
                return true; // equal
            }

            public int hashStrategyHashCode(final int arg0) {
                final byte[] memory = this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                final int offset = arg0 & MEMORY_BLOCK_MASK;
                final int hash = this.xxhash32.hash(memory, offset, this.stateSize, 0x9747b28c);
                return hash;
            }
//        } // private class HashStrategy

        /** this is a modified version of class Int2ByteOpenCustomHashMap
         * taken from the library "fastutil" <br>
         * http://fastutil.di.unimi.it/ <br>
         * https://github.com/vigna/fastutil
         */
        /*
         * Copyright (C) 2002-2014 Sebastiano Vigna
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
//        private class Int2ByteOpenCustomHashMapPutIfLess {
            /** The array of keys. */
            private transient int[] key;
            /** The array of values. */
            private transient byte[] value;
            /** The mask for wrapping a position counter. */
            private transient int mask;
            /** The current table size. */
            private transient int n;
            /** Threshold after which we rehash. It must be the table size times {@link #f}. */
            private transient int maxFill;
            /** Number of entries in the set (including the key zero, if present). */
            private int size;
            /** The acceptable load factor. */
            private final float f;
            /** constructor */
            private void constructorInt2ByteOpenCustomHashMapPutIfLess( final int expected) {
                if ( expected < 0 ) throw new IllegalArgumentException( "The expected number of elements must be nonnegative" );
                n = HashCommon.arraySize( expected, f );
                mask = n - 1;
                maxFill = HashCommon.maxFill( n, f );
                key = new int[ n + 1 ];
                value = new byte[ n + 1 ];
            }
            /**
             * put the key / value pair into this map if the key is not already in the
             * map or if it already exists and the new value is less than the old value.
             * @param k key
             * @param v value
             * @return true if key and value have been added to this map.
             */
            private boolean putIfLess(final int k, final byte v) {
                final int pos = this.insert( k, v );
                if ( pos < 0 ) return true; // key/value pair is new; added.
                final byte oldValue = this.value[ pos ];
                if (v < oldValue) { // putIfLess
                    this.value[ pos ] = v;
                    return true; // key already exists, new value is less than old value; added.
                } else {
                    return false;// key already exists, new value is not less than old value; not added.
                }
            }
            /** actually put the k/v pair into the hashmap */
            private int insert( final int k, final byte v ) {
                int pos, curr;
                final int[] key = this.key;
                if ( !( ( curr = key[ pos = ( this.hashStrategyHashCode( k ) ) & mask ] ) == ( 0 ) ) ) {
                    if ( ( this.hashStrategyEquals( ( curr ), ( k ) ) ) ) return pos;
                    while ( !( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) )
                        if ( ( this.hashStrategyEquals( ( curr ), ( k ) ) ) ) return pos;
                }
                key[ pos ] = k;
                this.value[ pos ] = v;
                if ( this.size++ >= this.maxFill ) { this.rehash( HashCommon.arraySize( size + 1, f ) ); }
                return -1;
            }
            /** Rehashes the map.
             * @param newN the new size */
            private void rehash( final int newN ) {
                final int key[] = this.key;
                final byte value[] = this.value;
                final int mask = newN - 1;
                final int newKey[] = new int[ newN + 1 ];
                final byte newValue[] = new byte[ newN + 1 ];
                int i = n, pos;
                for ( int j = size; j-- != 0; ) {
                    while ( ( ( key[ --i ] ) == ( 0 ) ) );
                    if ( !( ( newKey[ pos = ( this.hashStrategyHashCode( key[ i ] ) ) & mask ] ) == ( 0 ) ) ) while ( !( ( newKey[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) );
                    newKey[ pos ] = key[ i ];
                    newValue[ pos ] = value[ i ];
                }
                newValue[ newN ] = value[ n ];
                n = newN;
                this.mask = mask;
                maxFill = HashCommon.maxFill( n, f );
                this.key = newKey;
                this.value = newValue;
            }
//        } // private class Int2ByteOpenCustomHashMapPutIfLess
//    } // private static class StateMap

}
