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

import java.util.Comparator;

import colorfill.model.Board;
import colorfill.model.ColorArea;
import colorfill.model.ColorAreaSet;
import colorfill.solver.AStarSolver.SolutionTree;
import colorfill.solver.AStarSolver.StateStorage;

/**
 * the node used by the AStar (A*) solver.
 */
public class AStarNode {

    private int flooded;        // entry in StateStorage
    private int neighbors;      // entry in StateStorage
    private int solutionEntry;  // entry in SolutionTree

    /**
     * one 32bit-int data field that stores the values of two separate fields:
     * <p>
     * short estimatedCost = estimated total number of steps to end of search = solutionSize + estimation by the heuristic algorithm<br>
     * short solutionSize  = number of steps done, from start of search to the current (intermediate) state
     * <p>
     * the fields estimatedCost and solutionSize are stored in a particular way,
     * to facilitate the operation of "strongerComparator" in a single step, for increased performance.
     * field solutionSize is therefore located in the lower half of the packed field
     * and its value is stored in ones' complement format (0=0xffff, 1=0xfffe, 2=0xfffd, ...)
     */
    private int packedData;
    private static final int DATA_MASK_ESTIMATED_COST   = 0xffff0000;
    private static final int DATA_SHIFT_ESTIMATED_COST  = 16;
    private static final int DATA_MASK_SOLUTION_SIZE    = 0x0000ffff;

    /**
     * initial constructor.
     */
    public AStarNode(final Board board, final ColorArea startCa, final StateStorage storage, final SolutionTree solutionTree) {
        final long[] casFlooded = ColorAreaSet.constructor(board);
        ColorAreaSet.add(casFlooded, startCa);
        this.flooded = storage.put(casFlooded);
        final long[] casNeighbors = ColorAreaSet.constructor(board);
        ColorAreaSet.addAll(casNeighbors, startCa.getNeighborsColorAreaSet());
        this.neighbors = storage.put(casNeighbors);
        this.solutionEntry = solutionTree.init(startCa.getColor());
        this.packedData = DATA_MASK_SOLUTION_SIZE; // estimatedCost=0, solutionSize=0xffff=~zero
    }

    /**
     * empty constructor.
     */
    private AStarNode() {
        // nothing
    }

    /**
     * recycle the specified AStarNode object or create a new one if it's null.
     */
    public AStarNode recycleOrNew(final AStarNode recycle) {
        final AStarNode result = (recycle != null ? recycle : new AStarNode());
        // flooded and neighbors are not copied here
        result.solutionEntry = this.solutionEntry;
        result.packedData = this.packedData;
        return result;
    }

    /**
     * get the solution stored in this node.
     * @return
     */
    public byte[] getSolution(final SolutionTree solutionTree) {
        return solutionTree.materialize(this.solutionEntry, this.getSolutionSize());
    }

    /**
     * get the current solutionEntry stored in this node. (pointer in SolutionTree)
     * @return
     */
    public int getSolutionEntry() {
        return this.solutionEntry;
    }

    public void addSolutionEntry(final byte nextColor, final SolutionTree solutionTree) {
        --this.packedData; // increment solutionSize  TODO check overflow
        this.solutionEntry = solutionTree.add(this.solutionEntry, nextColor);
    }

    /**
     * create a "stronger" comparator for use in PriorityQueue
     * @return
     */
    public static Comparator<AStarNode> strongerComparator() {
        return new Comparator<AStarNode>() {
            @Override
            public int compare(AStarNode o1, AStarNode o2) {
                final int diff = o1.packedData - o2.packedData;
                return diff;
            }
        };
    }

    /**
     * get the number of steps in the solution of this node.
     * @return
     */
    public int getSolutionSize() {
        return (DATA_MASK_SOLUTION_SIZE & ~this.packedData); // ~ NOT operator is NEGATE in ones' complement
    }
    public static int getSolutionSize(final int data) {
        return (DATA_MASK_SOLUTION_SIZE & ~data); // ~ NOT operator is NEGATE in ones' complement
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
    public static int getEstimatedCost(final int data) {
        return ((data & DATA_MASK_ESTIMATED_COST) >>> DATA_SHIFT_ESTIMATED_COST);
    }
    public int getEstimatedCostSolutionSize() {
        return this.packedData;
    }

    public int getFlooded() {
        return this.flooded;
    }
    public void setFlooded(int flooded) {
        this.flooded = flooded;
    }

    public int getNeighbors() {
        return this.neighbors;
    }
    public void setNeighbors(int neighbors) {
        this.neighbors = neighbors;
    }
}
