/*  ColorFill game and solver
    Copyright (C) 2014, 2020, 2021 Michael Henke

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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import colorfill.model.Board;
import colorfill.model.ColorArea;
import colorfill.model.ColorAreaSet;

/**
 * a solver implementation that implements the AStar (A*) algorithm.
 */
public class AStarSolver extends AbstractSolver {

    private Class<? extends AStarStrategy> strategyClass = AStarPuchertStrategy.class; // default
    private AStarStrategy strategy;
    private final SolutionTree solutionTree = new SolutionTree();
    private final ColorAreaSet.IteratorAnd iterAnd;
    private final long[][] casByColorBits;
    private Queue<AStarNode> open;
    private HashMapLongArray2Byte map;
    private StateStorage storage;
    private final long[] casFlooded, casNeighbors, casNextFlooded, casNextNeighbors;

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    protected AStarSolver(Board board) {
        super(board);
        this.iterAnd = new ColorAreaSet.IteratorAnd();
        this.casByColorBits = board.getCasByColorBitsArray();
        this.casFlooded = ColorAreaSet.constructor(board);
        this.casNeighbors = ColorAreaSet.constructor(board);
        this.casNextFlooded = ColorAreaSet.constructor(board);
        this.casNextNeighbors = ColorAreaSet.constructor(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#setStrategy(java.lang.Class)
     */
    @Override
    public void setStrategy(final Class<? extends Strategy> strategyClass) {
        if (false == AStarStrategy.class.isAssignableFrom(strategyClass)) {
            throw new IllegalArgumentException(
                    "unsupported strategy class " + strategyClass.getName()
                    + "! " + this.getClass().getSimpleName() + " supports " + AStarStrategy.class.getSimpleName() + " only.");
        }
        this.strategyClass = strategyClass.asSubclass(AStarStrategy.class);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#getSolverName()
     */
    @Override
    public String getSolverName() {
        return this.strategyClass.getSimpleName();
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#getSolverInfo()
     */
    @Override
    public String getSolverInfo() {
        return null; // no info available
    }

    private AStarStrategy makeStrategy(final StateStorage storage) {
        final AStarStrategy result;
        if (AStarPuchertStrategy.class.equals(this.strategyClass)) {
            result = new AStarPuchertStrategy(this.board, storage);
        } else if (AStarFlolleStrategy.class.equals(this.strategyClass)) {
            result = new AStarFlolleStrategy(this.board, storage);
        } else {
            throw new IllegalArgumentException(
                    "unsupported strategy class " + this.strategyClass.getName());
        }
        return result;
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AbstractSolver#executeInternal(int)
     */
    @Override
    protected void executeInternal(final int startPos) throws InterruptedException {
        this.storage = new StateStorage(this.board);
        this.strategy = this.makeStrategy(this.storage);
        this.open = new PriorityQueue<AStarNode>(AStarNode.strongerComparator());
        this.open.offer(new AStarNode(this.board, this.board.getColorArea4Cell(startPos), this.storage, this.solutionTree));
        this.map = new HashMapLongArray2Byte(this.board, this.storage);
        AStarNode recycleNode = null;
        final int colorBitLimit = this.casByColorBits.length;
        while (this.open.size() > 0) {
            if (Thread.interrupted()) { throw new InterruptedException(); }
            final AStarNode currentNode = this.open.poll();
            this.storage.get(currentNode.getFlooded(), this.casFlooded);
            int nonCompletedColors = colorBitLimit - 1;
            for (int colorBit = 1;  colorBit < colorBitLimit;  colorBit <<= 1) {
                if (ColorAreaSet.containsAll(this.casFlooded, this.casByColorBits[colorBit])) {
                    nonCompletedColors ^= colorBit;
                }
            }
            final int prevColorBit = 1 << (currentNode.getSolutionEntry() & SolutionTree.COLOR_BIT_MASK);
            // play all possible colors
            this.storage.get(currentNode.getNeighbors(), this.casNeighbors);
            final int nextSolutionSize = currentNode.getSolutionSize() + 1;
            for (int colors = (nonCompletedColors & ~prevColorBit);  0 != colors;  ) {
                final int colorBit = colors & -colors;  // Integer.lowestOneBit(colors);
                colors ^= colorBit;
                final long[] casColorBit = this.casByColorBits[colorBit];
                if (ColorAreaSet.intersects(this.casNeighbors, casColorBit)
                        && this.canPlay(colorBit, this.iterAnd.init(this.casNeighbors, casColorBit), currentNode)) {
                    // play, part 1
                    ColorAreaSet.copyFrom(this.casNextFlooded, this.casFlooded);
                    ColorAreaSet.addAllAnd(this.casNextFlooded, this.casNeighbors, casColorBit);
                    final int nextFloodedEntry = this.map.putIfLess(this.casNextFlooded, nextSolutionSize);
                    if (nextFloodedEntry != 0) {
                        // play, part 2
                        ColorAreaSet.copyFrom(this.casNextNeighbors, this.casNeighbors);
                        ColorAreaSet.addAllAndLookup(this.casNextNeighbors, this.casNeighbors, casColorBit, this.board.getNeighborColorAreaSet4IdArray());
                        ColorAreaSet.removeAll(this.casNextNeighbors, this.casNextFlooded);
                        final AStarNode nextNode = currentNode.recycleOrNew(recycleNode);
                        nextNode.setFlooded(nextFloodedEntry);
                        nextNode.setNeighbors(this.storage.put(this.casNextNeighbors));
                        nextNode.addSolutionEntry((byte)(31 - Integer.numberOfLeadingZeros(colorBit)), this.solutionTree);
                        // finished?
                        if (ColorAreaSet.containsAll(this.casNextFlooded, casColorBit) // color completed
                                && (0 == ((nonCompletedColors ^= colorBit) & (nonCompletedColors - 1)))) { // one or zero colors remaining
                            if (0 != nonCompletedColors) {
                                nextNode.addSolutionEntry((byte)(31 - Integer.numberOfLeadingZeros(nonCompletedColors)), this.solutionTree);
                            }
                            this.addSolution(nextNode.getSolution(this.solutionTree));
                            assert printQueueStatistics(this.open);
                            return;
                        } else {
                            nextNode.setEstimatedCost(nextSolutionSize + this.strategy.estimateCost(nextNode, nonCompletedColors));
                            this.open.offer(nextNode);
                            nonCompletedColors |= colorBit;
                            recycleNode = null;
                        }
                    }
                }
            }
            recycleNode = currentNode;
        }
    }


    private boolean printQueueStatistics(final Queue<AStarNode> queue) {
        final SortedMap<Integer, Integer> histogram = new TreeMap<>();
        for (final AStarNode node : queue) {
            final Integer key = Integer.valueOf(node.getEstimatedCostSolutionSize());
            final Integer value = histogram.get(key);
            histogram.put(key, Integer.valueOf((value != null ? value.intValue() : 0) + 1));
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getSolverName()).append("_estimation,solutionsteps,count\n");
        for (final Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            sb.append(AStarNode.getEstimatedCost(entry.getKey().intValue())).append(',');
            sb.append(AStarNode.getSolutionSize (entry.getKey().intValue())).append(',');
            sb.append(entry.getValue()).append('\n');
        }
        System.out.print(sb.toString());
        return true;
    }


    /**
     * check if this color can be played. (avoid duplicate moves)
     * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
     * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
     */
    private boolean canPlay(final int nextColorBit, final ColorAreaSet.IteratorAnd nextColorNeighbors, final AStarNode currentNode) {
        final byte currColor = (byte)(currentNode.getSolutionEntry() & SolutionTree.COLOR_BIT_MASK);
        // did the previous move add any new "nextColor" neighbors?
next:   for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
            for (final ColorArea prevNeighbor : this.board.getColorArea4Id(nextColorNeighbor).getNeighborsArray()) {
                if ((prevNeighbor.getColor() != currColor) && ColorAreaSet.contains(this.casFlooded, prevNeighbor)) {
                    continue next;
                }
            }
            return true;
        }
        if (nextColorBit < (1 << currColor)) {
            return false;
        } else {
            nextColorNeighbors.restart();
            // should nextColor have been played before currColor?
            for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
                for (final ColorArea prevNeighbor : this.board.getColorArea4Id(nextColorNeighbor).getNeighborsArray()) {
                    if ((prevNeighbor.getColor() == currColor) && !ColorAreaSet.contains(this.casFlooded, prevNeighbor)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }


    /**
     * This class stores the moves of all (partial) solutions in a compact way.
     */
    protected static class SolutionTree {
        // configure this:
        private static final int MEMORY_BLOCK_SHIFT   = 20;   // 1 << 20 = 1*4 MiB
        // derived values:
        private static final int COLOR_BIT_SHIFT      = Integer.SIZE - Integer.numberOfLeadingZeros(Board.MAX_NUMBER_OF_COLORS - 1);
        private static final int COLOR_BIT_MASK       = (1 << COLOR_BIT_SHIFT) - 1;
        private static final int COLOR_BIT_MASK_INV   = ~COLOR_BIT_MASK;
        private static final int MEMORY_BLOCK_SIZE    = 1 << MEMORY_BLOCK_SHIFT;
        private static final int MEMORY_BLOCK_MASK    = MEMORY_BLOCK_SIZE - 1;

        private int[][] memoryBlocks;
        private int[] nextMemoryBlock;
        private int numMemoryBlocks, nextEntry, nextEntryOffset;

        private SolutionTree() {
            // private constructor
        }

        /**
         * Initialize this SolutionTree by adding the initial color, which is not part of the solution.
         * @param color of the starting area
         * @return initial entry
         */
        protected int init(final byte color) {
            this.numMemoryBlocks = 1;
            this.memoryBlocks = new int[this.numMemoryBlocks][MEMORY_BLOCK_SIZE];
            this.nextMemoryBlock = this.memoryBlocks[0];
            this.nextEntry = 0;
            this.nextEntryOffset = 0;
            return this.add(0, color);
        }

        /**
         * Add the next move to this SolutionTree.
         * @param previousEntry previous move
         * @param color of the next move
         * @return next entry
         */
        protected int add(final int previousEntry, final byte color) {
            final int entry = (previousEntry & COLOR_BIT_MASK_INV) | color;
            this.nextMemoryBlock[this.nextEntryOffset++] = entry;
            final int result = (this.nextEntry++ << COLOR_BIT_SHIFT) | color;
            if (this.nextEntryOffset == MEMORY_BLOCK_SIZE) {
                if (0 != (Integer.rotateLeft(this.nextEntry, COLOR_BIT_SHIFT) & COLOR_BIT_MASK)) {
                    throw new IllegalStateException(this.getClass().getSimpleName() + ".add() : memory capacity exceeded; number of entries stored=" + this.nextEntry);
                }
                if (this.memoryBlocks.length <= this.numMemoryBlocks) {
                    this.memoryBlocks = Arrays.copyOf(this.memoryBlocks, this.memoryBlocks.length * 2);
                }
                this.nextMemoryBlock = new int[MEMORY_BLOCK_SIZE];
                this.memoryBlocks[this.numMemoryBlocks++] = this.nextMemoryBlock;
                this.nextEntryOffset = 0;
            }
            return result;
        }

        /**
         * Extract the solution that ends with this move.
         * @param entry of last move
         * @param size of solution
         * @return array of moves
         */
        protected byte[] materialize(int entry, final int size) {
            final byte[] result = new byte[size];
            for (int i = size - 1;  i >= 0;  --i) {
                final int index = (entry >>> COLOR_BIT_SHIFT);
                entry = this.memoryBlocks[index >>> MEMORY_BLOCK_SHIFT][index & MEMORY_BLOCK_MASK];
                result[i] = (byte)(entry & COLOR_BIT_MASK);
            }
            return result;
        }
    }


    /**
     * This class is a minimal implementation of a HashMap, taylored to the specific use case in this AStarSolver,
     * with the aim of being faster and more efficient than the generic Java HashMap.
     * The data type of its keys is "fixed-size array of long" and its values are of type "byte".
     * Some simple and well-known methods are used: open addressing with linear probing and MurmurHash3-derived hashing (or tabulation hashing).
     */
    private static class HashMapLongArray2Byte {
        private final double LOAD_FACTOR = 0.9; // CONFIGURE THIS
        private final int KEY_SIZE; // number of "long" elements in each key
//        private final int[][] hashLookup; // lookup tables for tabulation hashing
        private long[] tableKeys;   // the table of keys
        private byte[] tableValues; // the table of values corresponding to the keys
        private int size;           // current number of data records stored in this map
        private int maxSize;        // maximum number of data records that can be stored before table size must be increased
        private int mask;           // bit mask based on current table size
        private final StateStorage storage;
        private final long[][] storageMemoryBlocks;

        /**
         * constructor
         */
        public HashMapLongArray2Byte(final Board board, final StateStorage storage) {
            this.KEY_SIZE = board.getSizeColorAreas64();
            final int initialTableSize = 1 << 16; // must be a power of two! CONFIGURE THIS
            this.tableKeys = new long[initialTableSize];
            this.tableValues = new byte[initialTableSize];
            this.size = 0;
            this.maxSize = (int)(this.tableValues.length * this.LOAD_FACTOR);
            this.mask = this.tableValues.length - 1;
            this.storage = storage;
            this.storageMemoryBlocks = storage.memoryBlocks;
//            this.hashLookup = new int[Long.BYTES * this.KEY_SIZE][1 << Byte.SIZE]; // tabulation hashing - split key into bytes
//            final long seed = Double.doubleToLongBits(Math.PI); // arbitrary, constant seed for random number generator
//            final java.util.Random random = new java.util.Random(seed); // constant seed = same pseudo-random values in each run
//            for (int y = 0, ym = this.hashLookup[0].length;  y < ym;  ++y) {
//                for (int x = 0, xm = this.hashLookup.length;  x < xm;  ++x) {
//                    this.hashLookup[x][y] = random.nextInt();
//                }
//            }
        }

        /**
         * try to put this key-value pair into the map. this will succeed if the key was not present
         * in the map before or if the new value is less than the previously stored value for the key.
         * @param newKey must be a ColorAreaSet
         * @param newValue must be greater than zero and less than 256
         * @return non-zero if the key-value pair was stored, zero if it was stored before with the same or a lower value.
         */
        public int putIfLess(final long[] newKey, final int newValue) {
            final int newHash = this.hash(newKey, 0);
            int index = newHash & this.mask;
            int oldValue, oldKey = 0;
            while ((oldValue = this.tableValues[index]) != 0) {
                // found an existing entry, now check if it's our key
                final long kh = this.tableKeys[index];
                final int oldHash = (int)kh;
                if (newHash == oldHash) {
                    oldKey = (int)(kh >>> Integer.SIZE);
                    final long[] oldKeyArray = this.storageMemoryBlocks[oldKey >>> StateStorage.MEMORY_BLOCK_SHIFT];
                    final int oldKeyIndex = oldKey & StateStorage.MEMORY_BLOCK_MASK;
                    boolean matchesKey = true;
                    for (int i = 0;  (i < this.KEY_SIZE) && matchesKey;  ++i) {
                        matchesKey = (newKey[i] == oldKeyArray[oldKeyIndex + i]);
                    }
                    if (matchesKey) {
                        break; // while
                    }
                }
                index = ++index & this.mask;
            }
            if (0 == oldValue) {
                // key not present yet
                // -> add new entry
                final int newKeyEntry = this.storage.put(newKey);
                this.tableKeys[index] = ((long)newKeyEntry << Integer.SIZE) | (0x00000000ffffffffL & newHash);
                this.tableValues[index] = (byte)newValue;
                if (++this.size > this.maxSize) {
                    this.increaseSize();
                }
                return newKeyEntry;
            } else if (newValue < (oldValue & 0xff)) {
                // entry present and new value is less than old value
                // -> update entry
                this.tableValues[index] = (byte)newValue;
                return oldKey;
            } else {
                // entry present and new value is same or greater than old value
                // -> do nothing
                return 0;
            }
        }

        /**
         * calculate the 32bit hash value of the array of long.
         */
//        private int hash(final long[] key, final int startIndex) {
//            // tabulation hashing
//            int result = 0;
//            for (int i = 0, k = startIndex, km = startIndex + this.KEY_SIZE;  k < km;  ++k, i += Long.BYTES) {
//                // manually unrolled loop that processes the 8 bytes of each long.
//                // the compiler might be able to optimize this code for a speedup.
//                final long l = key[k];
//                final int h0 = this.hashLookup[ i     ][ (int)(l                    ) & 0xff ];
//                final int h1 = this.hashLookup[ i + 1 ][ (int)(l >>> (1 * Byte.SIZE)) & 0xff ];
//                final int h2 = this.hashLookup[ i + 2 ][ (int)(l >>> (2 * Byte.SIZE)) & 0xff ];
//                final int h3 = this.hashLookup[ i + 3 ][ (int)(l >>> (3 * Byte.SIZE)) & 0xff ];
//                final int h4 = this.hashLookup[ i + 4 ][ (int)(l >>> (4 * Byte.SIZE)) & 0xff ];
//                final int h5 = this.hashLookup[ i + 5 ][ (int)(l >>> (5 * Byte.SIZE)) & 0xff ];
//                final int h6 = this.hashLookup[ i + 6 ][ (int)(l >>> (6 * Byte.SIZE)) & 0xff ];
//                final int h7 = this.hashLookup[ i + 7 ][ (int)(l >>> (7 * Byte.SIZE))        ];
//                result ^= h0 ^ h1 ^ h2 ^ h3 ^ h4 ^ h5 ^ h6 ^ h7;
//            }
//            return result;
//        }
        private int hash(final long[] key, final int startIndex) {
            // based on MurmurHash3_x86_32
            int h1 = 12345; // seed
            for (int k = startIndex, km = startIndex + this.KEY_SIZE;  k < km;  ++k) {
                final long l = key[k];
                int k1 = (int)l;
                k1 *= 0xcc9e2d51;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= 0x1b873593;
                h1 ^= k1;
                h1 = Integer.rotateLeft(h1, 13);
                h1 = h1 * 5 + 0xe6546b64;
                k1 = (int)(l >>> 32);
                k1 *= 0xcc9e2d51;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= 0x1b873593;
                h1 ^= k1;
                h1 = Integer.rotateLeft(h1, 13);
                h1 = h1 * 5 + 0xe6546b64;
            }
            h1 ^= h1 >>> 16;
            h1 *= 0x85ebca6b;
            h1 ^= h1 >>> 13;
            h1 *= 0xc2b2ae35;
            h1 ^= h1 >>> 16;
            return h1;
        }
//        // based on XXHash32
//        // https://github.com/lz4/lz4-java
//        // https://github.com/richardstartin/xxhash-benchmark
//        private static final int PRIME1 = 0x9E3779B1;
//        private static final int PRIME2 = 0x85EBCA77;
//        private static final int PRIME3 = 0xC2B2AE3D;
//        private static final int PRIME4 = 0x27D4EB2F;
//        private static final int PRIME5 = 0x165667B1;
//        private static final int seed = 12345;
//        private int hash(final long[] key, final int startIndex) {
//            int h32;
//            int k = startIndex;
//            final int km = startIndex + this.KEY_SIZE;
//            if (this.KEY_SIZE >= 2) { // operate on 4 streams of 32 bits in parallel
//                int v1 = seed + PRIME1 + PRIME2;
//                int v2 = seed + PRIME2;
//                int v3 = seed;
//                int v4 = seed - PRIME1;
//                do {
//                    final long l1 = key[k++];
//                    v1 += (int)(l1) * PRIME2;
//                    v1 = Integer.rotateLeft(v1, 13);
//                    v1 *= PRIME1;
//                    v2 += (int)(l1 >>> 32) * PRIME2;
//                    v2 = Integer.rotateLeft(v2, 13);
//                    v2 *= PRIME1;
//                    final long l2 = key[k++];
//                    v3 += (int)(l2) * PRIME2;
//                    v3 = Integer.rotateLeft(v3, 13);
//                    v3 *= PRIME1;
//                    v4 += (int)(l2 >>> 32) * PRIME2;
//                    v4 = Integer.rotateLeft(v4, 13);
//                    v4 *= PRIME1;
//                } while (k < km - 1);
//                h32 = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7) + Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
//            } else {
//                h32 = seed + PRIME5;
//            }
//            while (k < km) {
//                final long l = key[k++];
//                h32 += (int)(l) * PRIME3;
//                h32 = Integer.rotateLeft(h32, 17) * PRIME4;
//                h32 += (int)(l >>> 32) * PRIME3;
//                h32 = Integer.rotateLeft(h32, 17) * PRIME4;
//            }
//            h32 ^= h32 >>> 15;
//            h32 *= PRIME2;
//            h32 ^= h32 >>> 13;
//            h32 *= PRIME3;
//            h32 ^= h32 >>> 16;
//            return h32;
//        }

        /**
         * double the storage space in the internal tables
         */
        private void increaseSize() {
            // allocate new tables, twice as large as the current ones
            final long[] oldTableKeys = this.tableKeys;
            this.tableKeys = new long[oldTableKeys.length << 1];
            final byte[] oldTableValues = this.tableValues;
            this.tableValues = new byte[oldTableValues.length << 1];
            this.maxSize = (int)(this.tableValues.length * this.LOAD_FACTOR);
            this.mask = this.tableValues.length - 1;
            // add all entries to the new tables
            int oldIndex = 0;
            for (final byte value : oldTableValues) {
                if (value != 0) {
                    final long kh = oldTableKeys[oldIndex];
                    final int hash = (int)kh;
                    int newIndex = hash & this.mask;
                    while (this.tableValues[newIndex] != 0) {
                        // there can't be any duplicate keys, so just skip all occupied slots
                        newIndex = ++newIndex & this.mask;
                    }
                    this.tableKeys[newIndex] = kh;
                    this.tableValues[newIndex] = value;
                }
                ++oldIndex;
            }
        }
    }

    /**
     * This class implements a compact storage area of the contents of many
     * ColorAreaSet objects (which are actually just long[] = arrays of long).
     * Individual entries of this storage are accessed via integer keys.
     */
    protected static class StateStorage {
        public static final int MEMORY_BLOCK_SHIFT = 20; // 20 == 8 MiB
        public static final int MEMORY_BLOCK_SIZE = 1 << MEMORY_BLOCK_SHIFT; // must be a power of two
        public static final int MEMORY_BLOCK_MASK = MEMORY_BLOCK_SIZE - 1;
        public final long[][] memoryBlocks = new long[1 << (Integer.SIZE - MEMORY_BLOCK_SHIFT)][];
        private int numMemoryBlocks = 1, entry = 0, offset = 0;
        private final int stateSize, endOffset;

        /** the constructor */
        public StateStorage(final Board board) {
            memoryBlocks[0] = new long[MEMORY_BLOCK_SIZE];
            stateSize = board.getSizeColorAreas64(); // equal to the length of ColorAreaSet objects (arrays of "long")
            endOffset = MEMORY_BLOCK_SIZE - stateSize;
        }

        /** copy the contents of the storage entry to the ColorAreaSet. */
        public void get(int keySrc, long[] casDest) {
            System.arraycopy(memoryBlocks[keySrc >>> MEMORY_BLOCK_SHIFT], keySrc & MEMORY_BLOCK_MASK, casDest, 0, stateSize);
        }

        /** copy the contents of the ColorAreaSet to a new storage entry. */
        public int put(long[] casSrc) {
            final int keyDest = this.add();
            System.arraycopy(casSrc, 0, memoryBlocks[keyDest >>> MEMORY_BLOCK_SHIFT], keyDest & MEMORY_BLOCK_MASK, stateSize);
            return keyDest;
        }

        /** add a new storage entry and return the key. */
        private int add() {
            final int result = entry;
            entry += stateSize;
            offset += stateSize;
            if (offset > endOffset) { // must allocate another block of memory
                if (offset == MEMORY_BLOCK_SIZE) {
                    offset = 0;
                } else {
                    entry += stateSize;
                    offset -= endOffset;
                }
                if (numMemoryBlocks >= memoryBlocks.length) {
                    throw new IllegalStateException("Integer overflow! (32 GB of data storage exceeded)");
                }
                memoryBlocks[numMemoryBlocks++] = new long[MEMORY_BLOCK_SIZE];
            }
            return result;
        }
    }
}
