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

    final Board board;
    final Map<Integer, Set<ColorArea>> theMap;

    /**
     * the constructor
     */
    public ColorAreaGroup(final Board board) {
        this.board = board;
        this.theMap = new HashMap<>();
        for (final Integer color : this.board.getColors()) {
            this.theMap.put(color, new HashSet<ColorArea>());
        }
    }

    /**
     * basically a shallow clone() method: the color areas (not cloned!) are put into a new container object.
     * @return the copy object
     */
    public ColorAreaGroup copy() {
        final ColorAreaGroup result = new ColorAreaGroup(this.board);
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
