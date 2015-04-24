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

import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteListIterator;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.IntHash;

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
public class ExhaustiveDfsStrategy implements DfsStrategy {

    private final byte[] thisState;
    private final StateMap stateMap;

    public ExhaustiveDfsStrategy(final Board board) {
        final int stateBytes = board.getSizeColorAreas8();
        this.thisState = new byte[stateBytes];
        this.stateMap = new StateMap(stateBytes);
    }

    @Override
    public ByteList selectColors(final int depth,
            final byte thisColor,
            final byte[] solution,
            final ColorAreaSet flooded,
            final ColorAreaGroup notFlooded,
            final ColorAreaGroup neighbors) {
        ByteList result = neighbors.getColorsCompleted(notFlooded);
        if (null == result) {
            result = neighbors.getColorsNotEmpty();

            // filter the result: remove colors which result in already known states
            final ByteListIterator it = result.iterator();
            while (it.hasNext()) {
                final byte nextColor = it.nextByte();
                this.makeThisState(flooded, neighbors.getColor(nextColor));
                if (false == this.stateMap.put(this.thisState, depth + 1)) {
                    it.remove();
                }
            }
        }
        return result;
    }

    /** store the id's of the color areas as bits in thisState */
    private void makeThisState(final ColorAreaSet set1, final ColorAreaSet set2) {
        System.arraycopy(set1.getArray(), 0, this.thisState, 0, this.thisState.length);
        final byte[] arr2 = set2.getArray();
        for (int i = 0;  i < arr2.length;  ++i) {
            this.thisState[i] |= arr2[i];
        }
    }


    /** this class is a minimal HashMap implementation that is used here
     * to store the known states and the depths they were found at */
    private static class StateMap {

        private static final int MEMORY_BLOCK_SHIFT = 20; // 20 == 1 MiB
        private static final int MEMORY_BLOCK_SIZE = 1 << MEMORY_BLOCK_SHIFT;
        private static final int MEMORY_BLOCK_MASK = MEMORY_BLOCK_SIZE - 1;
        private byte[][] memoryBlocks = new byte[100][];
        private int numMemoryBlocks = 0, nextState = 0, nextMemoryBlock = 0;

        private final int stateSize;
        private final Int2ByteOpenCustomHashMap theMap = new Int2ByteOpenCustomHashMap(new HashStrategy());

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
            assert this.stateSize == state.length;
            // ensure that nextState points to next available memory position
            if (this.nextState + this.stateSize > this.nextMemoryBlock) {
                if (this.memoryBlocks.length <= this.numMemoryBlocks) {
                    this.memoryBlocks = Arrays.copyOf(this.memoryBlocks, this.memoryBlocks.length << 1);
                }
                this.memoryBlocks[this.numMemoryBlocks++] = new byte[MEMORY_BLOCK_SIZE];
                this.nextState = this.nextMemoryBlock;
                this.nextMemoryBlock += MEMORY_BLOCK_SIZE;
            }
            // copy state into memory at nextState
            final byte[] memory = this.memoryBlocks[this.nextState >>> MEMORY_BLOCK_SHIFT];
            final int offset = this.nextState & MEMORY_BLOCK_MASK;
            System.arraycopy(state, 0, memory, offset, this.stateSize);
            // add to theMap, increment nextState only if we want to accept the new state/depth pair
            final int oldDepth = 0xff & this.theMap.put(this.nextState, (byte)depth);
            if ((0 == oldDepth) || (depth < oldDepth)) { // not found or larger depth
                this.nextState += this.stateSize;
                return true; // added
            } else {
                // restore previous depth value
                if (depth > oldDepth) {
                    this.theMap.put(this.nextState, (byte)oldDepth);
                }
                return false; // not added
            }
        }
    }
}
