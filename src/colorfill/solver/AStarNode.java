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

import java.util.Comparator;
import java.util.Queue;

import colorfill.model.Board;
import colorfill.model.ColorArea;
import colorfill.model.ColorAreaSet;
import colorfill.solver.AStarSolver.SolutionTree;

/**
 * the node used by the AStar (A*) solver.
 */
public class AStarNode {

    private final long[] flooded;
    private final long[] neighbors;
    private int solutionEntry;

    /**
     * one 32bit-int data field that stores the values of two separate fields:
     * <p>
     * byte estimatedCost = estimated total number of steps to end of search = solutionSize + estimation by the heuristic algorithm<br>
     * byte solutionSize  = number of steps done, from start of search to the current (intermediate) state
     * <p>
     * the fields estimatedCost and solutionSize are stored in a particular way,
     * to facilitate the operation of "strongerComparator" in a single step, for increased performance.
     * field solutionSize is therefore located in the lower byte of the packed field
     * and its value is stored in ones' complement format (0=0xff, 1=0xfe, 2=0xfd, ...)
     * <p>
     * TODO find something useful to store in the upper 16 bits, which are currently not used
     */
    private int packedData;
    private static final int DATA_MASK_ESTIMATED_COST   = 0x0000ff00;
    private static final int DATA_SHIFT_ESTIMATED_COST  = 8;
    private static final int DATA_MASK_SOLUTION_SIZE    = 0x000000ff;
    private static final int DATA_MASK_ESTIMATED_COST_SOLUTION_SIZE = DATA_MASK_ESTIMATED_COST | DATA_MASK_SOLUTION_SIZE;

    /**
     * initial constructor.
     * @param startCa
     */
    public AStarNode(final Board board, final ColorArea startCa, final SolutionTree solutionTree) {
        this.flooded = ColorAreaSet.constructor(board);
        ColorAreaSet.add(this.flooded, startCa);
        this.neighbors = ColorAreaSet.constructor(board);
        ColorAreaSet.addAll(this.neighbors, startCa.getNeighborsColorAreaSet());
        this.solutionEntry = solutionTree.init(startCa.getColor());
        this.packedData = DATA_MASK_SOLUTION_SIZE; // estimatedCost=0, solutionSize=0xff=~zero
    }

    /**
     * copy constructor.
     * @param other
     */
    public AStarNode(final AStarNode other) {
        this.flooded = ColorAreaSet.constructor(other.flooded);
        this.neighbors = ColorAreaSet.constructor(other.neighbors);
        this.solutionEntry = other.solutionEntry;
        this.packedData = other.packedData;
    }

    /**
     * is this a final node?
     * @return
     */
    public boolean isSolved() {
        return ColorAreaSet.isEmpty(this.neighbors);
    }

    /**
     * get the solution stored in this node.
     * @return
     */
    public byte[] getSolution(final SolutionTree solutionTree) {
        return solutionTree.materialize(this.solutionEntry, this.getSolutionSize());
    }

    /**
     * get the number of steps in the solution of this node.
     * @return
     */
    public int getSolutionSize() {
        return (DATA_MASK_SOLUTION_SIZE & ~this.packedData); // ~ NOT operator is NEGATE in ones' complement
    }

    /**
     * get the current solutionEntry stored in this node. (pointer in SolutionTree)
     * @return
     */
    public int getSolutionEntry() {
        return this.solutionEntry;
    }

    /**
     * get the set of neighbors.
     * @return
     */
    public long[] getNeighbors() {
        return this.neighbors;
    }

    /**
     * get the set of flooded color areas.
     * @return
     */
    public long[] getFlooded() {
        return this.flooded;
    }

    /**
     * get the number of flooded color areas.
     */
    public int getFloodedSize() {
        return ColorAreaSet.size(this.flooded);
    }

    /**
     * copy contents of "flooded" set to this one.
     * @param other
     */
    public void copyFloodedTo(final long[] other) {
        ColorAreaSet.copyFrom(other, this.flooded);
    }

    /**
     * copy contents of "neighbors" set to this one.
     * @param other
     */
    public void copyNeighborsTo(final long[] other) {
        ColorAreaSet.copyFrom(other, this.neighbors);
    }

