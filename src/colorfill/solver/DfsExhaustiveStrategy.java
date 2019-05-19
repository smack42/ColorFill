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

import colorfill.model.Board;

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
//FIXME broken by moving ColorAreaSet to long[] array
@Deprecated // superseded by AStarPuchertStrategy, which runs faster and needs less memory
public class DfsExhaustiveStrategy implements DfsStrategy {

    private static final int MAX_BOARD_SIZE_NORMAL = 15*15;
    private static final int MAX_BOARD_SIZE_CODEGOLF26232 = 19*19;
    static int MAX_BOARD_SIZE = MAX_BOARD_SIZE_NORMAL; // DfsExhaustiveStrategy will not run for larger boards

    private static final float HASH_LOAD_FACTOR_FAST = 0.5f;
    private static final int HASH_EXPECTED_FAST = 100000000;
    private static final float HASH_LOAD_FACTOR_NORMAL = 0.75f;
    private static final int HASH_EXPECTED_NORMAL = 20000000;
    private static float HASH_LOAD_FACTOR = HASH_LOAD_FACTOR_FAST;
    private static int HASH_EXPECTED = HASH_EXPECTED_FAST;

    public static void setHashFast() {
        HASH_LOAD_FACTOR = HASH_LOAD_FACTOR_FAST;
        HASH_EXPECTED = HASH_EXPECTED_FAST;
    }
    public static void setHashNormal() {
        HASH_LOAD_FACTOR = HASH_LOAD_FACTOR_NORMAL;
        HASH_EXPECTED = HASH_EXPECTED_NORMAL;
    }
    public static void setCodeGolf26232() {
        HASH_LOAD_FACTOR = HASH_LOAD_FACTOR_NORMAL;
        HASH_EXPECTED = HASH_EXPECTED_FAST;
        MAX_BOARD_SIZE = MAX_BOARD_SIZE_CODEGOLF26232;
    }

    private int previousNumSteps = Integer.MAX_VALUE;

    public DfsExhaustiveStrategy(final Board board) {
        final int stateSizeBytes = board.getSizeColorAreas8();
        this.stateSize = (stateSizeBytes + 3) >> 2;
        this.f = HASH_LOAD_FACTOR;
        this.constructorInt2ByteOpenCustomHashMapPutIfLess(HASH_EXPECTED);
    }

    @Override
    public String getInfo() {
        final long mbHashK = (this.key.length >> 20) * 4;
        final long mbHashV = this.value.length >> 20;
        long bData = 0;
        for (final int[] i : this.memoryBlocks) {
            if (null != i) {
                bData += i.length * 4;
            }
        }
        final long mbData = bData >> 20;
        return (mbHashK + mbHashV + mbData) + " MB memory used (hashMap "
                + (mbHashK + mbHashV) + " MB, data " + mbData + " MB, size " + this.size + ")"
                + " stateSize=" + (this.stateSize*4) + " numStates=" + (bData / (this.stateSize*4));
                //+ " less=" + this.numLess + " notLess=" + this.numNotLess;
    }

    @Override
    public void setPreviousNumSteps(final int previousNumSteps) {
        this.previousNumSteps = previousNumSteps;
    }

    @Override
    public int selectColors(final int depth,
            final ColorAreaSet flooded,
            final ColorAreaGroup notFlooded,
            final ColorAreaGroup neighbors) {
        int result;
        final int diffNumSteps = depth + notFlooded.countColorsNotEmpty() - this.previousNumSteps;
        if (diffNumSteps >= 0) {
            result = 0;
        } else {
            result = neighbors.getColorsCompleted(notFlooded);
            // if no colors can be completed now
            // then we will need at least one step more than there are colors left
            if ((0 == result) && (diffNumSteps < -1)) {
                // filter the result:
                // only include colors which do not result in already known states (at this or lower depth)
                for (int color = 0, colorsBits = neighbors.getColorsNotEmpty();  0 != colorsBits;  color++, colorsBits >>= 1) {
                    if ((0 != (colorsBits & 1)) && this.put(flooded, neighbors.getColor((byte)color), depth + 1)) {
                        result |= 1 << color;
                    }
                }
            }
        }
        return result;
    }


