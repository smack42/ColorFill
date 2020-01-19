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
import colorfill.model.ColorAreaSet;

/**
 * a specific strategy for the AStar (A*) solver.
 * <p>
 * the idea is taken from the program "floodit" by Aaron and Simon Puchert,
 * which can be found at <a>https://github.com/aaronpuchert/floodit</a>
 */
public class AStarPuchertStrategy implements AStarStrategy {

    private final Board board;
    private final ColorAreaSet visited, tmp;
    private ColorAreaSet current, next;
    private final ColorAreaSet[] caByColor;
    private final int[] numCaNotFilledInitial;
    private final int[] numCaNotFilled;
    private final ColorAreaSet.FastIteratorColorAreaId iter;
    private final int allColors;

    public AStarPuchertStrategy(final Board board) {
        this.board = board;
        this.visited = new ColorAreaSet(board);
        this.tmp = new ColorAreaSet(board);
        this.current = new ColorAreaSet(board);
        this.next = new ColorAreaSet(board);
        this.numCaNotFilledInitial = new int[board.getNumColors()];
        this.caByColor = new ColorAreaSet[board.getNumColors()];
        for (int color = 0;  color < this.caByColor.length;  ++color) {
            this.caByColor[color] = new ColorAreaSet(board);
        }
        for (final ColorArea ca : board.getColorAreasArray()) {
            final int color = ca.getColor();
            ++this.numCaNotFilledInitial[color];
            this.caByColor[color].add(ca);
        }
        this.numCaNotFilled = new int[board.getNumColors()];
        this.iter = new ColorAreaSet(board).fastIteratorColorAreaId();
        this.allColors = (1 << board.getNumColors()) - 1;
    }

    /* (non-Javadoc)
     * @see colorfill.solver.AStarStrategy#setEstimatedCost(colorfill.solver.AStarNode)
     */
    @Override
    public void setEstimatedCost(final AStarNode node) {

        // quote from floodit.cpp: int State::computeValuation()
        // (in branch "performance")
        //
        // We compute an admissible heuristic recursively: If there are no nodes
        // left, return 0. Furthermore, if a color can be eliminated in one move
        // from the current position, that move is an optimal move and we can
        // simply use it. Otherwise, all moves fill a subset of the neighbors of
        // the filled nodes. Thus, filling that layer gets us at least one step
        // closer to the end.

        node.copyFloodedTo(this.visited);
        int nonCompletedColors = this.allColors;
        for (int color = 0;  color < this.numCaNotFilled.length;  ++color) {
            if (0 == (this.numCaNotFilled[color] = this.numCaNotFilledInitial[color] - this.visited.countIntersection(this.caByColor[color]))) {
                this.numCaNotFilled[color] = -1; // don't detect this completed color again
                nonCompletedColors ^= 1 << color;
            }
        }

        // visit the first layer of neighbors, which is never empty, i.e. the puzzle is not solved yet
        node.copyNeighborsTo(this.current);
        this.visited.addAll(this.current);
        for (int color = 0;  color < this.numCaNotFilled.length;  ++color) {
            this.numCaNotFilled[color] -= this.current.countIntersection(this.caByColor[color]);
        }
        int distance = 0;

        while(!this.current.isEmpty()) {
            this.next.clear();
            int completedColors = 0;
            for (int color = 0;  color < this.numCaNotFilled.length;  ++color) {
                if (this.numCaNotFilled[color] == 0) {
                    this.numCaNotFilled[color] = -1; // don't detect this completed color again
                    completedColors |= 1 << color;
                    nonCompletedColors ^= 1 << color;
                }
            }
            int caId;
            this.iter.init(this.current);
            if (0 != completedColors) {
                // We can eliminate colors. Do just that.
                // We also combine all these elimination moves.
                distance += Integer.bitCount(completedColors);
                if (0 != nonCompletedColors) { // don't need to expand the final layer
                    this.tmp.clear();
                    while ((caId = this.iter.nextOrNegative()) >= 0) {
                        if ((completedColors & (1 << this.board.getColor4Id(caId))) != 0) {
                            // completed color
                            this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                        } else {
                            // non-completed color
                            // move node to next layer
                            this.tmp.add(caId);
                        }
                    }
                    this.next.removeAll(this.visited);
                    for (int color = 0;  color < this.numCaNotFilled.length;  ++color) {
                        this.numCaNotFilled[color] -= this.next.countIntersection(this.caByColor[color]);
                    }
                    this.next.addAll(this.tmp);
                }
            } else {
                // Nothing found, do the color-blind pseudo-move
                // Expand current layer of nodes.
                ++distance;
                while ((caId = this.iter.nextOrNegative()) >= 0) {
                    this.next.addAll(this.board.getNeighborColorAreaSet4Id(caId));
                }
                this.next.removeAll(this.visited);
                for (int color = 0;  color < this.numCaNotFilled.length;  ++color) {
                    this.numCaNotFilled[color] -= this.next.countIntersection(this.caByColor[color]);
                }
            }
            this.visited.addAll(this.next);

            // Move the next layer into the current.
            final ColorAreaSet tmp = this.current;
            this.current = this.next;
            this.next = tmp;
        }
        node.setEstimatedCost(node.getSolutionSize() + distance);
    }
}
