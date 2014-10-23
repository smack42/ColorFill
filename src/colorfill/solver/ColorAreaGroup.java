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

package colorfill.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import colorfill.model.Board;
import colorfill.model.ColorArea;

/**
 * this class contains a collection for ColorAreas, grouped by their colors.
 * provides some helper functions for the search strategies.
 */
public class ColorAreaGroup {

    private final Board board;
    private final HashMap<Integer, HashSet<ColorArea>> theMap;

    /**
     * the standard constructor
     */
    public ColorAreaGroup(final Board board) {
        this.board = board;
        this.theMap = new HashMap<>();
        for (final Integer color : this.board.getColors()) {
            this.theMap.put(color, new HashSet<ColorArea>());
        }
    }

    /**
     * the copy constructor
     */
    @SuppressWarnings("unchecked")
    public ColorAreaGroup(final ColorAreaGroup other) {
        this.board = other.board;
        this.theMap = (HashMap<Integer, HashSet<ColorArea>>) other.theMap.clone();
        for (final Map.Entry<Integer, HashSet<ColorArea>> entry : this.theMap.entrySet()) {
            entry.setValue((HashSet<ColorArea>) entry.getValue().clone());
        }
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
     * count the number of colors that have at least one color area.
     * @return number of occupied colors
     */
    public int countColorsNotEmpty() {
        int result = 0;
        for (final Set<ColorArea> setCa : this.theMap.values()) {
            if (false == setCa.isEmpty()) {
                ++result;
            }
        }
        return result;
    }

    /**
     * get the colors that have at least one color area.
     * @return list of occupied colors, not expected to be empty
     */
    public List<Integer> getColorsNotEmpty() {
        final List<Integer> result = new ArrayList<>();
        for (final Map.Entry<Integer, HashSet<ColorArea>> entry : this.theMap.entrySet()) {
            if (false == entry.getValue().isEmpty()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * get the colors that are situated at the specified depth.
     * @return list of colors at depth, may be empty
     */
    public List<Integer> getColorsDepth(final int depth) {
        final List<Integer> result = new ArrayList<>();
        for (final Map.Entry<Integer, HashSet<ColorArea>> entry : this.theMap.entrySet()) {
            for (final ColorArea ca : entry.getValue()) {
                if (ca.getDepth() == depth) {
                    result.add(entry.getKey());
                    break; // for (ca)
                }
            }
        }
        return result;
    }

    /**
     * get the colors that are contained completely in other.
     * @param other
     * @return list of completed colors, may be empty
     */
    public List<Integer> getColorsCompleted(final ColorAreaGroup other) {
        final List<Integer> result = new ArrayList<>();
        for (final Map.Entry<Integer, HashSet<ColorArea>> entry : this.theMap.entrySet()) {
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
        for (final Map.Entry<Integer, HashSet<ColorArea>> entry : this.theMap.entrySet()) {
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
