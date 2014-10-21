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
    private final List<List<Integer>> solutions = new ArrayList<>();
    private int solutionSize = 0;

    /**
     * construct a new solver for this Board.
     * @param board the problem to be solved
     */
    public SolverGreedy(final Board board) {
        this.board = board;
    }

    /**
     * return the (best) found solution as a String.
     * @return the solution
     */
    public String getSolutionString() {
        final StringBuilder result = new StringBuilder();
        if (this.solutions.size() > 0) {
            for (final Integer color : this.solutions.get(0)) {
                result.append(color.intValue() + 1);
            }
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
        this.solutions.clear();
        this.solutionSize = Integer.MAX_VALUE;

        final ColorArea startCa = this.board.getColorArea(startPos);
        final Set<ColorArea> flooded = new HashSet<>();
        final ColorAreaGroup notFlooded = new ColorAreaGroup();
        notFlooded.addAll(this.board.getColorAreas(), flooded);
        final ColorAreaGroup neighbors = new ColorAreaGroup();
        neighbors.addAll(Collections.singleton(startCa), flooded);
        final List<Integer> solution = new ArrayList<>();

        this.solveRecursion(0, startCa.getColor(), solution, flooded, notFlooded, neighbors);

        return this.solutionSize;
    }

    private void solveRecursion(final int depth,
            final Integer thisColor,
            List<Integer> solution,
            Set<ColorArea> flooded,
            ColorAreaGroup notFlooded,
            ColorAreaGroup neighbors
            ) {
        // do this step
        final Collection<ColorArea> thisFlooded = neighbors.removeColor(thisColor);
        flooded.addAll(thisFlooded);
        notFlooded.removeAll(thisFlooded);
        solution.add(thisColor);
        assert solution.size() - 1 == depth : "solution size(" + solution.size() + ") doesn't match current depth(" + depth + ")";

        // finished the search?
        if (notFlooded.isEmpty()) {
            this.addSolution(solution);

        // do next step
        } else if (this.solutionSize > depth) {
            // add new neighbors
            for (final ColorArea ca : thisFlooded) {
                neighbors.addAll(ca.getNeighbors(), flooded);
            }
            // pick the "best" neighbor colors to go on
            List<Integer> nextColors = neighbors.getColorsCompleted(notFlooded);
            if (nextColors.isEmpty()) {
                nextColors = neighbors.getColorsMaxMembers();
            }
            assert nextColors.size() > 0 : "nextColors must not be empty";

            // go to next recursion level
            if (1 == nextColors.size()) {
                solveRecursion(depth + 1, nextColors.get(0),
                        solution, flooded, notFlooded, neighbors); // no need to clone the data structures
            } else {
                for (final Integer nextColor : nextColors) {
                    solveRecursion(depth + 1, nextColor,
                            new ArrayList<Integer>(solution),
                            new HashSet<ColorArea>(flooded),
                            notFlooded.copy(),
                            neighbors.copy());
                }
            }
        }
    }

    private void addSolution(final List<Integer> solution) {
        if (this.solutionSize > solution.size() - 1) {
            this.solutionSize = solution.size() - 1;
            this.solutions.clear();
        }
        if (this.solutionSize == solution.size() - 1) {
            this.solutions.add(new ArrayList<>(solution.subList(1, solution.size())));
        }
    }


    /**
     * this class contains a collection for ColorAreas, grouped by their colors.
     */
    private class ColorAreaGroup {

        final Map<Integer, Set<ColorArea>> theMap;

        /**
         * the constructor
         */
        public ColorAreaGroup() {
            this.theMap = new HashMap<>();
            for (final Integer color : SolverGreedy.this.board.getColors()) {
                this.theMap.put(color, new HashSet<ColorArea>());
            }
        }

        /**
         * basically a shallow clone() method: the color areas (not cloned!) are put into a new container object.
         * @return the copy object
         */
        public ColorAreaGroup copy() {
            final ColorAreaGroup result = new ColorAreaGroup();
            for (final Map.Entry<Integer, Set<ColorArea>> entry : this.theMap.entrySet()) {
                result.theMap.get(entry.getKey()).addAll(entry.getValue());
            }
            return result;
        }

        /**
         * add all color areas that are not members of the "exclude" set.
         * @param addColorAreas the color areas to be added
         * @param excludeColorAreas color areas that are also members of this set will not be added
         */
        public void addAll(final Collection<ColorArea> addColorAreas, final Set<ColorArea> excludeColorAreas) {
            for (final ColorArea ca : addColorAreas) {
                if (false == excludeColorAreas.contains(ca)) {
                    this.theMap.get(ca.getColor()).add(ca);
                }
            }
        }

        /**
         * remove from this object all the specified color areas.
         * @param removeColorAreas
         */
        public void removeAll(final Collection<ColorArea> removeColorAreas) {
            for (final ColorArea ca : removeColorAreas) {
                this.theMap.get(ca.getColor()).remove(ca);
            }
        }

        /**
         * returns true if this object contains no color areas.
         * @return true if this object contains no color areas
         */
        public boolean isEmpty() {
            for (final Set<ColorArea> setCa : this.theMap.values()) {
                if (false == setCa.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * get the colors from that are contained completely in other.
         * @param other
         * @return list of completed colors, may be empty
         */
        public List<Integer> getColorsCompleted(final ColorAreaGroup other) {
            final List<Integer> result = new ArrayList<>();
            for (final Map.Entry<Integer, Set<ColorArea>> entry : this.theMap.entrySet()) {
                final Integer color = entry.getKey();
                final Set<ColorArea> thisSet = entry.getValue();
                final Set<ColorArea> otherSet = other.theMap.get(color);
                if ((0 < thisSet.size()) && (thisSet.size() == otherSet.size()) && (thisSet.containsAll(otherSet))) {
                    result.add(color);
                }
            }
            return result;
        }

        /**
         * get the colors that have the maximum number of member cells.
         * @return list of colors, not expected to be empty
         */
        public List<Integer> getColorsMaxMembers() {
            final List<Integer> result = new ArrayList<>();
            int maxCount = 1; // return empty collection if all colors are empty. not expected!
            for (final Map.Entry<Integer, Set<ColorArea>> entry : this.theMap.entrySet()) {
                int count = 0;
                for (final ColorArea ca : entry.getValue()) {
                    count += ca.getMembers().size();
                }
                if (maxCount < count) {
                    maxCount = count;
                    result.clear();
                }
                if (maxCount == count) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }

        /**
         * remove this object and return the areas of this color.
         * @param color
         * @return the areas
         */
        public Collection<ColorArea> removeColor(final Integer color) {
            return this.theMap.put(color, new HashSet<ColorArea>());
        }
    }
}
