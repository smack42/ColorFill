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

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * a solver implementation that implements the AStar (A*) algorithm.
 */
public class AStarSolver extends AbstractSolver {

    private Class<? extends AStarStrategy> strategyClass = AStarTigrouStrategy.class; // default
    private AStarStrategy strategy;
    private final SolutionTree solutionTree = new SolutionTree();

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    protected AStarSolver(Board board) {
        super(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.Solver#setStrategy(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setStrategy(final Class<Strategy> strategyClass) {
        if (false == AStarStrategy.class.isAssignableFrom(strategyClass)) {
            throw new IllegalArgumentException(
                    "unsupported strategy class " + strategyClass.getName()
                    + "! " + this.getClass().getSimpleName() + " supports " + AStarStrategy.class.getSimpleName() + " only.");
        }
        this.strategyClass = (Class<? extends AStarStrategy>) strategyClass;
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
            result = new AStarTigrouStrategy(this.board, this.solutionTree);
        } else if (AStarPuchertStrategy.class.equals(this.strategyClass)) {
            result = new AStarPuchertStrategy(this.board);
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
                int nextColors = currentNode.getNeighborColors(this.board);
                while (0 != nextColors) {
                    final int l1b = nextColors & -nextColors; // Integer.lowestOneBit()
                    final int clz = Integer.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
                    nextColors ^= l1b; // clear lowest one bit
                    final byte color = (byte)(31 - clz);
                    final AStarNode nextNode = currentNode.copyAndPlay(color, recycleNode, this.board, this.solutionTree);
                    if (null != nextNode) {
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
                    int nextColors = currentNode.getNeighborColors(this.board);
                    while (0 != nextColors) {
                        final int l1b = nextColors & -nextColors; // Integer.lowestOneBit()
                        final int clz = Integer.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
                        nextColors ^= l1b; // clear lowest one bit
                        final AStarNode nextNode = new AStarNode(currentNode);
                        nextNode.play((byte)(31 - clz), this.board, this.solutionTree);
                        this.strategy.setEstimatedCost(nextNode);
                        open.offer(nextNode);
                    }
                }
            }
        }
        // use a LinkedList (slower but stronger!?)
//        final java.util.LinkedList<AStarNode> open = new java.util.LinkedList<AStarNode>();
//        open.addLast(new AStarNode(this.board, startCa));
//        int solvedEstimatedCost = Integer.MAX_VALUE;
//        while (open.size() > 0) {
//            if (Thread.interrupted()) { throw new InterruptedException(); }
//            final AStarNode currentNode = open.poll();
//            if (currentNode.isSolved()) {
//                if (this.addSolution(currentNode.getSolution())) {
//                    solvedEstimatedCost = currentNode.getEstimatedCost();
//                }
//            } else {
//                if (currentNode.getEstimatedCost() > solvedEstimatedCost) {
//                    return;  // finished!
//                } else {
//                    // play all possible colors
//                    for (final byte nextColor : currentNode.getNeighborColors()) {
//                        AStarNode nextNode = new AStarNode(currentNode);
//                        nextNode.play(nextColor);
//                        this.strategy.setEstimatedCost(nextNode);
//                        final java.util.ListIterator<AStarNode> li = open.listIterator();
//                        while (li.hasNext()) {
//                            final AStarNode n = li.next();
//                            if (n.getEstimatedCost() > nextNode.getEstimatedCost()) {
//                                li.previous();
//                                li.add(nextNode);
//                                nextNode = null;
//                                break;
//                            }
//                        }
//                        if (null != nextNode) {
//                            open.addLast(nextNode);
//                        }
//                    }
//                }
//            }
//        }
        // if we get here then we have not found any solution
    }


    /**
     * This class stores the moves of all (partial) solutions in a compact way.
     */
    protected static class SolutionTree {
        // configure this:
        private static int MAX_NUMBER_OF_COLORS = 8;    // 8 colors = 3 bits
        private static int MEMORY_BLOCK_SHIFT   = 20;   // 1 << 20 = 1*4 MiB
        // derived values:
        private static int COLOR_BIT_SHIFT      = Integer.SIZE - Integer.numberOfLeadingZeros(MAX_NUMBER_OF_COLORS - 1);
        private static int COLOR_BIT_MASK       = (1 << COLOR_BIT_SHIFT) - 1;
        private static int COLOR_BIT_MASK_INV   = ~COLOR_BIT_MASK;
        private static int MEMORY_BLOCK_SIZE    = 1 << MEMORY_BLOCK_SHIFT;
        private static int MEMORY_BLOCK_MASK    = MEMORY_BLOCK_SIZE - 1;

        private int[][] memoryBlocks;
        private int[] nextMemoryBlock;
        private int numMemoryBlocks, nextEntry, nextEntryOffset;

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