    /**
     * play the given color.
     * @param nextColor
     */
    public void play(final byte nextColor, final ColorAreaSet.IteratorAnd nextColorNeighbors, final SolutionTree solutionTree, final Board board) {
        for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
            ColorAreaSet.add(this.flooded, nextColorNeighbor);
            ColorAreaSet.addAll(this.neighbors, board.getNeighborColorAreaSet4Id(nextColorNeighbor));
        }
        ColorAreaSet.removeAll(this.neighbors, this.flooded);
        --this.packedData; // increment solutionSize  TODO check overflow
        this.solutionEntry = solutionTree.add(this.solutionEntry, nextColor);
    }

    /**
     * try to re-use the given node or create a new one
     * and then play the given color in the result node.
     * @param nextColor
     * @param recycleNode
     * @return
     */
    public AStarNode copyAndPlay(final AStarNode recycleNode, final ColorAreaSet.IteratorAnd nextColorNeighbors, final Board board) {
        final AStarNode result;
        if (null == recycleNode) {
            result = new AStarNode(this);
        } else {
            // copy - compare copy constructor
            result = recycleNode;
            ColorAreaSet.copyFrom(result.flooded, this.flooded);
            ColorAreaSet.copyFrom(result.neighbors, this.neighbors);
            result.solutionEntry = this.solutionEntry;
            result.packedData = this.packedData;
        }
        // play - compare method play()
        for (int nextColorNeighbor;  (nextColorNeighbor = nextColorNeighbors.nextOrNegative()) >= 0;  ) {
            ColorAreaSet.add(result.flooded, nextColorNeighbor);
            ColorAreaSet.addAll(result.neighbors, board.getNeighborColorAreaSet4Id(nextColorNeighbor));
        }
        ColorAreaSet.removeAll(result.neighbors, result.flooded);
        return result;
    }

    public void addSolutionEntry(final byte nextColor, final SolutionTree solutionTree) {
        --this.packedData; // increment solutionSize  TODO check overflow
        this.solutionEntry = solutionTree.add(this.solutionEntry, nextColor);
    }

    /**
     * create a "simple" comparator for use in PriorityQueue
     * @return
     */
    public static Comparator<AStarNode> simpleComparator() {
        return new Comparator<AStarNode>() {
            @Override
            public int compare(AStarNode o1, AStarNode o2) {
                final int diff = (o1.packedData & DATA_MASK_ESTIMATED_COST)
                               - (o2.packedData & DATA_MASK_ESTIMATED_COST);
                return diff;
            }
        };
    }

    /**
     * create a "stronger" comparator for use in PriorityQueue
     * @return
     */
    public static Comparator<AStarNode> strongerComparator() {
        return new Comparator<AStarNode>() {
            @Override
            public int compare(AStarNode o1, AStarNode o2) {
                final int diff = (o1.packedData & DATA_MASK_ESTIMATED_COST_SOLUTION_SIZE)
                               - (o2.packedData & DATA_MASK_ESTIMATED_COST_SOLUTION_SIZE);
                return diff;
            }
        };
    }

    /**
     * set estimated cost, which is used for natural ordering (in function compareTo)
     * @param estimatedCost
     */
    public void setEstimatedCost(final int estimatedCost) {
        this.packedData = (this.packedData & ~DATA_MASK_ESTIMATED_COST) | (estimatedCost << DATA_SHIFT_ESTIMATED_COST); // TODO check overflow
    }
    public int getEstimatedCost() {
        return ((this.packedData & DATA_MASK_ESTIMATED_COST) >>> DATA_SHIFT_ESTIMATED_COST);
    }

    /**
     * calculate the sum of distances from current flooded area to all remaining areas.
     * @param queue an empty Queue; used inside this function; will be empty on return.
     * @param depths an array of int; must be large enough to store a value for each ColorArea on the Board.
     * @return
     */
    public int getSumDistances(final Queue<ColorArea> queue, final Board board) {
        final int NO_DEPTH = -1;
        for (final ColorArea ca : board.getColorAreasArray()) {
            if (ColorAreaSet.contains(this.flooded, ca)) {
                ca.tmpAStarDepth = 0;  // start
                queue.offer(ca);
            } else {
                ca.tmpAStarDepth = NO_DEPTH;  // reset
            }
        }
        int sumDistances = 0;
        ColorArea currentCa;
        while (null != (currentCa = queue.poll())) { // while queue is not empty
            final int nextDepth = currentCa.tmpAStarDepth + 1;
            for (final ColorArea nextCa : currentCa.getNeighborsArray()) {
                if (nextCa.tmpAStarDepth == NO_DEPTH) {
                    nextCa.tmpAStarDepth = nextDepth;
                    sumDistances += nextDepth;
                    queue.offer(nextCa);
                }
            }
        }
        // queue is empty now
        return sumDistances;
    }
}
