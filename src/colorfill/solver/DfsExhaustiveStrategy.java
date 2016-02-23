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

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntHash;

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

    private final byte[] thisState;
    private final StateMap stateMap;

    public DfsExhaustiveStrategy(final Board board) {
        final int stateBytes = board.getSizeColorAreas8();
        this.thisState = new byte[(stateBytes + 3) & ~3];
        this.stateMap = new StateMap(stateBytes);
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

            // filter the result: remove colors which result in already known states
            for (final byte nextColor : tmpColors) {
                if (NO_COLOR == nextColor) break;
                this.makeThisState(flooded, neighbors.getColor(nextColor));
                if (true == this.stateMap.put(this.thisState, depth + 1)) {
                    result[idx++] = nextColor;
                }
            }
            result[idx] = NO_COLOR;
        }
        return result;
    }

    /** store the id's of the color areas as bits in thisState */
    private void makeThisState(final ColorAreaSet set1, final ColorAreaSet set2) {
        final int[] arr1 = set1.getArray();
        final int[] arr2 = set2.getArray();
        for (int b = -1, i = 0;  i < arr1.length;  ++i) {
            final int v = arr1[i] | arr2[i];
            this.thisState[++b] = (byte)(v);
            this.thisState[++b] = (byte)(v >> 8);
            this.thisState[++b] = (byte)(v >> 16);
            this.thisState[++b] = (byte)(v >> 24);
        }
    }


    /** this class is a minimal HashMap implementation that is used here
     * to store the known states and the depths they were found at */
    private static class StateMap {

        private static final int MEMORY_BLOCK_SHIFT = 20; // 20 == 1 MiB
        private static final int MEMORY_BLOCK_SIZE = 1 << MEMORY_BLOCK_SHIFT;
        private static final int MEMORY_BLOCK_MASK = MEMORY_BLOCK_SIZE - 1;
        private byte[][] memoryBlocks = new byte[1][MEMORY_BLOCK_SIZE];
        private byte[] nextStateMemory = memoryBlocks[0];
        private int numMemoryBlocks = 1, nextState = 1, nextStateOffset = 1, nextMemoryBlock = MEMORY_BLOCK_SIZE;
        private final int stateSize;
        private final Int2ByteOpenCustomHashMapPutIfLess theMap = new Int2ByteOpenCustomHashMapPutIfLess(100000000, Hash.FAST_LOAD_FACTOR, new HashStrategy());

        /** this hash strategy accesses the data in the StateMap memory arrays */
        private class HashStrategy implements IntHash.Strategy {
            private final XXHash32 xxhash32 = XXHashFactory.fastestJavaInstance().hash32();

            @Override
            public boolean equals(final int arg0, final int arg1) {
                final byte[] memory0 = StateMap.this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                int offset0 = (arg0 & MEMORY_BLOCK_MASK) - 1;
                final byte[] memory1 = StateMap.this.memoryBlocks[arg1 >>> MEMORY_BLOCK_SHIFT];
                int offset1 = (arg1 & MEMORY_BLOCK_MASK) - 1;
                int count = StateMap.this.stateSize;
                do {
                    if (memory0[++offset0] != memory1[++offset1]) {
                        return false; // not equal
                    }
                } while (--count > 0);
                return true; // equal
            }

            @Override
            public int hashCode(final int arg0) {
                final byte[] memory = StateMap.this.memoryBlocks[arg0 >>> MEMORY_BLOCK_SHIFT];
                final int offset = arg0 & MEMORY_BLOCK_MASK;
                final int hash = this.xxhash32.hash(memory, offset, StateMap.this.stateSize, 0x9747b28c);
                return hash;
            }
        }

        /** the constructor.
         * @param stateSize number of bytes in a single state
         */
        public StateMap(final int stateSize) {
            this.stateSize = stateSize;
        }

        /** add state to this map, assign depth to it and return true
         *  if the state is not present yet
         *  or if it's present and had a larger depth assigned to it.
         * @param state to be stored (as key)
         * @param depth to be assigned (as value) to state
         * @return true if the state/depth pair was added.
         */
        public boolean put(final byte[] state, final int depth) {
            // copy state into memory at nextState
            System.arraycopy(state, 0, this.nextStateMemory, this.nextStateOffset, this.stateSize);
            // add to theMap, increment nextState only if we want to accept the new state/depth pair
            if (this.theMap.putIfLess(this.nextState, (byte)depth)) {
                this.nextState += this.stateSize;
                this.nextStateOffset += this.stateSize;
                // ensure that nextState points to next available memory position
                if (this.nextState + this.stateSize > this.nextMemoryBlock) {
                    if (this.memoryBlocks.length <= this.numMemoryBlocks) {
                        this.memoryBlocks = Arrays.copyOf(this.memoryBlocks, this.memoryBlocks.length << 1);
                    }
                    this.nextStateMemory = new byte[MEMORY_BLOCK_SIZE];
                    this.memoryBlocks[this.numMemoryBlocks++] = this.nextStateMemory;
                    this.nextState = this.nextMemoryBlock;
                    this.nextStateOffset = 0;
                    this.nextMemoryBlock += MEMORY_BLOCK_SIZE;
                }
                return true; // added
            } else {
                return false; // not added
            }
        }
    }
}
