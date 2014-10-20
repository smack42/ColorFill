/*  ColorFill game and solver
    Copyright (C) 2014 Michael Henke

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

package colorfill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import colorfill.Board.ColorArea;

public class SolverGreedy {

    private final Board board;
    private final List<Integer> solution = new ArrayList<>();

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    public SolverGreedy(final Board board) {
        this.board = board;
    }

    /**
     * return the found solution as a String.
     * @return the solution
     */
    public String getSolutionString() {
        final StringBuilder result = new StringBuilder();
        for (int i = 0;  i < this.solution.size();  ++i) {
            final Integer color = this.solution.get(i);
            result.append(color.intValue() + 1);
        }
        return result.toString();
    }

    /**
     * run the solver algorithm that is implemented in this class
     * and store the found solution internally.
     * @param startPos position of the board cell where the color flood starts (0 == top left)
     * @return number of steps in the solution
     */
    public int solve(final int startPos) {
        this.solution.clear();
        final ColorArea startCa = this.board.getColorArea(startPos);
        final Set<ColorArea> flooded = new HashSet<>();
        final ColorAreaGroup notFlooded = new ColorAreaGroup();
        final ColorAreaGroup neighbors = new ColorAreaGroup();
        flooded.add(startCa);
        notFlooded.add(this.board.getColorAreas(), flooded);
        neighbors.add(startCa.getNeighbors(), flooded);
        while (notFlooded.isNotEmpty()) {
            Collection<ColorArea> nextFlooded = neighbors.removeColorComplete(notFlooded);
            if (nextFlooded.isEmpty()) {
                nextFlooded = neighbors.removeMaxMembers();
            }
            flooded.addAll(nextFlooded);
            notFlooded.remove(nextFlooded);
            for (final ColorArea nextCa : nextFlooded) {
                neighbors.add(nextCa.getNeighbors(), flooded);
            }
            this.solution.add(nextFlooded.iterator().next().getColor());
        }
        return this.solution.size();
    }



    private class ColorAreaGroup {

        final Map<Integer, Set<ColorArea>> theMap;

        public ColorAreaGroup() {
            this.theMap = new HashMap<>();
            for (final Integer color : SolverGreedy.this.board.getColors()) {
                this.theMap.put(color, new HashSet<ColorArea>());
            }
        }

        public void add(final Collection<ColorArea> addColorAreas, final Set<ColorArea> excludeColorAreas) {
            for (final ColorArea ca : addColorAreas) {
                if (false == excludeColorAreas.contains(ca)) {
                    this.theMap.get(ca.getColor()).add(ca);
                }
            }
        }

        public void remove(final Collection<ColorArea> removeColorAreas) {
            for (final ColorArea ca : removeColorAreas) {
                this.theMap.get(ca.getColor()).remove(ca);
            }
        }

        public boolean isNotEmpty() {
            for (final Set<ColorArea> setCa : this.theMap.values()) {
                if (false == setCa.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        public Collection<ColorArea> removeColorComplete(final ColorAreaGroup other) {
            for (final Map.Entry<Integer, Set<ColorArea>> entry : this.theMap.entrySet()) {
                final Integer color = entry.getKey();
                final Set<ColorArea> thisSet = entry.getValue();
                final Set<ColorArea> otherSet = other.theMap.get(color);
                if ((0 < thisSet.size()) && (thisSet.size() == otherSet.size()) && (thisSet.containsAll(otherSet))) {
                    return this.theMap.put(color, new HashSet<ColorArea>()); // get and remove
                }
            }
            return Collections.emptySet();
        }

        public Collection<ColorArea> removeMaxMembers() {
            int maxCount = Integer.MIN_VALUE;
            Integer maxColor = null; // must be set later!
            for (final Map.Entry<Integer, Set<ColorArea>> entry : this.theMap.entrySet()) {
                int count = 0;
                for (final ColorArea ca : entry.getValue()) {
                    count += ca.getMembers().size();
                }
                if (maxCount < count) {
                    maxCount = count;
                    maxColor = entry.getKey();
                }
            }
            return this.theMap.put(maxColor, new HashSet<ColorArea>()); // get and remove
        }
    }
}
