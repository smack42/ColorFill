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

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * a solver implementation that implements the AStar (A*) algorithm.
 */
public class AStarSolver extends AbstractSolver {

    private Class<? extends AStarStrategy> strategyClass = AStarTigrouStrategy.class; // default
    private AStarStrategy strategy;

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

    private AStarStrategy makeStrategy() {
        final AStarStrategy result;
        if (AStarTigrouStrategy.class.equals(this.strategyClass)) {
            result = new AStarTigrouStrategy();
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

        // use a PriorityQueue (faster!)
        final java.util.Queue<AStarNode> open = new java.util.PriorityQueue<AStarNode>();
        open.offer(new AStarNode(this.board, startCa));
        while (open.size() > 0) {
            if (Thread.interrupted()) { throw new InterruptedException(); }
            final AStarNode currentNode = open.poll();
            if (currentNode.isSolved()) {
                this.addSolution(currentNode.getSolution());
//                return;
            } else {
                if (currentNode.getEstimatedCost() > this.solutionSize) {
                    return;  // finished!
                } else {
                    // play all possible colors
                    for (final byte nextColor : currentNode.getNeighborColors()) {
                        final AStarNode nextNode = new AStarNode(currentNode);
                        nextNode.play(nextColor);
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
}