    /** this class is a minimal HashMap implementation that is used here
     * to store the known states and the depths they were found at */
//    private static class StateMap {

        private final int stateSize;

        private static final int MEMORY_BLOCK_SHIFT = 20; // 20 == 4 MiB
        private static final int MEMORY_BLOCK_SIZE = 1 << MEMORY_BLOCK_SHIFT;
        private static final int MEMORY_BLOCK_MASK = MEMORY_BLOCK_SIZE - 1;
        private int[][] memoryBlocks = new int[1][MEMORY_BLOCK_SIZE];
        private int[] nextStateMemory = memoryBlocks[0];
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
            final int[] arr1 = null;  //set1.getArray(); FIXME broken by moving ColorAreaSet to long[] array
            final int[] arr2 = null;  //set2.getArray(); FIXME broken by moving ColorAreaSet to long[] array
            // performance: inline hashcode computation; copy&paste from hashStrategyHashCode()
            int h32 = SEED + PRIME5;
            for (int b = this.nextStateOffset, i = 0, len = arr1.length;  i < len;  ++i, ++b) {
                final int k = arr1[i] | arr2[i];
                this.nextStateMemory[b] = k;
                h32 += k * PRIME3;
                h32 = Integer.rotateLeft(h32, 17) * PRIME4;
            }
            // add to the map, increment nextState only if we want to accept the new state/depth pair
            final int result = this.putIfLess(h32, (byte)depth);
            if (result > 0) {
                this.nextState += this.stateSize;
                this.nextStateOffset += this.stateSize;
                // ensure that nextState points to next available memory position
                if (this.nextMemoryBlock - this.nextState < this.stateSize) { // this.nextState + this.stateSize > this.nextMemoryBlock
                    if (this.memoryBlocks.length <= this.numMemoryBlocks) {
                        this.memoryBlocks = Arrays.copyOf(this.memoryBlocks, this.memoryBlocks.length << 1);
                    }
                    this.nextStateMemory = new int[MEMORY_BLOCK_SIZE];
                    this.memoryBlocks[this.numMemoryBlocks++] = this.nextStateMemory;
                    this.nextState = this.nextMemoryBlock;
                    this.nextStateOffset = 0;
                    this.nextMemoryBlock += MEMORY_BLOCK_SIZE;
                    if (0 == this.nextMemoryBlock) { // works only because MEMORY_BLOCK_SIZE is a power of 2
                        throw new IllegalStateException("Integer overflow! (16 GB data storage exceeded)");
                    }
                }
            }
            return result >= 0;
        }

        /** this hash strategy accesses the data in the StateMap memory arrays */
