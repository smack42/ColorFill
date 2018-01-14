/*  ColorFill game and solver
    Copyright (C) 2014, 2017 Michael Henke

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
 * a specific strategy for the AStar (A*) solver.
 * <p>
 * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
 * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
 */
public class AStarPuchertStrategy implements AStarStrategy {

    private final ColorAreaSet visited;
    private ColorAreaSet current, next;
    private final int[] colorsAtDistance = new int[AbstractSolver.MAX_SEARCH_DEPTH];

    AStarPuchertStrategy(final Board board) {
        this.visited = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AStarStrategy#setEstimatedCost(colorfill.solver.AStarNode)
     */
    @Override
    public void setEstimatedCost(final AStarNode node) {

        // quote from floodit.cpp: int State::computeValuation()
        //
        // We observe the following: for every distance d of which we have nodes,
        // the sum of the distance plus the number of colors of nodes of distance
        // larger than d is a lower bound for the number of moves needed. That is
        // because the first d moves can at most remove nodes of distance less than
        // or equal to d, and the remaining colors have to be cleared by separate
        // moves. The maximum of this number over all d for which we have nodes is
        // obviously still a lower bound, hence admissible. It is also consistent.

        node.copyFloodedTo(visited);
        //current.clear();  // clear() not necessary because it's empty
        //next.clear();     // clear() not necessary because it's empty

        // collect the immediate neighbors (distance = 0)
        int colors = node.getNeighborColors();
        colorsAtDistance[0] = colors;
        while (0 != colors) {
            final int l1b = colors & -colors; // Integer.lowestOneBit()
            final int clz = Integer.numberOfLeadingZeros(l1b); // hopefully an intrinsic function using instruction BSR / LZCNT / CLZ
            colors ^= l1b; // clear lowest one bit
            final ColorAreaSet caSet = node.getNeighbors(31 - clz);
            visited.addAll(caSet);
            current.addAll(caSet);
        }
        // collect colors at increasing distances
        int distance = 1;
        while(!current.isEmpty()) {
            for (final ColorArea thisCa : current) {
                for (final ColorArea nextCa : thisCa.getNeighborsArray()) {
                    if (!visited.contains(nextCa)) {
                        visited.add(nextCa);
                        next.add(nextCa);
                        colors |= (1 << nextCa.getColor());
                    }
                }
            }
            colorsAtDistance[distance++] = colors;
            colors = 0;
            final ColorAreaSet tmp = current;
            current = next;
            next = tmp;
            next.clear();
        }
        // (here, the ColorAreas current and next are empty)
        // in the collected colors, find the maximum of (distance + numcolors_at_greater_distance)
        int max = 0;
        for (--distance;  distance >= 0;  --distance) {
            max = Integer.max(max, distance + Integer.bitCount(colors));  // hopefully an intrinsic function using instruction POPCNT
            colors |= colorsAtDistance[distance];
        }
        node.setEstimatedCost(node.getSolutionSize() + max);
    }

}
