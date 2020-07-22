/*  ColorFill game and solver
    Copyright (C) 2014, 2020 Michael Henke

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
import java.util.PriorityQueue;
import java.util.Queue;

import colorfill.model.Board;
import colorfill.model.ColorArea;
import colorfill.model.ColorAreaSet;

/**
 * a solver implementation that implements the AStar (A*) algorithm.
 */
public class AStarSolver extends AbstractSolver {

    private Class<? extends AStarStrategy> strategyClass = AStarTigrouStrategy.class; // default
    private AStarStrategy strategy;
    private final SolutionTree solutionTree = new SolutionTree();
    private final ColorAreaSet.Iterator iter;
    private final ColorAreaSet.IteratorAnd iterAnd;
    private final ColorAreaSet[] casByColor;

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    protected AStarSolver(Board board) {
        super(board);
        this.iter = new ColorAreaSet.Iterator();
        this.iterAnd = new ColorAreaSet.IteratorAnd();
        this.casByColor = board.getCasByColorArray();
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

    private AStarStrategy makeStrategy() {
        final AStarStrategy result;
        if (AStarTigrouStrategy.class.equals(this.strategyClass)) {
            result = new AStarTigrouStrategy(this.board, this.solutionTree, this);
        } else if (AStarPuchertStrategy.class.equals(this.strategyClass)) {
            result = new AStarPuchertStrategy(this.board);
        } else if (AStarFlolleStrategy.class.equals(this.strategyClass)) {
            result = new AStarFlolleStrategy(this.board);
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
    protected void executeInternal(int startPos) throws InterruptedException {
        this.strategy = this.makeStrategy();

        final ColorArea startCa = this.board.getColorArea4Cell(startPos);

        if (this.strategy instanceof AStarTigrouStrategy) {
            this.executeInternalTigrou(startCa);
        } else if (this.strategy instanceof AStarPuchertStrategy) {
            this.executeInternalPuchert(startCa);
        } else if (this.strategy instanceof AStarFlolleStrategy) {
            this.executeInternalPuchert(startCa);
        }
    }


    private void executeInternalPuchert(final ColorArea startCa) throws InterruptedException {
        final Queue<AStarNode> open = new PriorityQueue<AStarNode>(AStarNode.strongerComparator());
        open.offer(new AStarNode(this.board, startCa, this.solutionTree));
        AStarNode recycleNode = null;
        while (open.size() > 0) {
            if (Thread.interrupted()) { throw new InterruptedException(); }
            final AStarNode currentNode = open.poll();
            if (currentNode.isSolved()) {
                this.addSolution(currentNode.getSolution(this.solutionTree));
                return;
            } else {
                // play all possible colors
                final ColorAreaSet neighbors = currentNode.getNeighbors();
                int nextColors = this.getColors(neighbors);
                while (0 != nextColors) {
                    final int l1b = nextColors & -nextColors; // Integer.lowestOneBit()
                    final int clz = Integer.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
                    nextColors ^= l1b; // clear lowest one bit
                    final byte color = (byte)(31 - clz);
                    final ColorAreaSet.IteratorAnd nextColorNeighbors = this.getColorAreas(neighbors, color);
                    if (this.canPlay(color, nextColorNeighbors, currentNode)) {
                        nextColorNeighbors.restart();
                        final AStarNode nextNode = currentNode.copyAndPlay(color, recycleNode, nextColorNeighbors, this.solutionTree, this.board);
                        recycleNode = null;
                        this.strategy.setEstimatedCost(nextNode);
                        open.offer(nextNode);
                    }
                }
            }
            recycleNode = currentNode;
        }
    }


    private void executeInternalTigrou(final ColorArea startCa) throws InterruptedException {
        // use a PriorityQueue (faster!)
        final Queue<AStarNode> open = new PriorityQueue<AStarNode>(AStarNode.simpleComparator());
        open.offer(new AStarNode(this.board, startCa, this.solutionTree));
        while (open.size() > 0) {
            if (Thread.interrupted()) { throw new InterruptedException(); }
            final AStarNode currentNode = open.poll();
            if (currentNode.isSolved()) {
                this.addSolution(currentNode.getSolution(this.solutionTree));
//                return;
            } else {
                if (currentNode.getEstimatedCost() > this.solutionSize) {
                    return;  // finished!
                } else {
                    // play all possible colors
                    final ColorAreaSet neighbors = currentNode.getNeighbors();
                    int nextColors = this.getColors(neighbors);
                    while (0 != nextColors) {
                        final int l1b = nextColors & -nextColors; // Integer.lowestOneBit()
                        final int clz = Integer.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
                        nextColors ^= l1b; // clear lowest one bit
                        final AStarNode nextNode = new AStarNode(currentNode);
                        final byte color = (byte)(31 - clz);
                        nextNode.play(color, this.getColorAreas(neighbors, color), this.solutionTree, this.board);
                        this.strategy.setEstimatedCost(nextNode);
                        open.offer(nextNode);
                    }
                }
            }
        }
        // if we get here then we have not found any solution
    }


    /**
     * get the list of neighbor colors.
     * NOTE: uses this.iter
     * @return
     */
    protected int getColors(final ColorAreaSet caSet) {
        int result = 0;
        this.iter.init(caSet);
        for (int nextId;  (nextId = this.iter.nextOrNegative()) >= 0;  ) {
            result |= 1 << this.board.getColor4Id(nextId);
        }
        return result;
    }


    /**
     * extract the ColorAreas of the specified color.
     * NOTE: uses this.iterAnd
     * @return ColorAreaSet.IteratorAnd
     */
    protected ColorAreaSet.IteratorAnd getColorAreas(final ColorAreaSet caSet, final byte color) {
        this.iterAnd.init(caSet, this.casByColor[color]);
        return this.iterAnd;
    }


    /**
     * check if this color can be played. (avoid duplicate moves)
     * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
     * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
     * NOTE: uses this.iter
     */
    private boolean canPlay(final byte nextColor, final ColorAreaSet.IteratorAnd nextColorNeighbors, final AStarNode currentNode) {
        final byte currColor = this.solutionTree.getColor(currentNode.getSolutionEntry());
        final ColorAreaSet flooded = currentNode.getFlooded();
        // did the previous move add any new "nextColor" neighbors?
        boolean newNext = false;
next:   for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
            for (final ColorArea prevNeighbor : this.board.getColorArea4Id(nextColorNeighbor).getNeighborsArray()) {
                if ((prevNeighbor.getColor() != currColor) && flooded.contains(prevNeighbor)) {
                    continue next;
                }
            }
            newNext = true;
            break next;
        }
        if (!newNext) {
            if (nextColor < currColor) {
                return false;
            } else {
                nextColorNeighbors.restart();
                // should nextColor have been played before currColor?
                for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
                    for (final ColorArea prevNeighbor : this.board.getColorArea4Id(nextColorNeighbor).getNeighborsArray()) {
                        if ((prevNeighbor.getColor() == currColor) && !flooded.contains(prevNeighbor)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        } else {
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

        /**
         * Extract the color of this move.
         * @param entry
         * @return color of entry
         */
        protected byte getColor(final int entry) {
            return (byte)(entry & COLOR_BIT_MASK);
        }
    }
}