//        private class HashStrategy {
            public boolean hashStrategyEquals(final int arg0, final int arg1) {
                final int[] memory0 = this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                int offset0 = arg0 & MEMORY_BLOCK_MASK;
                final int[] memory1 = this.memoryBlocks[arg1 >>> MEMORY_BLOCK_SHIFT];
                int offset1 = arg1 & MEMORY_BLOCK_MASK;
                final int limit0 = offset0 + this.stateSize;
                do {
                    if (memory0[offset0++] != memory1[offset1++]) { return false; } // not equal
                } while (offset0 < limit0);
                return true; // equal
            }

            /*
             * hashcode calculation based on xxhash32
             * <p>
             * Java implementation by Adrien Grand
             * https://github.com/jpountz/lz4-java
             * <p>
             * based on xxhash by Yann Collet
             * https://github.com/Cyan4973/xxHash
             */
            /*
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
            private static final int SEED = 0x9747b28c;
//            private static final int PRIME1 = -1640531535;
//            private static final int PRIME2 = -2048144777;
            private static final int PRIME3 = -1028477379;
            private static final int PRIME4 = 668265263;
            private static final int PRIME5 = 374761393;

            public int hashStrategyHashCode(final int arg0) {
                final int[] memory = this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                int offset = arg0 & MEMORY_BLOCK_MASK;
                // calculation taken from xxhash32
                final int len = this.stateSize;
                int h32 = SEED + PRIME5;
                // we don't need to mix in "len" because it's a constant value
//                h32 += len << 2;
                final int limit = offset + len;
                do {
                    h32 += memory[offset++] * PRIME3;
                    h32 = Integer.rotateLeft(h32, 17) * PRIME4;
                } while (offset < limit);
                // we omit the finalization step because we want faster computation; hash quality still seems to be good enough
//                h32 ^= h32 >>> 15;
//                h32 *= PRIME2;
//                h32 ^= h32 >>> 13;
//                h32 *= PRIME3;
//                h32 ^= h32 >>> 16;
                return h32;
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
            /** some counters, for info only */
            //private int numLess, numNotLess;
            /** constructor */
            private void constructorInt2ByteOpenCustomHashMapPutIfLess(final int expected) {
                if ( expected < 0 ) throw new IllegalArgumentException( "The expected number of elements must be nonnegative" );
                n = hashCommonArraySize( expected );
                mask = n - 1;
                maxFill = hashCommonMaxFill( n );
                key = new int[ n + 1 ];
                value = new byte[ n + 1 ];
            }
            /**
             * put the key / value pair into this map if the key is not already in the
             * map or if it already exists and the new value is less than the old value.
             * @param h32 pre-computed hashcode of key
             * @param v value
             * @return 1 if a new entry was added,
             *  0 if an existing entry was updated (new value is less than old value),
             *  -1 if nothing was changed (new value is NOT less than old value)
             */
            private int putIfLess(final int h32, final byte v) {
                final int k = this.nextState;
                int pos, curr;
                insert: {
                    final int[] key = this.key;
                    if ( !( ( curr = key[ pos = h32 & mask ] ) == ( 0 ) ) ) {
                        if ( ( this.hashStrategyEquals( ( curr ), ( k ) ) ) ) break insert;
                        while ( !( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) )
                            if ( ( this.hashStrategyEquals( ( curr ), ( k ) ) ) ) break insert;
                    }
                    key[ pos ] = k;
                    this.value[ pos ] = v;
                    if ( this.size++ >= this.maxFill ) { this.rehash( hashCommonArraySize( size + 1 ) ); }
                    return 1; // key/value pair is new; ADDED.
                }
                final byte oldValue = this.value[ pos ];
                if (oldValue - v > 0) { // putIfLess
                    //++this.numLess;
                    this.value[ pos ] = v;
                    return 0; // key already exists, new value is less than old value; UPDATED.
                } else {
                    //++this.numNotLess;
                    return -1;// key already exists, new value is not less than old value; NOT CHANGED.
                }
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
                maxFill = hashCommonMaxFill( n );
                this.key = newKey;
                this.value = newValue;
            }
            /** Returns the maximum number of entries that can be filled before rehashing. 
            * @param n the size of the backing array.
            * @param f the load factor.
            * @return the maximum number of entries before rehashing. 
            */
           private int hashCommonMaxFill(final int n) {
               /* We must guarantee that there is always at least 
                * one free entry (even with pathological load factors). */
               return Math.min( (int)Math.ceil( n * this.f ), n - 1 );
           }
           /** Returns the least power of two smaller than or equal to 2<sup>30</sup> and larger than or equal to <code>Math.ceil( expected / f )</code>. 
            * @param expected the expected number of elements in a hash table.
            * @param f the load factor.
            * @return the minimum possible size for a backing array.
            * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
            */
           private int hashCommonArraySize(final int expected) {
               final long s = Math.max( 2, hashCommonNextPowerOfTwo( (long)Math.ceil( expected / this.f ) ) );
               if ( s > (1 << 30) ) throw new IllegalArgumentException( "Too large (" + expected + " expected elements with load factor " + this.f + ")" );
               return (int)s;
           }
           /** Return the least power of two greater than or equal to the specified value.
            * <p>Note that this function will return 1 when the argument is 0.
            * @param x a long integer smaller than or equal to 2<sup>62</sup>.
            * @return the least power of two greater than or equal to the specified value.
            */
           private static long hashCommonNextPowerOfTwo( long x ) {
               if ( x == 0 ) return 1;
               x--;
               x |= x >> 1;
               x |= x >> 2;
               x |= x >> 4;
               x |= x >> 8;
               x |= x >> 16;
               return ( x | x >> 32 ) + 1;
           }
//        } // private class Int2ByteOpenCustomHashMapPutIfLess
//    } // private static class StateMap

}
